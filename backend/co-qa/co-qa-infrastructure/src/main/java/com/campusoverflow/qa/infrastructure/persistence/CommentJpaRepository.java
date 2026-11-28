package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {

    List<CommentJpaEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommentJpaEntity c where c.id = :id")
    Optional<CommentJpaEntity> findByIdForUpdate(@Param("id") long id);
}
