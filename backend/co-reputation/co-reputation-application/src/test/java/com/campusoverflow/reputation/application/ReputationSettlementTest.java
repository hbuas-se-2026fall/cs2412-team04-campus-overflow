package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Actor;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * S2 结算与反作弊：每日投票上限、改投增量、悬赏结算/退款、对账不变量。
 * 全部使用 {@link ReputationTestFixtures} 的真实服务 + 内存仓储。
 */
class ReputationSettlementTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("问题删除事件：关闭 OPEN 悬赏并退回冻结积分")
    void questionDeletedClosesBountyAndRefunds() {
        Actor teacher = fx.teacher(9);
        fx.seedAccount(9, 0, 1000, 0);
        fx.seedQuestion(10, 1, 100L);
        long bountyId = fx.openBounty(teacher, 10, 50, 7);

        fx.handlers.on(new ContentDeletedEvent("evt-del-1", fx.clock.instant(), "QUESTION", 10, 10));

        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(fx.accounts.findByUserId(9).orElseThrow().availablePoints()).isEqualTo(1000);
        assertThat(fx.accounts.findByUserId(9).orElseThrow().frozenPoints()).isZero();
        assertThat(fx.events.ofType(BountyClosedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("每日上限：今天已 +195，再被赞只加 5")
    void dailyVoteCapStopsAtTwoHundred() {
        fx.seedAccount(2, 195, 0, 0);
        fx.seedVoteGain(2, 195);

        fx.handlers.on(new VoteCastEvent("evt-cap-1", fx.clock.instant(),
                "ANSWER", 60, 2, 1, 10, 0, 1));

        ReputationAccount author = fx.accounts.findByUserId(2).orElseThrow();
        assertThat(author.reputation()).isEqualTo(200);
        assertThat(fx.ledger.countByUserAndReason(2, ReputationReason.ANSWER_VOTE)).isEqualTo(2);
        assertThat(fx.ledger.sumFor(2)).isEqualTo(200);
    }

    @Test
    @DisplayName("被日上限截断为 0 的投票事件，跨日重投也不补分")
    void cappedVoteEventIsNotReplayedNextDay() {
        fx.seedAccount(2, 195, 0, 0);
        fx.seedVoteGain(2, 195);
        VoteCastEvent event = new VoteCastEvent("evt-cap-replay", fx.clock.instant(),
                "ANSWER", 60, 2, 1, 10, 0, 1);

        fx.handlers.on(event);
        fx.clock.advance(Duration.ofDays(1));
        fx.handlers.on(event);

        assertThat(fx.accounts.findByUserId(2).orElseThrow().reputation()).isEqualTo(200);
        assertThat(fx.ledger.countByUserAndReason(2, ReputationReason.ANSWER_VOTE)).isEqualTo(2);
    }

    @Test
    @DisplayName("改投：赞→踩按「新效果 − 旧效果」记 −12")
    void voteChangeAppliesDifference() {
        fx.seedAccount(2, 0, 0, 0);

        fx.handlers.on(new VoteCastEvent("evt-v1", fx.clock.instant(), "ANSWER", 60, 2, 1, 10, 0, 1));
        fx.handlers.on(new VoteCastEvent("evt-v2", fx.clock.instant(), "ANSWER", 60, 2, 1, 10, 1, -1));

        assertThat(fx.accounts.findByUserId(2).orElseThrow().reputation()).isEqualTo(-2);
        assertThat(fx.ledger.sumFor(2)).isEqualTo(-2);
        assertThat(fx.ledger.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("悬赏结算：采纳后获奖者 +50，发起人冻结清零，重复事件不重复结算")
    void acceptedAnswerSettlesBountyOnce() {
        Actor teacher = fx.teacher(9);
        fx.seedAccount(9, 0, 1000, 0);
        fx.seedAccount(2, 0, 0, 0);
        fx.seedQuestion(10, 1, 100L);
        long bountyId = fx.openBounty(teacher, 10, 50, 7);
        fx.acceptQuestion(10, 50);

        AnswerAcceptedEvent event = new AnswerAcceptedEvent("evt-acc-bounty", fx.clock.instant(),
                50, 10, 2, 1, 1, "红黑树插入后为什么必须旋转？");
        fx.handlers.on(event);
        fx.handlers.on(event);

        ReputationAccount sponsor = fx.accounts.findByUserId(9).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(950);
        assertThat(sponsor.frozenPoints()).isZero();

        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(fx.ledger.countByUserAndReason(2, ReputationReason.BOUNTY_AWARDED)).isEqualTo(1);
        // 采纳 +15 与悬赏 +50 各一次
        assertThat(fx.accounts.findByUserId(2).orElseThrow().reputation()).isEqualTo(65);
        assertThat(fx.events.ofType(BountyAwardedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("自问自答被采纳：悬赏关闭退款，不产生悬赏流水")
    void selfAcceptedAnswerClosesBountyAndRefunds() {
        Actor teacher = fx.teacher(9);
        fx.seedAccount(9, 0, 1000, 0);
        fx.seedQuestion(10, 9, 100L);
        long bountyId = fx.openBounty(teacher, 10, 50, 7);
        fx.acceptQuestion(10, 50);

        fx.handlers.on(new AnswerAcceptedEvent("evt-acc-self", fx.clock.instant(),
                50, 10, 9, 9, 9, "红黑树插入后为什么必须旋转？"));

        ReputationAccount sponsor = fx.accounts.findByUserId(9).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(1000);
        assertThat(sponsor.frozenPoints()).isZero();
        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(fx.ledger.countByUserAndReason(9, ReputationReason.BOUNTY_AWARDED)).isZero();
        assertThat(fx.ledger.size()).isZero();
        assertThat(fx.events.ofType(BountyClosedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("到期任务先跑但问题已有可见采纳：不退款，等采纳事件结算")
    void expireDueSkipsWhenQuestionAlreadyHasVisibleAcceptedAnswer() {
        Actor teacher = fx.teacher(9);
        fx.seedAccount(9, 0, 1000, 0);
        fx.seedQuestion(10, 1, 100L);
        long bountyId = fx.openBounty(teacher, 10, 50, 1);
        Instant acceptedAt = fx.clock.instant();
        fx.acceptQuestion(10, 50);
        fx.clock.advance(Duration.ofDays(2));

        assertThat(fx.bountyService.expireDue()).isZero();
        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.OPEN);

        fx.handlers.on(new AnswerAcceptedEvent("evt-acc-before-expiry", acceptedAt, 50, 10, 2, 1, 1,
                "红黑树插入后为什么必须旋转？"));

        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(fx.accounts.findByUserId(9).orElseThrow().frozenPoints()).isZero();
    }

    @Test
    @DisplayName("悬赏到期：expireDue 退回冻结积分")
    void expiredBountyRefundsFrozenPoints() {
        Actor teacher = fx.teacher(9);
        fx.seedAccount(9, 0, 1000, 0);
        fx.seedQuestion(10, 9, 100L);
        long bountyId = fx.openBounty(teacher, 10, 50, 1);

        fx.clock.advance(Duration.ofDays(2));

        assertThat(fx.bountyService.expireDue()).isEqualTo(1);
        ReputationAccount sponsor = fx.accounts.findByUserId(9).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(1000);
        assertThat(sponsor.frozenPoints()).isZero();
        assertThat(fx.bounties.get(bountyId).status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(fx.events.ofType(BountyClosedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("对账：跑完采纳与投票后，账户声誉仍等于流水之和")
    void reconciliationStaysCleanAfterEvents() {
        fx.seedAccount(2, 0, 0, 0);

        fx.handlers.on(new AnswerAcceptedEvent("evt-acc-1", fx.clock.instant(),
                50, 10, 2, 1, 1, "红黑树插入后为什么必须旋转？"));
        fx.handlers.on(new VoteCastEvent("evt-vote-1", fx.clock.instant(),
                "ANSWER", 50, 2, 1, 10, 0, 1));

        ReconciliationService reconciliation = new ReconciliationService(fx.accounts, fx.ledger, fx.bounties);
        assertThat(reconciliation.reconcile()).isEmpty();
        assertThat(fx.accounts.findByUserId(2).orElseThrow().reputation()).isEqualTo(25);
    }
}
