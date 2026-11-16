package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 查询侧的权限：档案里积分只有本人/管理员可见，流水只有本人/管理员可读（403 REP-3002）。
 * 这是角色卡里写死的硬指标，换账号点一次就要能演。
 */
class ReputationQueryAuthorizationTest {

    private static final PageRequest FIRST_PAGE = new PageRequest(1, 20);

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    private final Actor owner = new Actor(3L, "刘俊琒", Role.STUDENT);
    private final Actor other = new Actor(4L, "魏志国", Role.STUDENT);
    private final Actor admin = new Actor(1L, "管理员", Role.ADMIN);

    @Test
    @DisplayName("他人看我的流水 → 403 REP-3002")
    void ledgerOfAnotherUserIsForbiddenWithRep3002() {
        fx.register(3L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 101L);

        assertThatThrownBy(() -> fx.queries.ledger(3L, other, FIRST_PAGE))
                .isInstanceOfSatisfying(ForbiddenException.class, e -> {
                    assertThat(e.code()).isEqualTo("REP-3002");
                    assertThat(e.suggestedStatus()).isEqualTo(403);
                });
    }

    @Test
    @DisplayName("未登录（viewer 为 null）也不能看别人的流水")
    void anonymousViewerCannotReadLedger() {
        fx.register(3L, Role.STUDENT);

        assertThatThrownBy(() -> fx.queries.ledger(3L, null, FIRST_PAGE))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.code()).isEqualTo("REP-3002"));
    }

    @Test
    @DisplayName("本人能看自己的流水，字段够渲染账本列表")
    void ownerCanReadOwnLedger() {
        fx.register(3L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 101L);

        PageResult<LedgerEntryView> page = fx.queries.ledger(3L, owner, FIRST_PAGE);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(entry -> {
            assertThat(entry.delta()).isEqualTo(15);
            assertThat(entry.reason()).isEqualTo("ANSWER_ACCEPTED");
            assertThat(entry.reasonLabel()).isEqualTo("回答被采纳");
            assertThat(entry.refType()).isEqualTo("ANSWER");
            assertThat(entry.refId()).isEqualTo(101L);
        });
    }

    @Test
    @DisplayName("管理员可以看任意人的流水")
    void adminCanReadOthersLedger() {
        fx.register(3L, Role.STUDENT);
        fx.ledgerService.record(3L, "evt-acc-101", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 101L);

        assertThat(fx.queries.ledger(3L, admin, FIRST_PAGE).total()).isEqualTo(1);
    }

    @Test
    @DisplayName("他人看我的档案：声誉与徽章可见，可用/冻结积分必须是 null")
    void profileOfAnotherUserHidesPoints() {
        fx.register(9L, Role.TEACHER);
        fx.identity.register(9L, "王老师");
        fx.badgeService.grant(9L, com.campusoverflow.reputation.domain.BadgeType.FIRST_ANSWER);

        ReputationProfileView view = fx.queries.profile(9L, other);

        assertThat(view.displayName()).isEqualTo("王老师");
        assertThat(view.reputation()).isZero();
        assertThat(view.availablePoints()).isNull();
        assertThat(view.frozenPoints()).isNull();
        assertThat(view.badges()).singleElement()
                .satisfies(b -> assertThat(b.name()).isEqualTo("初来乍到"));
    }

    @Test
    @DisplayName("本人看自己的档案：积分照常返回")
    void profileOfSelfShowsPoints() {
        fx.register(9L, Role.TEACHER);

        ReputationProfileView view = fx.queries.profile(9L, new Actor(9L, "王老师", Role.TEACHER));

        assertThat(view.availablePoints()).isEqualTo(1000);
        assertThat(view.frozenPoints()).isZero();
    }

    @Test
    @DisplayName("管理员看别人的档案也能看到积分")
    void adminSeesPointsInOthersProfile() {
        fx.register(9L, Role.TEACHER);

        assertThat(fx.queries.profile(9L, admin).availablePoints()).isEqualTo(1000);
    }

    @Test
    @DisplayName("账户不存在：404 REP-3001，而不是空档案")
    void profileOfUnknownUserIsNotFound() {
        assertThatThrownBy(() -> fx.queries.profile(404L, owner))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.code()).isEqualTo("REP-3001"));
    }
}
