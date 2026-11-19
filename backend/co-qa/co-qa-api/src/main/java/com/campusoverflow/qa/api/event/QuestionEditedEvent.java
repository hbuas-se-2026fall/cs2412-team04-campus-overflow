package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;
import java.util.List;

/** 问题被编辑。Discovery 订阅以更新投影的标题、正文与标签。
 *
 * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → questionId → title → plainText → tags
 */
public record QuestionEditedEvent(String eventId, Instant occurredAt, long questionId, String title, String plainText,
                                  List<String> tags) implements IntegrationEvent {
}
