package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

/**
 * 已采纳的回答被课程管理者删除后，采纳标记被撤销。
 * Discovery 订阅以清掉搜索投影里的 accepted 标记，使问题可被重新采纳。
 * 字段顺序契约（由 EventFieldOrderContractTest 钉死）：
 * eventId → occurredAt → answerId → questionId
 */
public record AnswerAcceptanceClearedEvent(String eventId, Instant occurredAt, long answerId, long questionId)
        implements IntegrationEvent {
}
