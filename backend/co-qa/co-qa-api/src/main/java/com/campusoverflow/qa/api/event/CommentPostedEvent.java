package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/** 评论被发布。Discovery 订阅以解析 @提及并通知（正文供提及解析）。
 *
 * 字段顺序契约（跨上下文消费方依赖位置参数，顺序由 EventFieldOrderContractTest 钉死，调整顺序必须同步该测试）：
 * eventId → occurredAt → commentId → questionId → authorId → body
 */
public record CommentPostedEvent(String eventId, Instant occurredAt, long commentId, long questionId, long authorId,
                                 String body) implements IntegrationEvent {
}
