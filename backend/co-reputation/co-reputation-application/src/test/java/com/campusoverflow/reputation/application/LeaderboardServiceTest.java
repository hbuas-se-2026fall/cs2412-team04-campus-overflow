package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 排行榜：事件幂等累加 + 用账本重建（审计 2-9）。 */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T02:00:00Z");

    @Mock
    private LeaderboardStore store;
    @Mock
    private ReputationAccountRepository accounts;
    @Mock
    private LedgerRepository ledger;
    @Mock
    private IdentityApi identity;
    @Mock
    private ProcessedEventStore processed;
    @Mock
    private Clock clock;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(store, accounts, ledger, identity, processed, clock);
    }

    @Test
    void onIncrementsLeaderboardOnFirstDelivery() {
        ReputationChangedEvent event =
                new ReputationChangedEvent("e1", Instant.parse("2026-10-08T02:00:00Z"), 7, 10, 25, "VOTE");
        when(processed.markProcessed("e1", "reputation.leaderboard")).thenReturn(true);

        service.on(event);

        verify(store).increment(7, 10, event.occurredAt());
    }

    @Test
    void onSkipsDuplicateDelivery() {
        ReputationChangedEvent event =
                new ReputationChangedEvent("e1", Instant.parse("2026-10-08T02:00:00Z"), 7, 10, 25, "VOTE");
        when(processed.markProcessed("e1", "reputation.leaderboard")).thenReturn(false);

        service.on(event);

        verify(store, never()).increment(anyLong(), anyInt(), eq(event.occurredAt()));
    }

    @Test
    void rebuildResetsAffectedKeysThenReplaysLedger() {
        when(clock.instant()).thenReturn(NOW);
        LedgerEntry first = new LedgerEntry(1L, 7L, "e1", ReputationReason.QUESTION_VOTE, 10, "QUESTION", 100L,
                NOW.minus(Duration.ofHours(3)));
        LedgerEntry second = new LedgerEntry(2L, 8L, "e2", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 200L,
                NOW.minus(Duration.ofHours(2)));
        when(ledger.findAllBetween(any(), any())).thenReturn(List.of(first, second));

        int replayed = service.rebuild(Duration.ofDays(200));

        assertThat(replayed).isEqualTo(2);
        // 先按重放流水的时间戳清 key，再按 id 顺序逐个重放
        verify(store).resetFor(List.of(first.createdAt(), second.createdAt()));
        verify(store).increment(7L, 10, first.createdAt());
        verify(store).increment(8L, 15, second.createdAt());
        verify(store, times(2)).increment(anyLong(), anyInt(), any());
    }

    @Test
    void rebuildWithEmptyLedgerTouchesNothing() {
        when(clock.instant()).thenReturn(NOW);
        when(ledger.findAllBetween(any(), any())).thenReturn(List.of());

        assertThat(service.rebuild(Duration.ofDays(30))).isZero();

        verify(store, never()).resetFor(any());
        verify(store, never()).increment(anyLong(), anyInt(), any());
    }

    @Test
    void rebuildWithNullWindowReplaysFullHistory() {
        when(clock.instant()).thenReturn(NOW);
        when(ledger.findAllBetween(Instant.EPOCH, NOW.plusSeconds(1)))
                .thenReturn(List.of(new LedgerEntry(1L, 7L, "e1", ReputationReason.QUESTION_VOTE, 10, "QUESTION",
                        100L, Instant.parse("2020-01-01T00:00:00Z"))));

        assertThat(service.rebuild(null)).isEqualTo(1);

        verify(ledger).findAllBetween(Instant.EPOCH, NOW.plusSeconds(1));
    }
}
