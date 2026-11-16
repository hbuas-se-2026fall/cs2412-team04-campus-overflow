package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;

/**
 * 声誉账户。不变量：reputation 恒等于该用户所有账本流水 delta 之和（由对账任务 FF-8 验证）。
 * 悬赏积分 = 可用 + 冻结，冻结部分在悬赏结算或到期时释放。
 */
public class ReputationAccount {

    /** 声誉不能为负：会使余额掉到 0 以下的变更直接拒绝，保证「余额 = 流水之和」在对账时仍成立。 */
    public static final String ERR_REPUTATION_NEGATIVE = "REP-1001";
    /** 积分数量非法：必须为正整数（开户初始积分不允许为负）。 */
    public static final String ERR_INVALID_POINTS = "REP-1003";

    private final long userId;
    private int reputation;
    private int availablePoints;
    private int frozenPoints;
    private final long version;

    private ReputationAccount(long userId, int reputation, int availablePoints, int frozenPoints, long version) {
        this.userId = userId;
        this.reputation = reputation;
        this.availablePoints = availablePoints;
        this.frozenPoints = frozenPoints;
        this.version = version;
    }

    /**
     * 开立账户：声誉从 0 起算，{@code initialPoints} 是开户时发放的悬赏积分
     * （教师/助教 1000、学生/管理员 0，由 {@link ReputationRulebook#initialPoints} 给出），全部计入可用部分。
     */
    public static ReputationAccount open(long userId, int initialPoints) {
        if (initialPoints < 0) {
            throw new BusinessRuleException(ERR_INVALID_POINTS, "初始悬赏积分不能为负数：" + initialPoints);
        }
        return new ReputationAccount(userId, 0, initialPoints, 0, 0L);
    }

    public static ReputationAccount reconstitute(long userId, int reputation, int availablePoints, int frozenPoints,
                                                 long version) {
        return new ReputationAccount(userId, reputation, availablePoints, frozenPoints, version);
    }

    /** 只允许通过账本流水改变声誉（由应用服务保证先写流水）。 */
    public void applyReputation(int delta) {
        if (delta == 0) {
            return;
        }
        int updated = reputation + delta;
        if (updated < 0) {
            throw new BusinessRuleException(ERR_REPUTATION_NEGATIVE,
                    "声誉不能为负：当前 " + reputation + "，增量 " + delta);
        }
        reputation = updated;
    }

    public void freezePoints(int points) {
        // TODO(S3)：实现 ReputationAccount.freezePoints——参考领域单元测试与 arc42 文档中的业务规则
        throw new UnsupportedOperationException("TODO(S3)：ReputationAccount.freezePoints 尚未实现");
    }

    /** 悬赏成功发放：冻结积分转出给获奖者（以声誉形式），发起人账户中消失。 */
    public void spendFrozen(int points) {
        // TODO(S3)：实现 ReputationAccount.spendFrozen——参考领域单元测试与 arc42 文档中的业务规则
        throw new UnsupportedOperationException("TODO(S3)：ReputationAccount.spendFrozen 尚未实现");
    }

    /** 悬赏到期：冻结积分退回可用。 */
    public void refundFrozen(int points) {
        // TODO(S3)：实现 ReputationAccount.refundFrozen——参考领域单元测试与 arc42 文档中的业务规则
        throw new UnsupportedOperationException("TODO(S3)：ReputationAccount.refundFrozen 尚未实现");
    }

    private void ensureFrozen(int points) {
        // TODO(S3)：实现 ReputationAccount.ensureFrozen——参考领域单元测试与 arc42 文档中的业务规则
        throw new UnsupportedOperationException("TODO(S3)：ReputationAccount.ensureFrozen 尚未实现");
    }

    private static void requirePositive(int points) {
        // TODO(S3)：实现 ReputationAccount.requirePositive——参考领域单元测试与 arc42 文档中的业务规则
        throw new UnsupportedOperationException("TODO(S3)：ReputationAccount.requirePositive 尚未实现");
    }

    public long userId() { return userId; }
    public int reputation() { return reputation; }
    public int availablePoints() { return availablePoints; }
    public int frozenPoints() { return frozenPoints; }
    public long version() { return version; }
}
