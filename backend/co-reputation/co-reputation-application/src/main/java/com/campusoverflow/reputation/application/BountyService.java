package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 用例：悬赏的发起、结算、到期退款。积分冻结与悬赏状态在同一事务中变更。 */
@Service
public class BountyService {

    private static final Logger log = LoggerFactory.getLogger(BountyService.class);

    private final BountyRepository bounties;
    private final ReputationAccountRepository accounts;
    private final ReputationLedgerService ledgerService;
    private final QaQueryApi qa;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final AuditTrail audit;
    private final Clock clock;
    /** 到期退款逐条使用独立事务；测试直接 new 时该字段为 null，退化为单事务顺序执行。 */
    private final TransactionTemplate expiryTx;

    public BountyService(BountyRepository bounties, ReputationAccountRepository accounts,
                         ReputationLedgerService ledgerService, QaQueryApi qa, IdentityApi identity,
                         IntegrationEventPublisher events, AuditTrail audit, Clock clock) {
        this(bounties, accounts, ledgerService, qa, identity, events, audit, clock, null);
    }

    @Autowired
    public BountyService(BountyRepository bounties, ReputationAccountRepository accounts,
                         ReputationLedgerService ledgerService, QaQueryApi qa, IdentityApi identity,
                         IntegrationEventPublisher events, AuditTrail audit, Clock clock,
                         PlatformTransactionManager txManager) {
        this.bounties = bounties;
        this.accounts = accounts;
        this.ledgerService = ledgerService;
        this.qa = qa;
        this.identity = identity;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
        this.expiryTx = txManager == null ? null : new TransactionTemplate(txManager);
        if (this.expiryTx != null) {
            this.expiryTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    @Transactional
    public long open(Actor actor, long questionId, int points, int days) {
        // 锁问题行：与采纳/删除问题串行化，避免“校验通过后又发生采纳/删除”仍开出 OPEN 悬赏。
        QuestionRef question = qa.findQuestionForUpdate(questionId)
                .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));

        // PO 决策（2026-11-25，审计 REP-02）：悬赏只能挂在课程问题上，与 docs/final/user-guide.md
        // 「发起悬赏需要教师/助教且必须是该课程的问题」保持一致。
        // 公开问题（courseId == null）没有课程归属，而 canManageCourse(userId, null) 会对任意
        // 全局 TEACHER 返回 true（等于放开给全校教师），与文档矛盾且无法审计归属。
        if (question.courseId() == null) {
            throw new BusinessRuleException("REP-1007", "公开问题不能发起悬赏，请选择所属课程的问题");
        }

        // 用“可见 accepted”而不是原始 accepted_answer_id：历史脏数据指向已删回答时仍应允许发起悬赏。
        boolean questionOpen = !question.deleted() && !qa.hasVisibleAcceptedAnswer(questionId);
        boolean managesCourse = identity.canManageCourse(actor.userId(), question.courseId());

        // 前置检查只用于友好错误；并发下的单 OPEN 由 rep_bounty.uk_bounty_open_question 兜底。
        if (bounties.findOpenByQuestion(questionId).isPresent()) {
            throw new ConflictException("REP-2002", "该问题已有进行中的悬赏");
        }

        Instant now = clock.instant();
        Bounty bounty = Bounty.open(questionId, questionOpen, actor, managesCourse, points, days, now);

        ReputationAccount sponsor = accounts.findByUserId(actor.userId())
                .orElseGet(() -> ReputationAccount.open(actor.userId(), 0));
        sponsor.freezePoints(points);
        accounts.save(sponsor);

        Bounty saved = bounties.save(bounty);
        events.publish(new BountyOpenedEvent(EventIds.next(), now, saved.id(), questionId, actor.userId(),
                points, saved.expiresAt()));
        audit.record("BOUNTY_OPEN", actor.userId(), "BOUNTY", String.valueOf(saved.id()),
                "points=" + points + ",days=" + days);
        return saved.id();
    }

    /** 由 AnswerAccepted 事件触发（在处理器事务内）。幂等：悬赏已结束则直接返回。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settleOnAccept(long questionId, long answerId, long winnerId, long questionAuthorId,
                               long acceptedById, Instant acceptedAt, String sourceEventId) {
        Optional<Bounty> found = bounties.findOpenByQuestion(questionId);
        if (found.isEmpty()) {
            return;
        }
        Bounty bounty = found.get();
        ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                .orElseThrow(() -> new NotFoundException("REP-4001", "悬赏发起人账户不存在"));
        Instant now = clock.instant();

        // 用“采纳事件发生时间”判断是否到期，而不是事件被消费的时间：
        // 采纳发生在到期前、只是 outbox 积压到到期后才处理时，仍应发奖。
        if (bounty.isDue(acceptedAt)) {
            closeAndRefund(bounty, sponsor, now);
            return;
        }

        // 自己的回答获得采纳：悬赏作废，冻结积分退回，不进入声誉流水。
        if (winnerId == bounty.sponsorId()) {
            closeAndRefund(bounty, sponsor, now);
            return;
        }
        // 防自问自答领奖：提问者采纳自己的回答，或采纳操作者自己就是获奖回答作者，均退款。
        if (winnerId == questionAuthorId || acceptedById == winnerId) {
            closeAndRefund(bounty, sponsor, now);
            return;
        }

        bounty.award(answerId, winnerId, now);
        sponsor.spendFrozen(bounty.points());
        accounts.save(sponsor);
        Bounty saved = bounties.save(bounty);

        ledgerService.record(winnerId, sourceEventId, ReputationReason.BOUNTY_AWARDED, bounty.points(),
                "BOUNTY", saved.id());
        events.publish(new BountyAwardedEvent(EventIds.next(), now, saved.id(), questionId, bounty.sponsorId(),
                winnerId, bounty.points()));
        audit.record("BOUNTY_AWARD", winnerId, "BOUNTY", String.valueOf(saved.id()),
                "question=" + questionId + ",points=" + bounty.points());
    }

    /**
     * 到期未采纳的悬赏：关闭并退回冻结积分。由定时任务调用。
     *
     * <p>每条悬赏在独立事务中处理：单条脏数据或乐观锁冲突不会回滚整批；
     * 失败会记录日志并在下一轮重试，其余悬赏继续退款。</p>
     */
    public int expireDue() {
        Instant now = clock.instant();
        List<Bounty> due = bounties.findDue(now, 100);
        int closed = 0;
        for (Bounty snapshot : due) {
            try {
                boolean done = expiryTx == null
                        ? expireOne(snapshot.id(), now)
                        : Boolean.TRUE.equals(expiryTx.execute(status -> expireOne(snapshot.id(), now)));
                if (done) {
                    closed++;
                }
            } catch (BusinessRuleException | NotFoundException e) {
                log.warn("悬赏 {} 到期处理跳过：{}", snapshot.id(), e.getMessage());
            } catch (RuntimeException e) {
                log.error("悬赏 {} 到期处理失败，下一轮重试", snapshot.id(), e);
            }
        }
        return closed;
    }

    /**
     * 必须在独立事务内按 id 重读并加锁，不能使用 findDue 返回的事务外快照：
     * 否则并发采纳已经关闭/发放的悬赏会被旧快照二次退款并覆盖状态。
     */
    private boolean expireOne(long bountyId, Instant now) {
        Optional<Bounty> found = bounties.findByIdForUpdate(bountyId);
        if (found.isEmpty()) {
            return false;
        }
        Bounty bounty = found.get();
        if (bounty.status() != BountyStatus.OPEN || !bounty.isDue(now)) {
            return false;
        }
        ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                .orElseThrow(() -> new NotFoundException("REP-4001", "悬赏发起人账户不存在"));
        closeAndRefund(bounty, sponsor, now);
        return true;
    }

    /** 问题被删除时关闭其进行中的悬赏并退款；幂等，没有 OPEN 悬赏时无操作。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeForDeletedQuestion(long questionId) {
        bounties.findOpenByQuestion(questionId).ifPresent(bounty -> {
            ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                    .orElseThrow(() -> new NotFoundException("REP-4001", "悬赏发起人账户不存在"));
            closeAndRefund(bounty, sponsor, clock.instant());
        });
    }

    @Transactional(readOnly = true)
    public Optional<BountyView> latest(long questionId, Actor viewerOrNull) {
        // 先存在性守卫：用 isEmpty 而不是丢掉 orElseThrow 的返回值（Sonar java:S2201）。
        QuestionRef question = qa.findQuestion(questionId)
                .filter(ref -> !ref.deleted())
                .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在或已被删除"));
        requireCanView(question, viewerOrNull);
        return bounties.findLatestByQuestion(questionId).map(b -> new BountyView(
                b.id(), b.questionId(), b.sponsorId(),
                identity.findUser(b.sponsorId()).map(UserSummary::displayName).orElse("未知用户"),
                b.points(), b.status().name(), b.createdAt(), b.expiresAt(), b.winnerId(), b.answerId()));
    }

    /** 与 QuestionQueryService.requireCanView 同口径：公开问题不限；课程问题仅作者/成员/管理员。 */
    private void requireCanView(QuestionRef question, Actor viewerOrNull) {
        if (question.courseId() == null) {
            return;
        }
        if (viewerOrNull == null) {
            throw new ForbiddenException("QA-3006", "无权查看此内容");
        }
        if (viewerOrNull.isAdmin() || viewerOrNull.is(question.authorId())
                || identity.isCourseMember(viewerOrNull.userId(), question.courseId())) {
            return;
        }
        throw new ForbiddenException("QA-3006", "无权查看此内容");
    }

    private void closeAndRefund(Bounty bounty, ReputationAccount sponsor, Instant now) {
        bounty.close(now);
        sponsor.refundFrozen(bounty.points());
        accounts.save(sponsor);
        Bounty saved = bounties.save(bounty);

        events.publish(new BountyClosedEvent(EventIds.next(), now, saved.id(), bounty.questionId(),
                bounty.sponsorId(), bounty.points()));
        audit.record("BOUNTY_CLOSE", null, "BOUNTY", String.valueOf(saved.id()),
                "question=" + bounty.questionId() + ",refunded=" + bounty.points());
    }
}
