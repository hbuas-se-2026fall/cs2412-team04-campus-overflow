package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.reputation.api.ReputationSummary;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 对外查询接口（Discovery 用）与记账幂等。 */
class ReputationApiImplTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("summary 返回用户声誉值")
    void summaryReturnsReputation() {
        fx.register(3L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 101L);

        ReputationSummary summary = fx.api.summary(3L);

        assertThat(summary.userId()).isEqualTo(3L);
        assertThat(summary.reputation()).isEqualTo(15);
    }

    @Test
    @DisplayName("未开户用户查询声誉按 0 返回，不让上游整页报错")
    void summaryOfUnknownUserIsZero() {
        assertThat(fx.api.summary(404L).reputation()).isZero();
    }

    @Test
    @DisplayName("批量查询：只返回存在的用户，避免列表页 N+1")
    void reputationsBatchSkipsUnknownUsers() {
        fx.register(3L, Role.STUDENT);
        fx.register(4L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 101L);

        Map<Long, Integer> result = fx.api.reputations(List.of(3L, 4L, 404L));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(3L, 15, 4L, 0));
    }

    @Test
    @DisplayName("批量查询传空集合：直接返回空 Map")
    void reputationsOfEmptyInputIsEmpty() {
        assertThat(fx.api.reputations(List.of())).isEmpty();
    }

    @Test
    @DisplayName("同一个账本事件记两次：声誉只动一次，事件只发一次")
    void recordingSameEventTwiceCountsOnce() {
        fx.register(3L, Role.STUDENT);

        boolean first = fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15,
                "ANSWER", 101L);
        boolean replay = fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15,
                "ANSWER", 101L);

        assertThat(first).isTrue();
        assertThat(replay).as("重复事件应被账本唯一键挡掉").isFalse();
        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(15);
        assertThat(fx.ledger.size(3L)).isEqualTo(1);
        assertThat(fx.publishedCount(ReputationChangedEvent.class)).isEqualTo(1);
    }
}
