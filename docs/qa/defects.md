# Sprint 0 缺陷与差距清单

> 负责人：青岑（QA）
> 记录规则：严重程度（高/中/低）、复现步骤、预期、实际、负责人、状态
> 说明：部分条目基于仓库与 GitHub 页面静态核查；2026-11-15 由陈明哲（E）按仓库树、GitHub API 与 Actions 记录复核，更新了已解决项的状态与证据。

| 编号 | 严重程度 | 模块 | 现象 | 复现/证据 | 预期 | 实际 | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|
| BUG-001 | 高 | 课程页 | 前端没有课程页，无法在 UI 完成课程列表/我的课程/加入课程 | `frontend/src/App.tsx` 已注册 `/courses`、`/courses/mine`；页面在 `frontend/src/pages/courses/`（`CourseListPage.tsx`、`MyCoursesPage.tsx`），PR #81 合入 | 存在课程页，支持列表/我的课程/加入 | 已具备 | 房国栋 | 已修复 |
| BUG-002 | 中 | CI | PR 上没有 CI 检查运行 | `.github/workflows/ci.yml` 已在 `pull_request` 上触发；PR #96、#101、#103 的 Checks 均运行且为绿（后端构建与验证 / 前端构建） | PR 触发 CI 并显示检查结果 | 已触发并显示 | 刘俊琒 | 已修复 |
| BUG-003 | 中 | 分支保护 | develop 允许管理员绕过，main/develop 未启用必需状态检查 | 已修复（2026-11-17）：陈明哲启用仓库规则集 #27436601「develop required checks」，对 `refs/heads/develop` 要求 `后端构建与验证`、`前端构建`（`bypass_actors` 为空）；classic protection 保留 `enforce_admins=true`、`required_approving_review_count=1`，`checks[].app_id=15368`（Actions）。`main` 为 2 Approve + 后端/前端/容器三项。PR #139/#140 的 check-runs 结论均 success | 不允许绕过；必需 CI 检查开启 | 已开启，管理员不可绕过 | 刘俊琒/魏志国 | 已修复 |
| BUG-004 | 中 | 文档一致性 | 事件风暴图 6 上下文/28 事件，与 V3 4 上下文和 D1 30 事件不一致 | 已修复：PR #75 合入（commit `0d7da3d`）重建 `docs/ddd/event-storming.png`（4 上下文/30 事件，与 `docs/ddd/events.md` 逐条一致）；`glossary.md:25-27` 改为“发现上下文(co-discovery)”；`context-map.md/png` 为 4 上下文、无伙伴关系 | 4 个上下文；30 事件；通知归 Discovery | 已一致 | 魏志国/陈明哲 | 已修复 |
| BUG-005 | 低 | 流程 | 房国栋提交使用 baigui 身份，Contributors 只有 4 人 | 新提交已绑定 GitHub 账号：`房国栋 <333220585+2177023844@users.noreply.github.com>`；历史 `baigui` 提交仍保留在 git 历史中（不改写历史） | 提交邮箱绑定 GitHub 账号 2177023844 | 新提交已绑定 | 房国栋 | 已修复（新提交） |
| BUG-006 | 低 | 文档 | `docs/team.md`、`docs/team-agreement.md`、ADR-009 缺失 | 三个文件均已存在：`docs/team.md`、`docs/team-agreement.md`、`docs/adr/009-渐进式质量门禁.md` | V3 C2/C3/C4/A4 要求存在 | 均存在 | 陈明哲/刘俊琒 | 已修复 |

## Q&A 查询与命令侧缺陷复核（2026-11-24，刘俊琒）

> 来源：SM 分派的 QA-01 / QA-02 / QA-04 三条（截图口径）。本节记录核查结论与代码处置，避免后续重复讨论。

| 编号 | 级别 | 结论 | 处置 |
|---|---|---|---|
| QA-01 | P1 已确认（查询侧） | 属实：管理者删除已采纳回答后 `Question.acceptedAnswerId` 未清空，导致「已采纳但无回答可展示」且无法重新采纳；搜索投影 `accepted` 也不清 | **已修复**（见下方证据） |
| QA-02 | P1 需求冲突（查询主责） | 前提「课程内问题仅成员可见」当前不成立：`IdentityApi` 无成员查询方法，`qa_question` 无可见范围字段，`US-04.feature:22-25` 的场景无数据模型支撑 | **暂缓**，待产品规则确认后排期（详见下方决策依据） |
| QA-04 | P2 潜在（写路径） | 与 QA-02 同一前提；成员制未确立前补写路径校验等于提前锁定未定规则 | **暂缓**，与 QA-02 一并决策 |

### QA-01 修复证据

采纳标记的唯一事实来源是 `Question.acceptedAnswerId`（见 `Answer.java` 类注释）。删除回答时必须同步清理，否则读模型过滤掉已删回答后会出现幽灵采纳态。四处改动：

1. 领域层：`Question.clearAccepted(Actor, boolean, Instant)`，仅课程管理者可撤销、幂等、已删问题拒绝。
2. 应用层：`AnswerCommandService.delete()` 记录 `wasAccepted`，删除已采纳回答时清空标记、保存问题、发布新事件 `AnswerAcceptanceClearedEvent` 并记审计。
3. 查询层：`QuestionQueryService.detail()` 计算 `effectiveAcceptedId`——已采纳回答若不在可见列表（已删）则按未采纳处理，兜住历史脏数据，`canAccept` 随之恢复。
4. 投影层：`SearchIndex.clearAccepted()` + `JdbcSearchIndex` SQL + `SearchIndexProjector` 监听新事件清 `dis_search_doc.accepted`，沿用 `ProcessedEventStore` 幂等。

事件 `AnswerAcceptanceClearedEvent` 字段顺序（eventId → occurredAt → answerId → questionId）已加入 `EventFieldOrderContractTest`。回归测试覆盖：领域层撤销后可重新采纳／非管理者拒绝／幂等；应用层删除已采纳回答清标记并发事件、删普通回答不误清；查询层幽灵采纳兜底与正常采纳保持；投影层清标记幂等。

验证：`mvn -o -pl co-qa/co-qa-api,co-qa/co-qa-domain,co-qa/co-qa-application,co-discovery/co-discovery-domain,co-discovery/co-discovery-application,co-discovery/co-discovery-infrastructure -am test` 共 111 测试，0 失败 / 0 错误 / 0 跳过。

### QA-02 / QA-04 暂缓的决策依据

- `docs/screenshots/s1-states/README.md:21-22` 已写明：「若产品要求课程内问题仅成员可见……这是一个待确认的产品规则，建议在 v1.1 排期时明确。」
- `docs/scrum/sprint-1-demo.md` 同源记录：S1 演示实测未加入课程的新账号也能查看课程内问题，当时用前端管理页 403 代替取证，并建议 v1.1 明确规则。
- 代码现状：`IdentityApi` 只暴露 `canManageCourse`（教师/助教/管理员），没有 `isCourseMember` 一类方法；`qa_question` 表无可见范围字段；因此 `US-04.feature:22-25` 的「仅特定用户可见」缺少实现载体。
- 若现在补成员制，需同时改 Identity OHS、list/detail/search 的 viewer 传参、缓存 key 维度、写路径校验与前端 403 分支；在规则未确认前实施，可能与最终产品口径冲突，故暂缓。

**解除暂缓的条件**：产品明确「课程内问题是否仅成员可见」并落到验收标准；届时需一并完成 QA-02（读）与 QA-04（写），避免只读不写造成新的不一致。

## 复核记录

- 2026-11-15：陈明哲复核 BUG-001/002/003/005/006，依据仓库文件树、GitHub Branch Protection API、Actions 运行记录；BUG-004 保留待修复。
- 2026-11-16：魏志国复核 BUG-004：`docs/ddd/` 三份文档已统一（4 上下文/30 事件，通知归 Discovery），缺陷关闭。BUG-003 维持“部分修复”：develop 必需状态检查仍未启用（按 V3 需在 CI 稳定后开启），另发现发布阻塞项——`ci.yml` 的 `容器镜像构建` 仅 push 触发，目标为 `main` 的发布 PR 永远等不到该必需检查，已在 `chore/s2-closeout` 修正触发条件。
- T1-15 冒烟脚本已补齐：`scripts/smoke/smoke.ps1`、`scripts/smoke/smoke.sh`，用法见 `scripts/smoke/README.md`。
- 2026-11-16：陈明哲将 ADR-009 从 `docs/adv/` 迁到 `docs/adr/009-渐进式质量门禁.md`，并补 ADR 索引与 Sprint 0 记录（PR #118）。
- 2026-11-17：陈明哲通过仓库规则集 #27436601 为 develop 启用必需检查（后端构建与验证、前端构建），BUG-003 关闭。刘俊琒同日按 API 复核：classic protection 的 `checks[].app_id=15368`、规则集 `bypass_actors` 为空、PR #139/#140 的 check-runs 均 success；另一条判断依据是 `容器镜像构建` 的 `if` 限定 `push` 或 `base=main`，故只挂 `main`——若加到 `develop`，普通 PR 会永久等待一个不会上报的检查。
