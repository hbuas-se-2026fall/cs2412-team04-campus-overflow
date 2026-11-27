package com.campusoverflow.reputation.domain;

import java.util.Optional;

/** 投票实际入账状态（按投票者 + 目标唯一），用于改投/撤销时精确回滚。 */
public interface VoteAwardRepository {

    Optional<VoteAward> find(long voterId, String targetType, long targetId);

    void save(VoteAward award);
}
