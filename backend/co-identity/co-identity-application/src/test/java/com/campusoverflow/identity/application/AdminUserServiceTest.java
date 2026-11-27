package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.domain.PasswordHasher;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private final Actor admin = new Actor(2, "管理员", Role.ADMIN);
    private final Actor teacher = new Actor(1, "王老师", Role.TEACHER);

    private final FakeUsers users = new FakeUsers();
    private final RecordingAudit audit = new RecordingAudit();
    private final RecordingSessions sessions = new RecordingSessions();
    private final AdminUserService service = new AdminUserService(users, new StubHasher(), event -> { }, audit,
            sessions, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        users.save(User.createStaff(admin, "T2001", "王老师", "wang@campus.edu.cn", "hash", Role.TEACHER,
                "计算机工程学院", NOW));
        users.save(User.createStaff(admin, "admin01", "管理员", "admin@campus.edu.cn", "hash", Role.ADMIN,
                "信息中心", NOW));
    }

    @Test
    void onlyAdminCanDisable() {
        assertThatThrownBy(() -> service.disable(teacher, 1L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminCannotDisableSelf() {
        assertThatThrownBy(() -> service.disable(admin, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("不能禁用自己");
    }

    @Test
    void disablePersistsStatusRevokesSessionsAuditsAndBlocksLogin() {
        service.disable(admin, 1L);

        User disabled = users.findById(1L).orElseThrow();
        assertThat(disabled.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(disabled.canLogin()).isFalse();
        assertThat(sessions.revoked).containsExactly("T2001");
        assertThat(audit.actions).contains("DISABLE_USER");
    }

    @Test
    void changeRoleAlsoRevokesSessionsSoStaleRoleCannotOutliveIt() {
        // 审计魏-9：会话里固化了登录时的角色；不撤会话则变更后的权限要等会话过期才生效
        service.changeRole(admin, 1L, Role.ADMIN);

        User promoted = users.findById(1L).orElseThrow();
        assertThat(promoted.role()).isEqualTo(Role.ADMIN);
        assertThat(sessions.revoked).containsExactly("T2001");
        assertThat(audit.actions).contains("ROLE_CHANGED");
    }

    @Test
    void verifyMarksUserVerifiedAndAudits() {
        users.save(User.reconstitute(null, "2026001", "待认证", "pending@campus.edu.cn", "hash",
                Role.STUDENT, UserStatus.ACTIVE, false, "计算机工程学院", "计科2301", NOW, 0));

        service.verify(admin, 3L);

        assertThat(users.findById(3L).orElseThrow().verified()).isTrue();
        assertThat(audit.actions).contains("USER_VERIFIED");
    }

    @Test
    void createStaffPersistsVerifiedRoleAndAudits() {
        long id = service.createStaff(admin, new CreateStaffCommand("T3001", "新老师", "new@campus.edu.cn",
                "abcd1234", Role.TEACHER, "计算机工程学院"));

        User created = users.findById(id).orElseThrow();
        assertThat(created.role()).isEqualTo(Role.TEACHER);
        assertThat(created.verified()).isTrue();
        assertThat(audit.actions).contains("STAFF_CREATED");
    }

    @Test
    void listByVerificationRejectsNonAdmin() {
        assertThatThrownBy(() -> service.listByVerification(teacher, false, new PageRequest(1, 20)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminViewExposesStatusSoDisabledRowsCanBeRendered() {
        service.disable(admin, 1L);

        UserView view = AdminUserService.toView(users.findById(1L).orElseThrow());

        assertThat(view.status()).isEqualTo(UserStatus.DISABLED);
    }

    private static final class FakeUsers implements UserRepository {
        private final Map<Long, User> store = new HashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override public Optional<User> findById(long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByUsername(String username) {
            return store.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }
        @Override public Optional<User> findByDisplayName(String displayName) {
            return store.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }
        @Override public boolean existsByUsername(String username) { return findByUsername(username).isPresent(); }
        @Override public boolean existsByDisplayName(String displayName) {
            return findByDisplayName(displayName).isPresent();
        }
        @Override public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(u -> u.email().equals(email));
        }
        @Override public List<User> findAllById(Collection<Long> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }
        @Override public PageResult<User> findByVerified(boolean verified, PageRequest page) {
            return PageResult.empty(page.page(), page.size());
        }
        @Override public User save(User user) {
            if (user.id() == null) {
                user.assignId(sequence.incrementAndGet());
            }
            store.put(user.id(), user);
            return user;
        }
    }

    private static final class StubHasher implements PasswordHasher {
        @Override public String hash(String raw) { return "hash"; }
        @Override public boolean matches(String raw, String hash) { return true; }
    }

    private static final class RecordingAudit implements AuditTrail {
        final List<String> actions = new ArrayList<>();
        @Override public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            actions.add(action);
        }
    }

    private static final class RecordingSessions implements UserSessionRevoker {
        final List<String> revoked = new ArrayList<>();
        @Override public void revokeAll(String username) { revoked.add(username); }
    }
}
