package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationRulebook;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 */

@Component
public class ReputationEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(ReputationEventHandlers.class);

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final ReputationLedgerService ledgerService;
    private final BadgeService badges;
    private final BountyService bounties;
    private final ProcessedEventStore processed;
    private final Clock clock;


    public ReputationEventHandlers(ReputationAccountRepository accounts, LedgerRepository ledger,
                                   ReputationLedgerService ledgerService, BadgeService badges,
                                   BountyService bounties, ProcessedEventStore processed, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.ledgerService = ledgerService;
        this.badges = badges;
        this.bounties = bounties;
        this.processed = processed;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        // 双重幂等：同一事件重复投递按 eventId 拦截；同一用户跨事件重复注册按持久化 marker
        // 拦截。不能靠账户余额判断——账户可能先被投票/悬赏事件懒创建并已有余额，
        // 后到的注册事件仍必须补发初始积分。
        if (!processed.markProcessed(e.eventId(), "reputation-user-registered")
                || !processed.markProcessed("initial-points:" + e.userId(), "reputation-initial-points")) {
            return;
        }
        ReputationAccount account = accounts.findByUserId(e.userId())
                .orElseGet(() -> ReputationAccount.open(e.userId(), 0));
        int initialPoints = ReputationRulebook.initialPoints(e.role());
        if (initialPoints > 0) {
            account.grantAvailablePoints(initialPoints);
        }
        accounts.save(account);
    }

    /** 场景 R-2 的异步后半段：采纳 → 悬赏结算 → 回答者 +15 → 徽章。 */
    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        try {
            // 先做 REQUIRES_NEW 的悬赏结算：它会把 +50 写进获奖者账户并提交。
            // 若先在本事务里把同一账户改脏（+15 尚未 flush），内层提交后外层再 flush 会命中
            // 旧 @Version，导致整条事件回滚重投（Q-33 之后的隐藏重试）。
            bounties.settleOnAccept(e.questionId(), e.answerId(), e.answerAuthorId(), e.questionAuthorId(),
                    e.acceptedById(), e.occurredAt(), e.eventId());
        } catch (BusinessRuleException | ConflictException | NotFoundException ex) {
            // 悬赏数据脏/已结算不能回滚随后要做的采纳加分/徽章；记录后交给对账处理。
            log.warn("采纳事件 {} 的悬赏结算跳过：{}", e.eventId(), ex.getMessage());
        }
        int delta = ReputationRulebook.acceptedDelta(e.answerAuthorId(), e.questionAuthorId());
        ledgerService.record(e.answerAuthorId(), e.eventId(), ReputationReason.ANSWER_ACCEPTED, delta,
                "ANSWER", e.answerId());
        long acceptedCount = ledger.countByUserAndReason(e.answerAuthorId(), ReputationReason.ANSWER_ACCEPTED);
        BadgePolicy.forAcceptedCount(acceptedCount)
                .forEach(type -> badges.grant(e.answerAuthorId(), type));
    }

    @EventListener
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void on(VoteCastEvent e) {
        // 被日上限截断为 0 时不会写账本；这里用独立 marker 保证跨日重投也不会补发。
        if (!processed.markProcessed(e.eventId(), "reputation-vote")) {
            return;
        }
        int delta = ReputationRulebook.voteDelta(e.oldValue(), e.newValue());
        if (delta == 0) {
            return;
        }
        ReputationReason reason = "QUESTION".equalsIgnoreCase(e.targetType())
                ? ReputationReason.QUESTION_VOTE : ReputationReason.ANSWER_VOTE;
        // 日累计、实际入账值与改投回滚都在 ledgerService 的账户行锁内完成，
        // 避免「先求和、后加载账户」两步之间被并发投票读到旧值；被截断为 0 也不再写流水。
        ledgerService.recordVoteCapped(e.targetAuthorId(), e.eventId(), reason,
                e.oldValue(), e.newValue(), e.voterId(), e.targetType(), e.targetId());
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.FIRST_ANSWER);
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.TEACHER_ENDORSED);
    }

    /** 问题被删除：关闭仍 OPEN 的悬赏并退款，避免发起人积分一直冻结到自然到期。 */
    @EventListener
    @Transactional
    public void on(ContentDeletedEvent e) {
        if ("QUESTION".equalsIgnoreCase(e.targetType())) {
            try {
                bounties.closeForDeletedQuestion(e.targetId());
            } catch (BusinessRuleException | ConflictException | NotFoundException ex) {
                // 不让脏悬赏数据回滚删除事件本身；退款欠账交给对账/人工处理。
                log.warn("删除问题 {} 时关闭悬赏失败：{}", e.targetId(), ex.getMessage());
            }
        }
    }
}
