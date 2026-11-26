# v1.0 最终测试报告（青岑本地复核）

> 日期：2026-11-25
> 基线：`develop` + 本次 QA 修复分支（含 Q&A 并发、悬赏可见性、缓存/浏览量修复）
> 环境：Windows 11 / JDK 21.0.10 / Maven 3.9.2；前端 Node.js
> 说明：本报告只记录本轮实际跑过的命令；Testcontainers 集成测试需 CI 的 Docker runner，本地未跑。

## 一、后端单元测试

```bash
cd backend
export JAVA_HOME="E:/java内容/jaavaa/jdk-21.0.10"
export PATH="$JAVA_HOME/bin:/e/java内容/IntelliJ IDEA Community Edition 2023.2.2/plugins/maven/lib/maven3/bin:$PATH"
mvn -o test -Djacoco.skip=true -DskipITs
```

结果：**BUILD SUCCESS，237 个测试，0 失败 / 0 错误**。

| 模块 | 测试数 |
| --- | ---: |
| `co-shared-kernel` | 3 |
| `co-identity-domain` | 13 |
| `co-identity-application` | 7 |
| `co-identity-infrastructure` | 1 |
| `co-qa-domain` | 42 |
| `co-qa-api` | 8 |
| `co-qa-application` | 56 |
| `co-qa-infrastructure` | 4 |
| `co-reputation-domain` | 20 |
| `co-reputation-application` | 50 |
| `co-reputation-infrastructure` | 1 |
| `co-discovery-domain` | 6 |
| `co-discovery-application` | 25 |
| `co-bootstrap` | 1（ArchUnit） |

> 另有 `CampusOverflowFlowIT` 4 条集成测试由 failsafe 执行，需要 Docker/Testcontainers，本地未跑；
> 完整口径为 **237 单测 + 4 集成 = 241**。

## 二、前端

```bash
cd frontend
npm run typecheck
npm run test:run
npm run build
```

结果：**typecheck 通过，Vitest 4 条通过（`BountyDialog.test.tsx`），Vite build 通过**。

## 三、未覆盖项

- `CampusOverflowFlowIT`（Testcontainers MySQL 8 + Redis 7，4 条）未在本地运行；必须在 CI `集成测试` 阶段通过后才能宣称端到端完成。
- JaCoCo 覆盖率本轮跳过（`-Djacoco.skip=true`）；覆盖率数字需在最终 `mvn verify` 重采。
- 前端只有 4 条组件测试（悬赏对话框），通知、权限、错误映射等关键路径仍缺自动化测试。
- 投票并发、Outbox 乱序、缓存代际等并发场景仍需要真实 MySQL/Redis 集成测试或故障注入验证。
