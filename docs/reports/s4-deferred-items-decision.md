# T4-06 · 容量项范围决策记录（PB-41 / PB-43 / PB-45）

> 记录人：刘俊琒（A）　复核：房国栋（D，T4-06 负责人）、魏志国（B，PO）
> 日期：2026-11-17（v1.0.0 发布当日）
> 背景：Sprint 4 在 2026-11-17 发布 `v1.0.0` 并冻结范围，T4-06 要求「容量项做或不做都要留痕」。本文即该留痕，三条 Could 项一律**不做**，理由、替代能力与后续处置逐条记录。

---

## 一、决策摘要

| PBI | 用户故事 | 优先级 | SP | Issue | 决策 | 处置 |
| --- | --- | --- | ---: | --- | --- | --- |
| PB-41 | 个人资料查看与修改 | Could | 3 | [#67](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues/67) | **v1.0.0 不做**（仅保留查看） | 移入 v1.1 候选；Issue 留决策评论 |
| PB-43 | 通知红点 SSE 实时刷新 | Could | 5 | [#69](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues/69) | **v1.0.0 不做** | 移入 v1.1 候选；SSE 基础设施另立 tech-debt |
| PB-45 | 问题列表压测与缓存 | Could | 3 | [#71](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues/71) | **缓存已在，压测不做** | 拆分为「已交付」+「压测」两部分，压测转 tech-debt |

合计 **11 SP** 从 S4 迭代移出：S4 计划 26 SP（PB-40～PB-47），实际交付 **15 SP**，与 [`sprint-4.md`](../scrum/sprint-4.md) 的发布范围一致。

---

## 二、逐条理由与证据

### PB-41 个人资料查看与修改（不做修改，只保留查看）

**现状证据**

- 查看：`GET /api/v1/auth/me` 已返回 `MeView`（昵称、角色、课程），见 `backend/co-identity/co-identity-interfaces/.../AuthController.java`。
- 修改：`co-identity-interfaces` 仅有 `AuthController` / `AdminUserController` / `CourseController` 三个控制器，**无 `PATCH /api/v1/users/me`**；`UserRegistrationService` 之后没有任何资料更新用例。

**不做的理由**

1. 修改资料会牵动唯一性约束（昵称/邮箱去重）、审计与通知投影三处，属于跨聚合改动，S4 冻结期引入回归风险高于演示价值；
2. 课程项目内资料由管理员在 `AdminUsersPage` 侧维护已够用，学生自助改昵称不是答辩关注点。

**替代能力**：查看已有（`/auth/me`，前端顶部栏与声誉页均在用）；改昵称/角色由管理员页面完成。

**后续**：v1.1 候选，需先补 `NicknameUniqueness` 与审计事件，再开用例。

### PB-43 通知红点 SSE 实时刷新（不做实时推送）

**现状证据**

- 前端 `frontend/src/pages/NotificationsPage.tsx` 为**拉取式**：进入页面 `load()`，另有「刷新」按钮，无 `EventSource`、无 `setInterval` 轮询。
- 后端通知写入走事务性发件箱（ADR-002）→ `NotificationProjector`，没有 `/api/v1/notifications/stream` 端点。

**不做的理由**

1. SSE 需要长连接与连接生命周期管理，与当前的会话/网关配置（Servlet 线程模型、`co-bootstrap` 无异步线程池约定）冲突，改造成本远超 5 SP 的估值；
2. 演示路径上"进入通知页即拉最新"已足够说明通知链路闭环，实时性不是验收项。

**替代能力**：页面进入拉取 + 手动「刷新」按钮；未读数在页面头部计算展示。

**后续**：v1.1 若做，建议整体上 WebSocket/SSE 广播而非只在通知页，需新开 ADR（连接鉴权与会话撤销联动，参考 ADR-010）。

### PB-45 问题列表压测与缓存（缓存已交付，压测不做）

**现状证据**

- 缓存**已实现并接线**：端口 `co-qa-application/.../port/QuestionListCache.java`；实现 `co-qa-infrastructure/.../cache/RedisQuestionListCache.java`；`QuestionQueryService` 读缓存，`QuestionCommandService` / `AnswerCommandService` 在写侧失效。
- 压测**未做**：仓库内无 `*.jmx`（JMeter）、无 Gatling 工程、无任何容量指标文档。

**不做的理由**

1. 课程项目为单机 demo profile（MySQL + Redis 单节点），压测结论无法外推到真实部署，写出来的 QPS/P95 数字反而容易被追问口径；
2. 性能相关的可观测性已由 T4-04 覆盖（Grafana HTTP P95 直方图 + `co_reputation_mismatches`），足以回答"服务是否变慢"。

**替代能力**：Redis 列表缓存 + Grafana HTTP P95 面板（[`t4-04-observability.md`](t4-04-observability.md)）。

**后续**：压测转为 tech-debt，若 v1.1 引入多实例部署，再按 docker compose 环境补 k6 脚本与基线。

---

## 三、留痕方式

1. 本文档（`docs/reports/s4-deferred-items-decision.md`）：决策、理由、证据、替代、后续四段齐全；
2. 三个 Issue（#67 / #69 / #71）各留一条决策评论，链接本文，**不关闭 Issue**——保留为 v1.1 候选，避免看板上丢失需求；
3. [`product-backlog.md`](../backlog/product-backlog.md) 表格「归属迭代」列对三项标注 `S4（未做，见决策记录）`；
4. [`sprint-4.md`](../scrum/sprint-4.md) 任务表补 T4-06 行指向本文。

> 复核意见征集中：房国栋（是否同意 v1.1 候选排序）、魏志国（PO 是否认可移出迭代）。若有异议，在本文末尾追加「四、复核意见」而非改写结论，保留决策轨迹。
