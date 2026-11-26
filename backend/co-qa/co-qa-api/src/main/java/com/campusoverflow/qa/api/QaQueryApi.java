package com.campusoverflow.qa.api;

import java.util.Optional;

/** Q&A 上下文对外的只读查询接口。 */
public interface QaQueryApi {
    Optional<QuestionRef> findQuestion(long questionId);

    /**
     * 问题的 accepted_answer_id 是否仍指向一条可见（未软删除、属于该问题）的回答。
     * 供悬赏等下游判断“是否真的已采纳”，避免历史脏数据把问题永久锁死。
     *
     * <p>默认实现只按 accepted_answer_id 是否存在判断，供测试替身与旧实现兼容；
     * {@link com.campusoverflow.qa.application.QaQueryApiImpl} 覆盖为真实可见性检查。</p>
     */
    default boolean hasVisibleAcceptedAnswer(long questionId) {
        return findQuestion(questionId).map(QuestionRef::acceptedAnswerId).isPresent();
    }
}
