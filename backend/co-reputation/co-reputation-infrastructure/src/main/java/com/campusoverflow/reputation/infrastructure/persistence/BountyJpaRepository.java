package com.campusoverflow.reputation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BountyJpaRepository extends JpaRepository<BountyJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BountyJpaEntity b where b.id = :id")
    Optional<BountyJpaEntity> findByIdForUpdate(@Param("id") long id);

    Optional<BountyJpaEntity> findFirstByQuestionIdAndStatus(long questionId, String status);

    Optional<BountyJpaEntity> findFirstByQuestionIdOrderByIdDesc(long questionId);

    List<BountyJpaEntity> findByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(String status, Instant now,
                                                                                 Pageable pageable);

    @Query("select b.sponsorId, sum(b.points) from BountyJpaEntity b where b.status = :status group by b.sponsorId")
    List<Object[]> sumPointsByStatus(@Param("status") String status);
}
