# v1.0.0 发布验证记录

> 验证人：陈明哲（SM / E）　验证时间：2026-11-17
> 候选提交：`develop` 最新 `0073294`（lint：本轮发布 PR 的 head）
> 对应任务：指导书 T4-01；Sprint 4 记录：[`docs/scrum/sprint-4.md`](../scrum/sprint-4.md)

## 一、发布前检查

| # | 检查项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | CI 范围 | develop 后端 ✅ / 前端 ✅；发布 PR→main 另跑容器镜像 ✅ | GitHub Actions（`0073294` 为发布时候选，main `10925c7`） |
| 2 | 全仓 `mvn verify`（27 模块，含 ArchUnit） | **BUILD SUCCESS** | 2026-11-17 本地 JDK 21 实跑 |
| 3 | domain 覆盖率门槛 0.70 | qa 83.12% / identity 75.74% / reputation 81.93% / discovery 78.26% | JaCoCo `jacoco.csv` |
| 4 | T1-15 主链路冒烟 | `SMOKE PASS` | `scripts/smoke/README.md`，2026-11-15 demo 环境 |
| 5 | demo profile 启动 + 演示数据 | 启动成功；6 账号 / 1 课程 / 3 问题，重启幂等 | [`t2-14-demo-profile.md`](t2-14-demo-profile.md) |
| 6 | 可观测性四指标 | 三项自定义指标 + HTTP P95 直方图；对账 1→0 演练通过 | [`t4-04-observability.md`](t4-04-observability.md) |
| 7 | 禁用账号（T4-02） | 接口 + 3 条测试 + 真实 Redis 会话验证（旧 Cookie 401、重登失败、管理员未被误踢） | [`s4-disable-account.md`](s4-disable-account.md) |
| 8 | Release Notes | [`docs/release/v1.0.0.md`](../release/v1.0.0.md)（新功能/已知问题/部署步骤） | 本仓库 |

## 二、发布步骤与结果

1. 发布 PR [#135](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/pull/135)：魏志国、青岑 Approve，后端/前端/镜像三检查绿，已合并 `10925c7`；
2. annotated tag `v1.0.0` 打在 main `10925c7`；
3. GitHub Release 已发布：https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/releases/tag/v1.0.0；
4. [`docs/final/README.md`](../final/README.md) 已更新 9.4 勾选。

## 三、Tag 与历史版本

```text
v0.1.0 -> 704cd60   Sprint 0 基线（补打）
v0.2.0 -> a381bb8   Sprint 1 基线（补打）
v0.3.0 -> a4114dc   Sprint 2 发布（main）
v0.9.0 -> 579ea59   Sprint 3 RC（develop）
v1.0.0 -> 10925c7   v1.0.0 发布（main，PR #135）
```

## 四、遗留风险

| 风险 | 影响 | 处置 |
| --- | --- | --- |
| 发布 PR 审批 | 已闭环 | #135 获魏志国、青岑 Approve 后合并（`10925c7`） |
| 干净环境全栈 compose | ✅ 已闭环 | 房国栋 2026-11-18 在 Docker Desktop 29.9.3 上 `docker compose up -d --build` 起 backend/web 四容器，Nginx 80 反代，health=UP；见 [`s4-compose-fullstack-verify.md`](s4-compose-fullstack-verify.md) |
| `docs/final` 视频/个人总结 | ✅ 已闭环 | 5 分钟视频 + 5/5 个人总结已归档，9.4 七项全勾 |
| 两次彩排未执行 | T4-08 未闭环 | 按 [`rehearsal-log.md`](../final/rehearsal-log.md) 排期 |

## 五、干净环境部署验证

| 项 | 结果 | 说明 |
| --- | --- | --- |
| 镜像构建 | ✅ backend / frontend Dockerfile 均 build success | CI `容器镜像构建` job（main `10925c7`） |
| compose 配置 | ✅ 已入库并接入 observability profile | `deploy/docker-compose.yml`、`deploy/prometheus.yml`、`deploy/grafana/` |
| 运行时 `docker compose up -d mysql redis` + 后端启动 | ✅ 已在有 Docker 的机器完成 | 房国栋 PR #150：[`s4-clean-env-deploy-verify.md`](s4-clean-env-deploy-verify.md)（compose 两容器 healthy、后端 health=UP）；`sprint-4.md` 对应验收已勾 |
| 全新库全流程（注册→登录→提问→回答） | ✅ 已完成 | PR #156：[`s4-clean-env-full-flow.md`](s4-clean-env-full-flow.md) |
| 本机限制（历史记录） | 编写本报告时作者本机无 Docker | 已由 #150/#156 在有 Docker 的机器上补验，遗留关闭 |

## 六、签署

| 角色 | 成员 | 结论 | 日期 |
| --- | --- | --- | --- |
| SM / E | 陈明哲 | 发布完成：PR #135、tag v1.0.0、GitHub Release | 2026-11-17 |
| PO | 魏志国 | 已批准发布 PR #135 | 2026-11-17 |
