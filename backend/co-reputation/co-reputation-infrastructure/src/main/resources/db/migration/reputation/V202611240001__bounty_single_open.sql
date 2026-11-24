-- 同一问题最多只能有一个 OPEN 悬赏。
-- MySQL 的普通索引无法表达部分唯一约束，因此用生成列把 OPEN 的 question_id 映射出来，
-- 非 OPEN 行为 NULL；唯一索引允许多个 NULL，于是历史悬赏记录不受限制。
ALTER TABLE rep_bounty
    ADD COLUMN open_question_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN question_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_bounty_open_question (open_question_id);
