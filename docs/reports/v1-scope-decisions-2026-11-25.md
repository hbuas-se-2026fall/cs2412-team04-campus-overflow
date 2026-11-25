# v1.0 范围决策记录 · PO（2026-11-25）

> 发起：青岑审计（QA-02 / REP-02 标记为「需求冲突，PO 决策」）。本文是 PO（魏志国）对两项冲突的正式裁决，供后端、前端、演示脚本与用户手册按同一口径执行。
> 关联：[`docs/final/user-guide.md`](../final/user-guide.md)、[`docs/backlog/stories/US-04.feature`](../backlog/stories/US-04.feature)、[`docs/scrum/sprint-1-demo.md`](../scrum/sprint-1-demo.md)

---

## 决策 1（审计 QA-02）：问题可见性 = **站内公开**

| 项 | 内容 |
| --- | --- |
| 冲突 | 后端 `SecurityConfig` 对 `GET /api/v1/questions/**` 等路径 `permitAll`，`QuestionQueryService.detail()` 不校验课程成员；而 `US-04.feature` 有「无权限查看受限问题」场景、演示脚本原设想用「未加入课程的学生」触发无权限 |
| V3 依据 | V3 未要求「按课程成员限制问题可见」；"受限问题"在数据模型里不存在（`Question` 只有 `deleted`，没有 visibility 字段） |
| **决策（v1.0）** | **站内公开**：所有**未删除**的问题对所有访问者可见（含未登录），课程只是分类/标签，**不构成可见性边界**；已删除问题统一 404 |
| 影响 | ① `US-04.feature` 的「无权限查看受限问题」场景标记为 **v1.1 候选**，不作为 v1.0 验收；② 演示脚本的"无权限界面"改用现有 `/admin` 的 403 演示（已有该分支）；③ 用户手册不再声称课程外不可见 |
| v1.1 计划 | 若引入私密课程/问题可见性：新增 `visibility` 或课程成员校验 + `SecurityConfig` 按课程成员授权 + 启用 US-04 该场景 |

## 决策 2（审计 REP-02）：悬赏**只能挂在课程问题上**

| 项 | 内容 |
| --- | --- |
| 冲突 | `BountyService.open` 把 `question.courseId()`（公开问题为 `null`）传给 `canManageCourse`；`IdentityApiImpl` 对 `courseId == null` 直接返回 `role == TEACHER` → **任意全局 TEACHER 都能对公开问题发起悬赏**；而 `docs/final/user-guide.md:110` 写「必须是该课程的问题」 |
| **决策** | **以文档为准**：悬赏必须是课程问题，公开问题（`courseId == null`）一律拒绝 |
| 实现 | `BountyService.open` 在取到 `QuestionRef` 后**前置**校验：`courseId == null` → `BusinessRuleException("REP-2003", "公开问题不支持悬赏，请选择课程问题")`；随后才做 questionOpen / canManageCourse / 单 OPEN 检查 |
| 测试 | `QingcenBountyServiceTest.openRejectsBountyOnPublicQuestion`（公开问题 → REP-2003 且不触碰账户） |
| 说明 | 教师若确实想悬赏某个公开问题，应先把问题关联课程——当前编辑接口不支持改课程，属 **v1.1 候选**（届时在 Issue 中登记） |

## 执行与复核

| 人 | 事项 | 状态 |
| --- | --- | --- |
| 魏志国（PO） | 两项裁决 + REP-02 代码/测试 + 文档同步 | ✅ 本 PR |
| 青岑（QA） | 审计提出方，复核两项裁决与实现是否一致 | ⏳ 请复核 |
| 陈明哲/刘俊琒 | 无需改动（`SecurityConfig` 维持现状即符合决策 1） | — |
