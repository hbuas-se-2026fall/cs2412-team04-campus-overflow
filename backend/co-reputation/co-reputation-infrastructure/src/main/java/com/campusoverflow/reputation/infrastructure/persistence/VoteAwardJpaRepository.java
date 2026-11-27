package com.campusoverflow.reputation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteAwardJpaRepository extends JpaRepository<VoteAwardJpaEntity, Long> {

    Optional<VoteAwardJpaEntity> findByVoterIdAndTargetTypeAndTargetId(long voterId, String targetType,
                                                                       long targetId);
}
