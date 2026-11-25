package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.search.RelatedQuestionPolicy;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：搜索与筛选、相关问题推荐。 */
@Service
@Transactional(readOnly = true)
public class DiscoveryQueryService {

    private final SearchPort search;
    private final SearchIndex index;
    private final IdentityApi identity;

    public DiscoveryQueryService(SearchPort search, SearchIndex index, IdentityApi identity) {
        this.search = search;
        this.index = index;
        this.identity = identity;
    }

    private static final int CANDIDATE_LIMIT = 50;

    /** 未登录入口：只看公开问题。 */
    public PageResult<SearchResultView> search(SearchQuery query) {
        return search(query, null);
    }

    /** 登录入口：公开问题 + 当前用户已加入课程的问题；管理员可看全部。 */
    public PageResult<SearchResultView> search(SearchQuery query, Actor viewerOrNull) {
        boolean allCourses = viewerOrNull != null && viewerOrNull.isAdmin();
        Set<Long> visibleCourseIds = viewerOrNull == null ? Set.of() : identity.courseIdsOfUser(viewerOrNull.userId());
        if (query.courseId() != null && !allCourses && !visibleCourseIds.contains(query.courseId())) {
            throw new ForbiddenException("DIS-3002", "无权查看该课程的问题");
        }
        SearchQuery scoped = new SearchQuery(query.keyword(), query.courseId(), query.tag(), query.sort(),
                query.unansweredOnly(), query.bountyOnly(), query.page(), allCourses, visibleCourseIds);
        PageResult<SearchHit> hits = search.search(scoped);
        Map<Long, UserSummary> authors = identity.findUsers(hits.items().stream()
                .map(SearchHit::authorId)
                .collect(java.util.stream.Collectors.toSet()));
        return hits.map(hit -> toView(hit, authors.get(hit.authorId())));
    }

    /** 未登录入口：相关问题只返回公开问题。 */
    public List<RelatedQuestionView> related(long questionId, int limit) {
        return related(questionId, limit, null);
    }

    /** 相关问题也按课程可见范围过滤，避免从搜索/详情侧绕过成员校验。 */
    public List<RelatedQuestionView> related(long questionId, int limit, Actor viewerOrNull) {
        if (limit <= 0) {
            return List.of();
        }
        List<String> ownTags = index.findTags(questionId);
        if (ownTags == null || ownTags.isEmpty()) {
            return List.of();
        }
        int ownTagCount = ownTags.size();
        boolean allCourses = viewerOrNull != null && viewerOrNull.isAdmin();
        Set<Long> visibleCourseIds = viewerOrNull == null ? Set.of() : identity.courseIdsOfUser(viewerOrNull.userId());
        List<SearchIndex.RelatedCandidate> visibleCandidates =
                index.findCandidatesSharingTags(questionId, ownTags, CANDIDATE_LIMIT).stream()
                        .filter(candidate -> allCourses || candidate.courseId() == null
                                || visibleCourseIds.contains(candidate.courseId()))
                        .toList();
        return RelatedQuestionPolicy.rank(visibleCandidates, ownTagCount, limit).stream()
                .map(candidate -> new RelatedQuestionView(candidate.questionId(), candidate.title(),
                        candidate.answerCount(), candidate.accepted(),
                        RelatedQuestionPolicy.jaccard(candidate.sharedTags(), ownTagCount, candidate.tagCount())))
                .toList();
    }

    private SearchResultView toView(SearchHit hit, UserSummary user) {
        SearchResultView.Author author = user == null
                ? new SearchResultView.Author(0, "未知用户", "STUDENT", false)
                : new SearchResultView.Author(user.id(), user.displayName(), user.role().name(), user.verified());
        return new SearchResultView(hit.questionId(), hit.title(), hit.snippet(), hit.tags(), hit.courseId(),
                author, hit.score(), hit.answerCount(), hit.accepted(), hit.bountyOpen(), hit.createdAt());
    }
}
