package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：投票 / 改投 / 撤销（value ∈ {-1, 0, 1}，0 表示撤销）。
 * 发布的事件同时携带旧值与新值，下游据此计算声誉增量。
 */
@Service
public class VoteService {

    private final VoteRepository votes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final Clock clock;

    public VoteService(VoteRepository votes, QuestionRepository questions, AnswerRepository answers,
                       IdentityApi identity, IntegrationEventPublisher events, QuestionListCache listCache,
                       Clock clock) {
        this.votes = votes;
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.events = events;
        this.listCache = listCache;
        this.clock = clock;
    }

    public record VoteResult(int myVote, int scoreDelta) {
    }

    private record Target(long authorId, long questionId, Long courseId) {
    }

    @Transactional
    public VoteResult cast(Actor actor, TargetType type, long targetId, int value) {
        if (value < -1 || value > 1) {
            throw new BusinessRuleException("QA-1014", "投票值只能是 -1、0 或 1");
        }

        Target target;
        if (type == TargetType.QUESTION) {
            // 问题投票直接锁问题行；锁后的 Question 是当前读，不会命中旧快照。
            Question locked = loadQuestionForUpdate(targetId);
            target = new Target(locked.authorId(), locked.id(), locked.courseId());
        } else if (type == TargetType.ANSWER) {
            // 先用标量投影拿 questionId（不加载 Answer），锁问题行后再首次加载 Answer，
            // 避免 resolve() 预加载 + 锁后 findById 命中一级缓存，看不到并发软删除。
            long questionId = answers.findQuestionId(targetId)
                    .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在或已删除"));
            Question locked = loadQuestionForUpdate(questionId);
            Answer fresh = loadAnswer(targetId);
            target = new Target(fresh.authorId(), fresh.questionId(), locked.courseId());
        } else {
            throw new BusinessRuleException("QA-1014", "不支持的投票目标");
        }
        requireCanParticipate(target.courseId(), actor);
        if (actor.userId() == target.authorId()) {
            throw new BusinessRuleException("QA-1015", "不能给自己的内容投票");
        }

        Optional<Vote> existing = votes.find(actor.userId(), type, targetId);
        int oldValue = existing.map(Vote::value).orElse(0);
        if (oldValue == value) {
            return new VoteResult(value, 0);
        }

        int delta = value - oldValue;
        Instant now = clock.instant();

        if (value == 0) {
            existing.ifPresent(votes::delete);
        } else if (existing.isPresent()) {
            Vote vote = existing.get();
            vote.change(value, now);
            votes.save(vote);
        } else {
            votes.save(Vote.cast(actor.userId(), target.authorId(), type, targetId, value, now));
        }

        if (type == TargetType.QUESTION) {
            questions.adjustScore(targetId, delta);
            // 列表摘要包含问题得分；投票后必须作废旧缓存，否则最多 75 秒内仍显示旧票数。
            listCache.invalidateAll();
        } else {
            answers.adjustScore(targetId, delta);
        }

        events.publish(new VoteCastEvent(EventIds.next(), now, type.name(), targetId, target.authorId(),
                actor.userId(), target.questionId(), oldValue, value));
        return new VoteResult(value, delta);
    }

    private Question loadQuestionForUpdate(long questionId) {
        return questions.findByIdForUpdate(questionId)
                .filter(question -> !question.isDeleted())
                .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));
    }

    private Answer loadAnswer(long answerId) {
        return answers.findById(answerId)
                .filter(answer -> !answer.isDeleted())
                .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在或已删除"));
    }

    /** 课程问题及其回答只允许课程成员投票；公开问题不受限制。 */
    private void requireCanParticipate(Long courseId, Actor actor) {
        if (courseId == null || actor.isAdmin() || identity.isCourseMember(actor.userId(), courseId)) {
            return;
        }
        throw new ForbiddenException("QA-3007", "只有课程成员可以参与该课程的问题");
    }
}
