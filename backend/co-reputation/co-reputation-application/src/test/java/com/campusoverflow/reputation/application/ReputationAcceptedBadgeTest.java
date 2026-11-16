package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 采纳链路：回答者 +15，回答被采纳的条数决定「初露锋芒 / 解惑达人」。
 * B（魏志国）要用这里的授予时机去对徽章显示，C（青岑）要用同一个事件结算悬赏。
 */
class ReputationAcceptedBadgeTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("第一次被采纳：回答者 +15，同时拿到「初露锋芒」")
    void firstAcceptanceAwardsFirstAcceptedBadge() {
        fx.register(3L, Role.STUDENT);

        fx.accept(101L, 3L, 1L);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(15);
        assertThat(fx.badgeCodes(3L)).containsExactly("FIRST_ACCEPTED");
        assertThat(fx.ledger.countByUserAndReason(3L, ReputationReason.ANSWER_ACCEPTED)).isEqualTo(1);
    }

    @Test
    @DisplayName("累计 10 条被采纳：补上「解惑达人」，一共两枚")
    void tenthAcceptanceAwardsHelperBadge() {
        fx.register(3L, Role.STUDENT);
        for (int i = 1; i <= 10; i++) {
            fx.accept(100L + i, 3L, 1L);
        }

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(150);
        assertThat(fx.badgeCodes(3L)).contains("FIRST_ACCEPTED", "HELPER_10");
    }

    @Test
    @DisplayName("采纳事件重放两次：声誉只加一次，账本只有一条")
    void answerAcceptedEventReplayedTwiceCreditsOnce() {
        fx.register(3L, Role.STUDENT);
        AnswerAcceptedEvent event = fx.accept(101L, 3L, 1L);

        fx.handlers.on(event);
        fx.handlers.on(event);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(15);
        assertThat(fx.ledger.size(3L)).isEqualTo(1);
        assertThat(fx.ledger.countByUserAndReason(3L, ReputationReason.ANSWER_ACCEPTED)).isEqualTo(1);
    }

    @Test
    @DisplayName("自问自答被采纳：声誉不动，也不给采纳类徽章")
    void selfAcceptedAnswerEarnsNothing() {
        fx.register(3L, Role.STUDENT);

        fx.accept(101L, 3L, 3L);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isZero();
        assertThat(fx.badgeCodes(3L)).isEmpty();
    }
}
