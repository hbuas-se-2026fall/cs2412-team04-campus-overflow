package com.campusoverflow.discovery.domain.search;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** 搜索投影的写端（由事件投影器维护）。 */
public interface SearchIndex {

    void insert(SearchDocument document);

    /** @return 受影响行数；0 表示投影缺失，调用方可用事件内容重建。 */
    int updateText(long questionId, String title, String bodyText, List<String> tags, Instant at);

    void adjustAnswerCount(long questionId, int delta, Instant at);

    void adjustScore(long questionId, int delta);

    void markAccepted(long questionId, Instant at);

    /** 已采纳回答被删除后清除 accepted 标记（只在 Q&A 确认该问题已无采纳答案时调用）。 */
    void clearAccepted(long questionId, Instant at);

    void setBountyOpen(long questionId, boolean open);

    void markDeleted(long questionId);

    List<String> findTags(long questionId);

    /**
     * 与给定标签至少共享一个标签的候选问题（排除自身与已删除）。
     * 课程可见性条件下推到查询中，避免“先 LIMIT 50、再在内存过滤”漏掉可见候选。
     */
    List<RelatedCandidate> findCandidatesSharingTags(long questionId, List<String> tags, boolean allCourses,
                                                     Collection<Long> visibleCourseIds, int limit);

    record RelatedCandidate(long questionId, String title, Long courseId, int sharedTags, int tagCount, int score,
                            int answerCount, boolean accepted) {
    }
}
