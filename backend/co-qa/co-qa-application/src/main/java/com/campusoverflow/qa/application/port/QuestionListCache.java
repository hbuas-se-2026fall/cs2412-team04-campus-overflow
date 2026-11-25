package com.campusoverflow.qa.application.port;

import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.Optional;

/** 问题列表缓存端口（Cache-Aside，TTL 60s + 抖动；写操作后整体失效）。 */
public interface QuestionListCache {
    /** viewerId 参与缓存 key：课程可见性取决于当前用户，不能把他人可见的列表复用给未加入课程的人。 */
    Optional<PageResult<QuestionSummaryView>> get(Long courseId, Long viewerId, PageRequest page);

    void put(Long courseId, Long viewerId, PageRequest page, PageResult<QuestionSummaryView> value);

    void invalidateAll();
}
