-- 记录每次投票当前实际入账的声誉，供改投/撤销按实际值回滚。
-- 旧数据没有该行，首次改投时按旧规则回退（见 ReputationLedgerService.recordVoteCapped）。
CREATE TABLE rep_vote_award (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    voter_id       BIGINT      NOT NULL,
    target_type    VARCHAR(16) NOT NULL,
    target_id      BIGINT      NOT NULL,
    target_user_id BIGINT      NOT NULL,
    applied_delta  INT         NOT NULL,
    applied_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_award (voter_id, target_type, target_id),
    KEY idx_vote_award_day (target_user_id, applied_at, applied_delta)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
