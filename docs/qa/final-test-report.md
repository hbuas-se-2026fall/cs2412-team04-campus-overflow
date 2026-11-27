# v1.0 最终测试报告（青岑本地复核）

> 日期：2026-11-27
> 基线：`develop` `5a74fb0`（含 P5 修复：Q&A 并发、悬赏可见性、缓存/浏览量、通知删除、初始积分幂等、投票上限实际入账值）；本轮工作区另含未提交的管理页状态改动
> 环境：Windows 11 / JDK 21.0.10 / Maven 3.9.2；前端 Node.js
> 说明：本报告只记录本轮实际跑过的命令；Testcontainers 集成测试已在 Docker Desktop 29.9.3 上跑通。
> 复跑记录：2026-11-27 以 `5a74fb0` 为基线、在含未提交管理页改动的工作区执行 `mvn -o verify`（含 Testcontainers），结果 BUILD SUCCESS、281 单测 + 5 FlowIT 全绿；原始日志 [`logs/mvn-verify-with-it-2026-11-27-v2.log`](logs/mvn-verify-with-it-2026-11-27-v2.log)。

## 一、后端单元测试

```bash
cd backend
export JAVA_HOME="E:/java内容/jaavaa/jdk-21.0.10"
export PATH="$JAVA_HOME/bin:/e/java内容/IntelliJ IDEA Community Edition 2023.2.2/plugins/maven/lib/maven3/bin:$PATH"
mvn -o test -Djacoco.skip=true -DskipITs
```

结果：**BUILD SUCCESS，281 个测试，0 失败 / 0 错误**。

| 模块 | 测试数 |
| --- | ---: |
| `co-shared-kernel` | 3 |
| `co-identity-domain` | 13 |
| `co-identity-application` | 12 |
| `co-identity-infrastructure` | 1 |
| `co-qa-domain` | 44 |
| `co-qa-api` | 8 |
| `co-qa-application` | 56 |
| `co-qa-infrastructure` | 6 |
| `co-reputation-domain` | 20 |
| `co-reputation-application` | 72 |
| `co-reputation-infrastructure` | 3 |
| `co-discovery-domain` | 6 |
| `co-discovery-application` | 29 |
| `co-bootstrap` | 8（ArchUnit 1 + EnvironmentGuard 7） |

> 另有 `CampusOverflowFlowIT` 5 条集成测试由 failsafe 执行，已于 2026-11-27 在 Docker Desktop 29.9.3 + Testcontainers 上跑通；
> 完整口径为 **281 单测 + 5 集成 = 286**。

## 二、前端

```bash
cd frontend
npm run typecheck
npm run test:run
npm run build
```

结果：**typecheck 通过，Vitest 8 条通过（`BountyDialog.test.tsx` 6 条 + `AdminUsersPage.test.tsx` 2 条），Vite build 通过**。

## 三、未覆盖项与已知限制

- `CampusOverflowFlowIT`（Testcontainers MySQL 8 + Redis 7，5 条）已在本地 2026-11-27 跑通；其中新增用例校验 P5 `rep_vote_award`/通知索引随 Flyway 落库，并在真实 MySQL 上并发投两票验证 score=2、qa_vote=2。
- JaCoCo 覆盖率本轮已用 `mvn verify -DskipITs` 重采（基线 `5a74fb0` + 未提交管理页改动）；但仓库路径含中文，JaCoCo agent 默认写不进 `target/jacoco.exec`，需显式 `-Djacoco.destFile=E:/tmp/coverage-all-6.exec -Djacoco.dataFile=...`；本地 `check-domain-coverage` 因此被跳过，阈值门禁仍以 CI Linux 为准。详见 [`final/screenshots/coverage/README.md`](../final/screenshots/coverage/README.md)。
- **Q-53～Q-71**：19 项（VoteAward 实际入账、READ_COMMITTED、悬赏锁、搜索投影防御等）已在本轮修复，但 Q-53/54/57/59 等仍需真实 MySQL 交错 IT；Q-63 已于 2026-11-27 在本地真实 MySQL 核对（非零账户缺 marker 0 条）。Q-64（排行榜 rebuild 窗口）、Q-65（浏览量故障注入）、Q-66（回答/评论删除通知）、Q-67（角色提升积分，待 PO）、Q-68（对账缺口）、Q-69/70（多实例）、Q-71（Outbox 跨模块）仍是已知限制/未验证项。完整状态见 [`defects.md`](defects.md) 末尾。
- 前端只有 8 条组件测试（悬赏对话框 6 条 + 管理页 2 条），通知分页/未读数、权限渲染、错误码映射等关键路径仍缺自动化测试。
- 投票并发、Outbox 乱序、缓存代际等并发场景仍需要真实 MySQL/Redis 集成测试或故障注入验证。
