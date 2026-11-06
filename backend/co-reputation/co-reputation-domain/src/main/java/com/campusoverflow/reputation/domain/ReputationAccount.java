package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;

/**
 * 声誉账户。不变量：reputation 恒等于该用户所有账本流水 delta 之和（由对账任务 FF-8 验证）。
 * 悬赏积分 = 可用 + 冻结，冻结部分在悬赏结算或到期时释放。
 */
public class ReputationAccount {

    /** 声誉不能为负：会使余额掉到 0 以下的变更直接拒绝，从而保证「余额 = 流水之和」在 FF-8 下仍成立。 */
    public static final String ERR_REPUTATION_NEGATIVE = "REP-1001";
    /** 可用悬赏积分不足（发起悬赏、冻结时校验）。 */
    public static final String ERR_INSUFFICIENT_AVAILABLE = "REP-1002";
    /** 积分数量非法：必须为正整数。 */
    public static final String ERR_INVALID_POINTS = "REP-1003";
    /** 冻结积分不足（结算或退款金额超过已冻结量）。 */
    public static final String ERR_INSUFFICIENT_FROZEN = "REP-1005";

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
     * 开立账户。声誉从 0 起算；{@code initialPoints} 是入学时发放的悬赏积分
     * （学生 0、教师 1000，由 {@link ReputationRulebook#initialPoints} 给出），全部计入可用部分。
     */
    public static ReputationAccount open(long userId, int initialPoints) {
        if (initialPoints < 0) {
            throw new BusinessRuleException(ERR_INVALID_POINTS, "初始悬赏积分不能为负数，实际 " + initialPoints);
        }
        return new ReputationAccount(userId, 0, initialPoints, 0, 0L);
    }

    public static ReputationAccount reconstitute(long userId, int reputation, int availablePoints, int frozenPoints,
                                                 long version) {
        return new ReputationAccount(userId, reputation, availablePoints, frozenPoints, version);
    }

    /**
     * 只允许通过账本流水改变声誉（由应用服务保证先写流水）。
     * 结果为负说明规则或幂等计算出了问题，直接拒绝，避免账户与流水出现偏差。
     */
    public void applyReputation(int delta) {
        long next = (long) reputation + delta;
        if (next < 0) {
            throw new BusinessRuleException(ERR_REPUTATION_NEGATIVE,
                    "声誉余额不能为负：当前 " + reputation + "，本次变动 " + delta);
        }
        reputation = (int) next;
    }

    /** 发起悬赏：可用积分转入冻结，总额不变。 */
    public void freezePoints(int points) {
        requirePositive(points);
        if (availablePoints < points) {
            throw new BusinessRuleException(ERR_INSUFFICIENT_AVAILABLE,
                    "可用悬赏积分不足：可用 " + availablePoints + "，需冻结 " + points);
        }
        availablePoints -= points;
        frozenPoints += points;
    }

    /** 悬赏成功发放：冻结积分转出给获奖者（以声誉形式），发起人账户中消失。 */
    public void spendFrozen(int points) {
        requirePositive(points);
        ensureFrozen(points);
        frozenPoints -= points;
    }

    /** 悬赏到期：冻结积分退回可用。 */
    public void refundFrozen(int points) {
        requirePositive(points);
        ensureFrozen(points);
        frozenPoints -= points;
        availablePoints += points;
    }

    private void ensureFrozen(int points) {
        if (frozenPoints < points) {
            throw new BusinessRuleException(ERR_INSUFFICIENT_FROZEN,
                    "冻结悬赏积分不足：已冻结 " + frozenPoints + "，需支出 " + points);
        }
    }

    private static void requirePositive(int points) {
        if (points <= 0) {
            throw new BusinessRuleException(ERR_INVALID_POINTS, "积分数量必须为正数，实际 " + points);
        }
    }

    public long userId() { return userId; }
    public int reputation() { return reputation; }
    public int availablePoints() { return availablePoints; }
    public int frozenPoints() { return frozenPoints; }
    public long version() { return version; }
}
