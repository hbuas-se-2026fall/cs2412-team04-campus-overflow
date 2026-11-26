-- 对历史重复 OPEN 清理后遗留的冻结积分做一次性对账。
-- 不变量：rep_account.frozen_points 应恰好等于该用户所有 OPEN 悬赏的 points 之和。
-- V202611230001 只把重复 OPEN 改成 EXPIRED，没有退回冻结积分；这里把多冻结的部分退回可用积分。
-- 只处理 frozen_points > expected 的账户；frozen < expected 说明还有更严重的历史不一致，留给对账告警。
UPDATE rep_account a
JOIN (
    SELECT sponsor_id, SUM(points) AS expected
    FROM rep_bounty
    WHERE status = 'OPEN'
    GROUP BY sponsor_id
) o ON o.sponsor_id = a.user_id
SET a.available_points = a.available_points + (a.frozen_points - o.expected),
    a.frozen_points = o.expected,
    a.version = a.version + 1
WHERE a.frozen_points > o.expected;

-- 没有任何 OPEN 悬赏但仍有冻结积分的账户：全部退回可用积分。
UPDATE rep_account a
LEFT JOIN (
    SELECT sponsor_id, SUM(points) AS expected
    FROM rep_bounty
    WHERE status = 'OPEN'
    GROUP BY sponsor_id
) o ON o.sponsor_id = a.user_id
SET a.available_points = a.available_points + a.frozen_points,
    a.frozen_points = 0,
    a.version = a.version + 1
WHERE o.sponsor_id IS NULL AND a.frozen_points > 0;
