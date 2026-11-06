package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 排行榜周期键的边界行为。时间点一律写 UTC，期望值按校园时区（{@code Asia/Shanghai}）判定，
 * 用来锁住「跨月/跨年瞬间归属哪个榜」这一容易出错的语义。
 */
class AcademicTermTest {

    /** 北京时区为 UTC+8：UTC 当日 16:00 即次日 00:00。 */
    private static final String LAST_SECOND_OF_DAY = "T15:59:59Z";
    private static final String FIRST_SECOND_OF_NEXT_DAY = "T16:00:00Z";

    @Nested
    @DisplayName("学期键 termKey")
    class TermKeys {

        @Test
        @DisplayName("8 月 1 日零点起算秋季学期")
        void fallStartsAtAugustFirst() {
            assertThat(AcademicTerm.termKey(Instant.parse("2026-07-31" + LAST_SECOND_OF_DAY))).isEqualTo("2026-SPRING");
            assertThat(AcademicTerm.termKey(Instant.parse("2026-07-31" + FIRST_SECOND_OF_NEXT_DAY))).isEqualTo("2026-FALL");
        }

        @Test
        @DisplayName("2 月 1 日零点起算春季学期，1 月整天仍属上一年秋季")
        void springStartsAtFebruaryFirst() {
            assertThat(AcademicTerm.termKey(Instant.parse("2027-01-31" + LAST_SECOND_OF_DAY))).isEqualTo("2026-FALL");
            assertThat(AcademicTerm.termKey(Instant.parse("2027-01-31" + FIRST_SECOND_OF_NEXT_DAY))).isEqualTo("2027-SPRING");
        }

        @Test
        @DisplayName("学期内的普通时间点归属正确")
        void termKeyWithinTerm() {
            assertThat(AcademicTerm.termKey(Instant.parse("2026-10-08T02:00:00Z"))).isEqualTo("2026-FALL");
            assertThat(AcademicTerm.termKey(Instant.parse("2026-12-31T02:00:00Z"))).isEqualTo("2026-FALL");
            assertThat(AcademicTerm.termKey(Instant.parse("2027-01-10T02:00:00Z"))).isEqualTo("2026-FALL");
            assertThat(AcademicTerm.termKey(Instant.parse("2027-03-01T02:00:00Z"))).isEqualTo("2027-SPRING");
            assertThat(AcademicTerm.termKey(Instant.parse("2027-06-30T02:00:00Z"))).isEqualTo("2027-SPRING");
        }
    }

    @Nested
    @DisplayName("月键 monthKey")
    class MonthKeys {

        @Test
        @DisplayName("按校园时区切月，而不是按 UTC 切月")
        void monthBoundaryFollowsCampusZone() {
            assertThat(AcademicTerm.monthKey(Instant.parse("2026-10-31" + LAST_SECOND_OF_DAY))).isEqualTo("2026-10");
            assertThat(AcademicTerm.monthKey(Instant.parse("2026-10-31" + FIRST_SECOND_OF_NEXT_DAY))).isEqualTo("2026-11");
        }

        @Test
        @DisplayName("月份补零，保证键可排序")
        void monthIsZeroPadded() {
            assertThat(AcademicTerm.monthKey(Instant.parse("2026-03-05T02:00:00Z"))).isEqualTo("2026-03");
        }
    }

    @Nested
    @DisplayName("周键 weekKey")
    class WeekKeys {

        @Test
        @DisplayName("遵循 ISO-8601 周编号")
        void weekKeyFollowsIsoWeek() {
            assertThat(AcademicTerm.weekKey(Instant.parse("2026-10-08T02:00:00Z"))).isEqualTo("2026-W41");
        }

        @Test
        @DisplayName("跨年周沿用周所属年，不会被拆成两个周榜")
        void crossYearWeekKeepsWeekBasedYear() {
            // 2026 年有 53 个 ISO 周；2027-01-01（周五）仍属 2026-W53，2027-01-04（周一）才进入 2027-W01
            assertThat(AcademicTerm.weekKey(Instant.parse("2026-12-31T02:00:00Z"))).isEqualTo("2026-W53");
            assertThat(AcademicTerm.weekKey(Instant.parse("2027-01-01T02:00:00Z"))).isEqualTo("2026-W53");
            assertThat(AcademicTerm.weekKey(Instant.parse("2027-01-04T02:00:00Z"))).isEqualTo("2027-W01");
        }

        @Test
        @DisplayName("周数补零，保证键可排序")
        void weekIsZeroPadded() {
            assertThat(AcademicTerm.weekKey(Instant.parse("2026-01-05T02:00:00Z"))).isEqualTo("2026-W02");
        }
    }
}
