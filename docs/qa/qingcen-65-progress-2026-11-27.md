# 青岑 65 项审计问题 · 修复进度（2026-11-27）

> 起始基线：`develop` `5a74fb0`；合并 `develop` `71ee629`（PR #253）解决 PR #254 冲突。本文件记录本轮修改，push/PR 由青岑手动执行。
> 口径：已闭环=代码/文档已改并有本地验证；部分完成=已改但仍缺运行截图/CI/最终数据；阻塞=需 Docker、其他机器、其他 owner 或 PO 决策。

## 一、已闭环（可本地验证）

| 类别 | 对应项 | 落地 |
| --- | --- | --- |
| 管理页 | H13、H29 | `AdminUsersPage` 增加禁用按钮/确认/会话失效提示；`api.disableUser`；管理列表返回 `status`，禁用行显示「（已禁用）」且不再给操作按钮；前端 `AdminUsersPage.test.tsx` 2 条；后端 `AdminUserServiceTest` 增加 verify/createStaff/list/status 4 条，单测 4→8 条；`admin-disable.png` 已用当前构建重拍 |
| 演示数据 | H15、H16、H17、H24、H25 | seeder 增加 `SeedUser`/`TestStudent` 与第二条红黑树问题；`reset-demo-account.sql` 已在本地执行，T2001 恢复 1000/0；search 2 条、mention @小红、related q4、admin 2 pending、通知 6→5 均已用当前构建重拍；重置后重跑 bounty smoke 得 950/50，截图 `docs/scrum/s2-bounty-950-50.png` |
| 用户手册 | M10、M22、M23、M24、M25、M26、L02 | 通知链接改为“跳到问题”；补待认证账号/禁用/改角色撤销会话/自采纳 +0/悬赏异常分支；补排查 SQL/重置命令 |
| 错误码 | L03、L04 | QA-1020 改为“问题或回答已删除”；前端补 QA-1021；错误码目录补 SYS-000 说明 |
| DDD | M11、M12、M13、M14、L05 | TA 权限、删除问题悬赏例外、账户缺失不退款、事件风暴图重绘（VoteCast 改为实际入账值）、角色称谓统一 |
| 测试计划/缺陷台账 | M07、M08、L01 | 补 test-plan→最终证据追溯表；缺陷表补记录格式说明；删除“TODO(S2) 仍未实现”的过时表述 |
| 范围决策 | H22 | 决策 3/4 标记青岑 2026-11-27 复核通过；SM 复核仍为 — |
| S2/S3 记录 | M05、M06、M15、M16、M17、M30 | 测试/覆盖率数字标为 S3 当时快照；S2 燃尽图标注“收口示意，不是实测”；S2 结论改为“功能通过、流程未完成”；S2 悬赏补重置脚本 + 2026-11-27 950/50 复跑证据；aggregates 将“冻结=OPEN 悬赏”降级并写明账户缺失例外，对账测试覆盖缺失发起人 |
| 质量证据 | H02、H03、H04、H05、H07、H10、H21、H30、M01、M02、M04、M09 | 重跑 281 单测 + 5 FlowIT + JaCoCo；更新四域 CSV/PNG；03-quality 改 281+5=286、rep 79.53%、Q 系列披露、Sonar baseline 措辞；final-test-report 改 HEAD/日志/已知限制 |
| 真实 DB/并发 | H08、H09、H26 | 新增 FlowIT 用例：校验 P5 `rep_vote_award` 表与通知 link 索引随 Flyway 落库；真实 MySQL 上两个会话并发投赞成票，断言 2 条投票、score=2；Q-63 在 `co-mysql-dev` 真实 MySQL 核对：非零账户缺 marker 0 条、8 账户均有 marker，日志 `docs/qa/logs/q63-initial-points-check-2026-11-27.log` |
| 发布索引 | H23 | final README 补 T4-08/v1.0.1/最终 CI 未完成项；口径从“main 最新”改为 develop `5a74fb0` |
| 个人总结 | H19、M20、M21、M27 | #116→#99；补 S2 SM；签字 2026-11-27；并发投票改“唯一键兜底/生产串行化，MySQL IT 待补”；Lab4 计数标历史快照 |
| Sonar/文档 | H10、M29 | 03-quality/final README 改“baseline PASSED，HEAD 待复扫”；todo-ownership 加历史结论说明 |
| 其他 | H14、M03、M15、M28 | 管理服务单测 8 条（禁用 3 条）+ FlowIT 1 条 + 前端 2 条；ADR-010/report/sprint-4 的 3/3、4、5/5 口径统一并标注历史值；sprint-4 覆盖率同时标 v1.0.0 快照与当前值；env-matrix 表标 Lab1 交付快照；S2 明确“未召开正式 Review、功能通过流程未完成” |

## 二、部分完成（仍缺运行/截图/CI）

| 项 | 已做 | 仍缺 |
| --- | --- | --- |
| H01 | 03-quality/final-test-report 已改 281；v1.0.1 notes 未动 | v1.0.1 release notes 的 268 与 PRIV-01 归发布 owner |
| H06 | final-test-report 已改当前 HEAD，本地 verify 日志已归档 | 最终 HEAD 的 GitHub Actions run/Testcontainers |

## 三、阻塞（需别人/环境/决策）

| 项 | 卡点 |
| --- | --- |
| H11/H12 | v1.0.1 tag/Release、PRIV-01 release note 属发布 owner |
| H18 | S3 PO 验收归属错误，需 PO/SM 重签 |
| H20 | T4-08 两次彩排需全员到场 |
| H27/H28 | Q-67 需 PO 决策；Q-71 Outbox 负责人未指派 |
| M18/M19 | S3/S4 正式 Review/Retro 属对应 SM，不能由 QA 补造 |

## 四、验证结果

```text
后端：mvn -o verify（含 Testcontainers）→ BUILD SUCCESS，281 单测 + 5 FlowIT
日志：docs/qa/logs/mvn-verify-with-it-2026-11-27-v2.log
演示重置：docs/qa/logs/reset-demo-account-2026-11-27.log（T2001 1000/0，OPEN 悬赏 EXPIRED）
悬赏复跑：docs/qa/logs/bounty-smoke-2026-11-27.log（1000/0 → 950/50），截图 docs/scrum/s2-bounty-950-50.png；复跑后已重置回 1000/0
Q-63 核对：docs/qa/logs/q63-initial-points-check-2026-11-27.log（非零账户缺 marker 0 条）
前端：npm run typecheck ✅ / npm run test:run ✅ 16 条（合并 develop 后：errors 8 + BountyDialog 6 + AdminUsersPage 2）/ npm run build ✅
覆盖率：identity 77.30% / qa 82.57% / reputation 79.53% / discovery 78.49%
限制：本机路径含中文，JaCoCo 需 -Djacoco.destFile=E:/tmp/...；check-domain-coverage 本地被跳过，CI 仍待跑
```

## 五、提交说明

以上修改整理为本地提交；没有 push，也没有改 v1.0.1/main/Release/Projects 等远程内容。339 项审计原始报告（`docs/qa/audit-2026-11-27/`、`full-audit-2026-11-27.md`）仅作本地分发，不随本提交入库。
