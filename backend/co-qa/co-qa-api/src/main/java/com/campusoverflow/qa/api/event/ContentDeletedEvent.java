package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/** 问题或回答被软删除。targetType 取值 QUESTION / ANSWER。  * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → targetType → targetId → questionId
 */

public record ContentDeletedEvent(String eventId, Instant occurredAt, String targetType, long targetId,
                                  long questionId) implements IntegrationEvent {
}
