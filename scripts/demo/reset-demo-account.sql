-- 仅用于 demo profile 的本地演示数据库；不要在生产库执行。
-- 作用：把 T2001 恢复为 1000/0，并关闭该教师当前的 OPEN 悬赏。
-- 场景：DemoDataSeeder 的 q2 自带一个 50 分 OPEN 悬赏，导致 950/50；
-- 演示“再发起 50 分”前先执行本脚本，才能得到 950/50/冻结 50 的可复现结果。

UPDATE rep_bounty b
JOIN id_user u ON u.id = b.sponsor_id
SET b.status = 'EXPIRED',
    b.closed_at = NOW(6),
    b.version = b.version + 1
WHERE u.username = 'T2001'
  AND b.status = 'OPEN';

UPDATE rep_account a
JOIN id_user u ON u.id = a.user_id
SET a.available_points = 1000,
    a.frozen_points = 0,
    a.version = a.version + 1
WHERE u.username = 'T2001';
