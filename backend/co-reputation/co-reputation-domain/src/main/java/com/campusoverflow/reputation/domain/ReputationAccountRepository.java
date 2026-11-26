package com.campusoverflow.reputation.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReputationAccountRepository {
    Optional<ReputationAccount> findByUserId(long userId);

    /**
     * 以**行锁**读取账户（JPA `PESSIMISTIC_WRITE`），用于「读-算-写」必须落在同一临界区的场景。
     *
     * <p>审计魏-11：投票日上限原实现是「先 sumPositiveVoteDeltaSince 求和，再加载账户并写入」，
     * 两步之间的并发窗口会让两次投票都读到未达上限的旧值；改为在账户行锁内重算日累计再写入。
     */
    Optional<ReputationAccount> findByUserIdForUpdate(long userId);

    List<ReputationAccount> findAllByUserId(Collection<Long> userIds);

    List<ReputationAccount> topByReputation(int limit);

    List<ReputationAccount> findAll();

    ReputationAccount save(ReputationAccount account);
}
