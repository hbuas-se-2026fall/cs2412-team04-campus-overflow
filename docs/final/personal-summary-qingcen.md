# 个人总结 · 青岑

| 项 | 内容 |
| --- | --- |
| 姓名 | 青岑 |
| 角色 | Developer（C）/ Sprint 2 SM |
| 负责 | Q&A 投票/评论、Bounty、Discovery 搜索、S4 管理页与文档 |
| 参与 Sprint | S0 / S1 / S2 / S3 / S4 |

## 一、我负责的交付物（带证据）

| 交付物 | 证据（文件 / PR / CI） | 结果 |
| --- | --- | --- |
| Q&A 领域不变量：MarkdownBody、Tags、Question、Answer、Vote、Comment | PR #21；`co-qa-domain` 测试 | 领域测试全绿；标题、删除、重复投票等边界有回归用例 |
| VoteService / CommentService：级联删除、投票并发唯一 | PR #78、#105；`QingcenVoteCommentServiceTest` | 顶层评论删除先删回复；并发首票由问题行锁串行化，唯一键为兜底；真实 MySQL 交错 IT 仍在待补清单 |
| Bounty 聚合与 BountyService：冻结、结算、退款 | 实现 commit `24ebbb8`；PR #110、#111 边界测试与 smoke | 9/10/500/501 分、0/1/14/15 天边界；950/50 演示与结算/到期证据 |
| Sprint 2 执行记录、燃尽图、评审演练 | PR #99；`docs/scrum/sprint-2.md`、`sprint-2-burndown.png` | S2 验收材料完整，评审演练表可复用 |
| Discovery：SearchQuery、RelatedQuestionPolicy、DiscoveryQueryService | PR #117；`DiscoveryQueryServiceTest`、`DiscoveryDomainTest` | 关键词去重/最多 5 个；相关问题按 Jaccard、分数、采纳、ID 排序 |
| 搜索验收证据：422、空结果、中文命中、相关问题 | PR #119、#122；`docs/scrum/sprint-3-search.md` | `红黑树` 命中 2 条；related 1 条 similarity=1.0 |
| T4-05 管理页：待认证/已认证、认证、建教职工、改角色、禁用账号 | PR #129；`frontend/src/pages/AdminUsersPage.tsx` | ADMIN 入口 + 路由守卫；实际接口 `verified=false`、`maskedUsername`；禁用按钮确认后撤销目标会话，列表返回 `status`，禁用行显示「（已禁用）」且不再给操作按钮 |
| T4-07 文档收口：README、ADR-010、用户手册 | PR #130；`docs/final/user-guide.md`、`docs/adr/010-禁用账号与会话清理.md` | 新人可按 README 起环境；用户手册覆盖主流程和管理页 |
| Lab 1 环境证据与 DDD 文档 | PR #17、#19、#98 | D1–D6 文档、US-01/02 场景、T0-03 环境采集归档 |
| P5 审计修复：Q-53～Q-71 | PR #251/#252；`defects.md`、P5 测试与迁移 | VoteAward 实际入账、READ_COMMITTED、悬赏锁、搜索投影防御；仍有多项待 MySQL IT/产品决策 |

> S3 收口时 discovery 进入 CI 的最终改动由魏志国在 PR #126 完成；我负责的是搜索领域实现、验收证据和回归，不把队友的实现写进个人贡献。

## 二、技术收获

1. **并发不变量要靠“会失败的环境”验证，而不是只写 happy path。** 投票测试先用 `CyclicBarrier` 让两个请求同时读，并用假仓库强制唯一键冲突；本轮又补了 Testcontainers + 真实 MySQL 的 FlowIT：两个会话并发投赞成票后 `qa_vote=2`、`qa_answer.score=2`，证明问题行锁串行化生效。其他交错场景仍待补。
2. **异步链路必须用最终状态证明。** 悬赏的冻结/结算/退款不能只看接口 204；我通过账户可用/冻结积分、账本和到期日志确认最终一致，避免“接口成功、事件丢失”。
3. **前端管理页要按真实后端契约写。** 角色卡写的是 `status=PENDING`，实际接口是 `verified=false`，返回字段是 `maskedUsername`；如果照文档假设写，页面会“看起来有数据、实际筛选错误”。先读 Controller/UserView，再写页面。

## 三、协作与流程

- 发起/参与 PR：#21、#78、#105、#110、#111、#116、#117、#119、#122、#129、#130；均走分支 → PR → 评审 → 合并。
- 主持/参与：S2 执行记录与燃尽图归档；S3 搜索验收；T4-05/T4-07 的文档与截图归档。
- 流程改进：把“验收证据”固定在 `docs/scrum/`、`docs/final/`，不只留在聊天记录；对跨模块功能先写清接口/边界再实现。

## 四、不足与改进

| 不足 | 影响 | 下次怎么做 |
| --- | --- | --- |
| 本地仓库路径含中文，JaCoCo agent 默认写不进 `target/jacoco.exec` | 首次 `mvn verify` 的 `report/check` 被跳过，容易误以为覆盖率门禁已通过 | 用 `-Djacoco.destFile=E:/tmp/coverage-all.exec` 指定 ASCII 路径；最终以 CI Linux 为准 |
| 早期把“队友机器代跑 Docker”只记进汇总表，没同步其本机截图 | 环境证据与表格不一致，验收时被要求二采 | 环境证据必须本人本机采集；代跑只能做参考，不能替代截图 |
| S3/S4 status 文档在多次合并后出现“已完成仍标待办” | 评审前需要反复核对 | PR 合并后当天更新对应 Sprint/交付检查表 |

## 五、自评

- 对团队目标的贡献：把 Q&A 投票/评论、Bounty、Discovery 搜索和 S4 管理页/文档从“能跑”推进到“有测试、有证据、可演示”。
- 需要团队/老师支持的地方：最终彩排需要陈明哲统一计时；SonarQube 最终 HEAD 复扫、线上 TLS/部署和运维手册需要 PO/SM 确认分工。
- 签字 / 日期：青岑 / 2026-11-27（P5 审计修复后补签）
