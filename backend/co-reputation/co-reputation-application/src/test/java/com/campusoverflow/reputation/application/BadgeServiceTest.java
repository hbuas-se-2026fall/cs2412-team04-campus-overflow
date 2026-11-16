package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.domain.BadgeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 徽章授予的幂等性：这是 Sprint 2 验收的第一条硬指标。 */
class BadgeServiceTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("同一徽章授予两次：只有一条记录，也只发一条 BadgeAwardedEvent")
    void grantCalledTwiceAwardsOneBadgeAndOneEvent() {
        fx.badgeService.grant(7L, BadgeType.FIRST_ANSWER);
        fx.badgeService.grant(7L, BadgeType.FIRST_ANSWER);

        assertThat(fx.badgeCodes(7L)).containsExactly("FIRST_ANSWER");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("四类徽章各授予一次：四枚都在，一条不重")
    void grantsFourDistinctBadgesWithoutDuplicates() {
        fx.badgeService.grant(7L, BadgeType.FIRST_ANSWER);
        fx.badgeService.grant(7L, BadgeType.FIRST_ACCEPTED);
        fx.badgeService.grant(7L, BadgeType.TEACHER_ENDORSED);
        fx.badgeService.grant(7L, BadgeType.REPUTATION_100);

        assertThat(fx.badgeCodes(7L)).containsExactlyInAnyOrder(
                "FIRST_ANSWER", "FIRST_ACCEPTED", "TEACHER_ENDORSED", "REPUTATION_100");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(4);
    }

    @Test
    @DisplayName("徽章授予后发 BadgeAwardedEvent：带徽章代码与中文名，Discovery 直接拿去发通知")
    void badgeAwardedEventCarriesCodeAndChineseName() {
        fx.badgeService.grant(7L, BadgeType.TEACHER_ENDORSED);

        BadgeAwardedEvent event = fx.publishedEvents.stream()
                .filter(BadgeAwardedEvent.class::isInstance)
                .map(BadgeAwardedEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.badgeCode()).isEqualTo("TEACHER_ENDORSED");
        assertThat(event.badgeName()).isEqualTo("名师认可");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isEqualTo(fx.clock.instant());
    }

    @Test
    @DisplayName("徽章只发给本人：别人不会白得")
    void badgesAreScopedToTheAwardedUser() {
        fx.badgeService.grant(7L, BadgeType.FIRST_ANSWER);

        assertThat(fx.badgeCodes(7L)).containsExactly("FIRST_ANSWER");
        assertThat(fx.badgeCodes(8L)).isEmpty();
    }
}
