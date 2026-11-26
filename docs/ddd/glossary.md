# 统一语言术语表 v1.0

> 统一语言：中文在前，英文对照，标注定义与所属限界上下文。

| 中文 | 英文 | 业务定义 | 所属限界上下文 |
| ---- | ---- | ---- | ---- |
| 用户 | User | 平台注册使用者（学号/工号登录） | 身份上下文(co-identity) |
| 课程 | Course | 课程基本信息与成员关系 | 身份上下文(co-identity) |
| 问题 | Question | 用户发布、等待他人解答的提问 | 问答上下文(co-qa) |
| 回答 | Answer | 用户针对问题给出的回复内容 | 问答上下文(co-qa) |
| 评论 | Comment | 依附于问题或回答的简短留言，支持一层回复 | 问答上下文(co-qa) |
| 点赞 | Upvote | 用户对问题/回答表达认可，票值 +1 | 问答上下文(co-qa) |
| 点踩 | Downvote | 用户对问题/回答表达不认可，票值 -1 | 问答上下文(co-qa) |
| 采纳回答 | AcceptAnswer | 提问者/课程管理者选定一个回答作为正确答案 | 问答上下文(co-qa) |
| 教师认证 | EndorseAnswer | 课程教师/助教给优质回答打认证标记 | 问答上下文(co-qa) |
| 发布问题 | PublishQuestion | 用户提交一条新问题 | 问答上下文(co-qa) |
| 提交回答 | SubmitAnswer | 用户针对问题提交回答 | 问答上下文(co-qa) |
| 编辑问题 | EditQuestion | 修改已发布问题内容 | 问答上下文(co-qa) |
| 编辑回答 | EditAnswer | 修改已提交回答内容 | 问答上下文(co-qa) |
| 删除问题 | DeleteQuestion | 软删除问题；已删除问题统一 404 | 问答上下文(co-qa) |
| 删除回答 | DeleteAnswer | 软删除回答；已采纳回答由管理者删除时释放采纳关系 | 问答上下文(co-qa) |
| 声誉 | Reputation | 用户由互动行为累计的整数分；允许为负，等于账本流水之和 | 声誉上下文(co-reputation) |
| 声誉流水 | LedgerEntry | 一次不可变的声誉变更记录（代码类名 `LedgerEntry`） | 声誉上下文(co-reputation) |
| 徽章 | Badge | 用户达成条件后获得的荣誉标识；当前没有收回徽章功能 | 声誉上下文(co-reputation) |
| 授予徽章 | GrantBadge | 系统按规则自动发放徽章 | 声誉上下文(co-reputation) |
| 悬赏 | Bounty | 课程管理者（课程 TEACHER/TA，或 ADMIN）在课程问题上冻结积分，采纳后发放给回答者 | 声誉上下文(co-reputation) |
| 冻结积分 | FrozenPoints | 发起悬赏时从可用积分划入冻结账户的部分 | 声誉上下文(co-reputation) |
| 排行榜 | Leaderboard | 按周/月/学期汇总的声誉 ZSET，实现位于 Reputation | 声誉上下文(co-reputation) |
| 通知 | Notification | 系统推送给用户的消息 | 发现上下文(co-discovery) |
| 通知已读 | MarkNotificationRead | 用户查看消息标记为已读 | 发现上下文(co-discovery) |
| @提及 | Mention | 评论中用 `@昵称` 提醒站内用户 | 发现上下文(co-discovery) |
| 搜索投影 | SearchDocument | Discovery 从问题/回答事件维护的全文检索读模型 | 发现上下文(co-discovery) |
| 相关问题 | RelatedQuestion | 与源问题共享标签、按 Jaccard 相似度排序的问题 | 发现上下文(co-discovery) |
| 角色 | Role | `STUDENT` / `TA` / `TEACHER` / `ADMIN` | 身份上下文(co-identity) |
| 账号 | Account | 用户登录凭证（学号/工号 + 密码），不保存手机号 | 身份上下文(co-identity) |
| 注册 | Register | 新学生创建账号 | 身份上下文(co-identity) |
| 登录 | Login | 用户校验凭证进入系统 | 身份上下文(co-identity) |
| 权限 | Permission | 用户可执行操作的范围，由后端 `permissions` 字段下发 | 身份上下文(co-identity) |
| 聚合根 | AggregateRoot | DDD 聚合的对外访问入口 | 领域通用 |
| 限界上下文 | BoundedContext | 独立业务子域边界 | 领域通用 |
| 领域事件 | DomainEvent | 领域内发生、可被其他模块感知的事情 | 领域通用 |
| 命令 | Command | 用户发起，要求系统执行动作的请求 | 领域通用 |

> 通知、搜索、相关问题不是独立限界上下文，属于 Discovery 的通用能力；V3 统一为 4 个限界上下文：Identity、Q&A、Reputation、Discovery。
> 未实现的“声誉增加/扣减事件、收回徽章、关注、收藏、手机号绑定”不再列入统一语言，避免把规划写成现状。
