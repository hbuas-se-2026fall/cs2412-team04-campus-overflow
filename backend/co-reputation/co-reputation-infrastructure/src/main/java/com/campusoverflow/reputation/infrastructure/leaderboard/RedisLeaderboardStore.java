package com.campusoverflow.reputation.infrastructure.leaderboard;

import com.campusoverflow.reputation.application.LeaderboardPeriod;
import com.campusoverflow.reputation.application.LeaderboardStore;
import com.campusoverflow.reputation.domain.AcademicTerm;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

/** 排行榜：每个周期一个 ZSET（lb:week:2026-W41 / lb:month:2026-10 / lb:term:2026-FALL），ZINCRBY 维护。 */
@Component
public class RedisLeaderboardStore implements LeaderboardStore {

    private final StringRedisTemplate redis;

    /** 三个 ZSET 一次 Lua 原子更新：避免 week 成功、month 失败后重试导致 week 重复计分。 */
    private static final RedisScript<Long> INCR_THREE = new DefaultRedisScript<>(
            "for i = 1, 3 do "
                    + "redis.call('ZINCRBY', KEYS[i], ARGV[1], ARGV[2]) "
                    + "redis.call('EXPIRE', KEYS[i], ARGV[2 + i]) "
                    + "end "
                    + "return 1",
            Long.class);

    public RedisLeaderboardStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void increment(long userId, int delta, Instant at) {
        String member = String.valueOf(userId);
        redis.execute(INCR_THREE,
                List.of("lb:week:" + AcademicTerm.weekKey(at),
                        "lb:month:" + AcademicTerm.monthKey(at),
                        "lb:term:" + AcademicTerm.termKey(at)),
                String.valueOf(delta), member,
                String.valueOf(Duration.ofDays(15).toSeconds()),
                String.valueOf(Duration.ofDays(40).toSeconds()),
                String.valueOf(Duration.ofDays(200).toSeconds()));
    }

    @Override
    public List<Entry> top(LeaderboardPeriod period, Instant at, int limit) {
        String key = switch (period) {
            case WEEK -> "lb:week:" + AcademicTerm.weekKey(at);
            case MONTH -> "lb:month:" + AcademicTerm.monthKey(at);
            case TERM -> "lb:term:" + AcademicTerm.termKey(at);
            case ALL -> throw new IllegalArgumentException("总榜不由 Redis 提供");
        };
        Set<TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, 0, limit - 1L);
        if (tuples == null) {
            return List.of();
        }
        return tuples.stream()
                .filter(t -> t.getValue() != null && t.getScore() != null)
                .map(t -> new Entry(Long.parseLong(t.getValue()), Math.round(t.getScore())))
                .toList();
    }
}
