package com.campusoverflow.qa.domain;

import java.util.Optional;

public interface QuestionRepository {
    Optional<Question> findById(long id);

    Question save(Question question);

    /** 原子地调整回答数（UPDATE ... SET answer_count = answer_count + ?）。 */
    void adjustAnswerCount(long questionId, int delta);

    /** 原子地调整得分。 */
    void adjustScore(long questionId, int delta);

    /** 仅当 answer_count = 0 时原子软删除，返回是否删除成功（ADR-008 并发窗口修复）。 */
    boolean softDeleteIfNoAnswers(long questionId, java.time.Instant now);
}
