package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/** 回答被提交。Reputation 订阅以授予 FIRST_ANSWER 徽章；Discovery 订阅以回答数 +1 并通知提问者。
 *
 * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → answerId → questionId → answerAuthorId → questionAuthorId → questionTitle
 */
public record AnswerSubmittedEvent(String eventId, Instant occurredAt, long answerId, long questionId,
                                   long answerAuthorId, long questionAuthorId, String questionTitle)
        implements IntegrationEvent {
}
