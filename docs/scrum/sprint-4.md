# Sprint 4 · 稳定化与发布 v1.0.0

> Scrum Master：陈明哲（E）　PO / 发布经理：魏志国
> 范围：稳定化、禁用账号、可观测性、v1.0.0 发布、彩排
> 状态：**已发布 v1.0.0（2026-11-17）**；T4-08 彩排待执行。
> 更新：T4-02（#127）、T4-04（#128）、T4-05（#129）、T4-07（#130）已合入；T4-01 已完成（PR #135 / tag v1.0.0 / Release），干净环境**全栈 compose 构建**已于 2026-11-18 验证并修复 Dockerfile 缺陷；T4-08 彩排待执行。

## 一、Sprint 目标

1. 完成 `develop → main` 发布 PR（≥2 Approve），打 `v1.0.0` 标签并发布 GitHub Release；
2. 管理员可禁用账号，被禁用者旧会话立即失效且不能再登录；
3. 四面板可观测性上线，`co_reputation_mismatches` 可演示造 1 归零；
4. 清理 9.4 交付物：覆盖率报告、docs/final 索引、彩排记录、个人总结。

## 二、任务与状态

| 任务 | 内容 | 负责人 | 状态 |
| --- | --- | --- | --- |
| T4-02 | `AdminUserService.disable` + `PUT /api/v1/admin/users/{userId}/disable` + 会话撤销 + 审计 + 测试 | 魏志国 | ✅ #127；验证见 [`s4-disable-account.md`](../reports/s4-disable-account.md) |
| T4-04 | Grafana 四面板 + `co_reputation_mismatches` 定时刷新 + HTTP P95 直方图 | 陈明哲 | ✅ #128；实测见 [`t4-04-observability.md`](../reports/t4-04-observability.md) |
| T4-03 | Markdown 实时预览 + 代码高亮（PB-42，S4/Should/3 SP） | 房国栋 | ✅ #151 |
| T4-05 | 管理页：待认证学生、认证、建教职工、改角色 | 青岑 | ✅ #129；截图 `docs/final/screenshots/admin/` |
| T4-07 | README、ADR-010、用户手册 | 青岑 | ✅ #130 |
| T4-01 | develop→main 发布 PR、v1.0.0 tag、GitHub Release、部署验证 | 陈明哲（SM）/ 魏志国（PO 审批） | ✅ PR #135 → `10925c7`；tag `v1.0.0`；[Release](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/releases/tag/v1.0.0) |
| T4-08 | 两次彩排：≤15 分钟、每人 ≥2 分钟、两次顺利 | 陈明哲组织 / 全员 | ⬜ 第一次 2026-11-17、第二次发布后，见 [`rehearsal-log.md`](../final/rehearsal-log.md) |
| T4-06 | 容量项（PB-41/43/45）范围决策：做或不做都要留痕 | 房国栋（记录：刘俊琒） | ✅ 三项均不做，理由/替代/后续见 [`s4-deferred-items-decision.md`](../reports/s4-deferred-items-decision.md) |
| 9.4 交付物 | 七项索引、个人总结、≤5 分钟视频 | 全员 | 🟨 索引、视频（`docs/final/demo-v1.0.0.webm`）与 **5/5 个人总结**（陈明哲、青岑、魏志国、刘俊琒、房国栋）已入库（#154）；仅剩两次彩排记录 |

## 三、发布检查（T4-01）

- [x] `develop` CI：后端构建与验证 / 前端构建绿；容器镜像由 `main`/PR→main 的 `images` job 覆盖
- [x] 全仓 27 模块 `mvn verify -DskipITs -Djacoco.skip=false` 通过（2026-11-17，JDK 21）
- [x] 覆盖率：qa 83.12% / identity 75.74% / reputation 81.93% / discovery 78.26%，均 ≥0.70（口径：`docs/final/screenshots/coverage/*.csv` 的 LINE COVERED / TOTAL）
- [x] T1-15 冒烟 `SMOKE PASS`；demo profile 数据可加载
- [x] `develop → main` 发布 PR #135（魏志国、青岑 Approve，三检查绿）
- [x] `v1.0.0` tag 指向 main 合并提交 `10925c7`
- [x] GitHub Release（[v1.0.0](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/releases/tag/v1.0.0)）
- [x] 干净环境 `docker compose up -d mysql redis` + backend 启动、health=UP（[`s4-clean-env-deploy-verify.md`](../reports/s4-clean-env-deploy-verify.md)，#150）

## 四、验收标准

- [x] v1.0.0 tag 与 GitHub Release 可见
- [x] 禁用账号：非 ADMIN 403、禁自己 ID-1007、禁用后旧会话失效且登录失败（[`s4-disable-account.md`](../reports/s4-disable-account.md)）
- [x] `mvn verify` / CI 全绿，覆盖率报告可点开（`docs/final/screenshots/coverage/`）
- [x] 干净环境按 README 启动并完成注册→登录→提问→回答（compose 基础设施 #150 + 全新库全流程 [`s4-clean-env-full-flow.md`](../reports/s4-clean-env-full-flow.md)）
- [x] 干净环境 `docker compose -f deploy/docker-compose.yml up -d --build` **全栈**构建运行（backend + web 四容器、Nginx 80 端口反代后端 API、health=UP）—— [`s4-compose-fullstack-verify.md`](../reports/s4-compose-fullstack-verify.md)，2026-11-18；本次同时修复 `deploy/backend.Dockerfile` 的 COPY 通配符缺陷（`co-bootstrap-*.jar` → `campus-overflow.jar`），并补 `.dockerignore`
- [x] `docs/final/README.md` 七项全部有可点链接（5 份个人总结已齐）
- [ ] 两次彩排记录（≤15 分钟、每人 ≥2 分钟）

## 五、已知阻塞

1. v1.0.0 发布 PR 需要 ≥2 Approve（含 SM/PO），且 main 的 `容器镜像构建` 必需检查需随 PR 触发（ci.yml 已支持 base=main）。
2. 彩排需全员到场；当前只有脚本、计时表和青岑的讲稿。
3. 5 分钟视频已入库；个人总结 **5/5 已交齐**（#154），本条阻塞关闭。
4. 干净环境部署已在有 Docker 的机器上验证（#150 基础设施 healthy、#156 全新库全流程），本条阻塞关闭；仅「两次彩排」仍待执行（T4-08）。

## 六、PO 验收结论（对照 V3 §9.1 七维度）

> 验收人：魏志国（PO）　日期：2026-11-25（补记，审计 W-09：S4 此前只有发布检查，无 PO 验收签字）

| # | 9.1 维度 | 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 可运行软件 | ✅ 通过（本地/容器） | compose 全栈四容器 + health=UP（`s4-compose-fullstack-verify.md`）；冒烟 9/9、悬赏 17/17、S4 禁用账号 5/5 |
| 2 | 提交历史 | ✅ 通过 | S4 相关 PR：#127–#130（T4-02/04/05/07）、#135（T4-01 发布）、#162（CI 收紧）、#222（审计修复） |
| 3 | 贡献者分布 | ✅ 通过 | 5 人个人总结 5/5（#154）；E4 身份核查 5/5 |
| 4 | PR 与评审 | ✅ 通过 | 所有 S4 PR 经非作者 Approve；release PR 满足 main 的 2 Approve |
| 5 | Issue | ✅ 通过 | PB-40/42/44/46/47 对应 Issue 关闭；不做的 PB-41/43/45 留决策评论作 v1.1 候选 |
| 6 | 分支 | ✅ 通过 | main/develop 双保护 + 必需检查；发布走 develop→main PR |
| 7 | CI / Scrum 工件 | ✅ 通过 | 集成测试必须绿；覆盖率 70.3%（Sonar）；本文件 + 彩排脚本 + 演示视频 |

**Sprint 4 结论：条件通过** —— 交付物与质量检查全部通过，**唯一未完成项是 T4-08（两次彩排）**；
另：`v1.0.0` tag 指向 2026-11-17 的 `10925c7`，不含 11-18 之后的安全/质量修复（审计 W-02），
需在所有 P1 修复合入后**重新走 develop→main 并重打 tag/Release**（计划见下）。

### 6.1 重发计划（W-02）

1. 前置：所有 P1 审计项（W-01~W-07 的代码部分、SEC 系列）合入 `develop` 且 CI 全绿；
2. 动作：`develop → main` 发布 PR（2 Approve + 三项必需检查）→ 打 `v1.0.1` tag → 以 `docs/release/` 更新后的 Release Notes 发布；
3. 验收：tag 内 `docs/final` 为最终版、个人总结 5 份齐全、彩排记录已填。
