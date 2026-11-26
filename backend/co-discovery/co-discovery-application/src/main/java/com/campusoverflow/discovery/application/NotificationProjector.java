package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.notification.MentionParser;
import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.shared.security.Role;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 通知中心：把上游事件转换为站内通知。幂等键 (sourceEventId, recipientId)。 */
@Component
public class NotificationProjector {

    private final NotificationRepository notifications;
    private final IdentityApi identity;
    private final QaQueryApi qa;

    public NotificationProjector(NotificationRepository notifications, IdentityApi identity, QaQueryApi qa) {
        this.notifications = notifications;
        this.identity = identity;
        this.qa = qa;
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        if (!questionVisible(e.questionId()) || e.answerAuthorId() == e.questionAuthorId()) {
            return;
        }
        notify(e.questionAuthorId(), NotificationType.ANSWER_RECEIVED,
                name(e.answerAuthorId()) + " 回答了你的问题：" + e.questionTitle(),
                "/questions/" + e.questionId(), e.eventId(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        if (!questionVisible(e.questionId()) || e.acceptedById() == e.answerAuthorId()) {
            return;
        }
        notify(e.answerAuthorId(), NotificationType.ANSWER_ACCEPTED,
                "你的回答被采纳：" + e.questionTitle(),
                "/questions/" + e.questionId(), e.eventId(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        if (!questionVisible(e.questionId()) || e.endorserId() == e.answerAuthorId()) {
            return;
        }
        notify(e.answerAuthorId(), NotificationType.ANSWER_ENDORSED,
                "你的回答被教师认证：" + e.questionTitle(),
                "/questions/" + e.questionId(), e.eventId(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(CommentPostedEvent e) {
        Set<String> mentioned = MentionParser.extract(e.body());
        if (mentioned.isEmpty()) {
            return;
        }
        String who = name(e.authorId());
        QuestionRef question = qa.findQuestion(e.questionId()).orElse(null);
        if (question != null && question.deleted()) {
            // 问题已删除：不再把标题和 404 链接发给被提及者。
            return;
        }
        Long courseId = question == null ? null : question.courseId();
        String title = question == null ? "一个问题" : question.title();
        for (String displayName : mentioned) {
            Optional<UserSummary> target = identity.findByDisplayName(displayName);
            if (target.isEmpty() || target.get().id() == e.authorId()) {
                continue;
            }
            // FE-12：课程问题仅课程成员/管理员可见（PO 决策 1，2026-11-25），
            // 不能把课程问题标题通过 @提及通知泄露给非成员；非成员直接不发这条通知。
            if (courseId != null && target.get().role() != Role.ADMIN
                    && !identity.isCourseMember(target.get().id(), courseId)) {
                continue;
            }
            notify(target.get().id(), NotificationType.MENTIONED,
                    who + " 在《" + title + "》的评论中提到了你",
                    "/questions/" + e.questionId(), e.eventId(), e.occurredAt());
        }
    }

    @EventListener
    @Transactional
    public void on(BountyAwardedEvent e) {
        if (!questionVisible(e.questionId())) {
            return;
        }
        notify(e.winnerId(), NotificationType.BOUNTY_AWARDED,
                "恭喜！你获得《" + questionTitle(e.questionId()) + "》的悬赏 " + e.points() + " 分",
                "/questions/" + e.questionId(), e.eventId(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(BadgeAwardedEvent e) {
        notify(e.userId(), NotificationType.BADGE_AWARDED,
                "你获得了新徽章：" + e.badgeName(),
                "/me/reputation", e.eventId(), e.occurredAt());
    }

    /**
     * 幂等写入：同一 (sourceEventId, recipientId) 只保存一条。
     * 一个事件可能通知多个人（如 @提及），他们共用同一个 eventId，靠接收者区分。
     */
    private void notify(long recipientId, NotificationType type, String title, String link, String eventId,
                        java.time.Instant at) {
        if (notifications.exists(eventId, recipientId)) {
            return;
        }
        notifications.save(Notification.create(recipientId, type, title, link, eventId, at));
    }

    /** 取昵称用于通知文案；用户被删除时降级为中性称呼，不让通知发不出去。 */
    private String name(long userId) {
        return identity.findUser(userId).map(UserSummary::displayName).orElse("某位用户");
    }

    /** 评论事件与悬赏事件不带标题，回查问答上下文的发布语言。 */
    private String questionTitle(long questionId) {
        return qa.findQuestion(questionId).filter(question -> !question.deleted())
                .map(QuestionRef::title).orElse("一个问题");
    }

    private boolean questionVisible(long questionId) {
        return qa.findQuestion(questionId).filter(question -> !question.deleted()).isPresent();
    }
}
