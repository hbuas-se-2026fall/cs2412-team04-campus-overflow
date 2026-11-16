package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.api.ReputationApi;
import com.campusoverflow.reputation.api.ReputationSummary;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationApiImpl implements ReputationApi {

    private final ReputationAccountRepository accounts;

    public ReputationApiImpl(ReputationAccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * 单用户声誉。Discovery 的问答卡片、通知都只用到 userId + reputation；
     * 未开户（账户还没建）的用户按 0 返回，避免上游因为一次查询失败就整页报错。
     */
    @Override
    public ReputationSummary summary(long userId) {
        return accounts.findByUserId(userId)
                .map(a -> new ReputationSummary(a.userId(), a.reputation()))
                .orElseGet(() -> new ReputationSummary(userId, 0));
    }

    /** 批量声誉，供列表页避免 N+1；不存在的用户不出现在结果里（调用方按 0 处理）。 */
    @Override
    public Map<Long, Integer> reputations(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return accounts.findAllByUserId(userIds).stream()
                .collect(Collectors.toMap(ReputationAccount::userId, ReputationAccount::reputation,
                        (left, right) -> left, LinkedHashMap::new));
    }
}
