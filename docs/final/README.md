# CampusOverflow · 最终答辩材料（docs/final）

> 维护：陈明哲（E，Sprint 4 SM）　创建：2026-11-16
> 用途：Sprint 4 答辩与 v1.0.0 发布的材料索引；答辩前只填内容，不再改结构。

## 六节结构

| # | 章节 | 文件 | 内容 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | 架构 | [`01-architecture.md`](01-architecture.md) | 模块化单体 + 六边形分层、4 上下文、关键流程 | 已完成 |
| 2 | 关键 ADR | [`02-adr.md`](02-adr.md) | ADR-001～010 一句话决策与影响 | 已完成 |
| 3 | 质量证据 | [`03-quality.md`](03-quality.md) | 测试/覆盖率/CI/冒烟/缺陷台账 | 已完成（2026-11-25 口径；Sonar Gate **PASSED**） |
| 4 | 过程证据 | [`04-process.md`](04-process.md) | Sprint 0～4 记录、燃尽图、评审与回顾 | 已完成 |
| 5 | 演示脚本 | [`05-demo-script.md`](05-demo-script.md) | ≤15 分钟彩排脚本与 ≤5 分钟视频分镜 | 已完成 |
| 6 | 个人总结 | [`06-personal-summary-template.md`](06-personal-summary-template.md) | 模板 + 每人一份 | 已完成（5/5） |

## 配套材料

- 用户手册：[`user-guide.md`](user-guide.md)
- 演示视频（**5:00，1280×870，十段字幕分镜**）：[`demo-v1.0.0-5min.mp4`](demo-v1.0.0-5min.mp4)（分镜见 [`05-demo-script.md`](05-demo-script.md) §5.3）
- 早期短版演示视频（55s，1280×800）：[`demo-v1.0.0.webm`](demo-v1.0.0.webm)
- 彩排排期与计时：[`rehearsal-log.md`](rehearsal-log.md)
- 青岑彩排计时稿：[`rehearsal-qingcen.md`](rehearsal-qingcen.md)
- 个人总结：[陈明哲](personal-summary-chenmingzhe.md)、[青岑](personal-summary-qingcen.md)、[魏志国](personal-summary-weizhiguo.md)、[刘俊琒](personal-summary-liujunfeng.md)、[房国栋](personal-summary-fangguodong.md)
- DDD 原文：[`docs/ddd/`](../ddd/)（上下文映射、事件风暴、词汇表）
- ADR 原文：[`docs/adr/`](../adr/README.md)（含 ADR-010 禁用账号与会话清理）
- T4-05 管理页截图：[`screenshots/admin/`](screenshots/admin/README.md)
- V3 Projects 看板截图（48 PBI / Sprint Backlog）：[`screenshots/projects/`](screenshots/projects/README.md)
- Discovery 覆盖率截图与 CSV：[`screenshots/coverage/`](screenshots/coverage/README.md)
- 覆盖率与测试报告：CI Actions 构件 + [`03-quality.md`](03-quality.md) + [`screenshots/coverage/`](screenshots/coverage/README.md)
- 发布记录：[`docs/release/v0.3.0.md`](../release/v0.3.0.md)、[`docs/reports/release-v0.2.0-v0.3.0.md`](../reports/release-v0.2.0-v0.3.0.md)
- v1.0.0 Release Notes：[`docs/release/v1.0.0.md`](../release/v1.0.0.md)（**已发布**：[Release v1.0.0](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/releases/tag/v1.0.0)，tag → `10925c7`）
- v1.0.0 发布验证：[`docs/reports/release-v1.0.0-verification.md`](../reports/release-v1.0.0-verification.md)
- Sprint 4 计划/执行：[`docs/scrum/sprint-4.md`](../scrum/sprint-4.md)（已发布 v1.0.0；仅 T4-08 彩排待执行，SM 陈明哲）
- 禁用账号验证：[`docs/reports/s4-disable-account.md`](../reports/s4-disable-account.md)（接口 + 测试 + 截图）

## 交付检查（对应指导书 9.4）

> 口径说明：`v1.0.0` tag 内的 `docs/final` 是发布时快照（当时只有 2 份个人总结、没有 5 分钟视频/四域覆盖率证据）；下面的 ✅ 指 **main 最新的最终归档**，tag 本身不包含后期补齐的材料。


- [x] main 上的 `v1.0.0` 与完整提交历史（tag `v1.0.0` → `10925c7`）
- [x] GitHub Release（[v1.0.0](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/releases/tag/v1.0.0)，含 Release Notes 与部署说明）
- [x] `docs/ddd/`、`docs/adr/`（S3 已就位）
- [x] `docs/scrum/sprint-0～4`（S4 记录已入库）
- [x] 覆盖率报告归档（`docs/final/screenshots/coverage/`）
- [x] `docs/final` 答辩材料 + ≤5 分钟演示视频（`demo-v1.0.0-5min.mp4`，5:00 字幕分镜版；另有 55s 短版 `demo-v1.0.0.webm`）
- [x] 每人一份个人总结（5/5）
