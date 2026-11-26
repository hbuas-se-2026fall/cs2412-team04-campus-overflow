# 4. 过程证据

> 答辩口径：用「每个 Sprint 都有可运行增量 + Scrum 工件 + CI 记录」证明过程真实，而不是最后补文档。

## 4.1 Sprint 总览

| Sprint | 周期 | Sprint Goal | 记录 | 版本 |
| --- | --- | --- | --- | --- |
| S0 启动 | 2026-10-22 ～ 11-02 | 仓库、环境、CI、Backlog 与团队文档就绪 | [`sprint-0.md`](../scrum/sprint-0.md) | `v0.1.0`（补打） |
| S1 问答核心 | 2026-11-03 ～ 11-13 | 提问 → 回答 → 投票 → 评论 → 采纳闭环 | [`sprint-1.md`](../scrum/sprint-1.md)、[`sprint-1-demo.md`](../scrum/sprint-1-demo.md) | `v0.2.0`（补打） |
| S2 声誉激励 | 2026-11-09 ～ 11-20 | 声誉/徽章/悬赏/排行榜，幂等与对账 | [`sprint-2.md`](../scrum/sprint-2.md) | `v0.3.0` |
| S3 内容发现 | 2026-11-16 ～ 11-17（实际，原计划 11-23～12-04） | 搜索、相关问题、通知；移除骨架，`mvn verify` 全绿 | [`sprint-3.md`](../scrum/sprint-3.md)、[`sprint-3-search.md`](../scrum/sprint-3-search.md) | `v0.9.0`（RC tag，内容随 v1.0.0 进 main） |
| S4 发布 | 2026-11-17 ～ 11-19（实际，原计划 12-07～12-11） | 稳定化、可观测、v1.0.0 与答辩 | [`sprint-4.md`](../scrum/sprint-4.md) | `v1.0.0`（GitHub Release） |

**时间线说明（审计魏-5 补记）**：上表是**计划窗口**，实际执行存在交叠，原因与口径如下：

| 交叠 | 事实 | 原因 |
| --- | --- | --- |
| S1（11-03～11-13）与 S2（11-09～11-20）重叠 5 天 | S2 启动时 S1 的收尾文档与评审仍在补 | 课程周历把 S2 的课堂时间排在 S1 收尾前；团队按「编码主线先行、收尾文档并行补」执行，各 Sprint 文件中记录的是**实际合并日期**而非计划日期 |
| S2（～11-20）与 S3（11-16 起）重叠 | S3 在 S2 收口阶段提前开工（#117 搜索、#120 投影器） | 同上：S2 的悬赏结算/到期验证需要时间窗，S3 的搜索线可独立并行，故提前启动 |
| S3 实际压缩为 11-16～11-17 | 原计划 11-23～12-04 | 团队合并执行：S3 范围内代码在 S2 期间已完成大半，收口只用 2 天（见 `sprint-3.md` §四） |
| S4 11-17 当天发布 v1.0.0 | S4 与 S3 收口同日 | 课程要求的答辩/提交时间早于原计划；团队选择压缩 S3 后立即发布 |

> 结论：**计划日期与实际日期分别记录，不互相改写**；Sprint 文件中的站会与合并记录均以实际日期为准。
> 不足：S1/S2 的重叠期内没有严格按「一个 Sprint 一个 Goal」执行，已在 `04-process.md` §4.4 与各 Sprint 回顾中记为过程改进项。

## 4.2 Scrum 工件
| 工件 | 位置 | 说明 |
| --- | --- | --- |
| 团队章程 / 角色承诺 | [`docs/team/`](../team/) | 5 人签署（2026-10-26） |
| 工作约定 / DoR / DoD | [`docs/team-agreement.md`](../team-agreement.md)、[`dor-dod.md`](../team/dor-dod.md) | 含 CI 红灯 30 分钟认领、事件重放命令 |
| Product Backlog | [`docs/backlog/product-backlog.md`](../backlog/product-backlog.md) | PB-01～48，含 PBI/Issue 映射 |
| Sprint 计划与记录 | [`docs/scrum/`](../scrum/) | S0～S4（S4 记录已入库，仅两次彩排待执行） |
| 燃尽图 | `sprint-1-burndown.png`、`sprint-2-burndown.png`、`sprint-3-burndown.png` | S1/S2/S3 已归档（S3 见 [`sprint-3.md`](../scrum/sprint-3.md) §五之二） |
| 评审与回顾 | 各 `sprint-N.md` 的 Review / 回顾章节 | 含未达标项与行动项 |
| CI 运行记录 | GitHub Actions | 每次 PR 的后端/前端检查；v0.3.0 三阶段绿 |

## 4.3 协作与贡献

- 分支策略：`feature/*` / `docs/*` / `test/*` → PR → 1～2 Approve → squash/merge，合并后删分支；
- PR 模板包含「关联 Issue / 变更内容 / 自测 / 影响范围」；Issue 模板区分 story/task/bug/tech-debt；
- 代码评审：PR #100、#102、#103、#104、#118、#121 等均有非作者 Approve；
- 个人贡献以合并 PR、评审、Issue 关闭记录为准，不以代码行数论。

## 4.4 过程改进（回顾落地）

| 问题 | 改进 | 证据 |
| --- | --- | --- |
| S0/S1 发布没有打 tag | S4 前补齐 v0.1.0～v0.3.0，并固化「发布 PR + tag + Release Notes」 | [`docs/reports/release-v0.2.0-v0.3.0.md`](../reports/release-v0.2.0-v0.3.0.md) |
| 环境证据只有 2/5 | 采集脚本 + 统一归档，**5/5 已齐**（刘俊琒 #159 补拍后收口） | [`docs/lab1-submission/env-matrix.md`](../lab1-submission/env-matrix.md) |
| CI 被重复 workflow 与额度拖累 | 删除重复 `ci.yaml`、仓库转 Public、固定 `SPRINT_MODULES` | `sprint-2.md` §五 |
| 缺陷台账状态过期 | 每次评审前复核并关闭已修复项 | [`docs/qa/defects.md`](../qa/defects.md) |
| S3 与 S2/S4 交叠，未逐日站会、无计划会纪要 | 压缩执行时改为「开工同步 + 收口同步」两次固定节点，站会记录按当日合并 PR 回填（只记可查证事实，不补写发言） | [`sprint-3.md`](../scrum/sprint-3.md) §四 |
| 48 条 Issue 的验收清单是模板文本、从未逐条勾选，Issue 状态与交付脱节 | 交付以 PR 合并为准；v1.0.0 发布后统一同步 Issue（刘俊琒执行，T4-06 决策见 [`s4-deferred-items-decision.md`](../reports/s4-deferred-items-decision.md)）。**状态只由 Issue 的 open/closed + `state_reason` 表达**：`status:done` / `status:backlog` 整族标签已停用（2026-11-17 从所有 Issue 上摘除；标签定义仍留在仓库 Labels 里未删，见 board-sync §二），看板列由 Projects 内置工作流按关闭事件流转，从机制上消除"已关闭却挂 backlog"的错位。**新增规约（v1.1 起执行）：关闭 Issue 的前提是有一条带 PR 编号的证据评论**——本次复核发现 48 条中有 28 条（#27–#32、#35–#56）没有任何评论，只能靠 PR 列表回溯 | [`product-backlog.md`](../backlog/product-backlog.md) S4 未交付项、[`board-sync-2026-11-17.md`](../backlog/board-sync-2026-11-17.md) §二 |
