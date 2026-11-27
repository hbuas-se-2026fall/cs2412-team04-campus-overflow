package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T2-07：BountyService 的独立服务层测试（外部账户/账本使用 mock）。 */
class QingcenBountyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private static final long QUESTION_ID = 10L;
    private static final long SPONSOR_ID = 9L;

    private final Actor teacher = new Actor(SPONSOR_ID, "王老师", Role.TEACHER);
    private final InMemoryBounties bounties = new InMemoryBounties();
    private final List<IntegrationEvent> events = new ArrayList<>();
    private final List<String> audits = new ArrayList<>();

    private ReputationAccountRepository accounts;
    private ReputationAccount sponsor;
    private ReputationLedgerService ledgerService;
    private QaQueryApi qa;
    private IdentityApi identity;
    private BountyService service;

    @BeforeEach
    void setUp() {
        accounts = mock(ReputationAccountRepository.class);
        sponsor = mock(ReputationAccount.class);
        ledgerService = mock(ReputationLedgerService.class);
        qa = mock(QaQueryApi.class);
        identity = mock(IdentityApi.class);

        when(accounts.findByUserId(SPONSOR_ID)).thenReturn(Optional.of(sponsor));
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "红黑树插入后为什么必须旋转？", null, false)));
        when(qa.findQuestionForUpdate(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "红黑树插入后为什么必须旋转？", null, false)));
        when(identity.canManageCourse(SPONSOR_ID, 100L)).thenReturn(true);

        IntegrationEventPublisher publisher = events::add;
        AuditTrail audit = (action, actorId, targetType, targetId, detail) -> audits.add(action);
        service = new BountyService(bounties, accounts, ledgerService, qa, identity, publisher, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void openFreezesPointsAndPublishesEvent() {
        long id = service.open(teacher, QUESTION_ID, 50, 7);

        verify(sponsor).freezePoints(50);
        verify(accounts).save(sponsor);

        Bounty saved = bounties.store.get(id);
        assertThat(saved.status()).isEqualTo(BountyStatus.OPEN);
        assertThat(saved.sponsorId()).isEqualTo(SPONSOR_ID);
        assertThat(saved.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));

        assertThat(events).hasSize(1);
        BountyOpenedEvent event = (BountyOpenedEvent) events.get(0);
        assertThat(event.bountyId()).isEqualTo(id);
        assertThat(event.questionId()).isEqualTo(QUESTION_ID);
        assertThat(event.points()).isEqualTo(50);
        assertThat(audits).containsExactly("BOUNTY_OPEN");
    }

    @Test
    void openRejectsPublicQuestionWithoutCourse() {
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, null, "没有关联课程的公开问题", null, false)));
        when(qa.findQuestionForUpdate(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, null, "没有关联课程的公开问题", null, false)));

        assertThatThrownBy(() -> service.open(teacher, QUESTION_ID, 50, 7))
                .isInstanceOf(com.campusoverflow.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("公开问题");
        verifyNoInteractions(sponsor);
    }

    @Test
    void openRejectsWhenQuestionAlreadyHasOpenBounty() {
        seedBounty(50, 7);

        assertThatThrownBy(() -> service.open(teacher, QUESTION_ID, 50, 7))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(sponsor);
    }

    @Test
    void openRejectsWhenAcceptedAnswerStillVisible() {
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "已采纳的问题", 50L, false)));
        when(qa.findQuestionForUpdate(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "已采纳的问题", 50L, false)));
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.open(teacher, QUESTION_ID, 50, 7))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("已采纳");
        verifyNoInteractions(sponsor);
    }

    @Test
    void openAllowsStaleAcceptedAnswerId() {
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "采纳回答已被删除的问题", 50L, false)));
        when(qa.findQuestionForUpdate(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "采纳回答已被删除的问题", 50L, false)));
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(false);

        long id = service.open(teacher, QUESTION_ID, 50, 7);

        assertThat(bounties.store.get(id).status()).isEqualTo(BountyStatus.OPEN);
    }

    @Test
    void openRejectsBountyOnPublicQuestion() {
        // PO 决策（2026-11-25，审计 REP-02）：悬赏只能挂在课程问题上，
        // 与 docs/final/user-guide.md「必须是该课程的问题」一致。
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, null, "不挂课程的公开提问", null, false)));
        when(qa.findQuestionForUpdate(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, null, "不挂课程的公开提问", null, false)));

        assertThatThrownBy(() -> service.open(teacher, QUESTION_ID, 50, 7))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("公开问题");
        verifyNoInteractions(sponsor);
    }

    @Test
    void settleAwardsWinnerAndRecordsLedger() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(bounty.winnerId()).isEqualTo(3L);
        assertThat(bounty.answerId()).isEqualTo(50L);
        verify(sponsor).spendFrozen(50);
        verify(ledgerService).record(3L, "event-1", ReputationReason.BOUNTY_AWARDED, 50,
                "BOUNTY", bounty.id());

        BountyAwardedEvent event = (BountyAwardedEvent) events.get(0);
        assertThat(event.winnerId()).isEqualTo(3L);
        assertThat(event.points()).isEqualTo(50);
        assertThat(audits).containsExactly("BOUNTY_AWARD");
    }

    @Test
    void settleRefundsWhenQuestionAuthorWinsOwnAnswer() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);

        service.settleOnAccept(QUESTION_ID, 50L, 1L, 1L, 1L, NOW, "event-self-answer");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        verify(sponsor, never()).spendFrozen(anyInt());
        verifyNoInteractions(ledgerService);
    }

    @Test
    void settleRefundsWhenAcceptorIsTheWinningAnswerAuthor() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 3L, NOW, "event-self-accept");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        verify(sponsor, never()).spendFrozen(anyInt());
        verifyNoInteractions(ledgerService);
    }

    @Test
    void selfWinClosesAndRefundsWithoutLedger() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);

        service.settleOnAccept(QUESTION_ID, 50L, SPONSOR_ID, 1L, 1L, NOW, "event-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(bounty.winnerId()).isNull();
        verify(sponsor).refundFrozen(50);
        verifyNoInteractions(ledgerService);

        BountyClosedEvent event = (BountyClosedEvent) events.get(0);
        assertThat(event.refundedPoints()).isEqualTo(50);
        assertThat(audits).containsExactly("BOUNTY_CLOSE");
    }

    @Test
    void settleIsNoOpWhenNoOpenBounty() {
        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-1");

        verifyNoInteractions(ledgerService);
        assertThat(events).isEmpty();
    }

    @Test
    void expireDueRefundsAllDueBounties() {
        Bounty bounty = seedBounty(50, 1);
        BountyService later = serviceAt(NOW.plus(Duration.ofDays(1)));

        int count = later.expireDue();

        assertThat(count).isEqualTo(1);
        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        assertThat(events).singleElement().isInstanceOf(BountyClosedEvent.class);
    }

    @Test
    void expireDueClosesBountyWithoutRefundWhenSponsorAccountMissing() {
        Bounty bounty = seedBounty(50, 1);
        when(accounts.findByUserId(SPONSOR_ID)).thenReturn(Optional.empty());
        BountyService later = serviceAt(NOW.plus(Duration.ofDays(1)));

        int count = later.expireDue();

        assertThat(count).isEqualTo(1);
        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor, never()).refundFrozen(anyInt());
        BountyClosedEvent event = (BountyClosedEvent) events.get(0);
        assertThat(event.refundedPoints()).isZero();
    }

    @Test
    void latestMapsSponsorDisplayName() {
        Bounty bounty = seedBounty(50, 7);
        when(identity.findUser(SPONSOR_ID)).thenReturn(Optional.of(
                new UserSummary(SPONSOR_ID, "王老师", Role.TEACHER, true, "计算机工程学院")));
        Actor author = new Actor(1L, "提问者", Role.STUDENT);

        Optional<BountyView> view = service.latest(QUESTION_ID, author);

        assertThat(view).isPresent();
        assertThat(view.get().id()).isEqualTo(bounty.id());
        assertThat(view.get().sponsorName()).isEqualTo("王老师");
        assertThat(view.get().status()).isEqualTo("OPEN");
    }

    @Test
    void latestRejectsMissingQuestion() {
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.latest(QUESTION_ID, null))
                .isInstanceOf(com.campusoverflow.shared.domain.NotFoundException.class);
    }

    @Test
    void latestRejectsAnonymousOnCourseQuestion() {
        seedBounty(50, 7);

        assertThatThrownBy(() -> service.latest(QUESTION_ID, null))
                .isInstanceOf(com.campusoverflow.shared.domain.ForbiddenException.class);
    }

    @Test
    void latestRejectsOutsiderOnCourseQuestion() {
        seedBounty(50, 7);
        Actor outsider = new Actor(2L, "路人", Role.STUDENT);
        when(identity.isCourseMember(2L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> service.latest(QUESTION_ID, outsider))
                .isInstanceOf(com.campusoverflow.shared.domain.ForbiddenException.class);
    }

    @Test
    void latestAllowsCourseMember() {
        Bounty bounty = seedBounty(50, 7);
        Actor member = new Actor(2L, "课程学生", Role.STUDENT);
        when(identity.isCourseMember(2L, 100L)).thenReturn(true);
        when(identity.findUser(SPONSOR_ID)).thenReturn(Optional.of(
                new UserSummary(SPONSOR_ID, "王老师", Role.TEACHER, true, "计算机工程学院")));

        Optional<BountyView> view = service.latest(QUESTION_ID, member);

        assertThat(view).isPresent();
        assertThat(view.get().id()).isEqualTo(bounty.id());
    }

    @Test
    void latestAllowsAnonymousOnPublicQuestion() {
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, null, "公开问题", null, false)));
        Bounty bounty = seedBounty(50, 7);
        when(identity.findUser(SPONSOR_ID)).thenReturn(Optional.of(
                new UserSummary(SPONSOR_ID, "王老师", Role.TEACHER, true, "计算机工程学院")));

        Optional<BountyView> view = service.latest(QUESTION_ID, null);

        assertThat(view).isPresent();
        assertThat(view.get().id()).isEqualTo(bounty.id());
    }

    @Test
    void settleIgnoresStaleAcceptanceForDifferentCurrentAnswer() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "问题", 99L, false)));

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-stale");

        assertThat(bounty.status()).isEqualTo(BountyStatus.OPEN);
        verifyNoInteractions(ledgerService);
    }

    @Test
    void settleIgnoresAcceptanceBeforeBountyCreation() {
        Bounty bounty = seedBounty(50, 7);

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW.minus(Duration.ofDays(1)), "event-before-bounty");

        assertThat(bounty.status()).isEqualTo(BountyStatus.OPEN);
        verifyNoInteractions(ledgerService);
    }

    @Test
    void settleIgnoresAcceptanceReleasedByAnswerDeletion() {
        Bounty bounty = seedBounty(50, 7);
        when(qa.findQuestion(QUESTION_ID)).thenReturn(Optional.of(
                new QuestionRef(QUESTION_ID, 1L, 100L, "问题", null, false)));
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(false);

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-deleted-answer");

        assertThat(bounty.status()).isEqualTo(BountyStatus.OPEN);
        verifyNoInteractions(ledgerService);
    }

    @Test
    void settleAfterExpiryRefundsInsteadOfAwarding() {
        Bounty bounty = seedBounty(50, 1);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);
        BountyService later = serviceAt(NOW.plus(Duration.ofDays(2)));

        later.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW.plus(Duration.ofDays(2)), "event-late");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        verify(sponsor, never()).spendFrozen(anyInt());
        verifyNoInteractions(ledgerService);
        assertThat(events).singleElement().isInstanceOf(BountyClosedEvent.class);
    }

    @Test
    void expireDueSkipsSnapshotThatWasAlreadyClosed() {
        Bounty closed = seedBounty(50, 1);
        closed.close(NOW.plus(Duration.ofDays(2)));

        BountyRepository repo = mock(BountyRepository.class);
        when(repo.findDue(org.mockito.ArgumentMatchers.any(), anyInt())).thenReturn(List.of(closed));
        when(repo.findByIdForUpdate(closed.id())).thenReturn(Optional.of(closed));
        BountyService later = new BountyService(repo, accounts, ledgerService, qa, identity, events::add,
                (action, actorId, targetType, targetId, detail) -> audits.add(action),
                Clock.fixed(NOW.plus(Duration.ofDays(2)), ZoneOffset.UTC));

        int count = later.expireDue();

        assertThat(count).isZero();
        verify(sponsor, never()).refundFrozen(anyInt());
    }

    @Test
    void deleteQuestionClosesOpenBountyOnceAndRefunds() {
        Bounty bounty = seedBounty(50, 7);

        service.closeForDeletedQuestion(QUESTION_ID);
        service.closeForDeletedQuestion(QUESTION_ID);

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        assertThat(events).singleElement().isInstanceOf(BountyClosedEvent.class);
    }

    @Test
    void settleAcceptedBeforeExpiryAwardsEvenIfProcessedLater() {
        Bounty bounty = seedBounty(50, 1);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);
        BountyService later = serviceAt(NOW.plus(Duration.ofDays(2)));

        later.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-before-expiry");

        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        verify(sponsor).spendFrozen(50);
        verify(ledgerService).record(3L, "event-before-expiry", ReputationReason.BOUNTY_AWARDED, 50,
                "BOUNTY", bounty.id());
    }

    @Test
    void settledBountyIsNotReturnedByExpireDue() {
        Bounty bounty = seedBounty(50, 1);
        when(qa.hasVisibleAcceptedAnswer(QUESTION_ID)).thenReturn(true);
        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-1");

        int expired = serviceAt(NOW.plus(Duration.ofDays(3))).expireDue();

        assertThat(expired).isZero();
        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        verify(sponsor).spendFrozen(50);
        verify(sponsor, never()).refundFrozen(anyInt());
    }

    @Test
    void expiredBountyCannotBeSettledAgain() {
        Bounty bounty = seedBounty(50, 1);
        serviceAt(NOW.plus(Duration.ofDays(3))).expireDue();

        service.settleOnAccept(QUESTION_ID, 50L, 3L, 1L, 1L, NOW, "event-1");

        assertThat(bounty.status()).isEqualTo(BountyStatus.EXPIRED);
        verify(sponsor).refundFrozen(50);
        verify(sponsor, never()).spendFrozen(anyInt());
        verifyNoInteractions(ledgerService);
    }

    private Bounty seedBounty(int points, int days) {
        return bounties.save(Bounty.open(QUESTION_ID, true, teacher, true, points, days, NOW));
    }

    private BountyService serviceAt(Instant now) {
        IntegrationEventPublisher publisher = events::add;
        AuditTrail audit = (action, actorId, targetType, targetId, detail) -> audits.add(action);
        return new BountyService(bounties, accounts, ledgerService, qa, identity, publisher, audit,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private static final class InMemoryBounties implements BountyRepository {
        private final Map<Long, Bounty> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Bounty> findOpenByQuestion(long questionId) {
            return store.values().stream()
                    .filter(b -> b.questionId() == questionId && b.status() == BountyStatus.OPEN)
                    .findFirst();
        }

        @Override
        public Optional<Bounty> findByIdForUpdate(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Bounty> findLatestByQuestion(long questionId) {
            return store.values().stream()
                    .filter(b -> b.questionId() == questionId)
                    .max(Comparator.comparing(Bounty::id));
        }

        @Override
        public List<Bounty> findDue(Instant now, int limit) {
            return store.values().stream().filter(b -> b.isDue(now)).limit(limit).toList();
        }

        @Override
        public Map<Long, Long> sumOpenPointsBySponsor() {
            return store.values().stream()
                    .filter(b -> b.status() == BountyStatus.OPEN)
                    .collect(java.util.stream.Collectors.groupingBy(Bounty::sponsorId,
                            java.util.stream.Collectors.summingLong(Bounty::points)));
        }

        @Override
        public Bounty save(Bounty bounty) {
            if (bounty.id() == null) {
                bounty.assignId(sequence.incrementAndGet());
            }
            store.put(bounty.id(), bounty);
            return bounty;
        }
    }
}
