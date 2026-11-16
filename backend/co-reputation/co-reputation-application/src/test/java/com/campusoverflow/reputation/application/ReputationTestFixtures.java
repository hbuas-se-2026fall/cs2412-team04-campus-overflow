package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;

/**
 * 声誉模块测试夹具：一套内存仓储 + 一套真实服务（BadgeService / ReputationLedgerService / 处理器 / 查询 / API），
 * 这样用例验证的是「事件进来 → 账户、流水、徽章、发件箱怎么变」，而不是某个 mock 被调用了几次。
 * 只有 BountyService 用 mock：悬赏结算是 C 的活，A 角色的处理器只负责把事件转发过去。
 */
public class ReputationTestFixtures {
    // 记录发出的事件
    public final List<IntegrationEvent> publishedEvents = new ArrayList<>();

    // 固定时间，方便断言
    public final Clock clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC);

    // 模拟事件发布器（空实现，只把事件存进列表）
    public final IntegrationEventPublisher events = event -> publishedEvents.add(event);

    public final FakeAccountRepository accounts = new FakeAccountRepository();
    public final FakeLedgerRepository ledger = new FakeLedgerRepository();
    public final FakeBadgeRepository badges = new FakeBadgeRepository();
    public final FakeIdentityApi identity = new FakeIdentityApi();
    public final BountyService bounties = Mockito.mock(BountyService.class);

    public final BadgeService badgeService;
    public final ReputationLedgerService ledgerService;
    public final ReputationEventHandlers handlers;
    public final ReputationQueryService queries;
    public final ReputationApiImpl api;

    public ReputationTestFixtures() {
        badgeService = new BadgeService(badges, events, clock);
        ledgerService = new ReputationLedgerService(accounts, ledger, badgeService, events, clock);
        handlers = new ReputationEventHandlers(accounts, ledger, ledgerService, badgeService, bounties, clock);
        queries = new ReputationQueryService(accounts, ledger, badges, identity);
        api = new ReputationApiImpl(accounts);
    }

    /** 注册事件：事件 ID 由 userId 派生，方便「同一个事件投两次」的用例复用同一个 ID。 */
    public UserRegisteredEvent register(long userId, Role role) {
        UserRegisteredEvent event = new UserRegisteredEvent("evt-reg-" + userId, clock.instant(), userId,
                "用户" + userId, role);
        handlers.on(event);
        return event;
    }

    public AnswerSubmittedEvent submitAnswer(long answerId, long answerAuthorId, long questionAuthorId) {
        AnswerSubmittedEvent event = new AnswerSubmittedEvent("evt-ans-" + answerId, clock.instant(), answerId,
                questionAuthorId * 100, answerAuthorId, questionAuthorId, "题目");
        handlers.on(event);
        return event;
    }

    public AnswerEndorsedEvent endorse(long answerId, long answerAuthorId, long endorserId) {
        AnswerEndorsedEvent event = new AnswerEndorsedEvent("evt-end-" + answerId, clock.instant(), answerId,
                1L, answerAuthorId, endorserId, "题目");
        handlers.on(event);
        return event;
    }

    public AnswerAcceptedEvent accept(long answerId, long answerAuthorId, long questionAuthorId) {
        AnswerAcceptedEvent event = new AnswerAcceptedEvent("evt-acc-" + answerId, clock.instant(), answerId,
                answerAuthorId * 100, answerAuthorId, questionAuthorId, questionAuthorId, "题目");
        handlers.on(event);
        return event;
    }

    /** 某用户当前持有的徽章代码，按授予顺序。 */
    public List<String> badgeCodes(long userId) {
        return badges.findByUser(userId).stream().map(BadgeAward::type).map(Enum::name).toList();
    }

    /** 发件箱里某类事件的条数（评审重放时要看「只发了一次」）。 */
    public long publishedCount(Class<? extends IntegrationEvent> type) {
        return publishedEvents.stream().filter(type::isInstance).count();
    }
}
