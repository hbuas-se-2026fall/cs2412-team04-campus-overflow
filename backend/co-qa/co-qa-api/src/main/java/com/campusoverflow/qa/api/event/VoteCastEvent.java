package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/**
 * 投票变化。oldValue/newValue ∈ {-1, 0, 1}，消费者据此计算增量（撤销、改投都可正确处理）。
 * targetType 取值 QUESTION / ANSWER。
  * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → targetType → targetId → targetAuthorId → voterId → questionId → oldValue → newValue
 */

public record VoteCastEvent(String eventId, Instant occurredAt, String targetType, long targetId, long targetAuthorId,
                            long voterId, long questionId, int oldValue, int newValue) implements IntegrationEvent {
}
