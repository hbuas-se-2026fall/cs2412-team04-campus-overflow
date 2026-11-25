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
| BUG-004 | 中 | 文档一致性 | 早期事件风暴图/目录写 30 事件，与代码不一致 | 2026-11-25 重写：`docs/ddd/events.md` 改为与代码一致的 14 个实际事件；`context-map.md` 去掉“30 事件已对齐”；`event-storming.png` 仍需重绘 | 4 个上下文；事件目录与代码一致 | 文档已改，PNG 待重绘 | 青岑 | 部分修复 |
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
| Q-07 | 中 | 悬赏到期批量任务单条脏数据回滚整批 | `expireDue` 跳过单条业务/不存在异常，其他异常仍回滚 | 部分修复（逐条事务待集成验证） |
| Q-08 | 中 | related/bounty 对不存在/已删除问题不返回 404 | related 先经 `QaQueryApi` 校验；`BountyService.latest` 同样校验 | 已修复 |
| Q-09 | 中 | 相关问题先 LIMIT 再过滤可见性 | 可见性条件下推到 `findCandidatesSharingTags` SQL，不再先取 50 再内存过滤 | 已修复 |
| Q-10 | 中 | REP-01 迁移遇历史重复 OPEN 会阻断、并发唯一键映射通用 409 | 新增 `V202611230001__bounty_dedupe_open.sql` 清理重复 OPEN；`out-of-order: true` 兼容已升级库；唯一键冲突映射 `REP-2002` | 已修复（需 MySQL 集成验证） |
| Q-11 | 低 | CACHE-02 多实例/崩溃重复计浏览量 | 单实例路径已修；多实例幂等键未实现 | 待办（低） |
| Q-12 | 中 | 事件目录 30 个，实际 14 个 | `docs/ddd/events.md` 重写为 14 个实际事件；PNG 待重绘 | 文档已修复 |
| Q-13 | 中 | 排行榜归属 Discovery，实际在 Reputation | `docs/ddd/context-map.md` 修正为 Reputation | 已修复 |
| Q-14 | 中 | 聚合文档写“声誉余额不能为负”，与代码相反；缺 Bounty/Notification/Search | `docs/ddd/aggregates.md` 修正并补聚合 | 已修复 |
| Q-15 | 中 | 术语表含未实现的声誉事件、收回徽章、手机号 | `docs/ddd/glossary.md` 删除并补实际术语 | 已修复 |
| Q-16 | 中 | 策略引用不存在的 `BountyExpiredEvent` | `docs/ddd/policies.md` 改为 `BountyClosedEvent` 与实际 14 事件 | 已修复 |
| Q-17 | 低 | actors 与 read-models 角色/外部系统重复且分叉 | `docs/ddd/actors.md` 改为实际角色/无外部服务 | 已修复；`read-models.md` 由房国栋处理 |
| Q-18 | 中 | 缺陷台账停在 Sprint 0 | 本节补入 v1.0 审计与状态 | 已更新 |
| Q-19 | 中 | user-guide/错误码与可见性口径 | PO 已在 #224 统一为“课程成员可见”；代码与文档一致 | 已关闭（PO 决策） |
| Q-20 | 低 | test-plan 是 Sprint 0 历史归档，不能充当最终报告 | 新增 `docs/qa/final-test-report.md` 记录本轮 214 个单测与前端构建；CI Testcontainers 仍待跑 | 部分修复 |
| Q-21 | 中 | S2 燃尽图把两条 5 SP 写成合计 5 SP，D5 从 24 跳 0 | `sprint-2.md:110-119` 改为各 5 SP、D1=32，D5 标注“收口”非实测 | 已修复 |

> 剩余未关闭：Q-07 的逐条事务集成测试、Q-10 的 MySQL 迁移验证、Q-11 的多实例幂等、Q-20 的 CI Testcontainers；这些不在本分支伪称完成。
