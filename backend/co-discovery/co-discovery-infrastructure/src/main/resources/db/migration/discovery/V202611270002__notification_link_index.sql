-- 问题删除时按 link 精确清理通知；没有索引会全表扫描并持有大量 next-key lock。
CREATE INDEX idx_notification_link ON dis_notification (link);
