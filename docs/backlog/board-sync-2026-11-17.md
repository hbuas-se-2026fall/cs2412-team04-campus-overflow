# 看板状态同步记录（48 条 Issue / Projects 48 卡）

> 执行：刘俊琒（A）　日期：2026-11-17（v1.0.0 发布当日）
> 背景：v1.0.0 已发布，但 48 条 Issue 中只有少量处于关闭状态，且**全部**仍挂 `status:backlog`——
> 看板上 48 张卡看起来"一条都没做完"。本记录说明同步口径、已执行动作，以及仍需人工完成的一步。

---

## 一、同步口径（可复核）

| 判据 | 说明 |
| --- | --- |
| 交付 | 该 PBI 的 PR 已合并 `develop`，且 `docs/final/` 有对应证据 |
| 不做 | v1.0.0 范围冻结时判定移出迭代，留决策评论（见 [`s4-deferred-items-decision.md`](../reports/s4-deferred-items-decision.md)） |
| Won't | V3 Backlog 优先级即为 Won't，不进入任何迭代 |

**不使用** Issue 正文里的 `- [ ]` 清单作为完成判据：48 条正文是同一模板，勾选框从未被逐条勾选，
用它判断会得出"全部未完成"的错误结论（这也是本次发现的过程缺陷，已记入 [`04-process.md`](../final/04-process.md) §4.4）。

## 二、同步结果

| 状态 | 条数 | Issue 编号 |
| --- | ---: | --- |
| 已交付 → `closed`（completed）+ `status:done` | **44** | #27–#66（PB-01～PB-40）、#68（PB-42）、#70（PB-44）、#72（PB-46）、#73（PB-47） |
| 不做 → `open` + 决策评论 + 保留 `status:backlog` | **3** | #67（PB-41）、#69（PB-43）、#71（PB-45） |
| Won't → `closed`（not planned）+ 决策评论 | **1** | #74（PB-48） |

- 每张关闭的 Issue 都附一条**交付证据评论**（对应 PR 编号 + 证据文件路径），答辩时点开任意一条即可对上代码；
- 三条不做项**不从看板删除**，保留 open 作为 v1.1 候选，避免需求在下次规划时"消失"；
- 历史遗留的 31 条（#27–#56、#58）原本已关闭却仍挂 `status:backlog`，本次一并改成 `status:done`。

## 三、仍需人工完成：Projects 看板导入（1 步）

自动化受限：GitHub Projects v2 的读写需要 classic PAT 的 **`project`**（读为 `read:project`）授权，
当前令牌只有 `gist / repo / workflow`，调用 `projectsV2` 会返回 `INSUFFICIENT_SCOPES`。
因此"把 48 张卡导入 Projects"必须由本人（或 PO）在网页上点一次，之后可由工作流自动维护状态：

1. 打开 <https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues?q=is%3Aissue>，筛选 `label:type:story`；
2. 右上角 **Select all**（若提示超过单页，点提示里的 `Select all N issues`）→ **Projects** → 选已有看板或 **New project**；
3. 看板右上 **⋯ → Workflows** 打开内置自动化：
   - `Item closed` → **Status: Done**（关闭即归档到 Done 列）
   - `Pull request merged` → **Status: Done**
4. 导入后看板会自动呈现：**Done 44 / Backlog 3 / 已关闭未计划 1**，与本文第二节一致。

> 若需要我把剩余字段（Iteration、Priority、SP）也批量写入，把令牌加上 `project` 授权后告诉我，
> 我用 GraphQL `addProjectV2ItemById` + `updateProjectV2ItemFieldValue` 一次跑完，不用你逐卡点。

## 四、复核用命令

```bash
# 当前 48 条 Issue 的状态分布（需已登录的 gh 或 API 令牌）
curl -s "https://api.github.com/repos/hbuas-se-2026fall/cs2412-team04-campus-overflow/issues?state=all&per_page=100" \
  | python -c "import sys,json,collections; d=json.load(sys.stdin); print(collections.Counter(i['state'] for i in d if i['number']>=27))"
```
