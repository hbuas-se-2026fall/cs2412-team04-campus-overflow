package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IdentityApi identity, IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        Question question = resolveQuestion(cmd.targetType(), cmd.targetId());
        requireCanParticipate(question, actor);
        long questionId = question.id();

        Comment parent = null;
        if (cmd.parentId() != null) {
            parent = comments.findById(cmd.parentId())
                    .orElseThrow(() -> new NotFoundException("QA-4003", "父评论不存在"));
        }

        Instant now = clock.instant();
        Comment saved = comments.save(Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(),
                parent, cmd.body(), now));
        events.publish(new CommentPostedEvent(EventIds.next(), now, saved.id(), questionId, actor.userId(),
                saved.body()));
        return saved.id();
    }

    /**
     * 删除评论。只有作者或管理员可以删；删除顶层评论时级联删除它的全部回复，
     * 避免留下挂在不存在的父评论下的孤儿回复。
     */
    @Transactional
    public void delete(Actor actor, long commentId) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("QA-4003", "评论不存在"));

        if (!actor.isAdmin() && !actor.is(comment.authorId())) {
            throw new ForbiddenException("QA-1018", "只能删除自己的评论");
        }

        if (comment.parentId() == null) {
            comments.findReplies(comment.id()).forEach(comments::delete);
        }
        comments.delete(comment);
    }

    private Question resolveQuestion(TargetType type, long targetId) {
        if (type == TargetType.QUESTION) {
            return questions.findById(targetId)
                    .filter(q -> !q.isDeleted())
                    .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));
        }
        if (type == TargetType.ANSWER) {
            Answer answer = answers.findById(targetId)
                    .filter(a -> !a.isDeleted())
                    .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在"));
            return questions.findById(answer.questionId())
                    .filter(q -> !q.isDeleted())
                    .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));
        }
        throw new BusinessRuleException("QA-4003", "不支持的评论目标");
    }

    /** 课程问题及其回答只允许课程成员评论；公开问题不受限制。 */
    private void requireCanParticipate(Question question, Actor actor) {
        if (question.courseId() == null || actor.isAdmin()
                || identity.isCourseMember(actor.userId(), question.courseId())) {
            return;
        }
        throw new ForbiddenException("QA-3007", "只有课程成员可以评论该课程的问题");
    }
}
