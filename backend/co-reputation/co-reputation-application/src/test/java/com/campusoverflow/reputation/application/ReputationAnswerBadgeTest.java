package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 回答相关徽章：on(AnswerSubmittedEvent) 给「初来乍到」，on(AnswerEndorsedEvent) 给「名师认可」。
 * 两条都要证明「同一事件调两次只有一枚」——评审会真的把 outbox 事件放回来重放。
 */
class ReputationAnswerBadgeTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("答一次得「初来乍到」，再答不重复给")
    void firstAnswerAwardsNewcomerBadgeOnlyOnce() {
        fx.register(3L, Role.STUDENT);

        fx.submitAnswer(101L, 3L, 1L);
        fx.submitAnswer(102L, 3L, 1L);

        assertThat(fx.badgeCodes(3L)).containsExactly("FIRST_ANSWER");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("AnswerSubmittedEvent 重放两次：只有一枚徽章、事件也只发一次")
    void answerSubmittedEventReplayedTwiceAwardsOneBadge() {
        fx.register(3L, Role.STUDENT);
        AnswerSubmittedEvent event = fx.submitAnswer(101L, 3L, 1L);

        fx.handlers.on(event);

        assertThat(fx.badgeCodes(3L)).containsExactly("FIRST_ANSWER");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("教师认证得「名师认可」")
    void teacherEndorsementAwardsTeacherEndorsedBadge() {
        fx.register(3L, Role.STUDENT);

        fx.endorse(101L, 3L, 9L);

        assertThat(fx.badgeCodes(3L)).containsExactly("TEACHER_ENDORSED");
    }

    @Test
    @DisplayName("AnswerEndorsedEvent 重放两次：只有一枚「名师认可」")
    void answerEndorsedEventReplayedTwiceAwardsOneBadge() {
        fx.register(3L, Role.STUDENT);
        AnswerEndorsedEvent event = fx.endorse(101L, 3L, 9L);

        fx.handlers.on(event);
        fx.handlers.on(event);

        assertThat(fx.badgeCodes(3L)).containsExactly("TEACHER_ENDORSED");
        assertThat(fx.publishedCount(BadgeAwardedEvent.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("「名师认可」与「初露锋芒」是两回事：教师认证不会冒充被采纳")
    void endorsementIsNotAcceptance() {
        fx.register(3L, Role.STUDENT);

        fx.endorse(101L, 3L, 9L);

        assertThat(fx.badgeCodes(3L)).doesNotContain("FIRST_ACCEPTED");
    }

    @Test
    @DisplayName("回答本身不加声誉：只有被采纳/被投票才进账本")
    void submittingAnswerDoesNotChangeReputation() {
        fx.register(3L, Role.STUDENT);

        fx.submitAnswer(101L, 3L, 1L);

        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isZero();
        assertThat(fx.ledger.size(3L)).isZero();
    }
}
