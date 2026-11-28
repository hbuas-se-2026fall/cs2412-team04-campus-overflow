package com.campusoverflow.qa.domain;

import java.util.Optional;

public interface AnswerRepository {
    Optional<Answer> findById(long id);

    /**
     * 只取回答所属问题 ID，不加载 Answer 实体。
     * 用于“先锁问题行、再读回答”的并发路径，避免第二次 findById 命中一级缓存看不到并发软删除。
     */
    Optional<Long> findQuestionId(long answerId);

    Answer save(Answer answer);

    void adjustScore(long answerId, int delta);
}
