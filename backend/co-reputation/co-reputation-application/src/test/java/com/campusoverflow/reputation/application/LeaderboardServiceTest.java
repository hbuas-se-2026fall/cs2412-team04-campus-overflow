package com.campusoverflow.reputation.application;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 排行榜事件处理器：只对首次投递累加，重复投递跳过（幂等键 reputation.leaderboard）。 */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardStore store;
    @Mock
    private ReputationAccountRepository accounts;
    @Mock
    private IdentityApi identity;
    @Mock
    private ProcessedEventStore processed;
    @Mock
    private Clock clock;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(store, accounts, identity, processed, clock);
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
}
