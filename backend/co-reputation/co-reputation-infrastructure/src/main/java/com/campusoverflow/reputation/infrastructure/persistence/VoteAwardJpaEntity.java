package com.campusoverflow.reputation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 一次投票当前实际入账的声誉；uk_vote_award 保证同一投票者/目标只有一行。 */
@Entity
@Table(name = "rep_vote_award")
public class VoteAwardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_id", nullable = false)
    private long voterId;

    @Column(name = "target_type", nullable = false, length = 16)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private long targetId;

    @Column(name = "target_user_id", nullable = false)
    private long targetUserId;

    @Column(name = "applied_delta", nullable = false)
    private int appliedDelta;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected VoteAwardJpaEntity() {
    }

    public VoteAwardJpaEntity(long voterId, String targetType, long targetId, long targetUserId,
                              int appliedDelta, Instant appliedAt) {
        this.voterId = voterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetUserId = targetUserId;
        this.appliedDelta = appliedDelta;
        this.appliedAt = appliedAt;
    }

    public Long getId() { return id; }
    public long getVoterId() { return voterId; }
    public String getTargetType() { return targetType; }
    public long getTargetId() { return targetId; }
    public long getTargetUserId() { return targetUserId; }
    public int getAppliedDelta() { return appliedDelta; }
    public void setAppliedDelta(int appliedDelta) { this.appliedDelta = appliedDelta; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
    public void setTargetUserId(long targetUserId) { this.targetUserId = targetUserId; }
}
