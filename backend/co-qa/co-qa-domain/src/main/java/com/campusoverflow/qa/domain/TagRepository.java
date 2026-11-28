package com.campusoverflow.qa.domain;

import java.util.Collection;
import java.util.Map;

/** 标签使用计数（热门标签）。 */
public interface TagRepository {
    void adjustUsage(Collection<String> tagNames, int delta);

    /**
     * 按净增量一次调整多个标签；实现必须按标签名排序后再批量执行，
     * 避免并发编辑以相反顺序加锁导致 InnoDB 死锁。
     */
    void adjustDeltas(Map<String, Integer> deltas);
}
