package com.campusoverflow.qa.infrastructure.cache;

import com.campusoverflow.qa.application.port.ViewCounter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
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
    /** 批次 ID 存在 hash 内，和计数一起被 RENAME 摘走；落库时与计数同一事务写入已应用表。 */
    static final String BATCH_ID_FIELD = "__batch_id";
    /** 防止多实例同时执行“recover → rename → 落库”，避免互相覆盖 FLUSHING。 */
    private static final String FLUSH_LOCK = PENDING + ":lock";
    private static final Duration FLUSH_LOCK_TTL = Duration.ofMinutes(2);
    private static final String MARK_APPLIED_SQL =
            "INSERT INTO qa_view_batch (batch_id, applied_at) VALUES (?, NOW(6))";
    private static final String IS_APPLIED_SQL = "SELECT COUNT(*) FROM qa_view_batch WHERE batch_id = ?";
    private static final String CLEAN_APPLIED_SQL =
            "DELETE FROM qa_view_batch WHERE applied_at < NOW(6) - INTERVAL 7 DAY";
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

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
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(FLUSH_LOCK, token, FLUSH_LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        try {
            flushLocked();
        } finally {
            try {
                // compare-and-delete：只删除自己持有的锁，避免 TTL 过期后误删另一个实例的锁。
                redis.execute(UNLOCK_SCRIPT, List.of(FLUSH_LOCK), token);
            } catch (RuntimeException e) {
                log.warn("浏览计数刷新锁释放失败，等待 TTL 自动过期: {}", e.getMessage());
            }
        }
    }

    private void flushLocked() {
        try {
            // 上轮落库失败时先把遗留在 FLUSHING 的计数合并回 PENDING，避免永久丢失。
            recoverFailedBatch();
            if (!Boolean.TRUE.equals(redis.hasKey(PENDING))) {
                return;
            }
            String batchId = UUID.randomUUID().toString();
            // 批次 ID 与计数在同一个 hash 中，RENAME 会一起摘走。
            redis.opsForHash().put(PENDING, BATCH_ID_FIELD, batchId);
            redis.rename(PENDING, FLUSHING); // 原子地“摘走”当前批次，新增量写入新的 PENDING
            Map<Object, Object> entries = new HashMap<>(redis.opsForHash().entries(FLUSHING));
            entries.remove(BATCH_ID_FIELD);
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
            // 计数更新与“已应用批次”标记必须同一事务提交：
            // 提交成功后即使 Redis 清理失败/进程崩溃，下一轮也不会把同一批再写一次。
            transaction.executeWithoutResult(status -> {
                jdbc.batchUpdate("UPDATE qa_question SET view_count = view_count + ? WHERE id = ?", args);
                jdbc.update(MARK_APPLIED_SQL, batchId);
            });
            redis.delete(FLUSHING);
            cleanupAppliedBatches();
        } catch (RuntimeException e) {
            // 保留 FLUSHING；下一轮 recoverFailedBatch() 会合并回 PENDING 后重试。
            log.warn("浏览计数落库失败，保留批次等待下个周期重试: {}", e.getMessage());
        }
    }

    /** 清理 7 天前的已应用标记；失败不影响计数，只记录日志。 */
    private void cleanupAppliedBatches() {
        try {
            jdbc.update(CLEAN_APPLIED_SQL);
        } catch (RuntimeException e) {
            log.warn("清理已应用浏览量批次失败: {}", e.getMessage());
        }
    }

    /** 把失败批次逐字段合并回 PENDING；已合并的字段立即从 FLUSHING 删除，避免重复累加。 */
    private void recoverFailedBatch() {
        if (!Boolean.TRUE.equals(redis.hasKey(FLUSHING))) {
            return;
        }
        Object appliedBatch = redis.opsForHash().get(FLUSHING, BATCH_ID_FIELD);
        if (appliedBatch != null && isApplied(appliedBatch.toString())) {
            // 数据库已提交但上次没来得及删 FLUSHING：直接丢弃，绝不能回灌。
            redis.delete(FLUSHING);
            return;
        }
        Map<Object, Object> entries = new HashMap<>(redis.opsForHash().entries(FLUSHING));
        entries.remove(BATCH_ID_FIELD);
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

    private boolean isApplied(String batchId) {
        Integer count = jdbc.queryForObject(IS_APPLIED_SQL, Integer.class, batchId);
        return count != null && count > 0;
    }
}
