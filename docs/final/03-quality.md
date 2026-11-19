# 3. 质量证据

> 答辩口径：先给结论（门禁全绿、覆盖率达标、端到端可跑），再按需展开 CI 与报告。
> 数据截至：2026-11-16（S3 收口口径）。

## 3.1 CI 门禁（五个后端阶段 + 前端）

`.github/workflows/ci.yml`：

| 阶段 | 命令 | 作用 |
| --- | --- | --- |
| 1 编译 | `mvn -f backend/pom.xml clean compile` | 全仓编译 |
| 2 单元测试 | `mvn test` | 全量单元测试（27 模块） |
| 3 架构约束 | `mvn test -pl co-bootstrap -Dtest=ArchitectureTest` | ArchUnit 分层/依赖（FF-4） |
| 4 覆盖率门禁 | `mvn verify -DskipITs -Djacoco.skip=false` | 全量 domain 行覆盖 ≥ 0.70 |
| 5 集成测试 | `mvn verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false` | 硬门禁：Testcontainers 起 MySQL/Redis，失败即阻断 |
| 前端 | `npm run typecheck` + `npm run build` | 类型检查与构建 |

S4 收口起删除 `SPRINT_MODULES`，单元测试与覆盖率门禁恢复全量模块；集成测试同时改为硬门禁。

## 3.2 测试与覆盖率

| 模块 | 测试数 | 行覆盖率 | 门槛 |
| --- | ---: | ---: | --- |
| co-shared-kernel | 2 | — | — |
| co-identity-domain | 9 | 75.74% | ✅ ≥70% |
| co-identity-application | 3 | — | — |
| co-qa-domain | 42 | 83.12% | ✅ |
| co-qa-application | 32 | — | — |
| co-reputation-domain | 20 | 81.93% | ✅ |
| co-reputation-application | 36 | — | — |
| co-discovery-domain | 6 | 78.26% | ✅ |
| co-discovery-application | 14 | — | — |
| co-bootstrap | 4（ArchUnit 1 + FlowIT 3） | — | 分层约束 + 端到端 |

> 覆盖率来自 JaCoCo `jacoco.csv` 汇总（LINE COVERED / (COVERED+MISSED)）；四个 domain 模块均通过 `check-domain-coverage`。

> 全量测试共 **168** 个用例（含 `CampusOverflowFlowIT` 3 个），数据来自 CI run 44213214268 的 surefire/failsafe XML；`@Test` 源码静态计数为 136 + 参数化展开/IT。

四个 domain 模块的报告截图与原始 CSV 逐模块归档（不复用同一张图，便于当场复核）：

| 模块 | 行覆盖 | 行覆盖率 | 分支覆盖 | 报告截图 | 原始 CSV |
| --- | ---: | ---: | ---: | --- | --- |
| `co-identity-domain` | 103 / 136 | **75.74%** | 40 / 66（60.61%） | [`coverage-identity.png`](screenshots/coverage/coverage-identity.png) | [`coverage-identity.csv`](screenshots/coverage/coverage-identity.csv) |
| `co-qa-domain` | 192 / 231 | **83.12%** | 71 / 90（78.89%） | [`coverage-qa.png`](screenshots/coverage/coverage-qa.png) | [`coverage-qa.csv`](screenshots/coverage/coverage-qa.csv) |
| `co-reputation-domain` | 136 / 166 | **81.93%** | 46 / 56（82.14%） | [`coverage-reputation.png`](screenshots/coverage/coverage-reputation.png) | [`coverage-reputation.csv`](screenshots/coverage/coverage-reputation.csv) |
| `co-discovery-domain` | 72 / 92 | **78.26%** | 41 / 60（68.33%） | [`coverage-discovery.png`](screenshots/coverage/coverage-discovery.png) | [`coverage-discovery.csv`](screenshots/coverage/coverage-discovery.csv) |

| | |
| :---: | :---: |
| ![co-identity-domain 75.74%](screenshots/coverage/coverage-identity.png) | ![co-qa-domain 83.12%](screenshots/coverage/coverage-qa.png) |
| ![co-reputation-domain 81.93%](screenshots/coverage/coverage-reputation.png) | ![co-discovery-domain 78.26%](screenshots/coverage/coverage-discovery.png) |

> 采集口径与复现命令见 [`screenshots/coverage/README.md`](screenshots/coverage/README.md)（采集人 陈明哲，2026-11-17，JDK 21 全仓 `mvn verify -DskipITs -Djacoco.skip=false` 后生成）。

## 3.3 端到端与冒烟

- **T1-15 主链路脚本**：CSRF → 注册 → 登录 → 发问题 → 另一账号回答 → 投票 → 采纳 → 详情校验，`SMOKE PASS`（2026-11-15，demo profile + MySQL/Redis）；用法见 [`scripts/smoke/README.md`](../../scripts/smoke/README.md)。
- **搜索**：单字关键词 422 `DIS-1001`；中文词命中依赖 `SearchIndexProjector` 投影（S3 已实现）。
- **声誉/悬赏**：采纳 +15、悬赏 50 分结算/自答退款/到期退款均有日志与测试（[`docs/scrum/sprint-2.md`](../scrum/sprint-2.md) §六）。
- **demo profile**：`DemoDataSeeder` 6 账号 / 1 课程 / 3 问题，重启幂等（[`docs/reports/t2-14-demo-profile.md`](../reports/t2-14-demo-profile.md)）。

## 3.4 缺陷台账

[`docs/qa/defects.md`](../qa/defects.md) 当前：

| 状态 | 条目 |
| --- | --- |
| 已修复 | BUG-001 课程页、BUG-002 PR 无 CI、BUG-003 分支保护必需检查、BUG-004 DDD 不一致、BUG-005 提交身份、BUG-006 团队文档/ADR-009 |

BUG-003 于 2026-11-17 复核关闭：`main` 为 2 Approve + 后端/前端/容器三项必需检查，`develop` 为 1 Approve + 后端/前端两项必需检查（规则集 `develop required checks`，`enforce_admins=true`）；`容器镜像构建` 因 `if` 限定 `push` / `base=main`，只挂 `main`。证据见 [`docs/qa/defects.md`](../qa/defects.md#复核记录)。

## 3.5 发布验证

- `v0.1.0` → `704cd60`（S0 基线，补打）
- `v0.2.0` → `a381bb8`（S1 基线，补打）
- `v0.3.0` → `a4114dc`（main，PR #115）：CI 后端/前端/容器镜像三阶段绿
- `v0.9.0` → `579ea59`（S3 RC tag；正式内容随 v1.0.0 进 main）
- `v1.0.0` → `10925c7`（main，PR #135；GitHub Release）

详见 [`docs/reports/release-v0.2.0-v0.3.0.md`](../reports/release-v0.2.0-v0.3.0.md)。
- 可观测性四面板与对账指标演练：[`docs/reports/t4-04-observability.md`](../reports/t4-04-observability.md)

## 3.6 复现命令

```powershell
# 全仓（27 模块）构建与测试
mvn -B -f backend/pom.xml verify -DskipITs -Djacoco.skip=false

# CI 阶段 2：全量单元测试
mvn -B -f backend/pom.xml test

# CI 阶段 5：集成测试硬门禁（需 Docker）
mvn -B -f backend/pom.xml verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false
```

> 2026-11-16 实跑：全仓 27 个模块 `mvn verify -DskipITs -Djacoco.skip=false` → **BUILD SUCCESS**（含 ArchUnit 1 条）。

## 3.7 已知风险

| 风险 | 影响 | 处置 |
| --- | --- | --- |
| BUG-003（已关闭） | 曾因 develop 无必需状态检查，CI 失败不一定阻断合并 | 2026-11-17 通过规则集 #27436601 启用 `后端构建与验证`/`前端构建`；`enforce_admins=true`，改作业名需同步规则集 |
| 本地 JDK 25 下 JaCoCo 0.8.12 agent 不兼容 | 本机取证需 `-Djacoco.skip=true` | 统一用 JDK 21 取证；CI 固定 21 |
| 集成测试已硬门禁 | 失败即阻断，不再漏过端到端问题 | 阶段 5 移除了 `continue-on-error`，Testcontainers 起 MySQL/Redis；见 ADR-009 |
