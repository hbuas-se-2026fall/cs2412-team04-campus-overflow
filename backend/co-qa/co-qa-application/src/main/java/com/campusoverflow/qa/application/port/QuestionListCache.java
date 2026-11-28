package com.campusoverflow.qa.application.port;

import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.Optional;

/** 问题列表缓存端口（Cache-Aside，TTL 60s + 抖动；写操作后整体失效）。 */
public interface QuestionListCache {

    /**
     * 读取结果必须同时返回本次 key 使用的代际号；调用方回填时只能写回该代际号。
     * 否则读请求在写事务提交前取到旧快照、提交后 put 时重新读到新代，会把旧数据写进新代。
     */
    record Lookup(long generation, Optional<PageResult<QuestionSummaryView>> value) {
    }

    Lookup get(Long courseId, Long viewerId, PageRequest page, String scope);

    void put(Long courseId, Long viewerId, PageRequest page, String scope, long generation,
             PageResult<QuestionSummaryView> value);

    void invalidateAll();
}
