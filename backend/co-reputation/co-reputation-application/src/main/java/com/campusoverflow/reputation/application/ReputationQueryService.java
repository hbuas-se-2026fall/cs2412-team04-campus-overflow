package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationQueryService {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final BadgeRepository badges;
    private final IdentityApi identity;

    public ReputationQueryService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                  BadgeRepository badges, IdentityApi identity) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.badges = badges;
        this.identity = identity;
    }

    /**
     * 声誉档案。声誉值与徽章是公开的（排行榜、主页都要用），但积分属于个人数据：
     * 只有本人与管理员能看到 availablePoints / frozenPoints，其他人拿到的是 null（前端必须判空）。
     */
    public ReputationProfileView profile(long userId, Actor viewerOrNull) {
        ReputationAccount account = accounts.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("REP-3001", "声誉账户不存在：" + userId));
        boolean seePoints = viewerOrNull != null && (viewerOrNull.is(userId) || viewerOrNull.isAdmin());
        String displayName = identity.findUser(userId).map(UserSummary::displayName).orElse(null);
        List<ReputationProfileView.BadgeView> badges = this.badges.findByUser(userId).stream()
                .sorted(Comparator.comparing(BadgeAward::awardedAt).reversed())
                .map(b -> new ReputationProfileView.BadgeView(b.type().name(), b.type().displayName(),
                        b.type().description(), b.awardedAt()))
                .toList();
        return new ReputationProfileView(userId, displayName, account.reputation(),
                seePoints ? account.availablePoints() : null,
                seePoints ? account.frozenPoints() : null,
                badges);
    }

    /** 声誉流水属于个人数据：仅本人与管理员可查看，其余一律 403 REP-3002。 */
    public PageResult<LedgerEntryView> ledger(long userId, Actor viewer, PageRequest page) {
        if (viewer == null || !(viewer.is(userId) || viewer.isAdmin())) {
            throw new ForbiddenException("REP-3002", "只能查看自己的声誉流水");
        }
        return ledger.findByUser(userId, page)
                .map(e -> new LedgerEntryView(e.id() == null ? 0L : e.id(), e.reason().name(),
                        e.reason().label(), e.delta(), e.refType(), e.refId(), e.createdAt()));
    }
}
