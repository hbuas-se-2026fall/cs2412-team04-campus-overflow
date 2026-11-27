# v1.0 最终测试报告（青岑本地复核）

> 日期：2026-11-27
> 基线：`develop` `6cf5e5c` + P5 修复分支（含 Q&A 并发、悬赏可见性、缓存/浏览量、通知删除、初始积分幂等、投票上限实际入账值修复）
> 环境：Windows 11 / JDK 21.0.10 / Maven 3.9.2；前端 Node.js
> 说明：本报告只记录本轮实际跑过的命令；Testcontainers 集成测试需 CI 的 Docker runner，本地未跑。
> 复跑记录：2026-11-27 在 P5 工作分支上重新执行下方命令，结果为 277 测试全绿。

## 一、后端单元测试

```bash
cd backend
export JAVA_HOME="E:/java内容/jaavaa/jdk-21.0.10"
export PATH="$JAVA_HOME/bin:/e/java内容/IntelliJ IDEA Community Edition 2023.2.2/plugins/maven/lib/maven3/bin:$PATH"
mvn -o test -Djacoco.skip=true -DskipITs
```

结果：**BUILD SUCCESS，277 个测试，0 失败 / 0 错误**。

| 模块 | 测试数 |
| --- | ---: |
| `co-shared-kernel` | 3 |
| `co-identity-domain` | 13 |
| `co-identity-application` | 8 |
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

> 另有 `CampusOverflowFlowIT` 4 条集成测试由 failsafe 执行，需要 Docker/Testcontainers，本地未跑；
> 完整口径为 **277 单测 + 4 集成 = 281**。

## 二、前端

```bash
cd frontend
npm run typecheck
npm run test:run
npm run build
```

结果：**typecheck 通过，Vitest 6 条通过（`BountyDialog.test.tsx`），Vite build 通过**。

## 三、未覆盖项

- `CampusOverflowFlowIT`（Testcontainers MySQL 8 + Redis 7，4 条）未在本地运行；CI 已在 `62332d2` 上跑过该 4 条（run 45742927110），但 #248 之后的当前 HEAD `6cf5e5c` 仍需重跑后才能宣称端到端完成。
- JaCoCo 覆盖率本轮跳过（`-Djacoco.skip=true`）；覆盖率数字需在最终 `mvn verify` 重采。
- 前端只有 6 条组件测试（悬赏对话框门控 4 条 + 加载失败态 2 条），通知分页/未读数、权限渲染、错误码映射等关键路径仍缺自动化测试（审计 2-12 记录为已知缺口）。
- 投票并发、Outbox 乱序、缓存代际等并发场景仍需要真实 MySQL/Redis 集成测试或故障注入验证。
