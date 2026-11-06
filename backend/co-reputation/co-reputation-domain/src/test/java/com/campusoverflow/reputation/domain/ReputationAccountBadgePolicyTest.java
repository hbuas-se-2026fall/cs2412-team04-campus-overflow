package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** S3 领域规则：账户积分流转与徽章阈值（边界与错误码一并覆盖）。 */
class ReputationAccountBadgePolicyTest {

    @Nested
    @DisplayName("开立账户")
    class Opening {

        @Test
        @DisplayName("新账户声誉从 0 起算，初始积分全部落在可用部分")
        void opensWithZeroReputationAndInitialPointsAvailable() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);

            assertThat(acc.userId()).isEqualTo(9);
            assertThat(acc.reputation()).isZero();
            assertThat(acc.availablePoints()).isEqualTo(1000);
            assertThat(acc.frozenPoints()).isZero();
            assertThat(acc.version()).isZero();
        }

        @Test
        @DisplayName("初始积分为负视为参数非法")
        void rejectsNegativeInitialPoints() {
            assertThatThrownBy(() -> ReputationAccount.open(9, -1))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_INVALID_POINTS));
        }
    }

    @Nested
    @DisplayName("声誉变更")
    class Applying {

        @Test
        @DisplayName("正负增量按流水累加：+10 → 8 → 0")
        void accumulatesDeltas() {
            ReputationAccount acc = ReputationAccount.open(1, 0);

            acc.applyReputation(10);
            assertThat(acc.reputation()).isEqualTo(10);

            acc.applyReputation(-2);
            assertThat(acc.reputation()).isEqualTo(8);

            acc.applyReputation(-8);
            assertThat(acc.reputation()).isZero();
        }

        @Test
        @DisplayName("会使声誉变负的变更被拒绝，且余额保持不变")
        void rejectsNegativeBalance() {
            ReputationAccount acc = ReputationAccount.open(1, 0);

            assertThatThrownBy(() -> acc.applyReputation(-1))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_REPUTATION_NEGATIVE));
            assertThat(acc.reputation()).isZero();
        }

        @Test
        @DisplayName("增量为 0 是合法空操作（应用层已提前忽略，领域层仍保持幂等语义）")
        void zeroDeltaIsNoOp() {
            ReputationAccount acc = ReputationAccount.reconstitute(1, 42, 0, 0, 0);
            acc.applyReputation(0);

            assertThat(acc.reputation()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("悬赏积分：冻结 → 结算 / 退款")
    class Points {

        @Test
        @DisplayName("冻结只在可用与冻结之间搬移，总额不变")
        void freezeMovesPointsWithoutChangingTotal() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);
            acc.freezePoints(500);

            assertThat(acc.availablePoints()).isEqualTo(500);
            assertThat(acc.frozenPoints()).isEqualTo(500);
            assertThat(acc.availablePoints() + acc.frozenPoints()).isEqualTo(1000);
        }

        @Test
        @DisplayName("冻结金额超过可用积分时拒绝，账户状态不变")
        void rejectsFreezeBeyondAvailable() {
            ReputationAccount acc = ReputationAccount.open(9, 100);
            acc.freezePoints(60);

            assertThatThrownBy(() -> acc.freezePoints(41))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_INSUFFICIENT_AVAILABLE));
            assertThat(acc.availablePoints()).isEqualTo(40);
            assertThat(acc.frozenPoints()).isEqualTo(60);
        }

        @ParameterizedTest(name = "冻结 {0} 点应被拒绝")
        @ValueSource(ints = {0, -1, -50})
        @DisplayName("非正数积分不构成有效操作")
        void rejectsNonPositivePoints(int points) {
            ReputationAccount acc = ReputationAccount.open(9, 100);

            assertThatThrownBy(() -> acc.freezePoints(points))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_INVALID_POINTS));
            assertThat(acc.availablePoints()).isEqualTo(100);
        }

        @Test
        @DisplayName("悬赏发放：冻结部分转出，可用部分不受影响")
        void spendFrozenOnlyConsumesFrozen() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);
            acc.freezePoints(300);

            acc.spendFrozen(300);

            assertThat(acc.frozenPoints()).isZero();
            assertThat(acc.availablePoints()).isEqualTo(700);
        }

        @Test
        @DisplayName("超出冻结量的结算被拒绝")
        void rejectsSpendBeyondFrozen() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);
            acc.freezePoints(50);

            assertThatThrownBy(() -> acc.spendFrozen(51))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_INSUFFICIENT_FROZEN));
            assertThat(acc.frozenPoints()).isEqualTo(50);
            assertThat(acc.availablePoints()).isEqualTo(950);
        }

        @Test
        @DisplayName("悬赏到期：冻结全额退回可用")
        void refundReturnsPointsToAvailable() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);
            acc.freezePoints(300);

            acc.refundFrozen(300);

            assertThat(acc.frozenPoints()).isZero();
            assertThat(acc.availablePoints()).isEqualTo(1000);
        }

        @Test
        @DisplayName("超出冻结量的退款同样被拒绝")
        void rejectsRefundBeyondFrozen() {
            ReputationAccount acc = ReputationAccount.open(9, 1000);

            assertThatThrownBy(() -> acc.refundFrozen(1))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            e -> assertThat(e.code()).isEqualTo(ReputationAccount.ERR_INSUFFICIENT_FROZEN));
        }
    }

    @Nested
    @DisplayName("徽章阈值")
    class Badges {

        @ParameterizedTest(name = "累计采纳 {0} 次 → {1}")
        @CsvSource({
                "0, 0",
                "1, 1",
                "9, 1",
                "10, 2",
                "11, 2",
                "100, 2"
        })
        @DisplayName("第 1 次采纳给「初露锋芒」，第 10 次再给「解惑达人」，之后不重复")
        void acceptedCountThresholds(long acceptedCount, int expectedSize) {
            assertThat(BadgePolicy.forAcceptedCount(acceptedCount)).hasSize(expectedSize);
            if (acceptedCount >= 1) {
                assertThat(BadgePolicy.forAcceptedCount(acceptedCount)).contains(BadgeType.FIRST_ACCEPTED);
            }
            if (acceptedCount >= 10) {
                assertThat(BadgePolicy.forAcceptedCount(acceptedCount)).contains(BadgeType.HELPER_10);
            }
        }

        @ParameterizedTest(name = "声誉 {0} → {1} 枚")
        @CsvSource({
                "0, 0",
                "99, 0",
                "100, 1",
                "999, 1",
                "1000, 2",
                "5000, 2"
        })
        @DisplayName("100 分给「小有名气」，1000 分再给「德高望重」")
        void reputationThresholds(int reputation, int expectedSize) {
            assertThat(BadgePolicy.forReputation(reputation)).hasSize(expectedSize);
            if (reputation >= 100) {
                assertThat(BadgePolicy.forReputation(reputation)).contains(BadgeType.REPUTATION_100);
            }
            if (reputation >= 1000) {
                assertThat(BadgePolicy.forReputation(reputation)).contains(BadgeType.REPUTATION_1000);
            }
        }

        @Test
        @DisplayName("负的累计值与声誉不会产生徽章")
        void negativeInputsYieldNoBadges() {
            assertThat(BadgePolicy.forAcceptedCount(-1)).isEmpty();
            assertThat(BadgePolicy.forReputation(-1)).isEmpty();
        }
    }
}
