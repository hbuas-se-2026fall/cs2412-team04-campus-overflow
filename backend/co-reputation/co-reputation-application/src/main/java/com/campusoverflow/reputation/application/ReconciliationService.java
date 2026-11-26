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

        // 1. 查出所有账户
        List<ReputationAccount> allAccounts = accounts.findAll();

        // 2. 一次性查出所有用户的流水汇总 (不需要传参数)
        Map<Long, Long> ledgerSums = ledger.sumByUser();

        for (ReputationAccount account : allAccounts) {
            // 3. 从 Map 里拿出该用户的流水总和，如果没有流水默认是 0
            long ledgerSum = ledgerSums.getOrDefault(account.userId(), 0L);

            // 4. 对比账户余额与流水总和
            if (account.reputation() != ledgerSum) {
                // 如果对不上，记录一条异常
                mismatches.add(new Mismatch(account.userId(), account.reputation(), ledgerSum));
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