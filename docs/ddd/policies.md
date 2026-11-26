# 策略（Policy）

> 负责人：青岑（Dev-1）
> 格式：当 X 事件发生，则 Y；只列当前代码实际消费的事件。

1. 当 `UserRegisteredEvent` 发生，则 Reputation 开立声誉账户，并按角色发放初始积分。
2. 当 `QuestionPostedEvent` 发生，则 Discovery 插入搜索投影。
3. 当 `QuestionEditedEvent` 发生，则 Discovery 更新搜索投影的标题、正文纯文本与标签。
4. 当 `AnswerSubmittedEvent` 发生，则 Discovery 回答数 +1 并通知提问者；Reputation 检查“初来乍到”徽章。
5. 当 `AnswerAcceptedEvent` 发生，则 Reputation 给回答者 +15 并检查徽章、结算悬赏；Discovery 标记“已采纳”并通知回答者。
6. 当 `AnswerEndorsedEvent` 发生，则 Reputation 记“名师认可”，Discovery 通知回答者。
7. 当 `VoteCastEvent` 发生，则 Reputation 按新旧票值差计分并受每日上限约束，Discovery 同步**问题**得分；回答得分不在搜索投影中（`SearchIndexProjector` 只处理 `QUESTION` 目标）。
8. 当 `CommentPostedEvent` 发生，则 Discovery 解析 `@昵称` 并创建站内通知。
9. 当 `ContentDeletedEvent` 发生，则 Discovery 标记搜索投影删除、回答数 -1；若删除的是已采纳回答且 Q&A 已释放采纳关系，则同时清除搜索投影的 `accepted` 标记。
10. 当 `BountyOpenedEvent` / `BountyAwardedEvent` / `BountyClosedEvent` 发生，则 Discovery 维护“悬赏中”标记，并在获奖时通知；悬赏到期和自答退款由 Reputation 内部处理，统一发布 `BountyClosedEvent`。
11. 当 `ReputationChangedEvent` 发生，则 Reputation 内部 `LeaderboardService` 更新周/月/学期排行榜 ZSET（经 `ProcessedEventStore` 幂等）。
12. 当 `BadgeAwardedEvent` 发生，则 Discovery 通知徽章获得者。
