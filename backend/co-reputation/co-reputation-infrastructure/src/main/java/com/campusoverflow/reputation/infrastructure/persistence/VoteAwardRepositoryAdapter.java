package com.campusoverflow.reputation.infrastructure.persistence;

import com.campusoverflow.reputation.domain.VoteAward;
import com.campusoverflow.reputation.domain.VoteAwardRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class VoteAwardRepositoryAdapter implements VoteAwardRepository {

    private final VoteAwardJpaRepository jpa;

    public VoteAwardRepositoryAdapter(VoteAwardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<VoteAward> find(long voterId, String targetType, long targetId) {
        return jpa.findByVoterIdAndTargetTypeAndTargetId(voterId, targetType, targetId).map(e ->
                new VoteAward(e.getVoterId(), e.getTargetType(), e.getTargetId(), e.getTargetUserId(),
                        e.getAppliedDelta(), e.getAppliedAt()));
    }

    @Override
    public void save(VoteAward award) {
        VoteAwardJpaEntity e = jpa.findByVoterIdAndTargetTypeAndTargetId(award.voterId(), award.targetType(),
                        award.targetId())
                .orElseGet(() -> new VoteAwardJpaEntity(award.voterId(), award.targetType(), award.targetId(),
                        award.targetUserId(), award.appliedDelta(), award.appliedAt()));
        e.setTargetUserId(award.targetUserId());
        e.setAppliedDelta(award.appliedDelta());
        e.setAppliedAt(award.appliedAt());
        jpa.save(e);
    }
}
