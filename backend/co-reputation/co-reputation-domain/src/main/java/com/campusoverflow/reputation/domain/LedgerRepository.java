package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LedgerRepository {
    boolean exists(String eventId, long userId, ReputationReason reason);

    void append(LedgerEntry entry);

    PageResult<LedgerEntry> findByUser(long userId, PageRequest page);

    long countByUserAndReason(long userId, ReputationReason reason);

    /** 某用户自 since 起因被投票获得的正向声誉总和（用于每日上限）。 */
    int sumPositiveVoteDeltaSince(long userId, Instant since);

    /** 对账：每个用户的流水总和。 */
    Map<Long, Long> sumByUser();

    /**
     * 重建排行榜用：读取 {@code [from, to)} 区间的全部流水（按 id 升序）。
     *
     * <p>排行榜的周/月/学期 ZSET 是可重建的派生数据，权威来源是这张只追加的账本；
     * 当 Redis 数据丢失或被误清时，用本方法重放即可恢复到与账本一致。</p>
     */
    List<LedgerEntry> findAllBetween(Instant from, Instant to);
}
