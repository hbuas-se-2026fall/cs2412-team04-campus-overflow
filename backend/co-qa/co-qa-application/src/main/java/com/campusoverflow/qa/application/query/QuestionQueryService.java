package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答查询用例（读路径）。
 *
 * <p>直接读投影表，不装配聚合：列表页只需要摘要，没必要把标签、正文、计数都从聚合里加载一遍。
 * 作者信息一律批量查 Identity，避免 N+1；列表结果走 Cache-Aside，写操作会把整代缓存作废。</p>
 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;
    private static final int MAX_TAG_LIMIT = 50;
    private static final int MIN_TAG_LIMIT = 1;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    /**
     * 列表：先看缓存，未命中再查读模型；作者信息一次批量补齐后回填缓存。
     *
     * <p>课程问题仅课程成员可见；公开问题（course_id 为空）始终可见。未登录用户只看公开问题。
     * 缓存 key 带 viewerId，避免把某成员可见的课程列表返回给未加入课程的人。</p>
     */
    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page, Actor viewerOrNull) {
        boolean allCourses = viewerOrNull != null && viewerOrNull.isAdmin();
        Set<Long> visibleCourseIds = viewerOrNull == null
                ? Set.of()
                : identity.courseIdsOfUser(viewerOrNull.userId());
        if (courseId != null && !allCourses && !visibleCourseIds.contains(courseId)) {
            throw new ForbiddenException("QA-3006", "无权查看该课程的问题");
        }

        Long viewerId = viewerOrNull == null ? null : viewerOrNull.userId();
        Optional<PageResult<QuestionSummaryView>> cached = cache.get(courseId, viewerId, page);
        if (cached.isPresent()) {
            return cached.get();
        }

        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, allCourses, visibleCourseIds, page);
        List<Long> authorIds = rows.items().stream().map(QuestionRow::authorId).distinct().toList();
        Map<Long, UserSummary> authors = authorIds.isEmpty() ? Map.of() : identity.findUsers(authorIds);

        PageResult<QuestionSummaryView> result = rows.map(row -> toSummary(row, authors.get(row.authorId())));
        cache.put(courseId, viewerId, page, result);
        return result;
    }

    /** 详情：正文渲染成安全 HTML，回答按“已采纳 → 得分 → 时间”排序，评论挂到各自的宿主上。 */
    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow row = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在或已被删除：" + questionId));
        requireCanView(row, actorOrNull);

        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        Map<Long, UserSummary> users = loadUsers(row, answerRows, commentRows);
        boolean managesCourse = actorOrNull != null && identity.canManageCourse(actorOrNull.userId(), row.courseId());

        List<Long> answerIds = answerRows.stream().map(AnswerRow::id).toList();
        // 历史脏数据容错：accepted_answer_id 可能仍指向一条已被删除的回答（写入侧修复前留下的）。
        // 只有采纳 ID 仍在可见回答列表里才算已采纳，否则按未采纳处理，避免出现“已采纳但无回答可展示”
        // 的幽灵标记，并让提问者重新获得 canAccept。新数据写入侧已清空，这里只兜旧数据。
        Long effectiveAcceptedId = row.acceptedAnswerId() != null && answerIds.contains(row.acceptedAnswerId())
                ? row.acceptedAnswerId()
                : null;
        Map<String, Integer> myVotes = actorOrNull == null
                ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId, answerIds);

        List<CommentView> questionComments = new ArrayList<>();
        Map<Long, List<CommentView>> answerComments = new HashMap<>();
        for (CommentRow comment : commentRows) {
            CommentView view = toCommentView(comment, users);
            if ("ANSWER".equals(comment.targetType())) {
                answerComments.computeIfAbsent(comment.targetId(), key -> new ArrayList<>()).add(view);
            } else {
                questionComments.add(view);
            }
        }

        List<AnswerView> answers = answerRows.stream()
                .map(answer -> toAnswerView(answer, effectiveAcceptedId, users, myVotes, answerComments,
                        actorOrNull, managesCourse))
                .sorted(Comparator.comparing(AnswerView::accepted).reversed()
                        .thenComparing(Comparator.comparingInt(AnswerView::score).reversed())
                        .thenComparing(AnswerView::createdAt))
                .toList();

        // 浏览数只写 Redis，由定时任务批量落库，所以放在最后、且不影响读取结果
        viewCounter.increment(questionId);

        return new QuestionDetailView(row.id(), row.title(), row.body(), renderer.toSafeHtml(row.body()),
                row.tags(), courseRefOf(row), AuthorView.of(row.authorId(), users.get(row.authorId())),
                row.score(), row.answerCount(), row.viewCount(), effectiveAcceptedId,
                myVotes.getOrDefault("QUESTION:" + questionId, 0),
                row.createdAt(), row.updatedAt(), permissionsOf(row, effectiveAcceptedId, actorOrNull, managesCourse),
                questionComments, answers);
    }

    /** 课程问题：管理员、作者或课程成员可见；公开问题不受限制。 */
    private void requireCanView(QuestionRow row, Actor actorOrNull) {
        if (row.courseId() == null) {
            return;
        }
        if (actorOrNull == null) {
            throw new ForbiddenException("QA-3006", "无权查看此内容");
        }
        if (actorOrNull.isAdmin() || actorOrNull.is(row.authorId())
                || identity.isCourseMember(actorOrNull.userId(), row.courseId())) {
            return;
        }
        throw new ForbiddenException("QA-3006", "无权查看此内容");
    }

    /** 热门标签：limit 一律夹在 1–50，防止被传 100000 拖垮查询。 */
    public List<TagView> popularTags(int limit) {
        int safeLimit = Math.min(Math.max(limit, MIN_TAG_LIMIT), MAX_TAG_LIMIT);
        return readModel.popularTags(safeLimit);
    }

    private QuestionSummaryView toSummary(QuestionRow row, UserSummary author) {
        return new QuestionSummaryView(row.id(), row.title(), renderer.toPlainText(row.body(), EXCERPT_LENGTH),
                row.tags(), row.courseId(), AuthorView.of(row.authorId(), author), row.score(), row.answerCount(),
                row.viewCount(), row.acceptedAnswerId() != null, row.createdAt());
    }

    private AnswerView toAnswerView(AnswerRow answer, Long effectiveAcceptedId, Map<Long, UserSummary> users,
                                    Map<String, Integer> myVotes, Map<Long, List<CommentView>> answerComments,
                                    Actor actorOrNull, boolean managesCourse) {
        boolean accepted = effectiveAcceptedId != null && effectiveAcceptedId == answer.id();
        boolean canEdit = actorOrNull != null && (actorOrNull.is(answer.authorId()) || managesCourse);
        return new AnswerView(answer.id(), answer.body(), renderer.toSafeHtml(answer.body()),
                AuthorView.of(answer.authorId(), users.get(answer.authorId())), answer.score(), accepted,
                answer.endorsedBy() == null ? null : AuthorView.of(answer.endorsedBy(), users.get(answer.endorsedBy())),
                myVotes.getOrDefault("ANSWER:" + answer.id(), 0), canEdit, answer.createdAt(), answer.updatedAt(),
                answerComments.getOrDefault(answer.id(), List.of()));
    }

    private static CommentView toCommentView(CommentRow comment, Map<Long, UserSummary> users) {
        return new CommentView(comment.id(), AuthorView.of(comment.authorId(), users.get(comment.authorId())),
                comment.parentId(), comment.body(), comment.createdAt());
    }

    private CourseRefView courseRefOf(QuestionRow row) {
        // 读模型只有 course_id，课程编码与名称属于 Identity 上下文，走 OHS 取一次
        return row.courseId() == null ? null
                : identity.findCourse(row.courseId())
                        .map(course -> new CourseRefView(course.id(), course.code(), course.name()))
                        .orElseGet(() -> new CourseRefView(row.courseId(), null, null));
    }

    private static PermissionsView permissionsOf(QuestionRow row, Long effectiveAcceptedId, Actor actorOrNull,
                                                 boolean managesCourse) {
        if (actorOrNull == null) {
            return PermissionsView.NONE;
        }
        boolean author = actorOrNull.is(row.authorId());
        boolean canEdit = author || managesCourse;
        boolean canDelete = managesCourse || (author && row.answerCount() == 0);
        // 用有效采纳 ID 而非库里的原始值：脏数据指向已删回答时仍允许重新采纳。
        boolean canAccept = (author || managesCourse) && effectiveAcceptedId == null;
        return new PermissionsView(canEdit, canDelete, canAccept, managesCourse);
    }

    /** 一次收集问题、回答、回答认证人、评论作者的全部 ID，只调一次 Identity。 */
    private Map<Long, UserSummary> loadUsers(QuestionRow row, List<AnswerRow> answers, List<CommentRow> comments) {
        Set<Long> ids = new HashSet<>();
        ids.add(row.authorId());
        answers.forEach(answer -> {
            ids.add(answer.authorId());
            if (answer.endorsedBy() != null) {
                ids.add(answer.endorsedBy());
            }
        });
        comments.forEach(comment -> ids.add(comment.authorId()));
        return ids.isEmpty() ? Map.of() : identity.findUsers(ids);
    }
}
