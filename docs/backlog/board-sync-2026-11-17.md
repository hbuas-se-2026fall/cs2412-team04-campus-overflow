# 看板状态同步记录（48 条 Issue / Projects 48 卡）

> 执行：刘俊琒（A）　日期：2026-11-17（v1.0.0 发布当日）；**2026-11-19 复核更新**
> 背景：v1.0.0 已发布时，48 条 Issue 中只有少量处于关闭状态，且**全部**仍挂 `status:backlog`——
> 看板上 48 张卡看起来"一条都没做完"。本记录说明同步口径、已执行动作，以及人工步骤（Projects 导入）的执行结果。

---

## 一、同步口径（可复核）

| 判据 | 说明 |
| --- | --- |
| 交付 | 该 PBI 的 PR 已合并 `develop`，且 `docs/final/` 有对应证据 |
| 不做 | v1.0.0 范围冻结时判定移出迭代，留决策评论（见 [`s4-deferred-items-decision.md`](../reports/s4-deferred-items-decision.md)） |
| Won't | V3 Backlog 优先级即为 Won't，不进入任何迭代 |

**不使用** Issue 正文里的 `- [ ]` 清单作为完成判据：48 条正文是同一模板，勾选框从未被逐条勾选，
用它判断会得出"全部未完成"的错误结论（这也是本次发现的过程缺陷，已记入 [`04-process.md`](../final/04-process.md) §4.4）。

## 二、同步结果（以 Issue 状态为准，`status:*` 标签已全部退役）

| 状态 | 条数 | Issue 编号 |
| --- | ---: | --- |
| 已交付 → `closed`（completed） | **44** | #27–#66（PB-01～PB-40）、#68（PB-42）、#70（PB-44）、#72（PB-46）、#73（PB-47） |
| 不做 → `open`（保留为 v1.1 候选，附决策评论） | **3** | #67（PB-41）、#69（PB-43）、#71（PB-45） |
| Won't → `closed`（not planned，附决策评论） | **1** | #74（PB-48） |
| 合计 | **48** | #27–#74 |

**标签口径变更（2026-11-17 执行，2026-11-19 复核）**：原先用 `status:done` / `status:backlog` 两种标签兼作状态，
导致"已关闭却仍挂 backlog"的 31 条历史遗留（#27–#56、#58）与看板显示不一致。同步时**不再改标签，而是把 `status:*`
标签整族移除**——状态只由 Issue 的 open/closed 与 `state_reason`（completed / not planned）表达，看板列由 Projects
内置工作流按关闭事件自动流转。复核结果：仓库现存标签中已无任何 `status:*`（`type:* / ctx:* / priority:* / scope:*` 保留），
因此"标签与实际状态不符"这类问题从机制上不再可能出现。

**逐条核对后的证据覆盖情况（2026-11-19 用 API 数了一遍，不修饰）**：48 条 PBI 中**只有 20 条带评论**
（#33、#34，以及 #57–#74 的 S3/S4 批次），另 28 条（#27–#32、#35–#56）**没有任何评论**——这些是 S0/S1 早期批次，
当时按"PR 合并即视为交付"处理，没有逐条回填证据；`#36/#40/#42/#45`（PB-10/14/16/19）也在其中。

- **影响要提前说清**：答辩时**不能**对所有已关闭项说"点开任意一条即可对上代码"，只能对 #33/#34 与 #57–#74 这么说；
  其余证据链以 PR 列表（#78/#86/#91/#92/…）+ [`docs/final/`](../final/README.md) 的交付清单为准。
- 这是本次同步没能补齐的过程缺陷（当时只做了状态同步、没做证据回填），已归入
  [`04-process.md`](../final/04-process.md) §4.4 的同类问题；补评论的代价不大但会污染 28 条 Issue 的时间线，
  留作 v1.1 规划时的规约（**关闭前置条件：必须有一条带 PR 编号的证据评论**）而非事后补写。
- 三条不做项**不从看板删除**，保留 open 作为 v1.1 候选，避免需求在下次规划时"消失"。

## 三、Projects 看板导入：已由本人完成（2026-11-17）

自动化受限的原因保留在此备查：GitHub Projects v2 的读写需要 classic PAT 的 **`project`**（读为 `read:project`）授权，
本项目令牌只有 `gist / repo / workflow`，调用 `projectsV2` 返回 `INSUFFICIENT_SCOPES`。因此导入这一步由本人网页操作：

1. Issues 列表筛选 `label:type:story` → **Select all**（48 条）→ **Projects** → 选看板；
2. 看板 **⋯ → Workflows** 打开内置自动化：`Item closed` → **Status: Done**、`Pull request merged` → **Status: Done**；
3. 导入后看板应为 **Done 45 / 未开始 3**（Done 含 PB-48 这张 Won't 卡，其决策评论见
   [`s4-deferred-items-decision.md`](../reports/s4-deferred-items-decision.md)），与第二节的 44 + 1 / 3 对齐。

> **复核边界（如实说明）**：本机的 API 令牌仍无 `read:project`，脚本无法直接读回看板条目数，
> 上述"已导入"与计数来自 2026-11-17 网页操作后的核对，2026-11-19 未能用脚本二次验证。
> 复核方式：打开 Projects 页签，用筛选 `is:issue` 对照 §四 的 Issue 基数逐列点数；若出现偏差以 Issue 侧为准。
> 需要把 Iteration / Priority / SP 字段也批量写入时，给令牌补 `project` 授权即可，脚本侧
> `addProjectV2ItemById` + `updateProjectV2ItemFieldValue` 已备好，不用逐卡点。

## 四、复核用命令

```bash
# 1) 48 条 PBI 的状态分布（state + state_reason），复现 §二 的 44 / 3 / 1
curl -s "https://api.github.com/repos/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues?state=all&per_page=100" \
  | python -c "import sys,json,collections; d=[i for i in json.load(sys.stdin) if 'pull_request' not in i and 27<=i['number']<=74]; print(len(d), collections.Counter((i['state'], i['state_reason']) for i in d))"

# 2) 确认 status:* 标签已退役（输出应为空列表）
curl -s "https://api.github.com/repos/hbuas-se-2026fall/cs2412-team04-campus-overflow/labels?per_page=100" \
  | python -c "import sys,json; print([l['name'] for l in json.load(sys.stdin) if l['name'].startswith('status:')])"
```
