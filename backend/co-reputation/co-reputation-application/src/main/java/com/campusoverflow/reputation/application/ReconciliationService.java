package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 适应度函数 FF-8：对账——声誉 = 流水之和；冻结积分 = OPEN 悬赏积分之和。 */
@Service
public class ReconciliationService {

    public record Mismatch(long userId, int accountReputation, long ledgerSum) {
    }

    public record PointsMismatch(long userId, int frozenPoints, long openBountyPoints) {
    }

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final BountyRepository bounties;

    public ReconciliationService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                 BountyRepository bounties) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.bounties = bounties;
    }

    @Transactional(readOnly = true)
    public List<Mismatch> reconcile() {
        List<Mismatch> mismatches = new java.util.ArrayList<>();

        // 1. 账户表与流水表取并集：账户行缺失但仍有流水的用户也必须被报出来。
        Map<Long, ReputationAccount> byUser = new HashMap<>();
        for (ReputationAccount account : accounts.findAll()) {
            byUser.put(account.userId(), account);
        }
        Map<Long, Long> ledgerSums = ledger.sumByUser();
        java.util.Set<Long> userIds = new java.util.LinkedHashSet<>(byUser.keySet());
        userIds.addAll(ledgerSums.keySet());

        for (Long userId : userIds) {
            ReputationAccount account = byUser.get(userId);
            long ledgerSum = ledgerSums.getOrDefault(userId, 0L);
            // 账户行缺失本身就要报，哪怕流水的净和为 0（+10 后被撤销的净零用户）。
            if (account == null) {
                mismatches.add(new Mismatch(userId, 0, ledgerSum));
                continue;
            }
            int accountReputation = account.reputation();
            if (accountReputation != ledgerSum) {
                mismatches.add(new Mismatch(userId, accountReputation, ledgerSum));
            }
        }

        return mismatches;
    }

    /**
     * 积分对账：frozen_points 必须等于该用户所有 OPEN 悬赏的 points 之和。
     * 同时可以发现“有 OPEN 悬赏但账户缺失”“frozen 为负”等问题。
     */
    @Transactional(readOnly = true)
    public List<PointsMismatch> reconcilePoints() {
        Map<Long, Long> expectedFrozen = new HashMap<>(bounties.sumOpenPointsBySponsor());
        Map<Long, ReputationAccount> byUser = new HashMap<>();
        List<PointsMismatch> mismatches = new ArrayList<>();

        for (ReputationAccount account : accounts.findAll()) {
            byUser.put(account.userId(), account);
            long expected = expectedFrozen.getOrDefault(account.userId(), 0L);
            if (account.frozenPoints() != expected) {
                mismatches.add(new PointsMismatch(account.userId(), account.frozenPoints(), expected));
            }
        }
        // OPEN 悬赏的发起人账户不存在：expected 有值但账户表没有记录。
        for (Map.Entry<Long, Long> entry : expectedFrozen.entrySet()) {
            if (!byUser.containsKey(entry.getKey())) {
                mismatches.add(new PointsMismatch(entry.getKey(), 0, entry.getValue()));
            }
        }
        return mismatches;
    }
}