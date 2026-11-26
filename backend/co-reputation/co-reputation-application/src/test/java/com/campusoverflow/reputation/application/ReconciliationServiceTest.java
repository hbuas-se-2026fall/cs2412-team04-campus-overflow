package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReputationAccountRepository accounts;

    @Mock
    private LedgerRepository ledger;

    @Mock
    private BountyRepository bounties;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void shouldReturnEmptyListWhenAllAccountsMatchLedger() {
        // 修正1：使用 reconstitute 创建对象
        ReputationAccount account = ReputationAccount.reconstitute(1L, 15, 0, 0, 0L);

        when(accounts.findAll()).thenReturn(List.of(account));
        when(ledger.sumByUser()).thenReturn(Map.of(1L, 15L));

        var mismatches = reconciliationService.reconcile();

        assertThat(mismatches).isEmpty();
    }

    @Test
    void shouldReturnMismatchWhenAccountDoesNotMatchLedger() {
        // 修正2：使用 reconstitute 创建对象
        ReputationAccount account = ReputationAccount.reconstitute(1L, 15, 0, 0, 0L);

        when(accounts.findAll()).thenReturn(List.of(account));
        when(ledger.sumByUser()).thenReturn(Map.of(1L, 10L));

        var mismatches = reconciliationService.reconcile();

        assertThat(mismatches).hasSize(1);

        // 修正3：使用 getFirst() 消除 IDEA 警告
        assertThat(mismatches.getFirst().userId()).isEqualTo(1L);
        assertThat(mismatches.getFirst().accountReputation()).isEqualTo(15);
        assertThat(mismatches.getFirst().ledgerSum()).isEqualTo(10L);
    }

    @Test
    void shouldReportLedgerUserWithMissingAccountRow() {
        when(accounts.findAll()).thenReturn(List.of());
        when(ledger.sumByUser()).thenReturn(Map.of(9L, 10L));

        var mismatches = reconciliationService.reconcile();

        assertThat(mismatches).hasSize(1);
        assertThat(mismatches.getFirst().userId()).isEqualTo(9L);
        assertThat(mismatches.getFirst().accountReputation()).isZero();
        assertThat(mismatches.getFirst().ledgerSum()).isEqualTo(10L);
    }

    @Test
    void pointsReconcilePassesWhenFrozenEqualsOpenBounties() {
        ReputationAccount account = ReputationAccount.reconstitute(1L, 0, 950, 50, 0L);
        when(accounts.findAll()).thenReturn(List.of(account));
        when(bounties.sumOpenPointsBySponsor()).thenReturn(Map.of(1L, 50L));

        assertThat(reconciliationService.reconcilePoints()).isEmpty();
    }

    @Test
    void pointsReconcileDetectsOverFrozenAndMissingSponsor() {
        ReputationAccount account = ReputationAccount.reconstitute(1L, 0, 900, 100, 0L);
        when(accounts.findAll()).thenReturn(List.of(account));
        when(bounties.sumOpenPointsBySponsor()).thenReturn(Map.of(1L, 50L, 2L, 30L));

        var mismatches = reconciliationService.reconcilePoints();

        assertThat(mismatches).extracting(ReconciliationService.PointsMismatch::userId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(mismatches).anySatisfy(m -> {
            assertThat(m.userId()).isEqualTo(1L);
            assertThat(m.frozenPoints()).isEqualTo(100);
            assertThat(m.openBountyPoints()).isEqualTo(50);
        });
        assertThat(mismatches).anySatisfy(m -> {
            assertThat(m.userId()).isEqualTo(2L);
            assertThat(m.frozenPoints()).isZero();
            assertThat(m.openBountyPoints()).isEqualTo(30);
        });
    }
}