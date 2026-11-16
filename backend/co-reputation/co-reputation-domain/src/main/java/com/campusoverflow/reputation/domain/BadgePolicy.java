package com.campusoverflow.reputation.domain;

import java.util.EnumSet;
import java.util.Set;

/** 徽章授予规则（纯函数，易于测试与扩展）。 */
public final class BadgePolicy {

    private BadgePolicy() {
    }

    /**
     * 按累计被采纳数计算应授予的徽章：首答被采纳给「初露锋芒」，累计 10 条给「解惑达人」。
     * 返回全集（而非仅跨越阈值的那一枚），调用方按 {@link BadgeRepository#has} 幂等授予即可。
     */
    public static Set<BadgeType> forAcceptedCount(long acceptedCount) {
        Set<BadgeType> result = EnumSet.noneOf(BadgeType.class);
        if (acceptedCount >= 1) {
            result.add(BadgeType.FIRST_ACCEPTED);
        }
        if (acceptedCount >= 10) {
            result.add(BadgeType.HELPER_10);
        }
        return result;
    }

    /** 按当前声誉值计算应授予的徽章：≥100「小有名气」，≥1000「德高望重」。 */
    public static Set<BadgeType> forReputation(int reputation) {
        Set<BadgeType> result = EnumSet.noneOf(BadgeType.class);
        if (reputation >= 100) {
            result.add(BadgeType.REPUTATION_100);
        }
        if (reputation >= 1000) {
            result.add(BadgeType.REPUTATION_1000);
        }
        return result;
    }
}
