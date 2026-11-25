package com.campusoverflow.qa.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 事件字段顺序契约测试（S1 遗留待办，见 docs/scrum/sprint-1.md §五）。
 *
 * <p>背景：S1 曾因 AnswerSubmittedEvent / AnswerAcceptedEvent 构造参数顺序错位，导致 Reputation
 * 拿到的 questionAuthorId / answerAuthorId 互换（PR #89 修复）。record 的位置参数没有编译期保护，
 * 调整字段顺序不会报错、只会让跨上下文消费方静默错位——故用本测试把每个事件的字段顺序钉死。
 *
 * <p>修改任一事件的字段顺序（新增字段请加在末尾并保持既有位置不变）时，必须同步更新本测试，
 * 并在 PR 描述中说明对 Reputation / Discovery / 通知消费方的影响。
 */
class EventFieldOrderContractTest {

    private static List<String> componentsOf(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @Test
    @DisplayName("AnswerSubmittedEvent：eventId, occurredAt, answerId, questionId, answerAuthorId, questionAuthorId, questionTitle")
    void answerSubmittedOrder() {
        assertThat(componentsOf(AnswerSubmittedEvent.class)).containsExactly(
                "eventId", "occurredAt", "answerId", "questionId", "answerAuthorId", "questionAuthorId",
                "questionTitle");
    }

    @Test
    @DisplayName("AnswerAcceptedEvent：eventId, occurredAt, answerId, questionId, answerAuthorId, questionAuthorId, acceptedById, questionTitle")
    void answerAcceptedOrder() {
        assertThat(componentsOf(AnswerAcceptedEvent.class)).containsExactly(
                "eventId", "occurredAt", "answerId", "questionId", "answerAuthorId", "questionAuthorId",
                "acceptedById", "questionTitle");
    }

    @Test
    @DisplayName("AnswerAcceptanceClearedEvent：eventId, occurredAt, answerId, questionId")
    void answerAcceptanceClearedOrder() {
        assertThat(componentsOf(AnswerAcceptanceClearedEvent.class)).containsExactly(
                "eventId", "occurredAt", "answerId", "questionId");
    }

    @Test
    @DisplayName("AnswerEndorsedEvent：eventId, occurredAt, answerId, questionId, answerAuthorId, endorserId, questionTitle")
    void answerEndorsedOrder() {
        assertThat(componentsOf(AnswerEndorsedEvent.class)).containsExactly(
                "eventId", "occurredAt", "answerId", "questionId", "answerAuthorId", "endorserId",
                "questionTitle");
    }

    @Test
    @DisplayName("VoteCastEvent：eventId, occurredAt, targetType, targetId, targetAuthorId, voterId, questionId, oldValue, newValue")
    void voteCastOrder() {
        assertThat(componentsOf(VoteCastEvent.class)).containsExactly(
                "eventId", "occurredAt", "targetType", "targetId", "targetAuthorId", "voterId", "questionId",
                "oldValue", "newValue");
    }

    @Test
    @DisplayName("CommentPostedEvent：eventId, occurredAt, commentId, questionId, authorId, body")
    void commentPostedOrder() {
        assertThat(componentsOf(CommentPostedEvent.class)).containsExactly(
                "eventId", "occurredAt", "commentId", "questionId", "authorId", "body");
    }

    @Test
    @DisplayName("ContentDeletedEvent：eventId, occurredAt, targetType, targetId, questionId")
    void contentDeletedOrder() {
        assertThat(componentsOf(ContentDeletedEvent.class)).containsExactly(
                "eventId", "occurredAt", "targetType", "targetId", "questionId");
    }

    @Test
    @DisplayName("QuestionPostedEvent：eventId, occurredAt, questionId, authorId, courseId, title, plainText, tags")
    void questionPostedOrder() {
        assertThat(componentsOf(QuestionPostedEvent.class)).containsExactly(
                "eventId", "occurredAt", "questionId", "authorId", "courseId", "title", "plainText", "tags");
    }

    @Test
    @DisplayName("QuestionEditedEvent：eventId, occurredAt, questionId, title, plainText, tags")
    void questionEditedOrder() {
        assertThat(componentsOf(QuestionEditedEvent.class)).containsExactly(
                "eventId", "occurredAt", "questionId", "title", "plainText", "tags");
    }
}
