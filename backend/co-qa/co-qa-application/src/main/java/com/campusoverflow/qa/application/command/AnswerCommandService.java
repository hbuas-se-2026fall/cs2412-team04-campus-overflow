package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：提交 / 编辑 / 删除回答，采纳最佳答案，教师认证。 */
@Service
public class AnswerCommandService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public AnswerCommandService(QuestionRepository questions, AnswerRepository answers, IdentityApi identity,
                                IntegrationEventPublisher events, QuestionListCache listCache, AuditTrail audit,
                                Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.events = events;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long submit(Actor actor, long questionId, String body) {
        Question question = loadQuestion(questionId);
        requireCanParticipate(question, actor);

        Answer answer = Answer.submit(question, actor.userId(), MarkdownBody.ofAnswer(body), clock.instant());
        answers.save(answer);
        questions.adjustAnswerCount(questionId, 1);

        events.publish(new AnswerSubmittedEvent(
                EventIds.next(), clock.instant(),
                answer.id(), questionId, answer.authorId(), question.authorId(), question.title().value()
        ));

        listCache.invalidateAll();
        return answer.id();
    }

    @Transactional
    public void edit(Actor actor, long answerId, String body) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());

        // 修正 3：参数类型改为 MarkdownBody 和 Instant
        answer.edit(actor, manages, MarkdownBody.ofAnswer(body), clock.instant());
        answers.save(answer);
    }

    @Transactional
    public void delete(Actor actor, long answerId) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());

        boolean accepted = question.isAccepted(answer.id());
        Instant now = clock.instant();
        // 修正 4：最后一个参数改为 Instant
        answer.softDelete(actor, manages, accepted, now);
        answers.save(answer);
        questions.adjustAnswerCount(question.id(), -1);

        if (accepted) {
            // 管理者删除已采纳回答后，释放采纳关系；否则问题会永久卡在“已采纳但无答案”。
            question.clearAcceptedAnswer(answer.id(), now);
            questions.save(question);
        }

        events.publish(new ContentDeletedEvent(
                com.campusoverflow.shared.event.EventIds.next(), now,
                "ANSWER", answerId, question.id()
        ));

        audit.record("ANSWER_DELETED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
        listCache.invalidateAll();
    }

    @Transactional
    public void accept(Actor actor, long questionId, long answerId) {
        Question question = loadQuestion(questionId);
        Answer answer = loadAnswer(answerId);
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());

        question.accept(answer, actor, manages);
        questions.save(question);

        events.publish(new AnswerAcceptedEvent(
                EventIds.next(), clock.instant(),
                answerId, questionId, answer.authorId(), question.authorId(), actor.userId(), question.title().value()
        ));

        audit.record("ANSWER_ACCEPTED", actor.userId(), "ANSWER", String.valueOf(answerId), null);
        listCache.invalidateAll();
    }

    @Transactional
    public void endorse(Actor actor, long answerId) {
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean manages = identity.canManageCourse(actor.userId(), question.courseId());

        answer.endorse(actor, manages); // 如果这里报错，说明 endorse 要的参数不一样
        answers.save(answer);

        events.publish(new AnswerEndorsedEvent(
                com.campusoverflow.shared.event.EventIds.next(), clock.instant(),
                answerId, question.id(), answer.authorId(), actor.userId(), question.title().value()
        ));
    }

    private Question loadQuestion(long id) {
        return questions.findById(id)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在或已删除"));
    }

    private Answer loadAnswer(long id) {
        return answers.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在或已删除"));
    }

    /** 课程问题只有课程成员可以回答；公开问题对所有登录用户开放。 */
    private void requireCanParticipate(Question question, Actor actor) {
        if (question.courseId() == null || actor.isAdmin()
                || identity.isCourseMember(actor.userId(), question.courseId())) {
            return;
        }
        throw new ForbiddenException("QA-3007", "只有课程成员可以回答该课程的问题");
    }
}