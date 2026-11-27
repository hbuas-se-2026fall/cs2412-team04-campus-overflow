# 个人总结 · 房国栋

> 角色：Developer（D 组 · 前端与验证）　Sprint 3 兼任 SM
> 提交日期：2026-11-18

## 基本信息

| 项 | 内容 |
| --- | --- |
| 姓名 | 房国栋 |
| 角色 | Developer（前端 + 验证）；Sprint 3 的 Scrum Master |
| 负责模块 | 前端全部页面（课程页、问答闭环、悬赏、声誉、排行榜、搜索、通知）、Discovery 通知中心、Reputation 排行榜与悬赏前端、环境与身份核查（E3/E4） |
| 参与的 Sprint | S0 / S1 / S2 / S3 / S4 |

## 一、我负责的交付物（带证据）

| 交付物 | 证据（文件 / PR / CI） | 结果 |
| --- | --- | --- |
| 课程页（列表 / 我的课程 / 加入课程）与提问页选课 | `frontend/src/pages/courses/*.tsx`、`AskPage.tsx` 课程下拉 | ✅ 注册→登录→课程列表→加入课程全流程可点通 |
| 问答闭环前端（编辑 / 删除 / 评论 / 采纳 / 投票） | `QuestionDetailPage.tsx`，按钮显隐由后端 `permissions` 驱动 | ✅ S1 演示 8 条 Given/When/Then 可在界面演完 |
| 错误码中文映射 | `frontend/src/errors.ts`（36 个业务码 + HTTP 状态兜底；2026-11-28 勘误：PR #253 补悬赏错误码后由 25 增至 36） | ✅ `QA-3001/2002/1015/1020`、`REP-1002/1003/1004` 均有中文提示 |
| 通知中心后端（T3-03/06/07） | `MentionParser.java`、`Notification.java`、`NotificationProjector.java`、`NotificationService.java` | ✅ `DiscoveryDomainTest` 6/6、`NotificationProjectorTest` 7/7、`NotificationServiceTest` 3/3 通过；`(eventId, recipient)` 幂等（原写 2/2 是早期批次） |
| 徽章墙与排行榜周期切换的前端呈现 | `pages/ReputationPage.tsx`（六枚徽章墙 + 未解锁置灰）、`pages/LeaderboardPage.tsx`（周/月/学期/总榜四处切换） | ✅ 徽章墙与四周期切换可点通；**领域侧 `BadgePolicy`/`AcademicTerm` 的实现由 E（提交 `d07dab91`，作者 `cmz666-BOT`）完成，不在本人提交内**（审计 2-20 更正） |
| 排行榜周期切换 + 悬赏对话框 + 声誉主页（T2-13） | `LeaderboardPage.tsx`、`components/BountyDialog.tsx`、`pages/ReputationPage.tsx` | ✅ 四周期可切；悬赏倒计时（剩 1 小时变红、过期「已结束」） |
| 搜索页与通知（T3-10） | `pages/SearchPage.tsx`、`NotificationsPage.tsx` | ✅ 标签/未回答/悬赏筛选 + 四排序 + 分页 |
| Markdown 实时预览与代码高亮（T4-03） | `components/MarkdownPreview.tsx`，`AskPage.tsx` 编辑/预览/分屏 | ✅ `marked` + 按需注册的 `highlight.js`，原始 HTML 已禁用；产物 **约 339 kB（gzip 约 109 kB，取 `npm run build` 输出；数字随代码变化）**。**注**：PR #151 合入时间为 2026-11-17 15:50（北京），晚于 v1.0.0 tag（11:01），属 tag 后增量，**不计入 v1.0.0 交付**（审计 2-13 更正） |
| 环境证据（E3） | `docs/lab1-submission/env-matrix.md`、`env/`、`scripts/dev-env/collect-env.*` | ✅ 5/5 人采集齐；本人 Docker 29.9.3 与 `mvn-test` 全绿 165 用例均已补 |
| 身份绑定核查（E4） | `docs/lab1-submission/identity-audit.md`、`scripts/dev-env/audit-authors.*` | ✅ 定位「邮箱绑在个人小号、课程账号贡献为 0」并整改为 noreply |
| ADR-003 主讲（T0-05） | `docs/notes/architecture.md` §9（2026-11-28 勘误：主讲笔记在 §9，§8 为分工表） | ✅ 含代码落点表与 `RedisLoginThrottle` 逐行讲解 |
| Sprint 3 过程记录（SM） | `docs/scrum/sprint-3.md` | ⚠️ **实际是压缩执行**：只做了「开工同步」与「收口同步」两次，**未出计划会纪要、未开正式 Sprint Review、未单独出回顾纪要**；燃尽图已归档（审计 2-18 更正） |

## 二、技术收获（2～3 条）

1. **「权限由后端给，前端只做渲染」是可维护的分界线。**
   例子：详情页原来写 `if (role === 'TEACHER')` 决定按钮，规则一改前端就错；改成读 `permissions.canEdit / canAccept / canManage` 后，换账号登录按钮自动跟着变，前端一行角色判断都没有。

2. **幂等要做在消费端，而不是指望事件只投递一次。**
   例子：通知处理器先用 `exists(sourceEventId, recipientId)` 查再存，排行榜先用 `markProcessed(eventId, "reputation.leaderboard")` 占位；这样评审把 `outbox_event.published_at` 置空重放，声誉与通知都不会重复变化。

3. **时间类规则必须写清「边界归属」，否则一定被测出来。**
   例子：`AcademicTerm.termKey` 里 1 月属于**上一年的秋季学期**（`2027-01 → 2026-FALL`），周键必须用 `IsoFields.WEEK_BASED_YEAR` 而不是 `Calendar.WEEK_OF_YEAR`，跨年那一周两者会差一年。

## 三、协作与流程（PR / 评审 / 会议）

- **发起或被评审的 PR**：#38、#39（Lab1–3 文档与课程域）、#100（悬赏对话框 + 声誉主页 + 排行榜）、#81（课程页 E1）、文档 PR（E3 死链、Docker 状态、T0-05 ADR 笔记）等。其中 #100、#81 各有非作者 Approve。
- **主持或参与的会议**：Sprint 3 期间因 S2 收口与 S4 发布交叠，实际只做了**开工同步**（11-16 09:00）与**收口同步**（11-17 08:30）两次，**未召开计划会、未按周一/三/五逐日站会、未开正式 Review**；Sprint Review 由 §八 PO 验收结论代替。S4 阶段参与演示环节（搜索/通知/管理页段落）与**彩排组织**；两次彩排实际均处于「脚本就绪、未完成顺利通过」状态（[`rehearsal-log.md`](rehearsal-log.md) §一 为「待执行」，审计 2-18 更正）。
- **对团队流程的改进**：
  1. 提出并落地「一个任务一个 Issue、一个改动一个 PR」的看板留痕，避免最后补文档；
  2. 把环境采集与身份审计做成脚本（`scripts/dev-env/`），把「口头确认版本」变成「可粘贴的表格行」；
  3. CI 红灯时先看第一个失败步骤，并沉淀了「`mvn test` 红是因为 `TODO` 桩、不是环境问题」的排查口径。

## 四、不足与改进（具体、可执行）

| 不足 | 影响 | 下次怎么做 |
| --- | --- | --- |
| 早期把实验要求的文档路径当成自己独占（把演示脚本写到 SM 的 `sprint-1.md`，导致 PR 冲突） | 一次 rebase 冲突与返工 | 动笔前先 `git log -- 该路径` 看有没有人维护，新内容另起文件名并在原文件加指路链接 |
| 本地 git remote 没跟上仓库迁移，代码推到旧仓库 | 评审方在团队仓库看不到 E1，产生一轮往返澄清 | 每周一核对 `git remote -v` 与团队公告；推送后立刻在 PR 里确认链接可达 |
| Docker 拖到最后一周才装，期间 E3 缺证据、干净环境部署验证只能转嫁他人（最终 11-18 自行补齐，并顺带发现 `backend.Dockerfile` 的 jar 通配符缺陷） | E3 一度只能做到 4/5；全栈 compose 构建路径长期未被验证 | 环境类任务在 Sprint **第一天**完成安装并截图；「跑一次完整 compose」应进 Sprint 计划而非临到发布才做 |
| 前端新增依赖时没先看产物体积（highlight.js 全量引入使包体从 203 kB 涨到 1.2 MB） | 首屏加载变慢 | 引入依赖前先看 bundle 分析，按需注册子模块（本次改为 `lib/core` + 10 种语言，回落到 **约 339 kB（gzip 约 109 kB）**；早期文档写的 314/334 kB 都是不同批次，已改成「约」并注明取 `npm run build` 输出） |
| 演示脚本写得详细，但彩排次数不够 | S4 彩排只有脚本、未完成两次顺利通过 | 把「彩排」当成有工时的任务排进 Sprint，而不是评审前顺手做 |

## 五、自评

- **对团队目标的贡献**：承担了除管理页（T4-05，青岑）之外的前端页面与验证类任务，并补齐了通知中心后端；徽章规则的后端实现由陈明哲提交，本人负责徽章墙与周期榜的前端呈现；把环境与身份核查从口头确认变成可复核的材料，v1.0.0 的演示路径在前端侧全部可点通。
- **需要团队/老师支持的地方**：① 发布流程中 tag 与 Release Notes 的分工由 SM/PO 主导，我配合提供前端变更段落；② 彩排希望全员到场并计时，避免演示当日第一次跑完整流程。（原第①项「Docker 环境」已于 2026-11-18 自行解决，按审计 P-12 删除）
- **签字 / 日期**：房国栋　2026-11-25（原签 2026-11-18；因 §六「更正说明」于 11-25 补记，签字日期一并顺延以避免正文与签字不一致，审计 P-11）

---

## 六、更正说明（2026-11-25，审计 2-18/2-13/2-20）

本文原版有三处与仓库事实不符，已按 git 历史与 PR 时间线改正：

| 原表述 | 事实 | 处理 |
| --- | --- | --- |
| 「徽章规则与学期键（T2-08）」列入本人交付物 | `BadgePolicy.java`、`AcademicTerm.java` 的全部提交作者为 `cmz666-BOT`（`620a67c6` 脚手架、`d07dab91` 实现） | 改为只声明前端徽章墙/周期切换等实际本人的部分 |
| 「T4-03 Markdown 预览」列为交付物 | PR #151 合入时间 2026-11-17T07:50:41Z（北京 15:50），晚于 v1.0.0 tag | 标注为 tag 后增量，不计入 v1.0.0 |
| 「计划会、站会、回顾与行动项」「两次发布彩排」 | `sprint-3.md` 只记录了 2 次同步；`rehearsal-log.md` 两次彩排均为「待执行」 | 改为「压缩执行：2 次同步」，彩排状态改为待执行 |

> 与本文同时更正的关联文档：`docs/scrum/sprint-3.md` §五（CI 门禁收紧时间与 BUG-004 状态）、`docs/reports/s4-compose-fullstack-verify.md`（compose 命令与端口口径）。
