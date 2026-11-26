package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import java.time.Clock;
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
    private final BadgeService badges;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public ReputationLedgerService(ReputationAccountRepository accounts, LedgerRepository ledger, BadgeService badges,
                                   IntegrationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
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
     * 带每日上限的投票记账（审计魏-11）：**在账户行锁内**重算当日已得正向票分，再决定本次实际入账值。
     *
     * <p>修复前的窗口：调用方先 {@code sumPositiveVoteDeltaSince} 求和、再进入本方法加载账户写入，
     * 两次并发投票可能都读到「未达上限」的旧值；行锁把「读累计 → 截断 → 改余额 → 写流水」收进同一临界区。
     *
     * <p>截断语义（明确）：正向票被上限截断为 0 时**不写流水、返回 false**（不产生 0 值流水）；
     * 负数（踩票）不受上限约束，按原值入账。
     *
     * @param rawDelta 未截断的票分变化（已由 Rulebook 的 voteDelta 算出 old/new 差值）
     * @param gainedToday 由调用方提供的「当日已得正向票分」计算函数（在锁内求值）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean recordVoteCapped(long userId, String sourceEventId, ReputationReason reason, int rawDelta,
                                    String refType, long refId, java.util.function.IntSupplier gainedToday) {
        if (rawDelta == 0) {
            return false;
        }
        // 先锁账户行、再做幂等 exists 与日累计读取：避免 exists 的普通 SELECT
        // 在 REPEATABLE READ 下先建立旧快照，锁后仍按旧快照重算日上限。
        ReputationAccount account = accounts.findByUserIdForUpdate(userId)
                .orElseGet(() -> ReputationAccount.open(userId, 0));
        if (ledger.exists(sourceEventId, userId, reason)) {
            return false;
        }
        int capped = com.campusoverflow.reputation.domain.ReputationRulebook
                .applyDailyCap(rawDelta, gainedToday.getAsInt());
        if (capped == 0) {
            return false;
        }
        account.applyReputation(capped);
        accounts.save(account);
        ledger.append(LedgerEntry.of(userId, sourceEventId, reason, capped, refType, refId, clock.instant()));
        events.publish(new ReputationChangedEvent(EventIds.next(), clock.instant(), userId, capped,
                account.reputation(), reason.name()));
        BadgePolicy.forReputation(account.reputation()).forEach(b -> badges.grant(userId, b));
        return true;
    }
}
