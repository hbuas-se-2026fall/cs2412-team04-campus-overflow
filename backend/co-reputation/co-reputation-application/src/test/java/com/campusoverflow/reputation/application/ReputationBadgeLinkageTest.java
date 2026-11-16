package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 声誉阈值与徽章的联动（依赖 D 的 BadgePolicy.forReputation）：
 * 声誉过 100 给「小有名气」，过 1000 再给「德高望重」。由 ReputationLedgerService 记账后统一触发。
 */
class ReputationBadgeLinkageTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("声誉到 100 → 小有名气")
    void reputationReaching100AwardsRisingStar() {
        fx.register(3L, Role.STUDENT);

        fx.ledgerService.record(3L, "evt-1", ReputationReason.ANSWER_ACCEPTED, 100, "ANSWER", 1L);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(100);
        assertThat(fx.badgeCodes(3L)).containsExactly("REPUTATION_100");
    }

    @Test
    @DisplayName("声誉到 1000 → 小有名气 + 德高望重，两枚都在")
    void reputationReaching1000AwardsWellRenowned() {
        fx.register(3L, Role.STUDENT);

        fx.ledgerService.record(3L, "evt-1", ReputationReason.ANSWER_ACCEPTED, 100, "ANSWER", 1L);
        fx.ledgerService.record(3L, "evt-2", ReputationReason.ANSWER_ACCEPTED, 900, "ANSWER", 2L);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(1000);
        assertThat(fx.badgeCodes(3L)).containsExactlyInAnyOrder("REPUTATION_100", "REPUTATION_1000");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("没到 100 不给声誉徽章，避免「刚注册就德高望重」")
    void reputationBelow100AwardsNoBadge() {
        fx.register(3L, Role.STUDENT);

        fx.ledgerService.record(3L, "evt-1", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 1L);

        assertThat(fx.badgeCodes(3L)).isEmpty();
    }

    @Test
    @DisplayName("阈值徽章不会重复发：事件重放时声誉徽章仍只有两枚")
    void replayingThresholdEventDoesNotDuplicateBadges() {
        fx.register(3L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-1", ReputationReason.ANSWER_ACCEPTED, 100, "ANSWER", 1L);

        fx.ledgerService.record(3L, "evt-1", ReputationReason.ANSWER_ACCEPTED, 100, "ANSWER", 1L);

        assertThat(fx.badgeCodes(3L)).containsExactly("REPUTATION_100");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(1);
    }
}
