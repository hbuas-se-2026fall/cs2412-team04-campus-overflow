-- 为已在 2026-11-26 之前创建、且已有余额/冻结/声誉的账户补“初始积分已发放”标记。
-- 这些账户不再重复发放；0/0/0 的懒创建账户不标记，等注册事件到达时补发。
INSERT IGNORE INTO processed_event (event_id, consumer, processed_at)
SELECT CONCAT('initial-points:', user_id), 'reputation-initial-points', NOW(6)
FROM rep_account
WHERE available_points <> 0 OR frozen_points <> 0 OR reputation <> 0;
