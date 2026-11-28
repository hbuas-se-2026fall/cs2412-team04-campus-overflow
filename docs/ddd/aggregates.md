# Lab 3 领域建模：聚合识别与不变式

> 负责人：青岑（Developer C）
> 范围：Q&A + Reputation 聚合，含 Identity 的 UserRegistered 事件，附 Discovery 的 Notification / SearchIndex 读模型
> 依据：`backend/co-qa/co-qa-domain`、`backend/co-reputation/co-reputation-domain`、`backend/co-identity/co-identity-domain` 与 `backend/co-discovery/co-discovery-domain`
## 1. 领域事件（实际发布，14 个）

| # | 事件（过去时） | 中文 | 所属聚合 |
|---|---|---|---|
| 1 | UserRegistered | 用户已注册 | User |
| 2 | QuestionPosted | 问题已发布 | Question |
| 3 | QuestionEdited | 问题已编辑 | Question |
| 4 | AnswerSubmitted | 回答已提交 | Answer |
| 5 | AnswerAccepted | 回答已采纳 | Question |
| 6 | AnswerEndorsed | 回答已认证 | Answer |
| 7 | VoteCast | 投票已投出/变更 | Vote |
| 8 | CommentPosted | 评论已发布 | Comment |
| 9 | ContentDeleted | 问题/回答已删除 | Question / Answer |
| 10 | ReputationChanged | 声誉已变更 | ReputationAccount |
| 11 | BadgeAwarded | 徽章已授予 | Badge |
| 12 | BountyOpened | 悬赏已发起 | Bounty |
| 13 | BountyAwarded | 悬赏已发放 | Bounty |
| 14 | BountyClosed | 悬赏已关闭/到期 | Bounty |

时间线：UserRegistered → QuestionPosted → AnswerSubmitted → (VoteCast / CommentPosted) → AnswerAccepted / AnswerEndorsed → ReputationChanged；悬赏走 BountyOpened → BountyAwarded / BountyClosed。

## 2. 命令-事件对照表

| 命令 | 触发角色 | 集成事件 | 聚合 | 关键规则 |
|---|---|---|---|---|
| 发布问题 | 登录用户（课程问题需成员） | QuestionPosted | Question | 标题 10–150 字，标签 1–5 个 |
| 编辑问题 | 作者/课程管理者 | QuestionEdited | Question | 仅作者或管理者 |
| 删除问题 | 提问者（无回答时）/管理者 | ContentDeleted | Question | 有回答时提问者不可删；Reputation 关闭该问题 OPEN 悬赏并退款 |
| 发布回答 | 登录用户（课程问题需成员） | AnswerSubmitted | Answer | 问题必须存在且未删除 |
| 编辑回答 | 回答作者/管理者 | 无集成事件 | Answer | 仅作者或管理者；不改变搜索投影文本 |
| 删除回答 | 回答作者/管理者 | ContentDeleted | Answer | 被采纳的回答作者不可删；管理者删除时问题释放采纳关系 |
| 采纳回答 | 提问者/课程管理者（TEACHER/TA/ADMIN） | AnswerAccepted | Question | 只能采纳一个；课程管理者不能采纳自己的回答（提问者自采纳允许，声誉 +0）；历史脏数据允许重新采纳 |
| 认证回答 | 课程管理者（TEACHER/TA/ADMIN） | AnswerEndorsed | Answer | 只能认证一次；不能认证自己的回答 |
| 投出/修改/撤销投票 | 登录用户（课程问题需成员） | VoteCast | Vote | 不能给自己投票，值 ±1 |
| 发表评论 | 登录用户（课程问题需成员） | CommentPosted | Comment | 同一目标下一层回复 |
| 发起悬赏 | 课程教师/助教/管理员 | BountyOpened | Bounty | 只能课程问题；同一问题仅一个 OPEN |
| 采纳结算悬赏 | 系统（AnswerAccepted 处理器） | BountyAwarded / BountyClosed | Bounty | 自答退款；正常发放给回答者 |
| 到期退款 | 定时任务 | BountyClosed | Bounty | 单条脏数据跳过，不阻断整批 |

## 3. 聚合与不变式

### Question（根：Question）

- 内部值对象：Title、MarkdownBody、Tags/TagName
- 不变式：
    1. 标题必须是 10–150 字，标签去重后必须是 1–5 个。
    2. 一个问题最多只能采纳一个回答；被采纳的回答必须属于该问题且未被删除。
    3. 已有回答的问题不能被提问者删除；已删除的问题不能再被回答。

### Answer（根：Answer）

- 不变式：
    1. 每个回答必须属于一个存在且未删除的问题。
    2. 每个回答最多只能被课程教师加精一次。
    3. 被采纳的回答不能被作者删除；只有作者或课程管理者可以编辑或删除回答。

### Vote（根：Vote）

- 不变式：
    1. 用户不能给自己的问题或回答投票。
    2. 票值只能是 +1 或 -1。
    3. 同一用户对同一目标最多只有一个有效票；重复投票是变更，不是新增。

### Comment（根：Comment）

- 不变式：
    1. 评论必须挂在与父评论相同的目标（问题或回答）下。
    2. 回复最多一层：不能回复一条已经是回复的评论。
    3. 评论正文长度为 2–600 个字符。
    4. 只有评论作者或管理员可以删除评论；删除顶层评论时级联删除它的全部回复。

### ReputationAccount（根：ReputationAccount）

- 不变式：
  1. 声誉余额不截断到 0，必须等于该用户全部账本流水 `delta` 之和（FF-8 对账）；可以为负。
  2. 冻结积分必须等于该用户全部 OPEN 悬赏的 points 之和；冻结时不得使可用积分变为负数，但 frozen 可以大于当前 available；冻结积分只能由对应悬赏的结算或退款释放。
  3. 同一 `(event_id, user_id, reason)` 只能入账一次（幂等）。
  4. 每张投票的**实际入账值**记录在 `VoteAward`（可能被日上限截断）；改投/撤销的增量必须按“新实际值 − 旧实际值”计算，不能按票面效果（赞 +10/踩 −2）回滚。

### Bounty（根：Bounty）

- 不变式：
  1. 只有课程问题可以发起悬赏；公开问题（`course_id=null`）拒绝 `REP-1007`。
  2. 同一问题最多一个 `OPEN` 悬赏；数据库唯一索引兜底，冲突映射 `REP-2002`。
  3. 终态只有 `AWARDED` / `EXPIRED`，终态不可再变；自答只能关闭退款。
  4. **异常分支**：发起人账户缺失时，`closeWithoutRefund` 会关闭悬赏但不退款（`refunded=0`，写审计/日志）；此时“冻结积分 = 全部 OPEN 悬赏”不成立，对账应报告账户缺失。

### Notification（根：Notification）

- 不变式：
  1. 通知只属于一个接收者；读取/标记已读必须校验接收者。
  2. 同一事件投影按 `(event_id, user_id, reason)`/幂等记录去重，重放不重复发通知。
  3. 问题被删除时，指向该问题的通知随 `ContentDeletedEvent(QUESTION)` 一并清理，不保留 404 链接。

### SearchIndex（根：SearchDocument）

- 不变式：
  1. 只有未删除问题出现在搜索结果；删除事件把 `dis_search_doc.deleted` 置为 TRUE，所有候选查询必须带 `deleted = FALSE`。
  2. 投影更新按事件顺序应用；`accepted`/`bounty_open`/`answer_count` 必须可由事件重放收敛。
## 4. 聚合与上下文归属


| 聚合 | 根实体 | 限界上下文 | Maven 模块 |
|---|---|---|---|
| Question / Answer / Vote / Comment | Question 等 | Q&A | co-qa |
| User / Course | User | Identity | co-identity |
| ReputationAccount | ReputationAccount | Reputation | co-reputation |
| Badge | Badge | Reputation | co-reputation |
| Bounty | Bounty | Reputation | co-reputation |
| Notification | Notification | Discovery | co-discovery |
| SearchDocument / SearchIndex | SearchDocument | Discovery | co-discovery |
| Actor / 事件契约 / 异常 / 分页 | — | Shared Kernel | co-shared-kernel |

上下文关系：

- Q&A → Identity：客户-供应商（通过 identity-api 取用户/角色，ACL 防腐）
- Q&A → Reputation：事件驱动（AnswerAccepted / VoteCast → 声誉账本；ContentDeleted(QUESTION) → 关闭 OPEN 悬赏并退款）
- Q&A → Discovery：事件驱动（问题/回答变更 → 搜索索引；回答/采纳 → 通知）
- Identity → Reputation：客户-供应商
- 全体共享 co-shared-kernel

## 5. 聚合之间的引用方式

- Question 只保存 acceptedAnswerId，不持有 Answer 对象。
- Answer 只保存 questionId / authorId，不持有 Question 或 User 对象。
- Vote 只保存 voterId / targetId / targetType。
- Comment 只保存 targetId / parentId / authorId。
- 所有跨聚合引用均通过 ID，不直接持有对象。

## 6. 待 PO 确认的问题

| 问题 | 现有代码/测试的答案 | 建议写入不变式 |
|---|---|---|
| 已采纳的回答能否取消采纳？ | 测试只允许采纳一次，代码没有取消采纳方法 | 采纳是终态，不可取消 |
| 标签是否必选？ | `requiresOneToFiveTags` 测试要求 1–5 个 | 标签必选，1–5 个 |