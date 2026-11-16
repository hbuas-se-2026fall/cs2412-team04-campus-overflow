package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 徽章授予（幂等：已拥有则忽略）。 */
@Service
public class BadgeService {

    private final BadgeRepository badges;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public BadgeService(BadgeRepository badges, IntegrationEventPublisher events, Clock clock) {
        this.badges = badges;
        this.events = events;
        this.clock = clock;
    }

    /**
     * 授予徽章。幂等：已持有该徽章直接返回，不再落库也不重复发 {@link BadgeAwardedEvent}。
     * 评审可能把某条 outbox_event 的 published_at 置空让它重放，因此同一个事件调两次只能有一枚徽章。
     * 数据库侧 {@code uk_badge(user_id, badge_code)} 是最后一道防线（并发授予时由唯一键兜底）。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void grant(long userId, BadgeType type) {
        if (badges.has(userId, type)) {
            return;
        }
        badges.award(new BadgeAward(userId, type, clock.instant()));
        events.publish(new BadgeAwardedEvent(EventIds.next(), clock.instant(), userId, type.name(),
                type.displayName()));
    }
}
