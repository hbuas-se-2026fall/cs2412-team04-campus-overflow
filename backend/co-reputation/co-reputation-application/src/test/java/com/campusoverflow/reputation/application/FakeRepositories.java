package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内存版仓储，供应用层用例测试使用（不依赖 Spring / 数据库）。
 * 行为与 JPA 适配器对齐：账户按 userId 覆盖保存、流水只追加且带自增 id、徽章按 (userId, type) 去重。
 */
final class FakeAccountRepository implements ReputationAccountRepository {

    private final Map<Long, ReputationAccount> store = new LinkedHashMap<>();

    @Override
    public Optional<ReputationAccount> findByUserId(long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public List<ReputationAccount> findAllByUserId(Collection<Long> userIds) {
        return userIds.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<ReputationAccount> topByReputation(int limit) {
        return store.values().stream()
                .sorted(Comparator.comparingInt(ReputationAccount::reputation).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<ReputationAccount> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public ReputationAccount save(ReputationAccount account) {
        store.put(account.userId(), account);
        return account;
    }
}

final class FakeLedgerRepository implements LedgerRepository {

    private final List<LedgerEntry> entries = new ArrayList<>();
    private long nextId = 1;

    @Override
    public boolean exists(String eventId, long userId, ReputationReason reason) {
        return entries.stream().anyMatch(e -> e.eventId().equals(eventId)
                && e.userId() == userId && e.reason() == reason);
    }

    @Override
    public void append(LedgerEntry entry) {
        entries.add(new LedgerEntry(nextId++, entry.userId(), entry.eventId(), entry.reason(), entry.delta(),
                entry.refType(), entry.refId(), entry.createdAt()));
    }

    @Override
    public PageResult<LedgerEntry> findByUser(long userId, PageRequest page) {
        List<LedgerEntry> mine = entries.stream()
                .filter(e -> e.userId() == userId)
                .sorted(Comparator.comparing(LedgerEntry::id).reversed())
                .toList();
        int from = Math.min(page.offset(), mine.size());
        int to = Math.min(from + page.size(), mine.size());
        return new PageResult<>(mine.subList(from, to), mine.size(), page.page(), page.size());
    }

    @Override
    public long countByUserAndReason(long userId, ReputationReason reason) {
        return entries.stream().filter(e -> e.userId() == userId && e.reason() == reason).count();
    }

    @Override
    public int sumPositiveVoteDeltaSince(long userId, Instant since) {
        return entries.stream()
                .filter(e -> e.userId() == userId && e.reason().isVote() && e.delta() > 0
                        && !e.createdAt().isBefore(since))
                .mapToInt(LedgerEntry::delta)
                .sum();
    }

    @Override
    public Map<Long, Long> sumByUser() {
        Map<Long, Long> sums = new HashMap<>();
        entries.forEach(e -> sums.merge(e.userId(), (long) e.delta(), Long::sum));
        return sums;
    }

    int size(long userId) {
        return (int) entries.stream().filter(e -> e.userId() == userId).count();
    }
}

final class FakeBadgeRepository implements BadgeRepository {

    private final List<BadgeAward> awards = new ArrayList<>();

    @Override
    public boolean has(long userId, BadgeType type) {
        return awards.stream().anyMatch(a -> a.userId() == userId && a.type() == type);
    }

    @Override
    public void award(BadgeAward award) {
        awards.add(award);
    }

    @Override
    public List<BadgeAward> findByUser(long userId) {
        return awards.stream().filter(a -> a.userId() == userId).toList();
    }

    int countAwards() {
        return awards.size();
    }
}

/** Identity OHS 的桩：只提供 displayName，够档案页展示用。 */
final class FakeIdentityApi implements IdentityApi {

    private final Map<Long, UserSummary> users = new LinkedHashMap<>();

    FakeIdentityApi register(long userId, String displayName) {
        users.put(userId, new UserSummary(userId, displayName, com.campusoverflow.shared.security.Role.STUDENT,
                true, "计算机工程学院"));
        return this;
    }

    @Override
    public Optional<UserSummary> findUser(long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
        Map<Long, UserSummary> found = new LinkedHashMap<>();
        userIds.forEach(id -> {
            UserSummary summary = users.get(id);
            if (summary != null) {
                found.put(id, summary);
            }
        });
        return found;
    }

    @Override
    public Optional<UserSummary> findByDisplayName(String displayName) {
        return users.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
    }

    @Override
    public Optional<CourseSummary> findCourse(long courseId) {
        return Optional.empty();
    }

    @Override
    public boolean canManageCourse(long userId, Long courseId) {
        return false;
    }
}
