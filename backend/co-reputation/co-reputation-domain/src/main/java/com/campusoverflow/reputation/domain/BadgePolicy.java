package com.campusoverflow.reputation.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * 徽章授予规则（纯函数，易于测试与扩展）。
 * <p>只覆盖「达到累计阈值」这一类徽章；{@code FIRST_ANSWER} 与 {@code TEACHER_ENDORSED} 由具体事件触发，
 * 不在此处判定。返回值为累计集合——用户达到高阈值时同时满足低阈值，调用方按「已拥有则忽略」幂等授予。</p>
 */
public final class BadgePolicy {

    /** 第一个回答被采纳。 */
    public static final int FIRST_ACCEPTED_THRESHOLD = 1;
    /** 累计 10 个回答被采纳。 */
    public static final int HELPER_10_THRESHOLD = 10;
    /** 声誉 100。 */
    public static final int REPUTATION_100_THRESHOLD = 100;
    /** 声誉 1000。 */
    public static final int REPUTATION_1000_THRESHOLD = 1000;

    private BadgePolicy() {
    }

    public static Set<BadgeType> forAcceptedCount(long acceptedCount) {
        EnumSet<BadgeType> badges = EnumSet.noneOf(BadgeType.class);
        if (acceptedCount >= FIRST_ACCEPTED_THRESHOLD) {
            badges.add(BadgeType.FIRST_ACCEPTED);
        }
        if (acceptedCount >= HELPER_10_THRESHOLD) {
            badges.add(BadgeType.HELPER_10);
        }
        return badges;
    }

    public static Set<BadgeType> forReputation(int reputation) {
        EnumSet<BadgeType> badges = EnumSet.noneOf(BadgeType.class);
        if (reputation >= REPUTATION_100_THRESHOLD) {
            badges.add(BadgeType.REPUTATION_100);
        }
        if (reputation >= REPUTATION_1000_THRESHOLD) {
            badges.add(BadgeType.REPUTATION_1000);
        }
        return badges;
    }
}
