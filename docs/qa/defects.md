# v1.0 缺陷与差距清单（含 Sprint 0 历史）

> 负责人：青岑（QA）
> 记录规则：严重程度（高/中/低）、复现步骤、预期、实际、负责人、状态
> 说明：部分条目基于仓库与 GitHub 页面静态核查；2026-11-15 由陈明哲复核，2026-11-25 由青岑按最新 develop 重新审计。

## 一、Sprint 0 历史条目

| 编号 | 严重程度 | 模块 | 现象 | 复现/证据 | 预期 | 实际 | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|
| BUG-001 | 高 | 课程页 | 前端没有课程页，无法在 UI 完成课程列表/我的课程/加入课程 | `frontend/src/App.tsx` 已注册 `/courses`、`/courses/mine`；页面在 `frontend/src/pages/courses/`，PR #81 合入 | 存在课程页，支持列表/我的课程/加入 | 已具备 | 房国栋 | 已修复 |
| BUG-002 | 中 | CI | PR 上没有 CI 检查运行 | `.github/workflows/ci.yml` 已在 `pull_request` 上触发；PR #96、#101、#103 的 Checks 均运行且为绿 | PR 触发 CI 并显示检查结果 | 已触发并显示 | 刘俊琒 | 已修复 |
| BUG-003 | 中 | 分支保护 | develop 允许管理员绕过，main/develop 未启用必需状态检查 | 2026-11-17 陈明哲启用规则集 #27436601；刘俊琒按 API 复核 `bypass_actors` 为空、PR #139/#140 check-runs success | 不允许绕过；必需 CI 检查开启 | 已开启 | 刘俊琒/魏志国 | 已修复 |
| BUG-004 | 中 | 文档一致性 | 早期事件风暴图/目录写 30 事件，与代码不一致 | 2026-11-25 重写 `events.md` 为 14 个实际事件；2026-11-25 重绘 `event-storming.png` 为 14 事件；`01-architecture.md` 同步改为 14 事件 | 4 个上下文；事件目录与代码一致 | 文档与图均已改为 14 事件 | 青岑 | 已修复 |
| BUG-005 | 低 | 流程 | 房国栋提交使用 baigui 身份，Contributors 只有 4 人 | 新提交已绑定 GitHub 账号 2177023844；历史提交保留 | 提交邮箱绑定 GitHub 账号 | 新提交已绑定 | 房国栋 | 已修复（新提交） |
| BUG-006 | 低 | 文档 | `docs/team.md`、`docs/team-agreement.md`、ADR-009 缺失 | 三个文件均已存在 | V3 C2/C3/C4/A4 要求存在 | 均存在 | 陈明哲/刘俊琒 | 已修复 |

## 二、2026-11-25 审计（青岑负责项）

> 范围：Q&A、Discovery、Reputation 应用层、QA/DDD/Scrum 文档。其他成员的 W/C/L/F/R 项由对应负责人在各自分支处理。
> 最新 develop 已由队友修掉 Q-02（#225）与 Q-01（PO 修订为“课程成员可见”）；本表按最新事实更新。

| 编号 | 严重度 | 问题 | 修复/证据 | 状态 |
| --- | --- | --- | --- | --- |
| Q-01 | 高 | QA-02 可见性口径 | PO 2026-11-25 晚修订为“课程成员可见”；代码保持 #219 实现，未合入“站内公开”改动 | 已关闭（按 PO 决策，不改代码） |
| Q-02 | 高 | #223 只修详情读取，`accept` 写侧仍抛 QA-2002，列表 accepted 状态不一致 | #225 `c6ab251` 已补齐写侧与列表 effective accepted；`AnswerAndVoteServiceTest` 增加脏数据用例 | 已修复（#225） |
| Q-03 | 中 | 投票并发改投/撤销可能重复计分 | `VoteJpaRepository.find...` 加 `PESSIMISTIC_WRITE`，同票行串行化（分支 `fix/qingcen-qa-concurrency`） | 已修复（需 MySQL 并发集成测试） |
| Q-04 | 中 | 列表缓存 get/put 各读一次代际，旧快照可写入新代 | `QuestionListCache.Lookup` 携带读取时代际，put 只写该代际 | 已修复 |
| Q-05 | 中 | 回答提交与作者删除仍有 in-flight 窗口 | 新增 `adjustAnswerCountIfOpen`，问题已删时回滚回答 | 已修复 |
| Q-06 | 中 | 删除问题不回收标签计数 | `QuestionCommandService.delete` 成功后 `adjustUsage(-1)` | 已修复 |
| Q-07 | 中 | 悬赏到期批量任务单条脏数据回滚整批 | `expireDue` 改为每条悬赏 `REQUIRES_NEW` 独立事务，失败只记录日志并继续 | 已修复（逐条事务待 MySQL 集成验证） |
| Q-08 | 中 | related/bounty 对不存在/已删除问题不返回 404 | related 先经 `QaQueryApi` 校验；`BountyService.latest` 同样校验 | 已修复 |
| Q-09 | 中 | 相关问题先 LIMIT 再过滤可见性 | 可见性条件下推到 `findCandidatesSharingTags` SQL，不再先取 50 再内存过滤 | 已修复 |
| Q-10 | 中 | REP-01 迁移遇历史重复 OPEN 会阻断、并发唯一键映射通用 409 | 新增 `V202611230001__bounty_dedupe_open.sql` 清理重复 OPEN；`out-of-order: true` 兼容已升级库；唯一键冲突映射 `REP-2002`；新增 `V202611250001` 对账迁移退回多冻结积分 | 已修复（需 MySQL 集成验证） |
| Q-11 | 低 | CACHE-02 多实例/崩溃重复计浏览量 | 批次 ID + `qa_view_batch` 已应用表（与计数同事务）；Redis 刷新锁避免多实例覆盖 FLUSHING | 已修复（多实例需集成验证） |
| Q-12 | 中 | 事件目录 30 个，实际 14 个 | `docs/ddd/events.md` 重写为 14 个实际事件；`event-storming.png` 已重绘为 14 事件；`01-architecture.md` 同步 | 已修复 |
| Q-13 | 中 | 排行榜归属 Discovery，实际在 Reputation | `docs/ddd/context-map.md` 修正为 Reputation | 已修复 |
| Q-14 | 中 | 聚合文档写“声誉余额不能为负”，与代码相反；缺 Bounty/Notification/Search | `docs/ddd/aggregates.md` 修正并补聚合 | 已修复 |
| Q-15 | 中 | 术语表含未实现的声誉事件、收回徽章、手机号 | `docs/ddd/glossary.md` 删除并补实际术语 | 已修复 |
| Q-16 | 中 | 策略引用不存在的 `BountyExpiredEvent` | `docs/ddd/policies.md` 改为 `BountyClosedEvent` 与实际 14 事件 | 已修复 |
| Q-17 | 低 | actors 与 read-models 角色/外部系统重复且分叉 | `docs/ddd/actors.md` 改为实际角色/无外部服务 | 已修复；`read-models.md` 由房国栋处理 |
| Q-18 | 中 | 缺陷台账停在 Sprint 0 | 本节补入 v1.0 审计与状态 | 已更新 |
| Q-19 | 中 | user-guide/错误码与可见性口径 | PO 已在 #224 统一为“课程成员可见”；代码与文档一致 | 已关闭（PO 决策） |
| Q-20 | 低 | test-plan 是 Sprint 0 历史归档，不能充当最终报告 | `docs/qa/final-test-report.md` 更新为 281 个单测 + 前端 Vitest（当时 8 条，2026-11-28 已增至 16 条）；本地已跑 5 条 FlowIT，但 CI 只在 `62332d2` 跑过 4 条（run 45742927110），P5 后的新 HEAD 仍需重跑 | 部分修复 |
| Q-21 | 中 | S2 燃尽图把两条 5 SP 写成合计 5 SP，D5 从 24 跳 0 | `sprint-2.md:110-119` 改为各 5 SP、D1=32，D5 标注“收口”非实测 | 已修复 |
| Q-22 | 高 | `GET /questions/{id}/bounty` 绕过课程可见性，匿名可读悬赏金额/发起人 | Controller 增加 `Optional<Actor>`；Service 复用作者/成员/管理员判定，否则 403 | 已修复 |
| Q-23 | 中 | 相关问题只校验源问题存在，外部人可间接看到课程问题标签 | `DiscoveryQueryService.related` 先校验源问题可见性，再查候选 | 已修复 |
| Q-24 | 中 | 投票并发首次插入一方 409，与幂等 PUT 不符 | 投票前锁问题行，首次/改投/撤销统一串行化；保留唯一键兜底 | 已修复 |
| Q-25 | 高 | 删除已采纳回答后 answer_count 回涨；软删除可被并发编辑复活；采纳/删除竞态 | 实体 `@DynamicUpdate`、软删除递增 version、所有状态变更先锁问题行 | 已修复（需 MySQL 并发 IT 验证） |
| Q-26 | 中 | 列表缓存 key 不含角色/课程成员范围，加入课程/改角色后 75 秒内读旧列表 | key 增加 `scope`（anon/admin/member:课程ID集合） | 已修复 |
| Q-27 | 高 | `expireDue` 用事务外快照逐条退款，可二次退款并覆盖已 AWARDED 悬赏 | 每条在 `REQUIRES_NEW` 内按 id 加锁重读，校验 `OPEN && due` 后才退款；补快照已关闭用例 | 已修复（待 MySQL 并发 IT） |
| Q-28 | 中 | 悬赏发起人账户缺失会让事件处理回滚并最终死信，连带丢失采纳加分 | Reputation 处理器捕获脏数据异常并记录日志，不再回滚整个事件 | 已修复 |
| Q-29 | 中 | 浏览量刷新锁释放未校验 token，多实例可能误删他人锁 | Redis Lua compare-and-delete | 已修复（待多实例 IT） |
| Q-30 | 中 | 对账只比较 reputation/ledger，不检查 frozen 与 OPEN 悬赏 | 新增 `reconcilePoints()`、`co_reputation_points_mismatches` 指标和 `/admin/reputation/reconciliation/points` | 已修复 |
| Q-31 | 中 | 回答锁后二次 `findById` 命中一级缓存，竞态退化为 409 | 新增加 `AnswerRepository.findQuestionId` 标量投影，锁后首次加载回答 | 已修复 |
| Q-32 | 低 | `qa_view_batch` 只增不删 | 每次刷新后清理 7 天前的已应用批次 | 已修复 |
| Q-33 | 高 | Q-28 捕获悬赏异常后，内层 MANDATORY 事务仍会把共享事务标记 rollback-only，+15 仍整体回滚 | `settleOnAccept` / `closeForDeletedQuestion` 改 `REQUIRES_NEW`，失败只回滚悬赏子事务 | 已修复（待 Spring+MySQL 脏账户 IT） |
| Q-34 | 中 | 每日投票上限在 REPEATABLE READ 下锁后仍读旧快照，可被并发突破 | 投票处理器 `READ_COMMITTED`；`recordVoteCapped` 先锁账户再做幂等 exists 与日累计 | 已修复（待 MySQL 并发 IT） |
| Q-35 | 中 | VoteService 预加载回答后锁问题，锁后重读命中一级缓存，可给已删回答投票 | 先用 `findQuestionId` 标量投影，锁问题后再首次加载回答 | 已修复（待 MySQL 交错 IT） |
| Q-36 | 中 | 自问自答可领他人悬赏；管理者可采纳/认证自己的回答 | 悬赏结算对 `winner==提问者` 或 `acceptedById==winner` 退款；管理者不能采纳/认证自己的回答（`QA-1021`）；提问者自采纳允许且声誉 +0（PO 口径） | 已修复 |
| Q-37 | 中 | 发起悬赏与采纳/删除问题存在 TOCTOU，可产生已采纳/已删问题上的 OPEN 悬赏 | `QaQueryApi.findQuestionForUpdate` 锁问题行；`BountyService.open` 改用它 | 已修复（待 MySQL 并发 IT） |
| Q-38 | 中 | 悬赏结算用处理时间判到期，采纳在到期前但事件积压到到期后会被退款 | `settleOnAccept` 增加 `acceptedAt`，用事件发生时间判断 `isDue` | 已修复 |
| Q-39 | 中 | 评论/回复与目标删除、父评论删除无锁，可能产生孤儿评论 | 评论 post/delete 先锁问题行，再锁父评论/目标评论（`CommentRepository.findByIdForUpdate`） | 已修复（待 MySQL 交错 IT） |
| Q-40 | 中 | 投票被日上限截断为 0 时不写幂等标记，跨日重投可补分 | `VoteCastEvent` 用 `ProcessedEventStore` 标记（consumer=`reputation-vote`） | 已修复 |
| Q-41 | 中 | 教师/助教账户先被懒创建为 0（或已有声誉），后到注册事件不再补 1000 分；且重放依赖 eventId，换事件 ID 可重复补分 | 注册事件用 eventId + 持久化 marker `initial-points:<userId>` 双重幂等；存量账户由迁移补 marker；补非零懒创建账户用例 | 已修复 |
| Q-42 | 低 | 浏览量失败批次 HINCRBY+HDEL 非原子；刷新锁 TTL 过短 | 合并改为单次 Lua 原子执行；锁 TTL 由 2 分钟提高到 10 分钟 | 已修复（待故障注入 IT） |
| Q-43 | 低 | 缓存代际 key 被驱逐后 `INCR` 可能重新产生小编号 | `invalidateAll` 用 Lua：代际缺失或过小时先重置为时间戳再 INCR | 已修复 |
| Q-44 | 低 | 声誉对账漏“有流水但账户行缺失”的用户 | `reconcile()` 遍历账户与流水用户并集，缺失账户按 0 声誉上报 | 已修复 |
| Q-45 | 低 | 标签计数按输入顺序批量 UPSERT，并发可死锁 500 | `adjustDeltas` 按标签名 `TreeMap` 排序一次批量执行；编辑合并 added/removed 净增量 | 已修复（待 MySQL 并发 IT） |
| Q-46 | 低 | 并发首票测试仍固化旧 409 路径，未验证生产串行化 | 测试重命名为“唯一键兜底”并注明生产由问题行锁串行化；真实并发需 MySQL IT | 部分修复（验证缺口） |
| Q-47 | 低 | 问题删除后已入队事件仍发通知 | 通知处理器统一过滤 `QuestionRef.deleted()`；补删除问题用例 | 已修复 |
| Q-48 | 低 | Q-47 只拦截新投影；删除事件之前已落库的通知仍残留，点开指向已删问题 | `NotificationProjector` 订阅 `ContentDeletedEvent`，按 `/questions/{id}` 删除该问题相关通知；补删除通知用例 | 已修复 |
| Q-49 | 中 | `expireDue` 到期退款不检查是否已有可见采纳回答，采纳事件积压时会先退款再发奖 | 退款前调用 `QaQueryApi.hasVisibleAcceptedAnswer`，已采纳则跳过并由结算处理器发奖 | 已修复 |
| Q-50 | 中 | 迟到的旧 `AnswerAccepted` 事件可结算新悬赏（采纳发生在悬赏创建前/当前采纳已是别的回答/回答已被删除） | `settleOnAccept` 校验 `acceptedAt >= createdAt`，并重读当前 `acceptedAnswerId` 一致且 `hasVisibleAcceptedAnswer` 为真后才结算；补“删除释放采纳”用例 | 已修复 |
| Q-51 | 低 | 浏览量失败批次合并后 `__batch_id` 留在 FLUSHING；测试未断言合并脚本 | Lua 脚本补 `HDEL KEYS[2] '__batch_id'`；测试改为区分合并/解锁两次 `execute` | 已修复 |
| Q-52 | 低 | W-06 已让 `canManageCourse(userId, null)` 对非管理员返回 false，但 `IdentityApi` 接口注释与 `BountyService.open` 注释仍写“全局教师可管理公开问题” | 两处注释改为当前口径：公开问题仅作者与 ADMIN；悬赏仍按 REP-1007 拒绝公开问题 | 已修复 |
| Q-53 | 高 | 投票日上限按票面差回滚：被上限截断的赞撤销/改踩会多扣分；反复“赞→撤”可绕过上限把声誉刷成负数 | 新增 `rep_vote_award` 记录每次投票实际入账值；改投/撤销增量 = 新实际入账值 − 旧实际入账值；补 3 个上限反转用例。旧版本投票没有该行，首次改投按票面回退一次（仅历史数据边界） | 已修复（需 MySQL 并发 IT） |
| Q-54 | 中 | Q-31/Q-35/Q-39 的“标量投影→锁问题→再读回答”仍受 MySQL RR 旧快照影响：并发删除已提交后，投票/评论/编辑仍可能读到未删回答 | `VoteService`、`AnswerCommandService`、`CommentService` 改 `READ_COMMITTED`，锁问题后读取看到已提交删除 | 已修复（需 MySQL 交错 IT） |
| Q-55 | 中 | 评论删除的级联查询是普通读，并发回复提交后可能漏删，留下孤儿回复 | `CommentService.delete` 改 `READ_COMMITTED`；问题→评论锁序不变 | 已修复（需 MySQL 交错 IT） |
| Q-56 | 中 | 认证列表请求在 `courseIdsOfUser` 建立 RR 旧快照，可能在读到新代际后把旧页写进新代际，污染约 75 秒缓存 | `QuestionQueryService.list` 改 `READ_COMMITTED` | 已修复（需 MySQL/缓存 IT） |
| Q-57 | 高（罕见） | 到期退款与 in-flight accept 竞态：退款检查看不到未提交采纳，事件到达时悬赏已 EXPIRED，回答者永远拿不到悬赏 | `expireOne` 先锁问题行，与 accept 串行化，再复查可见采纳 | 已修复（需 MySQL 交错 IT） |
| Q-58 | 中 | 发起人账户缺失的到期悬赏永远 OPEN，并可能占满到期批次前 100 条，饿死后续悬赏 | 到期时关闭悬赏但不退款（`refunded=0` + 审计/日志），不再抛异常卡住 | 已修复 |
| Q-59 | 中 | `AnswerAccepted` 处理先写 +15 再 `REQUIRES_NEW` 结算，同一获奖者账户版本冲突，每次悬赏奖励都多一次回滚重投 | 先结算悬赏，再记 +15/徽章，避免外层脏账户与内层提交冲突 | 已修复（需 Spring+MySQL IT） |
| Q-60 | 低 | 对账漏“有流水但账户缺失且净额为 0”的用户 | 账户缺失即上报，不再只看余额是否相等 | 已修复 |
| Q-61 | 中 | 投影防御：晚到的 QuestionPosted 可把已删除问题重新插入搜索；编辑遇到缺失投影会静默丢失 | QuestionPosted 先查 Q&A 未删除；`updateText` 返回影响行数，0 行时用编辑事件+Q&A 信息重建 | 已修复（乱序根因在 OutboxRelay，跨模块） |
| Q-62 | 低 | P4 按 `link` 删通知无索引，问题删除时全表扫描并持有大量 next-key lock | 新增 `V202611270002__notification_link_index.sql` | 已修复 |
| Q-63 | 中 | P4 初始积分迁移把所有非零账户标记为“已发”；若账户是懒创建且注册事件仍在死信/待重放，会永久少 1000 | 迁移已合入；2026-11-27 在真实 MySQL 执行核对：非零账户缺 marker 0 条，8 个账户均有 marker（见下方记录）。仅覆盖本地 demo 库，生产历史快照不可得 | 已核对（本地真实 MySQL，2026-11-27） |
| Q-64 | 中 | 排行榜 `rebuild(days)` 只重置有流水的周期 key；部分窗口会丢分、幽灵 key 无法清除、与实时事件无互斥 | 记录为已知限制；建议全学期 rebuild + 独立锁，本次未改 | 已知限制 |
| Q-65 | 中 | 浏览量刷新锁 TTL 到期后，另一实例可把同一 FLUSHING 批次合并重放；A 的 DB 事务若 10 分钟后才提交会双计 | 需故障注入 IT；建议事务超时 < 锁 TTL 或恢复时保留原 batch id，本次未改 | 已知限制（待故障注入） |
| Q-66 | 低 | 回答/评论删除后已有通知不清（问题删除已清）；评论删除没有事件 | 记录为已知限制；要修需通知存 targetId 并补评论删除事件 | 已知限制 |
| Q-67 | 低 | 学生被提升为 TEACHER/TA 不补 1000 悬赏积分；课程级 TA 无积分 | 属产品口径，待 PO 决策；文档不宣称可用 | 待产品确认 |
| Q-68 | 低 | 对账未检查 `available < 0`、积分守恒、AWARDED 悬赏与流水一一对应 | 净零账户缺失已修；其余记录为缺口 | 部分修复 |
| Q-69 | 低 | `acceptedAt >= createdAt` 跨实例时钟偏差可能把合法采纳判旧；单实例 compose 不受影响 | 多实例部署需序列号/容忍窗口 | 已知限制（多实例） |
| Q-70 | 低 | 首账户创建在单 outbox 线程下安全；多实例/调度池 >1 时 read-modify-overwrite 可覆盖注册发放的 1000 | 当前单实例设计；多实例需原子 upsert | 已知限制（多实例） |
| Q-71 | 低 | 跨模块：OutboxRelay 严格顺序在“批次外新事件”/崩溃窗口下仍可能被越过；同一事件多消费者共命运，死信会丢投影 | 不在青岑模块改；已报告 Outbox 负责人，并在 Q-61 做投影侧防御 | 跨模块遗留 |

> **Q-63 人工核对记录（2026-11-27，`co-mysql-dev` 真实 MySQL 8）**：
> 查询「非零账户但无 initial-points marker」返回 0 行；全量 8 个账户（含 T2001/TA3001 1000 分、学生声誉分）均 `has_marker=YES`。原始输出见 [`logs/q63-initial-points-check-2026-11-27.log`](logs/q63-initial-points-check-2026-11-27.log)。以下 SQL 保留用于复核：
> ```sql
> -- 找出有注册处理记录、但没有任何 initial-points marker 的账户；
> -- 再按 identity 的角色确认是否 TEACHER/TA，并核对 1000 分是否确实从未发放。
> SELECT a.user_id, a.available_points, a.frozen_points, a.reputation
> FROM rep_account a
> WHERE a.available_points <> 0 OR a.frozen_points <> 0 OR a.reputation <> 0;
> -- 只处理“确认从未发放”的账户，人工补 1000 并写入 marker：
> -- INSERT IGNORE INTO processed_event(event_id, consumer, processed_at)
> -- VALUES (CONCAT('initial-points:', ?), 'reputation-initial-points', NOW(6));
> ```

> 剩余未关闭：Q-03/Q-07/Q-10/Q-11/Q-25/Q-27/Q-29/Q-30/Q-31/Q-33/Q-34/Q-35/Q-37/Q-38/Q-39/Q-42/Q-45/Q-46/Q-53/Q-54/Q-55/Q-56/Q-57/Q-59 的真实 MySQL/多实例/故障注入验证、Q-20 的 CI Testcontainers、Q-64/Q-65/Q-66/Q-67/Q-68/Q-69/Q-70 的已知限制；这些不在本分支伪称完成。
