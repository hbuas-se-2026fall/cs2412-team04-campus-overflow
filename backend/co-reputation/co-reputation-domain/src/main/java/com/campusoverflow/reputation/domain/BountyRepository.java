package com.campusoverflow.reputation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BountyRepository {
    Optional<Bounty> findOpenByQuestion(long questionId);

    /**
     * 以行级悲观锁读取悬赏，供到期处理使用。
     * 必须在事务内按 id 重读后再判断状态，不能用事务外的 findDue 快照直接退款。
     */
    Optional<Bounty> findByIdForUpdate(long id);

    Optional<Bounty> findLatestByQuestion(long questionId);

    List<Bounty> findDue(Instant now, int limit);

    /** 每个发起人当前 OPEN 悬赏的积分总和，用于 frozen_points 对账。 */
    Map<Long, Long> sumOpenPointsBySponsor();

    Bounty save(Bounty bounty);
}
