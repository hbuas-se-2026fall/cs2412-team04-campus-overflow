package com.campusoverflow.qa.domain;

import java.time.Instant;
import java.util.Optional;

public interface QuestionRepository {
    Optional<Question> findById(long id);

    Question save(Question question);

    /**
     * 原子软删除：仅当问题未删除且回答数仍为 0 时成功。
     * 用于作者删除，避免“读取后并发回答”使内存快照判断失效。
     */
    boolean softDeleteIfUnanswered(long questionId, Instant now);

    /** 原子地调整回答数（UPDATE ... SET answer_count = answer_count + ?）。 */
    void adjustAnswerCount(long questionId, int delta);

    /**
     * 仅在问题仍未删除时原子增加回答数。返回 false 表示问题已被并发删除，
     * 调用方必须回滚，避免出现“问题已删、回答仍插入”的孤儿数据。
     */
    boolean adjustAnswerCountIfOpen(long questionId, int delta);

    /** 原子地调整得分。 */
    void adjustScore(long questionId, int delta);
}
