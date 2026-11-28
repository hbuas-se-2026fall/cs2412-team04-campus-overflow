-- 浏览量批次幂等标记：计数更新与 batch_id 在同一事务提交。
-- 若提交后 Redis 清理失败或进程崩溃，下一轮看到标记就直接丢弃 FLUSHING，避免重复累加。
CREATE TABLE qa_view_batch (
    batch_id  VARCHAR(64) NOT NULL,
    applied_at DATETIME(6) NOT NULL,
    PRIMARY KEY (batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
