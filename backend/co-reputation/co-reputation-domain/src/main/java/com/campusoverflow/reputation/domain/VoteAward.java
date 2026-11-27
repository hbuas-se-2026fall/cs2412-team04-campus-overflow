package com.campusoverflow.reputation.domain;

import java.time.Instant;

/**
 * 一次投票当前实际计入的声誉（可能被每日上限截断）。
 *
 * <p>改投/撤销必须按“实际入账值”回滚，而不是按票面效果（赞 +10 / 踩 -2）回滚；
 * 例如赞被上限截断为 0 时，撤销不能再扣 10。该记录按 (voterId, targetType, targetId) 唯一。</p>
 */
public record VoteAward(long voterId, String targetType, long targetId, long targetUserId,
                        int appliedDelta, Instant appliedAt) {
}
