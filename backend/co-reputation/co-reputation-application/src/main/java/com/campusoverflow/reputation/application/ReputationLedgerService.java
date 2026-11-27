package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationRulebook;
import com.campusoverflow.reputation.domain.VoteAward;
import com.campusoverflow.reputation.domain.VoteAwardRepository;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 声誉记账核心：先查重（幂等）→ 改余额 → 追加流水 → 发布 ReputationChanged。
 * 余额与流水在同一事务中提交，保证“余额 = 流水之和”。
 */
@Service
public class ReputationLedgerService {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final VoteAwardRepository voteAwards;
    private final BadgeService badges;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public ReputationLedgerService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                   VoteAwardRepository voteAwards, BadgeService badges,
                                   IntegrationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.voteAwards = voteAwards;
        this.badges = badges;
        this.events = events;
        this.clock = clock;
    }

    /** @return true 表示本次确实记账；false 表示重复事件已被忽略或增量为 0。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(long userId, String sourceEventId, ReputationReason reason, int delta, String refType,
                          long refId) {
        if (delta == 0 || ledger.exists(sourceEventId, userId, reason)) {
            return false;
        }
        ReputationAccount account = accounts.findByUserId(userId).orElseGet(() -> ReputationAccount.open(userId, 0));
        account.applyReputation(delta);
        accounts.save(account);
        ledger.append(LedgerEntry.of(userId, sourceEventId, reason, delta, refType, refId, clock.instant()));
        events.publish(new ReputationChangedEvent(EventIds.next(), clock.instant(), userId, delta,
                account.reputation(), reason.name()));
        BadgePolicy.forReputation(account.reputation()).forEach(b -> badges.grant(userId, b));
        return true;
    }

    /**
     * 带每日上限的投票记账（审计魏-11 + P5）：**在账户行锁内**读取该投票的实际入账值、
     * 计算当日其他正向票分，再决定本次实际入账值，并把“当前实际入账值”持久化到
     * {@code rep_vote_award}。
     *
     * <p>为什么不能直接用 old/new 票面差：赞 +10 可能被日上限截断为 0 或部分值；若之后撤销/改踩
     * 仍按票面差回滚（-10/-12），会把从未入账的分扣掉，甚至可被“赞→撤→赞→撤”无限刷负。
     * 因此改投/撤销的增量 = 新实际入账值 − 上一次实际入账值。</p>
     *
     * <p>兼容旧数据：没有 {@code rep_vote_award} 行的历史投票按票面效果回退一次；新投票从本版本起
     * 都会写入该行（包括被截断为 0 的情况）。</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean recordVoteCapped(long userId, String sourceEventId, ReputationReason reason,
                                    int oldValue, int newValue, long voterId, String targetType, long targetId) {
        // 先锁账户行、再做幂等 exists 与日累计读取：避免 exists 的普通 SELECT
        // 在 REPEATABLE READ 下先建立旧快照，锁后仍按旧快照重算日上限。
        ReputationAccount account = accounts.findByUserIdForUpdate(userId)
                .orElseGet(() -> ReputationAccount.open(userId, 0));
        if (ledger.exists(sourceEventId, userId, reason)) {
            return false;
        }
        Instant now = clock.instant();
        Instant todayStart = LocalDate.now(clock.withZone(ZoneOffset.UTC))
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Optional<VoteAward> prior = voteAwards.find(voterId, targetType, targetId);
        // 无历史行 = 旧版本投票或首次投票：首次投票 oldValue=0 → 0；旧投票按票面效果回退一次。
        int previousApplied = prior.map(VoteAward::appliedDelta)
                .orElseGet(() -> ReputationRulebook.voteEffect(oldValue));
        int newEffect = ReputationRulebook.voteEffect(newValue);
        // 其他投票今天的正向入账；当前投票的旧正向值要先从累计里扣除，避免重复计算。
        int positiveToday = ledger.sumPositiveVoteDeltaSince(userId, todayStart);
        int priorPositiveToday = prior.filter(a -> !a.appliedAt().isBefore(todayStart) && a.appliedDelta() > 0)
                .map(VoteAward::appliedDelta).orElse(0);
        int otherPositive = Math.max(0, positiveToday - priorPositiveToday);
        int newApplied = newEffect > 0
                ? Math.min(newEffect, Math.max(0, ReputationRulebook.DAILY_VOTE_CAP - otherPositive))
                : newEffect;
        int delta = newApplied - previousApplied;

        if (delta != 0) {
            account.applyReputation(delta);
            accounts.save(account);
            ledger.append(LedgerEntry.of(userId, sourceEventId, reason, delta, targetType, targetId, now));
            events.publish(new ReputationChangedEvent(EventIds.next(), now, userId, delta,
                    account.reputation(), reason.name()));
            BadgePolicy.forReputation(account.reputation()).forEach(b -> badges.grant(userId, b));
        }
        // 即使 delta=0（被上限截断/撤销了从未入账的赞）也要记录当前实际入账值，
        // 否则下一次改投无法判断应回滚多少。
        voteAwards.save(new VoteAward(voterId, targetType, targetId, userId, newApplied, now));
        return delta != 0;
    }
}
