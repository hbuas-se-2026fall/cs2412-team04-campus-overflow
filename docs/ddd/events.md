# 领域事件目录 v1.0

> 负责人：青岑（Dev-1）
> 口径：与当前代码 `backend/**/api/event/*.java` 对齐；V3 3.5 的 `BountyOpened / BountyAwarded / BountyClosed` 合并行在代码中拆为 3 个事件。
> 说明：早期 30 事件清单包含未实现/未来事件，已作废；本文件只列实际发布并消费的事件。

## 一、时间线

Identity → Q&A → Reputation → Discovery

## 二、实际事件清单（14 个）

| # | 事件（英文，过去时） | 中文 | 发布上下文 | 聚合 | 订阅方 / 处理 |
|---|---|---|---|---|---|
| 1 | `UserRegisteredEvent` | 用户已注册 | Identity | User | Reputation 开立账户并按角色发初始积分 |
| 2 | `QuestionPostedEvent` | 问题已发布 | Q&A | Question | Discovery 插入搜索投影 |
| 3 | `QuestionEditedEvent` | 问题已编辑 | Q&A | Question | Discovery 更新投影标题/正文/标签 |
| 4 | `AnswerSubmittedEvent` | 回答已提交 | Q&A | Answer | Discovery 回答数 +1、通知提问者；Reputation 检查徽章 |
| 5 | `AnswerAcceptedEvent` | 回答已采纳 | Q&A | Question | Reputation +15、徽章、悬赏结算；Discovery 标记已采纳、通知回答者 |
| 6 | `AnswerEndorsedEvent` | 回答已认证 | Q&A | Answer | Reputation 名师认可；Discovery 通知回答者 |
| 7 | `VoteCastEvent` | 投票已投出/变更 | Q&A | Vote | Reputation 按新旧值差计分（受日上限）；Discovery 同步**问题**得分（回答得分不在搜索投影中） |
| 8 | `CommentPostedEvent` | 评论已发布 | Q&A | Comment | Discovery 解析 @提及并通知 |
| 9 | `ContentDeletedEvent` | 内容已删除 | Q&A | Question / Answer | Discovery 标记投影删除、回答数 -1；删除问题（QUESTION）时 Reputation 关闭 OPEN 悬赏并退款；已采纳回答删除时清除 accepted |
| 10 | `ReputationChangedEvent` | 声誉已变更 | Reputation | ReputationAccount | Reputation 内部排行榜更新周/月/学期 ZSET |
| 11 | `BadgeAwardedEvent` | 徽章已授予 | Reputation | Badge | Discovery 通知获得者 |
| 12 | `BountyOpenedEvent` | 悬赏已发起 | Reputation | Bounty | Discovery 维护“悬赏中”标记 |
| 13 | `BountyAwardedEvent` | 悬赏已发放 | Reputation | Bounty | Discovery 通知获奖者、清除“悬赏中” |
| 14 | `BountyClosedEvent` | 悬赏已关闭/到期 | Reputation | Bounty | Discovery 清除“悬赏中” |

## 三、明确不存在的“事件”

以下名称只出现在早期设计稿或旧版本中，当前代码没有发布者/消费者：

- `UserVerifiedEvent`、`UserRoleChangedEvent`、`UserDisabledEvent`
- `CourseCreatedEvent`、`CourseJoinedEvent`、`CourseMemberRoleChangedEvent`
- `AnswerEditedEvent`、`BountyExpiredEvent`
- `SearchDocumentIndexedEvent`、`SearchDocumentUpdatedEvent`、`SearchDocumentRemovedEvent`
- `NotificationCreatedEvent`、`NotificationReadEvent`、`MentionNotifiedEvent`
- `LeaderboardUpdatedEvent`

删除问题/回答统一使用 `ContentDeletedEvent`；悬赏到期也使用 `BountyClosedEvent`，没有单独的 `BountyExpiredEvent`。
