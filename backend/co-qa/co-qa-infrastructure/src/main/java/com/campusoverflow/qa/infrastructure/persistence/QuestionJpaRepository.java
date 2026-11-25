package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuestionJpaEntity q where q.id = :id")
    Optional<QuestionJpaEntity> findByIdForUpdate(@Param("id") long id);

    @Modifying
    @Query("update QuestionJpaEntity q set q.deletedAt = :now, q.updatedAt = :now, "
            + "q.version = q.version + 1 "
            + "where q.id = :id and q.deletedAt is null and q.answerCount = 0")
    int softDeleteIfUnanswered(@Param("id") long id, @Param("now") Instant now);

    @Modifying
    @Query("update QuestionJpaEntity q set q.answerCount = q.answerCount + :delta where q.id = :id")
    int adjustAnswerCount(@Param("id") long id, @Param("delta") int delta);

    @Modifying
    @Query("update QuestionJpaEntity q set q.answerCount = q.answerCount + :delta "
            + "where q.id = :id and q.deletedAt is null")
    int adjustAnswerCountIfOpen(@Param("id") long id, @Param("delta") int delta);

    @Modifying
    @Query("update QuestionJpaEntity q set q.score = q.score + :delta where q.id = :id")
    int adjustScore(@Param("id") long id, @Param("delta") int delta);
}
