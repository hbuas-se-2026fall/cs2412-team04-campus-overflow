# CampusOverflow 架构理解笔记

> 负责人：青岑（Dev-1）　主讲：房国栋（§9 · ADR-003）
> 依据：arc42 第 2/3/5/6 章、ADR-001～010、《实验指导书 V3》第 2～3 章

## 1. 一句话架构

模块化单体 + 六边形分层：按 4 个限界上下文物理分模块，每个上下文内部再分 domain / application / infrastructure / interfaces 四层；跨上下文只通过 api 或领域事件集成。

## 2. 模块结构
backend/pom.xml（父 POM）
├── co-shared-kernel：Actor、DomainEvent、领域异常、分页
├── co-bootstrap：启动入口、装配、安全链、Flyway
├── co-identity：用户、课程、角色、会话（完整参考实现）
├── co-qa：问题、回答、投票、评论、采纳
├── co-reputation：声誉账本、徽章、悬赏、排行榜
└── co-discovery：搜索投影、通知

每个上下文内部：

- `co-xxx-api`：发布语言（DTO、事件契约）
- `co-xxx-domain`：聚合、值对象、领域服务、仓储接口
- `co-xxx-application`：用例编排、事务边界、事件发布
- `co-xxx-infrastructure`：JPA / Redis / JDBC 适配器、仓储实现
- `co-xxx-interfaces`：REST 控制器、请求/响应 DTO

## 3. 分层规则

| 层 | 允许依赖 | 禁止 |
|---|---|---|
| domain | JDK、shared-kernel | 框架、Spring、JPA、其他上下文 |
| application | domain、api、shared-kernel | 直接依赖 infrastructure 实现 |
| infrastructure | domain 端口、JPA/Redis | 被 domain 反向依赖 |
| interfaces | application、api | 在控制器里写业务规则 |

依赖方向：`interfaces → application → domain`；infrastructure 实现 domain 端口，由 Spring 注入。

## 4. 上下文边界

- Identity → Q&A / Discovery：OHS/PL，同步 `IdentityApi`（`UserSummary`/`CourseSummary`）；
- Identity → Reputation：同步 `IdentityApi` + 异步 `UserRegisteredEvent`（仅 Reputation 消费）。
- Q&A → Reputation：客户/供应商，Answer* / VoteCast / ContentDeleted(QUESTION) 事件 + QaQueryApi.findQuestion。
- Q&A → Discovery：遵奉者，Question* / Answer* / Vote / Comment / ContentDeleted 事件。
- Reputation → Discovery：遵奉者，Bounty* / BadgeAwarded 事件。
- 全部依赖 `co-shared-kernel`；修改共享内核需全组评审。

## 5. 关键机制

- **事务性发件箱**：业务数据与 `outbox_event` 同事务；中继异步投递；消费端按 `(event_id, user_id, reason)` 或 `processed_event` 幂等。
- **服务端会话**：Spring Session + Redis；SESSION Cookie 设置 HttpOnly、SameSite=Lax；CSRF 双提交；登录后轮换会话 ID；不用 JWT（ADR-003）。
- **软删除**：ContentDeletedEvent；读模型过滤 `deleted_at`。
- **原子计数**：score / answerCount / viewCount 由仓储原子 SQL 更新，不整行回写（ADR-008）。
- **Flyway**：每个上下文独立迁移目录；`ddl-auto: none`。
- **读模型**：Discovery 的 SearchDocument / Notification 由事件投影维护；Leaderboard 归 Reputation，由 `LeaderboardService` 维护 Redis ZSET。

## 6. 为什么这样分

- 业务边界清晰：4 个上下文对应 4 个业务能力，避免大泥球。
- 可测试：domain 零框架依赖，单元测试毫秒级；ArchUnit 强制约束。
- 可演进：跨上下文只经 api / 事件，后续可拆服务。
- 并发安全：聚合只引用 ID，不直接持有其他聚合对象。

## 7. 新同学阅读顺序

1. `docs/adr/` 10 条 ADR
2. `backend/co-identity`（完整参考实现）
3. `co-qa-domain` 的领域测试（需求说明书）
4. `docs/ddd/` 的事件、上下文映射、聚合与不变式
5. `ArchitectureTest`（架构适应度函数）

## 8. 各成员讲解分工与署名（T0-05）

> 验收口径（角色卡全员任务 T0-05）：每人认领 1~2 条 ADR + 一层代码，讲透关键点后在本表署名。

| 成员 | 认领内容 | 关键点（讲什么） | 状态 |
| --- | --- | --- | --- |
| 青岑 | ADR-001 模块化单体 + 全文整理 | 4 上下文物理分模块；domain 零框架依赖；跨上下文只走 api/事件（即第 1~7 节） | 已署名（第 1~7 节） |
| 魏志国 | ADR-002 事务性发件箱 + application 层 | 业务数据与 `outbox_event` 同事务写入；中继异步投递；消费端按 `(event_id, user_id, reason)` / `processed_event` 幂等；`AnswerCommandService` 为典型编排：save → 原子计数 → 发事件 → 失效缓存 | ✅ 已署名（2026-11-17 讲评：`AnswerCommandService.submit` 的四步编排与发件箱同事务写入口） |
| 刘俊琒 | ADR-008 领域与持久化分离 + co-qa-domain | 聚合只引用 ID、不直接持有其他聚合对象；score/answerCount 用原子 SQL 而非整行回写；`Question` 五方法的不变式即领域测试 | ✅ 已署名（2026-11-19 本人复核：逐条核对 ADR-008 与 `Question`/`QuestionRepositoryAdapter`/`QuestionJpaRepository`，复核结论与两处口径偏差见 §10.4） |
| 房国栋 | ADR-003 服务端会话 + interfaces 层 | Spring Session + Redis；HttpOnly / SameSite=Lax / CSRF 双提交；控制器不写业务规则，前端按 `permissions` 渲染按钮 | ✅ 已署名（§9 ADR-003 主讲笔记） |
| 陈明哲 | ArchUnit 架构适应度函数 + ADR-009 渐进门禁 | `ArchitectureTest` 分层断言；渐进期 `SPRINT_MODULES` 扩容，2026-11-18 已删除并恢复全量 `mvn test`/`mvn verify`，集成测试硬门禁；覆盖率门槛 0.70；红灯 30 分钟内认领 | 已署名（2026-11-16） |

> 讲评排期：Sprint 3 计划会上每人约 5 分钟讲完各自认领项，讲完即更新本表状态。

---

## 9. ADR 主讲：ADR-003 服务端会话（房国栋）

> 本节为 T0-05 交付内容，由本人讲解 ADR-003 并对照一层代码。
> 对应 ADR：[`docs/adr/003-服务端会话.md`](../adr/003-服务端会话.md)
### 9.1 这个决策要解决什么问题
校园场景有两个硬约束：
1. **能立即封禁**。教师/管理员发现违规账号，要求立刻踢下线；如果令牌自包含（JWT），签出去就收不回，只能等它自然过期；
2. **浏览器 SPA 面临 XSS 与 CSRF**。JWT 通常放 `localStorage`，一旦页面被注入脚本，令牌可直接被读走。
ADR-003 的选择是：**不做无状态令牌，改用 Redis 服务端会话**。
### 9.2 决策要点与代码落点
| 决策点 | 代码位置 | 说明 |
| --- | --- | --- |
| 会话存储用 Spring Session + Redis | `co-bootstrap/pom.xml`（`spring-session-data-redis`） | 会话数据落在 Redis，多实例共享；扩容不需要粘性会话 |
| CSRF 双提交 Cookie | `SecurityConfig`（`CookieCsrfTokenRepository.withHttpOnlyFalse()`）+ `CsrfCookieFilter` | 服务端下发 `XSRF-TOKEN` Cookie（必须 JS 可读，所以 `HttpOnly=false`），前端把它放进 `X-XSRF-TOKEN` 请求头；攻击者跨站拿不到该头 |
| 登录后轮换会话 ID | `AuthController` → `request.changeSessionId()` | 防**会话固定攻击**：攻击者先埋一个已知 SID，等受害者登录后复用 |
| 登录失败限流 | `LoginThrottle` 接口 + `RedisLoginThrottle` | Redis 固定窗口计数，同一用户名 1 分钟内失败 5 次即锁定到窗口结束，返回 `AUTH-429` |
### 9.3 `RedisLoginThrottle` 逐行理解（我讲的那一层代码）

> 说明：以下按 **2026-11-26 当前实现**更新；旧版“GET 后 parse + 固定 1 分钟窗口 + 连字符配置键”已作废。

```java
private final int maxFailures;   // co.security.login.max-failures，默认 5
private final Duration window;   // co.security.login.window-seconds，默认 60 秒

// recordFailure：INCR 与首次 PEXPIRE 在单条 Lua 脚本内原子完成
private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
        "local c = redis.call('INCR', KEYS[1]) "
                + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                + "return c",
        Long.class);

public boolean isBlocked(String username) {
    String key = key(username);
    Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
    if (ttl != null && ttl == -1L) {
        redis.expire(key, window);            // 兼容旧版遗留的无 TTL key：补过期时间，避免永久锁定
    }
    String v = redis.opsForValue().get(key);
    if (v == null) {
        return false;
    }
    try {
        return Integer.parseInt(v) >= maxFailures;
    } catch (NumberFormatException e) {
        redis.delete(key);                    // 计数损坏：按未锁定处理并删除脏 key 自愈
        return false;
    }
}

public void recordFailure(String username) {
    redis.execute(INCR_WITH_TTL, List.of(key(username)), String.valueOf(window.toMillis()));
}

public void reset(String username) {
    redis.delete(key(username));              // 登录成功后清零
}

private static String key(String username) {
    return "id:login-fail:" + Usernames.normalize(username);   // 按归一化后的用户名计数
}
```
四个值得注意的设计点：
1. **INCR 与 PEXPIRE 必须原子**。早期实现是「先 INCR、计数为 1 时再 EXPIRE」两步，进程在两步之间中断就留下无 TTL 的 key，该账号会被永久锁定；现在两条命令在同一个 Lua 脚本里执行，不存在中间态，`isBlocked` 还会给历史遗留的无 TTL key 补过期时间；
2. **`PEXPIRE` 只设在计数为 1 时**。如果每次失败都刷新过期时间，攻击者持续尝试就会让窗口永远不过期，等于把"1 分钟内 5 次"变成"永久锁定"——锁死账号反而成了新的攻击面；
3. **登录成功后 `reset` 清零**。否则用户白天输错几次、成功登录后计数还留着，晚上再错一次就被锁；
4. **按用户名计数而不是按 IP**。按 IP 会误伤同一出口 IP 的整个机房/校园网用户；按用户名则精确落在被攻击的账号上（代价是可被用来恶意锁定他人账号，这是本方案已知的取舍）。
### 9.4 与 JWT 方案的对比（为什么否掉）
| 维度 | 服务端会话（本方案） | JWT（localStorage） |
| --- | --- | --- |
| 立即失效 | ✅ 删除 Redis 中的会话即可，封禁即时生效 | ❌ 令牌自包含，签发后无法收回 |
| XSS 窃取风险 | ✅ 会话 ID 在 `HttpOnly` Cookie，JS 读不到 | ❌ `localStorage` 可被任意脚本读取 |
| CSRF 防护 | 需要双提交 Cookie（已实现） | 天然免疫（无 Cookie 自动携带） |
| 水平扩容 | 依赖 Redis 共享会话 | ✅ 完全无状态 |
| Redis 故障影响 | ❌ Redis 不可用则无法登录 | ✅ 不受影响 |
ADR 里的结论是：**「JWT + 黑名单」是最差组合**——黑名单本身就要 Redis，既付出了状态成本，又没得到无状态的好处。
### 9.5 我的理解与疑问
- **理解**：这个决策的本质是**用一点可用性依赖换安全可运维性**。校园场景里"能立刻封人"的价值高于"Redis 挂了还能登录"，所以选会话。
- **疑问 1**：`RedisLoginThrottle` 按用户名锁定，恶意者可以故意输错来锁死别人的账号。是否需要叠加 IP 维度或验证码？这属于 QS-08 的延伸讨论。
- **疑问 2**：ADR 说"Redis 不可用则无法登录"，但未说明降级策略。是否需要在 Redis 故障时允许只读访问并给出明确提示？
- 以上两点已在组内提出，作为后续 ADR 修订的输入。

---

## 10. ADR 主讲：ADR-008 领域与持久化分离（刘俊琒）

> 本节为 T0-05 交付内容，由本人复核 ADR-008 并对照 `co-qa-domain` 一层代码后署名。
> 对应 ADR：[`docs/adr/008-领域与持久化分离.md`](../adr/008-领域与持久化分离.md)
> 代码落点：`backend/co-qa/co-qa-domain/.../Question.java`、`QuestionRepository.java`、`backend/co-qa/co-qa-infrastructure/.../QuestionRepositoryAdapter.java`、`QuestionJpaRepository.java`

### 10.1 这个决策要解决什么问题

如果聚合直接标 `@Entity`，会同时丢两样东西：

1. **可测试性**。`Question` 一标 `@Entity` 就必须有无参构造、可变字段，单元测试要么启动持久化上下文，要么用反射塞值；`QuestionTest` 的 33 条断言就跑不进毫秒级。
2. **业务规则的可读性**。领域对象的字段一旦被 JPA 的延迟加载代理包住，"什么状态是合法的"就散落在实体注解和 Service 里，讲不清六边形架构的内核与外延。

ADR-008 的选择是：**领域层纯 Java，仓储接口在领域层，映射与事务在基础设施层**；计数器（score / answerCount / viewCount）不由聚合回写，改用原子 `UPDATE`。

### 10.2 决策要点与代码落点

| 决策点 | 代码位置 | 说明 |
| --- | --- | --- |
| 领域层零框架依赖 | `Question.java` 无任何 `jakarta.persistence` / `org.springframework` 引用 | 由 `ArchitectureTest.domain_layer_is_framework_free` 钉死，违反即 CI 红 |
| 仓储接口定义在领域层 | `QuestionRepository`（domain 包） | 端口是业务语言：`findById / save / adjustAnswerCount / adjustScore` |
| 计数器只暴露"增量"语义 | `QuestionRepository.adjustAnswerCount(long, int delta)` | 端口**不提供** `setScore(int)`，从接口层面就断掉"读-改-写"的可能 |
| 原子 SQL 落地 | `QuestionJpaRepository`：`update QuestionJpaEntity q set q.score = q.score + :delta where q.id = :id` | 并发投票不会互相覆盖，也不会把整行其他字段写回旧值 |
| 映射与重建分离 | `Question.post(...)` vs `Question.reconstitute(...)` | 新建走完整校验；重建用于从库中还原，不再跑一遍业务校验（否则历史数据一旦放宽规则就再也读不出来） |
| ID 只分配一次 | `Question.assignId(long)`，第二次抛 `IllegalStateException("ID 已分配")` | 防止 `save` 后被误当成新对象再插一行 |

### 10.3 我讲的那一层代码：`QuestionRepositoryAdapter.save`

```java
public Question save(Question q) {
    QuestionJpaEntity e = q.id() == null
            ? new QuestionJpaEntity(q.authorId(), q.courseId(), q.createdAt())
            : jpa.findById(q.id()).orElseThrow();   // ① 先取实体，不 new 一个"影子实体"
    e.setTitle(q.title().value());
    e.setBody(q.body().value());
    e.getTags().clear();
    e.getTags().addAll(q.tags().names());           // ② 集合值对象整体替换
    e.setAcceptedAnswerId(q.acceptedAnswerId());
    e.setUpdatedAt(q.updatedAt());
    e.setDeletedAt(q.deletedAt());                  // ③ 注意：score / answerCount / viewCount 一个都没搬
    ...
}
```

三个点值得讲：

1. **① 是"重新加载"而不是"造实体"**。若用领域对象新建一个实体再 `save`，JPA 会把它当成 merge，`@Version` 被覆盖，乐观锁形同虚设；重新加载后由实体自己持有版本号，冲突在提交时抛 `OptimisticLockException`。
2. **③ 是 ADR 的核心**。`score / answerCount` 故意不搬——它们只由 `adjustScore / adjustAnswerCount` 的原子 SQL 维护。否则一次"编辑标题"的 `save` 就会把并发投票累加出来的分数写回旧值，这类 bug 在压测里表现为"票数偶尔少 1"，极难复现。
3. **② 的代价**：tags 是值对象的集合，只能 clear + addAll 整体替换，每次保存都产生一轮子表 delete/insert。数据量在校园场景可接受，但这是"领域模型干净"换来的真实成本，ADR 里写的"每个聚合约 40 行映射样板"就指这类代码。

### 10.4 本人复核发现（两处口径偏差 + 一条约定，均已回写 ADR-008）

复核不是照抄 ADR，下面三条是逐行比对这个实现与 ADR 文本后发现的差异：

1. **ADR 初版「后果」里写的"需要显式处理乐观锁版本号的搬运"，在本实现里没有发生**（该句已在 2026-11-19 回写时改正）。`toDomain` 把 `e.getVersion()` 读进领域对象，但 `save` 从不回写；版本号完全由 JPA 实体自己维护，领域对象上的 `version` 是只读快照。好处是映射代码简单，代价是**领域层感知不到并发冲突**——冲突以 `ObjectOptimisticLockingFailureException` 在适配器层抛出。这个"感知不到"由一处兜底补齐：`GlobalExceptionHandler.optimisticLock` 把它翻译成 `409 / SYS-409-CONCURRENT /「内容已被他人修改，请刷新后重试」`。也就是说 ADR 描述的"搬运版本号"被"统一在边界翻译异常"替代了，这个替代关系原 ADR 没写。**→ 已于 2026-11-19 回写 ADR-008「后果 · 实现注记 1」。**
2. **`view_count` 是同一类计数器的第二种实现，且不在 `QuestionRepository` 端口上**。它走 `RedisViewCounter`：先在 Redis 里累加（`qa:views:pending`），再批量 `UPDATE qa_question SET view_count = view_count + ? WHERE id = ?`。只读 `QuestionRepository` 的人会以为浏览数也走整行回写。建议在 ADR-008 补一句"计数器有两种落地：即时原子 SQL（score/answerCount）与缓冲批量累加（viewCount）"。**→ 已于 2026-11-19 回写 ADR-008「后果 · 实现注记 2」。**
3. **已写入约定**：`Question` 持有 `score / answerCount / viewCount` 三个只读字段，意味着聚合内的计数器值可能与库中最新值不一致（并发原子更新后内存对象仍是旧值）。目前对外展示都走 `JdbcQaReadModel` 的读模型、不读聚合的计数器，所以没出问题；但这条约束必须写进约定，否则后续有人在命令侧读 `question.score()` 做业务判断就会踩坑。**→ 已于 2026-11-19 作为"约定"写入 ADR-008「后果 · 实现注记 3」**：展示走读模型；并记下现存的一处"聚合内读计数器"——`Question.softDelete` 的 `answerCount > 0` 拦删是快照判断，存在毫秒级窄窗口，要强一致得把条件下推成 `UPDATE ... WHERE answer_count = 0`。

### 10.5 我的理解与结论

- **理解**：ADR-008 换来的不是"代码好看"，而是**把并发正确性从"小心写代码"变成"接口上做不到错"**——端口没有 `setScore`，就没有读-改-写的入口；领域层没有框架注解，`QuestionTest` 就永远是毫秒级。
- **结论**：ADR-008 的决策在本实现中成立，两处口径偏差（版本号搬运、浏览数计数器）不改变决策本身，**已于 2026-11-19 回写进 ADR-008「后果」一节**（正面/负面之后新增「实现注记」三条，第 3 条为约定），避免后来者按文档去找不存在的代码。
- **署名**：刘俊琒，2026-11-19，已逐条复核 §10.2～§10.4 所列代码位置。
