# T4-05 管理页证据

> 对应任务：Sprint 4 · T4-05 管理页（待认证学生、认证、建教职工、改角色）
> 采集地址：`http://127.0.0.1:5173/admin`
> 采集日期：2026-11-17（pending/disable 于 2026-11-27 用当前构建重拍）

| 文件 | 场景 | 图中要点 |
| --- | --- | --- |
| [`admin-users-pending.png`](admin-users-pending.png) | 管理员登录后进入 `/admin` | 右上角 `ADMIN`、导航有「管理」；当前待认证学生 2 人（`SeedUser`/`TestStudent`），含认证/改角色/禁用按钮、分页（2026-11-17 历史截图，见文末说明） |
| [`admin-forbidden.png`](admin-forbidden.png) | 学生登录后直接访问 `/admin` | 导航没有「管理」；页面显示「无权限访问 / 管理页仅管理员可用」 |
| [`admin-verify.png`](admin-verify.png) | 对 `TestStudent` 点「认证」 | 绿色提示“已认证 TestStudent”；待认证列表从 3 人变 2 人（2026-11-17 历史截图，见文末说明） |
| [`admin-create-staff.png`](admin-create-staff.png) | 创建教职工账号 `T9001` | 绿色提示“教职工账号已创建”；已认证用户出现“测试教师 / 教师” |
| [`admin-change-role.png`](admin-change-role.png) | 将 `测试教师` 从教师改为助教 | 绿色提示“已将 测试教师 的角色改为助教”；角色列显示“助教” |
| [`admin-disable.png`](admin-disable.png) | 对 `TestStudent` 点「禁用」并确认 | 绿色提示“已禁用 TestStudent，其现有会话已失效”；角色列显示“学生（已禁用）”，操作区显示“已禁用”，认证/改角色/禁用按钮均消失（2026-11-27 当前构建） |

对应实现：`frontend/src/pages/AdminUsersPage.tsx`、`frontend/src/App.tsx` 的 `RequireAdmin` 路由守卫。
后端接口：`GET /api/v1/admin/users?verified=false`、`POST /api/v1/admin/users/{id}/verify`、
`POST /api/v1/admin/users`、`PUT /api/v1/admin/users/{id}/role`、`PUT /api/v1/admin/users/{id}/disable`。

> 说明：`admin-users-pending.png` 与 `admin-disable.png` 已用当前构建（2026-11-27）重拍；`admin-verify/create-staff/change-role` 仍是 2026-11-17 历史截图（当时待认证列表 3 人、`T9001` 为手工创建），不作为当前 seeder 的可复现证据。
> 当前 `DemoDataSeeder` 会创建 `SeedUser`、`TestStudent` 两个待认证账号；管理页「禁用」按钮会撤销目标账号现有会话。
