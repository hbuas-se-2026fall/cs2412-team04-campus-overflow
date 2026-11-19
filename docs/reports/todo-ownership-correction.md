# 遗留 TODO 归属核查记录（2026-11-19）

> 背景：一份流传于群/会话中的"遗留 TODO 归属表"把三条代码问题分别归给了陈明哲/魏志国、青岑、青岑（默认）。本文用 `git log` / `git show` 证据逐条核对，纠正归属口径，避免后续评审按错误的负责人追踪。
>
> 核查方法：**归属以提交历史为准，不以"模块名像谁的角色"推断**。

---

## 一、先澄清一个高频误解：`co-qa` ≠ QA 角色

| 项 | 事实 | 证据 |
| --- | --- | --- |
| `co-qa` 是什么 | **Q&A（Question & Answer）限界上下文**：问题、回答、投票、评论、采纳 | [`docs/final/01-architecture.md:16`](../final/01-architecture.md)「co-qa　Q&A：问题、回答、投票、评论、采纳」；同文件 §上下文映射表 |
| "QA" 角色是什么 | **Quality Assurance（测试计划与回归）**，是团队角色，不是模块 | [`docs/team.md`](../team.md) 角色表：青岑 = Developer / QA |
| 结论 | 两者只是缩写相同。`co-qa` 模块的代码归属按**提交历史**判定，与谁担任 QA 角色无关 | — |

---

## 二、三条归属逐条核对

### 1. `co-identity` 的 `User.java`

| 项 | 结论 |
| --- | --- |
| 传播表里的写法 | Identity 模块 owner（陈明哲/魏志国），"按表归陈/魏合理" |
| **实际归属** | **课程模板导入，无个人实现提交** |
| 证据 | `git log -- User.java` 仅 `620a67c cmz666-BOT init: 导入课程模板代码` 一条；该文件当前为完整实现（`USERNAME`/`EMAIL` 正则校验、不可变字段），无 TODO |
| 建议口径 | "模板导入（cmz666-BOT），`co-identity` 由陈明哲/魏志国按角色表共同维护；具体代码无个人归属"——不写"owner=某人" |

### 2. `co-qa-application` 的 `QuestionCommandService.java`

| 项 | 结论 |
| --- | --- |
| 传播表里的写法 | 归青岑 |
| **实际归属** | **刘俊琒（A）** |
| 证据 | `git show -s f217e5f` → `Liujunfeng520`，提交信息 `feat(qa): implement QuestionCommandService and QuestionQueryService`；`docs/scrum/sprint-1.md` 亦记 A 负责该服务 |
| 建议口径 | "刘俊琒（f217e5f）"，传播表的写法是错的 |

### 3. `co-qa-infrastructure` 的 `RedisQuestionListCache.java`

| 项 | 结论 |
| --- | --- |
| 传播表里的写法 | "需确认，至少不是默认归青岑" |
| **实际归属** | **课程模板导入，无个人实现提交**（与第 1 条同理） |
| 证据 | `git log -- RedisQuestionListCache.java` 仅 `620a67c cmz666-BOT init: 导入课程模板代码`；文件当前已实现（TTL 抖动防雪崩、序列化失败降级），无 TODO |
| 建议口径 | "模板导入（cmz666-BOT）；缓存写入路径属 Q&A 上下文公共设施";若需指定维护人，按角色表由 PO 指派并单独记录，**不得默认落给 QA 角色** |

---

## 三、结论

1. **三处代码均无遗留 TODO**（`grep -l TODO` 三文件均为空），不存在"待修"事项；
2. 传播表的三条归属中，第 2 条（刘俊琒）成立，第 1、3 条应改为"模板导入、无个人归属"；
3. **`co-qa` 是 Q&A 上下文，与 QA 角色无关**——这是本次纠错的核心；
4. 归属口径建议写进 Team Agreement 的评审 SLA 一节：**涉及"谁负责某段代码"时，以 `git log`/`git blame` 为唯一依据，禁止按角色名推测**。

## 四、复核

| 复核人 | 结论 |
| --- | --- |
| 魏志国（PO） | 已按上述证据核查并收口；本文即更正记录 |
