package com.campusoverflow.qa.infrastructure.cache;

import com.campusoverflow.qa.application.port.ViewCounter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 浏览量：请求路径只做一次 Redis HINCRBY；定时任务把增量批量落库。
 * Redis 不可用时静默降级（丢失少量浏览数可以接受，但不能影响详情页可用性）。
 */
@Component
public class RedisViewCounter implements ViewCounter {

    private static final Logger log = LoggerFactory.getLogger(RedisViewCounter.class);
    static final String PENDING = "qa:views:pending";
    /** 上一轮摘走但尚未落库的批次；失败后由下一轮合并回 PENDING。 */
    static final String FLUSHING = PENDING + ":flushing";

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public RedisViewCounter(StringRedisTemplate redis, JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.redis = redis;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(txManager);
    }

    @Override
    public void increment(long questionId) {
        try {
            redis.opsForHash().increment(PENDING, String.valueOf(questionId), 1);
        } catch (RuntimeException e) {
            log.warn("浏览计数写入 Redis 失败，已降级忽略: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${co.qa.view-flush-interval-ms:60000}")
    public void flush() {
        try {
            // 上轮落库失败时先把遗留在 FLUSHING 的计数合并回 PENDING，避免永久丢失。
            recoverFailedBatch();
            if (!Boolean.TRUE.equals(redis.hasKey(PENDING))) {
                return;
            }
            redis.rename(PENDING, FLUSHING); // 原子地“摘走”当前批次，新增量写入新的 PENDING
            Map<Object, Object> entries = redis.opsForHash().entries(FLUSHING);
            if (entries.isEmpty()) {
                redis.delete(FLUSHING);
                return;
            }
            List<Object[]> args = new ArrayList<>();
            for (Map.Entry<Object, Object> e : entries.entrySet()) {
                try {
                    args.add(new Object[] {Long.parseLong(e.getValue().toString()),
                            Long.parseLong(e.getKey().toString())});
                } catch (NumberFormatException bad) {
                    log.warn("丢弃无法解析的浏览计数 {}={}", e.getKey(), e.getValue());
                }
            }
            if (args.isEmpty()) {
                redis.delete(FLUSHING);
                return;
            }
            // 批次必须原子提交；否则“部分行已加、整个批次又回滚重试”会重复计数。
            transaction.executeWithoutResult(status ->
                    jdbc.batchUpdate("UPDATE qa_question SET view_count = view_count + ? WHERE id = ?", args));
            redis.delete(FLUSHING);
        } catch (RuntimeException e) {
            // 保留 FLUSHING；下一轮 recoverFailedBatch() 会合并回 PENDING 后重试。
            log.warn("浏览计数落库失败，保留批次等待下个周期重试: {}", e.getMessage());
        }
    }

    /** 把失败批次逐字段合并回 PENDING；已合并的字段立即从 FLUSHING 删除，避免重复累加。 */
    private void recoverFailedBatch() {
        if (!Boolean.TRUE.equals(redis.hasKey(FLUSHING))) {
            return;
        }
        Map<Object, Object> entries = redis.opsForHash().entries(FLUSHING);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                long delta = Long.parseLong(entry.getValue().toString());
                if (delta != 0) {
                    redis.opsForHash().increment(PENDING, entry.getKey().toString(), delta);
                }
            } catch (NumberFormatException e) {
                log.warn("丢弃损坏的浏览量计数 key={} value={}", entry.getKey(), entry.getValue());
            }
            redis.opsForHash().delete(FLUSHING, entry.getKey());
        }
        redis.delete(FLUSHING);
    }
}
