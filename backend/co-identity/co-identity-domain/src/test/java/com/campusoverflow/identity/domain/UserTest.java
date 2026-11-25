package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;


class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor admin = new Actor(1, "admin", Role.ADMIN);
    private final Actor student = new Actor(2, "alice", Role.STUDENT);

    private User newStudent() {
        return User.register("2023001217", "小明", "Ming@Campus.edu", "hash", "计算机工程学院", "计科2301", NOW);
    }

    @Test
    void registerCreatesUnverifiedStudentWithNormalizedEmail() {
        User u = newStudent();
        assertThat(u.role()).isEqualTo(Role.STUDENT);
        assertThat(u.verified()).isFalse();
        assertThat(u.email()).isEqualTo("ming@campus.edu");
        assertThat(u.canLogin()).isTrue();
    }

    @Test
    void rejectsInvalidUsernameDisplayNameAndEmail() {
        assertThatThrownBy(() -> User.register("ab", "小明", "a@b.cn", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("学号");
        assertThatThrownBy(() -> User.register("20230001", "小 明", "a@b.cn", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.register("20230001", "小明", "not-an-email", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("邮箱");
    }

    @Test
    void onlyAdminCanVerifyAndOnlyOnce() {
        User u = newStudent();
        u.assignId(10);
        assertThatThrownBy(() -> u.verify(student)).isInstanceOf(ForbiddenException.class);
        u.verify(admin);
        assertThat(u.verified()).isTrue();
        assertThatThrownBy(() -> u.verify(admin)).isInstanceOf(ConflictException.class);
    }

    @Test
    void adminCannotChangeOwnRole() {
        User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                true, null, null, NOW, 0);
        assertThatThrownBy(() -> self.changeRole(admin, Role.STUDENT)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void staffAccountsAreVerifiedAndCannotBeStudents() {
        User t = User.createStaff(admin, "T2001", "王老师", "wang@campus.edu", "h", Role.TEACHER, "计算机工程学院", NOW);
        assertThat(t.verified()).isTrue();
        assertThatThrownBy(() -> User.createStaff(admin, "T2002", "李老师", "li@campus.edu", "h", Role.STUDENT, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.createStaff(student, "T2003", "赵老师", "z@campus.edu", "h", Role.TEACHER, null, NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void passwordPolicyRequiresLettersAndDigits() {
        assertThatThrownBy(() -> PasswordPolicy.validate("short1")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("onlyletters")).isInstanceOf(BusinessRuleException.class);
        PasswordPolicy.validate("Passw0rd!");
    }

    @Test
    void emailPatternIsLinearTimeAndKeepsOriginalSemantics() {
        // Sonar java:S5852 回归：语义必须保持 —— @ 后至少一段点域名
        assertThatCode(() -> User.register("20230001", "小明", "a@b.c", "h", null, null, NOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> User.register("20230001", "小明", "a@b.c.d", "h", null, null, NOW))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> User.register("20230001", "小明", "a@b", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.register("20230001", "小明", "a@b.", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> User.register("20230001", "小明", "a@.b", "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void emailPatternItselfIsLinearOnPathologicalInput() {
        // 审计 TEST-01：以前用 4005 字符输入断言"无灾难性回溯"，但 checkEmail 的 length()>100
        // 会先拒绝、正则根本不会执行 —— 那条用例只证明了长度守卫。
        // 这里改为「≤100 字符 + 直接调用 emailShapeMatches(仅正则)」，确保正则真的被执行。
        String evil = "a@" + "b.".repeat(48) + "!"; // 99 字符，旧正则在此形态上会指数回溯
        assertThat(evil.length()).isLessThanOrEqualTo(100);
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () ->
                assertThat(User.emailShapeMatches(evil)).isFalse());
        // 正常形态在同样预算内通过
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () ->
                assertThat(User.emailShapeMatches("a@b.c")).isTrue());
    }

    @Test
    void emailLengthGuardRejectsOver100Characters() {
        // 长度守卫独立成测：>100 由 checkEmail 直接拒绝（与正则无关）
        String tooLong = "a@" + "b.".repeat(50) + "c";
        assertThat(tooLong.length()).isGreaterThan(100);
        assertThatThrownBy(() -> User.register("20230001", "小明", tooLong, "h", null, null, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("邮箱");
    }
}
