package com.campusoverflow.reputation.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;

/**
 * 排行榜周期键。学期划分（可按学校校历调整）：2–7 月为春季学期，8–12 月与次年 1 月为秋季学期。
 */
public final class AcademicTerm {

    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 春季学期起始月（含）。 */
    private static final int SPRING_START_MONTH = 2;
    /** 秋季学期起始月（含）。 */
    private static final int FALL_START_MONTH = 8;

    private AcademicTerm() {
    }

    /**
     * 学期键，形如 {@code 2026-FALL}。
     *
     * <p>8–12 月归属当年的秋季学期；次年 1 月仍归属上一年的秋季学期（寒假不另立学期）；
     * 2–7 月归属当年的春季学期。全部按 {@link #CAMPUS_ZONE} 判定，避免 UTC 跨月把开学首日算进上一学期。
     */
    public static String termKey(Instant at) {
        LocalDate date = LocalDate.ofInstant(at, CAMPUS_ZONE);
        int month = date.getMonthValue();
        if (month >= FALL_START_MONTH) {
            return date.getYear() + "-FALL";
        }
        if (month < SPRING_START_MONTH) {
            return (date.getYear() - 1) + "-FALL";
        }
        return date.getYear() + "-SPRING";
    }

    /** 月键，形如 {@code 2026-10}；按校园时区切月，以北京时间零点为界。 */
    public static String monthKey(Instant at) {
        LocalDate date = LocalDate.ofInstant(at, CAMPUS_ZONE);
        return "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
    }

    /**
     * 周键，形如 {@code 2026-W41}。遵循 ISO-8601 周规则：周一为一周之首，年份取「周所属年」，
     * 因此跨年周（如 2027-01-01 属 2026-W53）不会被拆成两个周榜。
     */
    public static String weekKey(Instant at) {
        LocalDate date = LocalDate.ofInstant(at, CAMPUS_ZONE);
        int weekYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return "%04d-W%02d".formatted(weekYear, week);
    }
}
