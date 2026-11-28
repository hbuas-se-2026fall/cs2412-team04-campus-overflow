-- 在 V202611240001 加“每题仅一个 OPEN”唯一索引前，先清理历史重复 OPEN。
-- 保留同问题下 id 最大的一条，其余标记为 EXPIRED；已有唯一索引的数据库执行时通常是无操作。
UPDATE rep_bounty b
JOIN (
    SELECT question_id, MAX(id) AS keep_id
    FROM rep_bounty
    WHERE status = 'OPEN'
    GROUP BY question_id
    HAVING COUNT(*) > 1
) d ON b.question_id = d.question_id AND b.id <> d.keep_id
SET b.status = 'EXPIRED',
    b.closed_at = COALESCE(b.closed_at, NOW(6)),
    b.version = b.version + 1
WHERE b.status = 'OPEN';
