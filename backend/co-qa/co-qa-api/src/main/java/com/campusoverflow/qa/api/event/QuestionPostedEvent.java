package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;
import java.util.List;

/** 问题被发布。Discovery 订阅以插入搜索投影（plainText 供全文索引）。
 *
 * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → questionId → authorId → courseId → title → plainText → tags
 */
public record QuestionPostedEvent(String eventId, Instant occurredAt, long questionId, long authorId, Long courseId,
                                  String title, String plainText, List<String> tags) implements IntegrationEvent {
}
