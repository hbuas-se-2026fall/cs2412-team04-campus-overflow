# v1.0 范围决策记录 · PO（2026-11-25，2026-11-25 晚修订）

> 发起：青岑审计（QA-02 / REP-02 标记为「需求冲突，PO 决策」）。本文是 PO（魏志国）对两项冲突的正式裁决，供后端、前端、演示脚本与用户手册按同一口径执行。
> 关联：[`docs/final/user-guide.md`](../final/user-guide.md)、[`docs/backlog/stories/US-04.feature`](../backlog/stories/US-04.feature)、[`docs/scrum/sprint-1-demo.md`](../scrum/sprint-1-demo.md)

---

## 决策 1（审计 QA-02 / 复核 W-01）：问题可见性 = **课程成员可见**

> ⚠️ **本决策已修订**：初版（同日）裁定为「站内公开」，依据是 `SecurityConfig` 对 `GET /api/v1/questions/**` 的 `permitAll`。
> 青岑复核（W-01）指出**代码、测试、前端实际都是「课程成员可见」**——初版只看了安全配置层，漏了 `QuestionQueryService.requireCanView()`。
> 经逐行核对，**以已实现且更合理的「课程成员可见」为准，撤销「站内公开」的初版裁决**。

| 项 | 内容 |
| --- | --- |
| 实际实现（已核对的代码事实） | `QuestionQueryService.requireCanView()`（`co-qa-application`）：<br>① `courseId == null`（未挂课程的公开问题）→ **所有人可见（含未登录）**；<br>② 课程问题 → 未登录 **403 `QA-3006`**；需 `ADMIN` / **问题作者** / **课程成员**之一，否则 403 `QA-3006`「无权查看此内容」 |
| 列表侧 | `list()`：非管理员只能查自己**已加入课程**的 `courseId`，否则 `ForbiddenException("QA-3006", "无权查看该课程的问题")`；未传 `courseId` 时按 `visibleCourseIds` 过滤 |
| 前端 | `QuestionDetailPage` 有 `forbidden` 态（「没有查看权限 / 这个问题仅对课程成员开放」+「去课程页加入」）；`CourseCard` 未加入时按钮为「加入后查看课程问题」 |
| **决策（v1.0）** | **课程成员可见**：未挂课程的公开问题对所有访问者可见；**课程内问题仅课程成员 / 作者 / 管理员可见**，未加入者看到「无权限」引导去加入课程 |
| 影响 | ① `US-04.feature` **两条无权限场景均为 v1.0 已实现功能**，撤销先前的 `@v1.1-candidate` 标记；② 演示脚本按「未加入课程的学生打开课程问题 → 无权限页」即可演示（无需改用 `/admin` 方案，保留两者）；③ `user-guide` 的可见性口径按本表更新；④ 代码无需改动 |
| v1.1 计划 | 若需要「公开课程问题 / 私密问题」等更细粒度，再引入 `visibility` 字段；当前不做 |

## 决策 2（审计 REP-02）：悬赏**只能挂在课程问题上**

| 项 | 内容 |
| --- | --- |
| 冲突 | `BountyService.open` 把 `question.courseId()`（公开问题为 `null`）传给 `canManageCourse`；`IdentityApiImpl` 对 `courseId == null` 直接返回 `role == TEACHER` → **任意全局 TEACHER 都能对公开问题发起悬赏**；而 `docs/final/user-guide.md` 写「必须是该课程的问题」 |
| **决策** | **以文档为准**：悬赏必须是课程问题，公开问题（`courseId == null`）一律拒绝 |
| 实现 | `BountyService.open` 前置校验：`courseId == null` → `BusinessRuleException("REP-1007", "公开问题不能发起悬赏，请选择所属课程的问题")`；随后才做 questionOpen / canManageCourse / 单 OPEN 检查 |
| 测试 | `QingcenBountyServiceTest.openRejectsBountyOnPublicQuestion`（公开问题 → REP-1007 且不触碰账户） |
| 说明 | 教师若确实想悬赏某个公开问题，应先把问题关联课程——当前编辑接口不支持改课程，属 **v1.1 候选**（届时在 Issue 中登记） |

## 决策 3（审计 W-06）：公开问题的管理权 = **作者本人 + ADMIN**

| 项 | 内容 |
| --- | --- |
| 冲突 | `IdentityApiImpl.canManageCourse(userId, null)` 原返回 `role == TEACHER` → **任意全局 TEACHER** 都能编辑/删除他人的公开问题、替他人采纳、教师认证回答（REP-02 只拦了悬赏） |
| **决策** | 公开问题（未关联课程）没有课程归属，**全局 TEACHER 不再自动获得管理权**；仅 **作者本人**（各聚合的 author 分支放行）与 **ADMIN** 可管理 |
| 实现 | `IdentityApiImpl.canManageCourse`：`courseId == null` → `return false`（ADMIN 已在上方提前返回 true） |
| 测试 | 新增 `IdentityApiImplPermissionTest` 4 条：TEACHER 对公开问题 false / ADMIN true / 课程问题按课程内角色 / 非成员 false |
| 影响面 | `QuestionCommandService.edit/delete`、`AnswerCommandService.accept/endorse/delete` 在公开问题上只允许作者或管理员；qa 模块 45 条测试全绿 |

## 决策 4（审计 W-07 / PRIV-01）：课程成员名单 = **仅登录可读**

| 项 | 内容 |
| --- | --- |
| 问题 | `GET /api/v1/courses/*/members` 在 `SecurityConfig` 里 `permitAll`，匿名可拿到 `userId/displayName/role` |
| **决策** | 属校内人员信息，**不对匿名开放**；仅登录用户可读（v1.0 不做更细的"仅课程成员"限制） |
| 实现 | `SecurityConfig` 从 `permitAll` 列表移除 `/api/v1/courses/*/members`，落到 `anyRequest().authenticated()` |
| 影响 | 前端无匿名调用（已 grep 确认无引用）；匿名访问该端点将返回 401 |

## 执行与复核

| 人 | 事项 | 状态 |
| --- | --- | --- |
| 魏志国（PO） | 四项裁决 + REP-02/W-06 代码与测试 + W-07 安全配置 + 文档同步；**W-01 复核后修订决策 1** | ✅ 已完成（2026-11-25） |
| 青岑（QA） | 审计提出方，复核裁决与实现是否一致 | ✅ 决策 1/2 已复核并 APPROVED（PR #222）；✅ 决策 3/4 于 2026-11-27 复核通过（`IdentityApiImpl.canManageCourse` 已补 4 条测试；`SecurityConfig` 已移除 members 的 permitAll） |
| 陈明哲 / 刘俊琒 | 决策 1：`SecurityConfig` 的 `permitAll` 只放行到控制器、服务层做课程成员校验，维持现状即符合；决策 4 已由 PO 直接改配置，请 SM 复核 | SM 复核仍为 —（不阻塞代码，但文档签字未闭环） |
