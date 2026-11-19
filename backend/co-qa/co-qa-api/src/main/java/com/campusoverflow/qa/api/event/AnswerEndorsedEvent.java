package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/** 教师认证了一个回答（“教师认证”标记）。  * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → answerId → questionId → answerAuthorId → endorserId → questionTitle
 */

public record AnswerEndorsedEvent(String eventId, Instant occurredAt, long answerId, long questionId,
                                  long answerAuthorId, long endorserId, String questionTitle)
        implements IntegrationEvent {
}
