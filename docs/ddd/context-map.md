# CampusOverflow 上下文映射（Context Map）

> 负责人：青岑（Dev-1）
> 依据：《实验指导书 V3》第 3.2 节
> 口径：4 个限界上下文；事件目录以 [`events.md`](events.md) 的 14 个实际事件为准。

## 一、限界上下文

| 上下文 | 模块 | 职责 |
|---|---|---|
| Identity | `co-identity` | 用户、课程、角色、会话 |
| Q&A | `co-qa` | 问题、回答、投票、评论、采纳、软删除 |
| Reputation | `co-reputation` | 声誉账本、徽章、悬赏、排行榜 |
| Discovery | `co-discovery` | 搜索投影、相关问题、通知（不持有排行榜实现） |

> 排行榜的写模型和读模型都在 `co-reputation`：`LeaderboardService`、`RedisLeaderboardStore`、`LeaderboardController`。Discovery 只负责搜索与通知，不拥有排行榜聚合。

## 二、上下文关系

| 上游 → 下游 | 关系类型 | 集成方式 |
|---|---|---|
| Identity → Q&A / Reputation / Discovery | OHS/PL | 同步 `IdentityApi`；异步 `UserRegisteredEvent`；只暴露 `UserSummary`，下游各自 ACL 转换 |
| Q&A → Reputation | 事件驱动 + 查询契约 | `AnswerSubmitted / AnswerAccepted / VoteCast` 事件；`QaQueryApi.findQuestion` 查询问题引用 |
| Q&A → Discovery | 事件驱动 | `QuestionPosted / QuestionEdited / AnswerSubmitted / VoteCast / CommentPosted / ContentDeleted / AnswerAccepted` |
| Reputation → Discovery | 事件驱动 | `BountyOpened / BountyAwarded / BountyClosed / BadgeAwarded` |
| 全部上下文 → Shared Kernel | 共享内核 | 依赖 `co-shared-kernel`；修改需全组评审 |

## 三、共享内核

`co-shared-kernel` 提供：`Actor`、`DomainEvent`、领域异常、分页。

## 四、ACL 防腐层

- Q&A、Reputation、Discovery 在消费 `IdentityApi` 时各自实现 ACL，把上游的 `UserSummary` 转换为本地只读模型，避免上游 DTO 进入领域层。
- Reputation 在消费 `QaQueryApi.findQuestion` 时同样通过 ACL 转换 `QuestionRef`。
- ACL 不改变上表的上下游关系，只说明下游如何隔离上游模型。

## 五、说明

- 不存在独立的“检索”“通知”“内容治理”限界上下文。
- 检索、通知属于 Discovery 的能力；内容审核属于 Q&A 核心域内的业务逻辑。
- 早期“30 事件 / 排行榜归 Discovery”的口径已作废；以本文件和 `events.md` 为准。
