package com.campusoverflow.reputation.infrastructure.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.reputation.domain.AcademicTerm;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 排行榜存储的契约测试：一次 increment 必须原子地写 week/month/term 三个 ZSET 并各设 TTL。
 * 说明（审计 F-13）：这里只验证「调用契约」；Lua 中途 WRONGTYPE 不回滚已执行命令属于
 * Redis 服务端行为，无法用 mock 复现，已在 security-hardening-20261124.md 的措辞中如实标注。
 */
@ExtendWith(MockitoExtension.class)
class RedisLeaderboardStoreTest {

    @Mock
    private StringRedisTemplate redis;

    private RedisLeaderboardStore store;

    @BeforeEach
    void setUp() {
        store = new RedisLeaderboardStore(redis);
    }

    @Test
    void incrementWritesThreeZsetsWithTtl() {
        Instant at = Instant.parse("2026-10-08T02:00:00Z");

        store.increment(7, 10, at);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);

        // ARGV[1]=delta, ARGV[2]=member, ARGV[3..5]=week/month/term 的 TTL（秒）
        verify(redis).execute(script.capture(), keys.capture(),
                eq("10"), eq("7"),
                eq("1296000"), eq("3456000"), eq("17280000"));

        assertThat(keys.getValue()).containsExactly(
                "lb:week:" + AcademicTerm.weekKey(at),
                "lb:month:" + AcademicTerm.monthKey(at),
                "lb:term:" + AcademicTerm.termKey(at));
        assertThat(script.getValue().getScriptAsString())
                .contains("for i = 1, 3 do", "ZINCRBY", "EXPIRE");
    }

    @Test
    void resetForDeletesOnlyKeysDerivedFromGivenInstants() {
        Instant at = Instant.parse("2026-10-08T02:00:00Z");
        when(redis.delete(anyCollection())).thenReturn(3L);

        int deleted = store.resetFor(List.of(at, at));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> keys = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(redis).delete(keys.capture());
        // 同一时间戳重复传入不能重复拼 key（用 LinkedHashSet 去重）
        assertThat(keys.getValue()).containsExactly(
                "lb:week:" + AcademicTerm.weekKey(at),
                "lb:month:" + AcademicTerm.monthKey(at),
                "lb:term:" + AcademicTerm.termKey(at));
        assertThat(deleted).isEqualTo(3);
    }

    @Test
    void resetForWithoutInstantsDoesNotTouchRedis() {
        assertThat(store.resetFor(List.of())).isZero();
        verify(redis, never()).delete(anyCollection());
    }
}
