package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VoteJpaRepository extends JpaRepository<VoteJpaEntity, Long> {
    /**
     * 改投/撤销必须串行化同一票行：否则两个并发请求都读到旧值，
     * 票表只保留一条，但 score/声誉增量会被各应用一次。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VoteJpaEntity> findByVoterIdAndTargetTypeAndTargetId(long voterId, String targetType, long targetId);
}
