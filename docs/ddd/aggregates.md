# Lab 3 领域建模：聚合识别与不变式

> 负责人：青岑（Dev-1 后端视角）
> 范围：Q&A + Reputation 限界上下文
> 依据：`backend/co-qa/co-qa-domain` 与 `backend/co-reputation/co-reputation-domain`
## 1. 领域事件（实际发布）

| # | 事件（过去时） | 中文 | 所属聚合 |
|---|---|---|---|
| 1 | QuestionPosted | 问题已发布 | Question |
| 2 | QuestionEdited | 问题已编辑 | Question |
| 3 | AnswerSubmitted | 回答已提交 | Answer |
| 4 | AnswerAccepted | 回答已采纳 | Question |
| 5 | AnswerEndorsed | 回答已认证 | Answer |
| 6 | VoteCast | 投票已投出/变更 | Vote |
| 7 | CommentPosted | 评论已发布 | Comment |
| 8 | ContentDeleted | 问题/回答已删除 | Question / Answer |
| 9 | ReputationChanged | 声誉已变更 | ReputationAccount |
| 10 | BadgeAwarded | 徽章已授予 | Badge |
| 11 | BountyOpened | 悬赏已发起 | Bounty |
| 12 | BountyAwarded | 悬赏已发放 | Bounty |
| 13 | BountyClosed | 悬赏已关闭/到期 | Bounty |

时间线：QuestionPosted → AnswerSubmitted → (VoteCast / CommentPosted) → AnswerAccepted / AnswerEndorsed → ReputationChanged；悬赏走 BountyOpened → BountyAwarded / BountyClosed。

## 2. 命令-事件对照表

| 命令 | 触发角色 | 集成事件 | 聚合 | 关键规则 |
|---|---|---|---|---|
| 发布问题 | 登录用户（课程问题需成员） | QuestionPosted | Question | 标题 10–150 字，标签 1–5 个 |
| 编辑问题 | 作者/课程管理者 | QuestionEdited | Question | 仅作者或管理者 |
| 删除问题 | 提问者（无回答时）/管理者 | ContentDeleted | Question | 有回答时提问者不可删 |
| 发布回答 | 登录用户（课程问题需成员） | AnswerSubmitted | Answer | 问题必须存在且未删除 |
| 编辑回答 | 回答作者/管理者 | 无集成事件 | Answer | 仅作者或管理者；不改变搜索投影文本 |
| 删除回答 | 回答作者/管理者 | ContentDeleted | Answer | 被采纳的回答作者不可删；管理者删除时问题释放采纳关系 |
| 采纳回答 | 提问者/课程教师 | AnswerAccepted | Question | 只能采纳一个；历史脏数据允许重新采纳 |
| 认证回答 | 课程教师/助教 | AnswerEndorsed | Answer | 只能认证一次 |
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
    3. 评论正文不能为空，长度不超过 600 个字符。
    4. 只有评论作者或管理员可以删除评论；删除顶层评论时级联删除它的全部回复。

### ReputationAccount（根：ReputationAccount）

- 不变式：
  1. 声誉余额不截断到 0，必须等于该用户全部账本流水 `delta` 之和（FF-8 对账）；可以为负。
  2. 冻结积分不能超过可用积分；冻结积分只能由对应悬赏的结算或退款释放。
  3. 同一 `(event_id, user_id, reason)` 只能入账一次（幂等）。

### Bounty（根：Bounty）

- 不变式：
  1. 只有课程问题可以发起悬赏；公开问题（`course_id=null`）拒绝 `REP-1007`。
  2. 同一问题最多一个 `OPEN` 悬赏；数据库唯一索引兜底，冲突映射 `REP-2002`。
  3. 终态只有 `AWARDED` / `EXPIRED`，终态不可再变；自答只能关闭退款。

### Notification（根：Notification）

- 不变式：
  1. 通知只属于一个接收者；读取/标记已读必须校验接收者。
  2. 同一事件投影按 `(event_id, user_id, reason)`/幂等记录去重，重放不重复发通知。

### SearchIndex（根：SearchDocument）

- 不变式：
  1. 只有未删除问题出现在搜索结果；删除事件必须清理 `dis_question_tag`。
  2. 投影更新按事件顺序应用；`accepted`/`bounty_open`/`answer_count` 必须可由事件重放收敛。
## 4. 聚合与上下文归属


| 聚合 | 根实体 | 限界上下文 | Maven 模块 |
|---|---|---|---|
| Question / Answer / Vote / Comment | Question 等 | Q&A | co-qa |
| User / Course | User | Identity | co-identity |
| ReputationAccount / Badge / Bounty | ReputationAccount | Reputation | co-reputation |
| Notification / SearchIndex | Notification | Discovery | co-discovery |
| Actor / 事件契约 / 异常 / 分页 | — | Shared Kernel | co-shared-kernel |

上下文关系：

- Q&A → Identity：客户-供应商（通过 identity-api 取用户/角色，ACL 防腐）
- Q&A → Reputation：事件驱动（AnswerAccepted / VoteCast → 声誉账本）
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