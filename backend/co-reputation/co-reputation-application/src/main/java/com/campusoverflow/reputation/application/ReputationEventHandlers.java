package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationRulebook;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 */

@Component
public class ReputationEventHandlers {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final ReputationLedgerService ledgerService;
    private final BadgeService badges;
    private final BountyService bounties;
    private final Clock clock;


    public ReputationEventHandlers(ReputationAccountRepository accounts, LedgerRepository ledger,
                                   ReputationLedgerService ledgerService, BadgeService badges,
                                   BountyService bounties, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.ledgerService = ledgerService;
        this.badges = badges;
        this.bounties = bounties;
        this.clock = clock;
    }

    /**
     * 注册即开户：声誉 0 起算，悬赏积分按角色发放（教师/助教 1000，学生/管理员 0）。
     * 幂等：账户已存在说明这个事件已经处理过（或用户是管理员补建的），直接跳过，不重建、不覆盖积分。
     */
    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        if (accounts.findByUserId(e.userId()).isPresent()) {
            return;
        }
        accounts.save(ReputationAccount.open(e.userId(), ReputationRulebook.initialPoints(e.role())));
    }

    /**
     * 场景 R-2 的异步后半段：采纳 → 回答者 +15 → 徽章 → 悬赏结算。
     * 采纳数徽章的授予时机就在这里：先记账（重复事件会被账本唯一键挡掉），
     * 再用「累计被采纳条数」问 {@link BadgePolicy#forAcceptedCount}，1 条给「初露锋芒」、10 条给「解惑达人」。
     */
    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        int delta = ReputationRulebook.acceptedDelta(e.answerAuthorId(), e.questionAuthorId());
        if (delta != 0) {
            ledgerService.record(e.answerAuthorId(), e.eventId(), ReputationReason.ANSWER_ACCEPTED, delta,
                    "ANSWER", e.answerId());
        }
        long acceptedCount = ledger.countByUserAndReason(e.answerAuthorId(), ReputationReason.ANSWER_ACCEPTED);
        BadgePolicy.forAcceptedCount(acceptedCount)
                .forEach(type -> badges.grant(e.answerAuthorId(), type));
        bounties.settleOnAccept(e.questionId(), e.answerId(), e.answerAuthorId(), e.eventId());
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        // TODO(S3)：实现 ReputationEventHandlers.on——参考领域单元测试与 arc42 文档中的业务规则
        System.out.println("[walking-skeleton] Reputation 暂未实现，忽略事件 " + e.eventId());
    }

    /**
     * 提交回答 →「初来乍到」。只要有一条回答就有，之后重复提交不再给第二枚（由 BadgeService.grant 去重）。
     * 回答本身不加声誉（只有被采纳、被投票才加分），所以这里不记账。
     */
    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.FIRST_ANSWER);
    }

    /**
     * 教师认证回答 →「名师认可」。与「初露锋芒」区分：那个是被提问者采纳，这个是被教师背书。
     * 认证只发徽章、不改声誉（认证不是投票，不进每日上限）。重复投递由徽章存在性检查吞掉。
     */
    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.TEACHER_ENDORSED);
    }
}
