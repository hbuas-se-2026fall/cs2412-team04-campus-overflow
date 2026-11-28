# 个人总结 · 魏志国

| 项 | 内容 |
| --- | --- |
| 姓名 | 魏志国 |
| 角色 | PO（产品负责人）/ 发布经理；兼任 B 组的验收与复核（不承担 B 组开发任务，角色边界见 §三） |
| 负责 | 48 条 PBI 与 Backlog 口径、分支保护与仓库规则、采纳/声誉计分的验收与复核、Release 发布、V3 看板 |
| 参与 Sprint | S0 / S1 / S2 / S3 / S4 |

## 一、我负责的交付物（带证据）

| 交付物 | 证据 | 结果 |
| --- | --- | --- |
| 48 条 PBI Issue 与 Backlog 单一口径 | Issue #27~#74（`[PB-xx]` + User Story + Given/When/Then）、`docs/backlog/product-backlog.md` | 权威口径为 **48 条 PBI**：#160 看板同步（board-sync 2026-11-17）按交付事实更新为 **48 条全部处置：44 closed / 3 open（v1.1 候选）/ 1 Won't**；「49 项」为早期草稿的未核对数字，已作废，不作为结论引用。最终口径见 [`docs/backlog/board-sync-2026-11-17.md`](../backlog/board-sync-2026-11-17.md) |
| 分支保护与必需状态检查（BUG-003 关闭） | Branch Protection API 核验记录 `docs/lab1-submission/README.md`；`develop` 勾上 `后端构建与验证`、`前端构建`（1 Approve + dismiss stale + enforce admins） | main/develop 双分支保护完整，CI 失败阻断合并 |
| Question.accept 五步校验（T1-03，**验收与复核**） | 实现：`Question.java` 域逻辑由**青岑**完成（`6edd60b`）；我负责按角色卡梳理五步校验清单并复核 `QuestionTest.Accepting` 5 条 | 全绿（归属已按 git 历史更正） |
| **AnswerCommandService 应用层编排（T1-08）** | 首版由我实现（`479a42a feat(qa): implement AnswerCommandService for Lab4`，2026-11-04）：save → 原子 answerCount → 发事件 → 缓存失效；后续并发/缓存修复由青岑、刘俊琒迭代；事件参数顺序修复 PR #89 | 全绿；具体用例数以实现当次 CI 报告为准，不在此写死计数 |
| Answer 聚合（T1-04，**验收**） | 实现：**青岑**（`6edd60b feat(qa): implement Answer aggregate invariants`）；`AnswerTest`、`AnswerAndVoteServiceTest` 作为验收 | 全绿（归属已按 git 历史更正）；用例数以最后一次全量 CI 报告为准，避免写死计数与实际 XML 不符 |
| 声誉计分规则/徽章/查询 API（T2-01/05/09） | `ReputationRulebookTest` 9/9、PR #91（`(10,195)→5`、`(10,200)→0` 日上限边界） | 合入 develop |
| 声誉账户与采纳/投票计分（T2-02/06） | `ReputationAccount` 七方法、`ReputationEventHandlers` 幂等、PR #102 | 合入 develop；用例数以全量 CI 报告为准 |
| Lab4 端到端证据 | `docs/lab4-submission/answer-e2e.md` + 4 张截图（POST 问题→回答→采纳→详情 answerCount=1 + SQL 落库） | 入库 |
| D3 文档口径收口（4 上下文 / **14 事件**） | PR #75 先按当时的 30 事件清单重建 `event-storming.png`；2026-11-25 由青岑按代码实际事件重写 `events.md` 为 **14 个**并重绘图，`01-architecture.md` 同步 | BUG-004 已修复（早期 30 事件清单已废弃） |
| v0.1.0 / v0.2.0 / v0.3.0 发布 | `docs/release/v0.3.0.md`、PR #115（develop→main）、GitHub Releases 三份 | tag 已打；Release Notes 仅 v0.3.0 有独立文档，v0.1.0/v0.2.0 为补打、只有合并记录 `docs/reports/release-v0.2.0-v0.3.0.md`，不宣称「三份 notes 齐全」 |
| E3/E4 环境与身份核查表 | `env-weizhiguo.png/txt`、`mvn-test-weizhiguo.png`、PR #123 | 魏志国本人 env/mvn-test 两列齐备（5/5）；Docker 列为全组例外（2 人未装，见 `docs/lab1-submission/env-matrix.md` §6），不写成「全组 5/5」 |
| Sprint 3 计划与执行收口 | PR #124（PO 代拟计划）→ #160（分支 `docs/s4-closeout-liujunfeng`，其中一并完成 S3 执行记录收口 + T4-06 决策 + 看板同步，刘俊琒） | 已执行并收口；#160 属 S4 收口分支，非纯 S3 PR |
| T1-15 qa.http 问题侧 | PR #101：发布/编辑/详情/删除问题请求 | 一键跑通 |

## 二、技术收获

1. **聚合校验顺序本身就是接口契约。** `Question.accept` 的“未删→有权限→未采纳过→答案属于本题→答案未删”五步不是业务细节，而是 403/409/422 错误码的来源——领域测试写的就是验收单。
2. **事件计分要在“边界值”上证明正确，而不是只证明正向。** 每日上限 `applyDailyCap` 用 `(10,195)→5`、`(10,200)→0` 两条边界锁死；采纳 +15 用“自问自答 delta=0 不写流水”证明幂等不变量“余额=流水之和”不被破坏。
3. **仓库规则要靠 API 核验，不能靠截图。** 分支保护、必需检查、看板状态都能用 API 读写并复核；截图只能证明“当时看过”，核验记录才能证明“配置存在”。

## 三、协作与流程

- 发起/参与 PR：#75（D3 统一）、#89（事件参数修复）、#91/#102（声誉）、#101（qa.http）、#114（S2 收尾）、#115（发布）、#123（E3/E4）、#124（S3 计划）等；均经队友 Approve 后合并。
- 主持/参与：Sprint 计划会、Planning Poker 口径确认（PB-11=5 基准）、按 V3 §9.1 七维度做的 PO 自评（事后补记，**非**已召开的正式 Sprint Review——S1~S4 均未开独立评审会）；代查刘俊琒身份绑定（E4）并标注“代查”。
- 角色边界：章程与 `docs/team.md` 定义魏志国为 PO（维护 Backlog、排优先级、验收增量）兼发布经理；B 组（回答/声誉）的代码由青岑、刘俊琒等实现，我承担验收与复核，不把 B 组开发计入个人交付。科技企业项目中的「PO 兼发布经理」为团队确认的兼任安排，非独立角色。
- 流程改进：把“两份文档互相‘以对方为准’”改为 **backlog 单一来源**（Sprint1 点数曾出现 13 vs 16 不一致）；把“发布 PR + tag + Release Notes”固化为 Sprint 末动作（与陈明哲共同沉淀）；对外代拟内容一律标注“XX 代整理，待本人复核”。

## 四、不足与改进

| 不足 | 影响 | 下次怎么做 |
| --- | --- | --- |
| Planning Poker 估点会没有留会议记录 | 评审时只能给“口径已回写”的间接证据 | 估点会当场把每人牌面与分歧记进 Milestone 描述 |
| 起始模板的 `characterEncoding=utf8mb4` 配置到 S2 联调才发现 | 后端首次以默认配置启动直接失败 | 收到模板代码先做一次“默认配置可启动”冒烟，而不是先写业务 |
| 对队友 TODO 的依赖（如 QuestionCommandService）发现偏晚 | Lab4 端到端一度卡住 | 每周扫一次全仓 `TODO(负责人)`，提前一周暴露依赖 |
| 验收与彩排未做到「每个 Sprint 结束前」 | S1/S3/S4 的 PO 验收均在 11-25 补签，两次彩排仍未执行 | Sprint 收口当天完成验收签字；彩排按 `rehearsal-log.md` 排期执行，不再事后补记 |
| 发布口径回填过度（v1.0.0 notes 发布后改写、v1.0.1 只有 notes 无 tag） | 评审时结论与 tag 快照不一致 | Release Notes 发布即冻结；未打 tag 的版本明确标注「草稿/未发布」 |
| 评审批准曾被外部自动化批量撤销，应急时临时放宽保护合并 | 流程上有 ~3 秒保护空窗 | 事后补了核验记录；下次先查撤销来源、再按预案操作并全程留痕 |

## 五、自评

- 对团队目标的贡献：守住“做什么、先做哪个”的需求口径与 48 条 PBI 的验收标准；保证采纳/计分链路正确并有端到端证据；仓库规则（分支保护、必需检查、发布流程）从口头约定变成可核验配置；v0.1.0~v1.0.0 的发布链路可追溯。
- 需要团队/老师支持的地方：两次彩排需全员到场；最终 HEAD 的 Sonar 复扫需在装有 SonarQube 的机器上执行。Issue/Release 可经 GitHub API 复核，Projects 看板仅经网页截图（当前 token 无 `read:project` scope）。
- 签字 / 日期：魏志国 / 2026-11-25（修订版；初稿 2026-11-17，因 11-25 的 #160 收口与审计补记更新，故日期以修订版为准）
