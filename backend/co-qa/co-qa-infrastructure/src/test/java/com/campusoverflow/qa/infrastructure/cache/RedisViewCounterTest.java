package com.campusoverflow.qa.infrastructure.cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class RedisViewCounterTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private HashOperations<String, Object, Object> hash;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private PlatformTransactionManager txManager;
    @Mock
    private TransactionStatus txStatus;

    private RedisViewCounter counter;

    @BeforeEach
    void setUp() {
        when(redis.opsForHash()).thenReturn(hash);
        when(txManager.getTransaction(any())).thenReturn(txStatus);
        counter = new RedisViewCounter(redis, jdbc, txManager);
    }

    @Test
    @DisplayName("落库成功后提交事务并删除 FLUSHING 批次")
    void successfulFlushCommitsAndDeletesBatch() {
        when(redis.hasKey(RedisViewCounter.PENDING)).thenReturn(true);
        when(redis.hasKey(RedisViewCounter.FLUSHING)).thenReturn(false);
        when(hash.entries(RedisViewCounter.FLUSHING))
                .thenReturn(Map.<Object, Object>of("7", "3"));
        when(jdbc.batchUpdate(anyString(), anyList())).thenReturn(new int[] {1});

        counter.flush();

        verify(redis).rename(RedisViewCounter.PENDING, RedisViewCounter.FLUSHING);
        verify(jdbc).batchUpdate(anyString(), anyList());
        verify(txManager).commit(txStatus);
        verify(redis).delete(RedisViewCounter.FLUSHING);
    }

    @Test
    @DisplayName("落库失败时保留 FLUSHING 批次，下个周期再重试")
    void failedFlushKeepsBatch() {
        when(redis.hasKey(RedisViewCounter.PENDING)).thenReturn(true);
        when(redis.hasKey(RedisViewCounter.FLUSHING)).thenReturn(false);
        when(hash.entries(RedisViewCounter.FLUSHING))
                .thenReturn(Map.<Object, Object>of("7", "3"));
        when(jdbc.batchUpdate(anyString(), anyList()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        counter.flush();

        verify(txManager).rollback(txStatus);
        verify(redis, never()).delete(RedisViewCounter.FLUSHING);
    }

    @Test
    @DisplayName("下一轮先合并失败批次，再处理新一批浏览量")
    void nextFlushRecoversFailedBatch() {
        when(redis.hasKey(RedisViewCounter.FLUSHING)).thenReturn(true);
        when(redis.hasKey(RedisViewCounter.PENDING)).thenReturn(true);
        when(hash.entries(RedisViewCounter.FLUSHING))
                .thenReturn(Map.<Object, Object>of("7", "3"), Map.<Object, Object>of("7", "1"));
        when(jdbc.batchUpdate(anyString(), anyList())).thenReturn(new int[] {1});

        counter.flush();

        verify(hash).increment(RedisViewCounter.PENDING, "7", 3L);
        verify(hash).delete(RedisViewCounter.FLUSHING, "7");
        verify(redis).rename(RedisViewCounter.PENDING, RedisViewCounter.FLUSHING);
    }
}
