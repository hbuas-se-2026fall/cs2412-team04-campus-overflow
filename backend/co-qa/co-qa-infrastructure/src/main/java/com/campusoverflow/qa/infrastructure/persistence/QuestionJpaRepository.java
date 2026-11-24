package com.campusoverflow.qa.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, Long> {

    @Modifying
    @Query("update QuestionJpaEntity q set q.deletedAt = :now, q.updatedAt = :now "
            + "where q.id = :id and q.deletedAt is null and q.answerCount = 0")
    int softDeleteIfUnanswered(@Param("id") long id, @Param("now") Instant now);

    @Modifying
    @Query("update QuestionJpaEntity q set q.answerCount = q.answerCount + :delta where q.id = :id")
    int adjustAnswerCount(@Param("id") long id, @Param("delta") int delta);

    @Modifying
    @Query("update QuestionJpaEntity q set q.score = q.score + :delta where q.id = :id")
    int adjustScore(@Param("id") long id, @Param("delta") int delta);
}
