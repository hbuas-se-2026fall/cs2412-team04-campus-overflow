package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** on(UserRegisteredEvent)：注册即开户，教师/助教 1000 悬赏积分，学生/管理员 0，重复投递不重建。 */
class ReputationRegistrationTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("教师注册 → 开户 1000 悬赏积分，声誉仍从 0 起算")
    void teacherRegistrationOpensAccountWith1000Points() {
        fx.register(9L, Role.TEACHER);

        ReputationAccount account = fx.accounts.findByUserId(9L).orElseThrow();
        assertThat(account.availablePoints()).isEqualTo(1000);
        assertThat(account.frozenPoints()).isZero();
        assertThat(account.reputation()).isZero();
    }

    @Test
    @DisplayName("学生注册 → 开户 0 积分")
    void studentRegistrationOpensAccountWithZeroPoints() {
        fx.register(10L, Role.STUDENT);

        ReputationAccount account = fx.accounts.findByUserId(10L).orElseThrow();
        assertThat(account.availablePoints()).isZero();
        assertThat(account.reputation()).isZero();
    }

    @ParameterizedTest(name = "{0} 开户后可用积分符合角色规则")
    @EnumSource(Role.class)
    @DisplayName("四种角色开户的初始积分：TEACHER/TA=1000，STUDENT/ADMIN=0")
    void initialPointsFollowRole(Role role) {
        fx.register(11L, role);

        int expected = (role == Role.TEACHER || role == Role.TA) ? 1000 : 0;
        assertThat(fx.accounts.findByUserId(11L).orElseThrow().availablePoints()).isEqualTo(expected);
    }

    @Test
    @DisplayName("同一个注册事件投两次：账户只有一条，积分不会被重置回 1000")
    void userRegisteredEventReplayedTwiceKeepsSingleAccount() {
        UserRegisteredEvent event = fx.register(9L, Role.TEACHER);

        // 用户开户后先花掉/冻结一部分积分（这里用一次记账把声誉抬起来，模拟账户已被改动）
        fx.ledgerService.record(9L, "evt-acc-900", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 900L);

        // 评审把 outbox_event.published_at 置空，事件重放一次
        fx.handlers.on(event);
        fx.handlers.on(event);

        assertThat(fx.accounts.findAll()).hasSize(1);
        ReputationAccount account = fx.accounts.findByUserId(9L).orElseThrow();
        assertThat(account.reputation()).isEqualTo(15);
        assertThat(account.availablePoints())
                .as("重放不能把账户覆盖成新建状态")
                .isEqualTo(1000);
    }
}
