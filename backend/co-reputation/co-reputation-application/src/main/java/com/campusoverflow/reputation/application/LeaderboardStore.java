package com.campusoverflow.reputation.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** 排行榜存储端口（实现：Redis ZSET，ADR 讨论见 8.x 缓存章节）。 */
public interface LeaderboardStore {

    record Entry(long userId, long score) {
    }

    void increment(long userId, int delta, Instant at);

    List<Entry> top(LeaderboardPeriod period, Instant at, int limit);

    /**
     * 重建前先清掉这些时间戳所对应的周/月/学期 ZSET（只删真正会被重放的 key）。
     *
     * @return 实际删除的 key 数量
     */
    int resetFor(Collection<Instant> instants);
}
