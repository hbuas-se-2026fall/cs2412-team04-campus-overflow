# 安全与质量审计修复记录（2026-11-24）

> 对应组员审计：SEC-01/02、SEC-03、SEC-04、QUAL-01、DOC-01、QA-02、QA-03、DOC-02、CACHE-02、PRIV-01、LEADER-01
> 分支：`fix/security-hardening-1124`（PR #209 → #210）；状态：main 已同步安全修复，后续 #216 修复 QA-03。

## 一、已修复

### SEC-01/02 登录限流与禁用会话可被大小写变体绕过

| 项 | 修复 |
| --- | --- |
| 账号规范化 | 新增 `identity.domain.Usernames.normalize`（trim + lower-case）；登录限流键、会话 principal-name 索引、禁用撤销统一使用规范名 |
| 登录 | `AuthController` 用规范名做 throttle key；会话索引写 `auth.getName()`（认证后的真实用户名），不再写请求原文 |
| 撤销 | `SpringSessionUserSessionRevoker` 按规范名 `findByPrincipalName` |
| /me | `UserQueryService.me` 检查 `canLogin()`，禁用账号返回 401 `AUTH-401` |
| 测试 | `CampusOverflowFlowIT` 新增：`ITCase01` 用 `itcase01` 登录成功 → DB 置 DISABLED → 旧会话 `/me` 401、重新登录 401 |

> 说明：数据库排序规则 `utf8mb4_0900_ai_ci` 使其视为同一账号；修复后不存在“原始大小写写索引、规范名撤销”的错配。

### SEC-03 字段密钥默认值与 AES/HMAC 共用

- `application.yml` 不再提供 `CO_FIELD_KEY` 默认值，数据库密码默认值移入 **application-dev.yml**；
- 新增 **application-prod.yml**：`CO_FIELD_KEY`、`CO_FIELD_MAC_KEY`、`DB_PASSWORD` 全部无默认值，缺失即启动失败；
- `FieldEncryptor` 支持独立的 HMAC 密钥；缺省时用 HKDF-SHA256 从主密钥派生 MAC 子密钥，避免 AES 与 HMAC 共用同一把 key；
- `EnvironmentGuard` 在 prod profile 下拒绝：demo profile、仓库里的开发 key、默认数据库密码。

> ⚠️ 遗留：`digest()` 的密钥变化会影响已有 email 摘要；历史数据需要一次离线重算/轮换（新库/演示库不受影响）。生产切换前应按 `CO_FIELD_KEY`/`CO_FIELD_MAC_KEY` 生成新密钥并迁移。

### SEC-04 默认 demo/密码/端口暴露

- `deploy/docker-compose.yml` 改为**生产**：默认 `prod`（不含 demo）、不映射 3306/6379；`DB_PASSWORD`/`MYSQL_ROOT_PASSWORD`/`CO_FIELD_KEY`/`CO_FIELD_MAC_KEY` 用 `:?` 必填；`GRAFANA_PASSWORD` 用 `${GRAFANA_PASSWORD:-}` 加 entrypoint 非空自检（避免未启用 observability 时整份 compose 插入失败）；
- 新增 `deploy/docker-compose.dev.yml`：本地开发专用，保留默认密码、端口映射和 demo；
- 新增 `deploy/.env.example`；README §5 改为「生产 `.env` + dev compose」两套命令；
- `EnvironmentGuard` 兜底拒绝 prod+demo/默认密钥/默认密码启动。

### QUAL-01 Sonar S5998

- 按方案 A 修改：`^[\\w.+-]+@[\\w.-]+$`，去掉所有重复分组；
- `User.checkEmail` 补域名校验：不含 `..`、不以 `.` 开头/结尾、必须含 `.`；
- `UserTest` 增加长 local part 与畸形域名用例。
- ✅ **已复扫确认归零（2026-11-25）**：在 `3750111` 基线上 Quality Gate **PASSED**，Bugs 0 / Vulnerabilities 0 / Coverage 73.8% / 219 单测。该基线不是当前 HEAD；当前 HEAD 为 277 单测，需在最终提交上重扫。证据：[`lab7-sonar-final-rescan.md`](lab7-sonar-final-rescan.md)。

### DOC-01 / DOC-02

- `03-quality.md`：数据截止 2026-11-24；明确“CI 门禁绿 ≠ Sonar 门禁绿”，补 `S5998` 风险行；
- `final/README.md`：03-quality 状态在 2026-11-24 时点为“已完成（Sonar Gate 仍 Failed 1 Bug）”；2026-11-25 复扫已 PASSED，以上文 QUAL-01 为准；
- `release/v1.0.0.md`：已知问题补 `QUAL-01`；
- 文档中的 `@Size(max=254)` 改为真实的 **100**（注册 DTO 与领域 `checkEmail` 均为 100）；
- `AdminUserController.CreateStaffRequest` 已补全 `@Size`（username 4–20、displayName 2–20、email ≤100、password 8–64、college ≤50），接口层直接 400，不再只靠领域兜底。

### CACHE-02 浏览量落库失败丢失

`RedisViewCounter.flush()`（PR #213 最终实现）：

- 用固定 `FLUSHING` key 原子摘出当前批次；
- 数据库批次写入使用事务，避免部分成功、部分失败；
- 失败时保留 `FLUSHING`，下一轮 `recoverFailedBatch()` 合并回 PENDING 后重试；
- 解析失败的条目记录日志并丢弃，不再让整批走异常路径。

### LEADER-01 排行榜三 ZSET 部分失败重复计分

`RedisLeaderboardStore.increment` 改为 **Lua 脚本一次提交** week/month/term 三个 ZSET 的 `ZINCRBY` 与 `EXPIRE`。

> 措辞修订（2026-11-25，审计 F-13）：原「任一失败则三个都不生效」不严格。Lua 脚本保证的是**原子执行**
> （三个命令之间不会被并发请求穿插），但脚本中途的命令错误（如第 2/3 个 key 类型不对、`WRONGTYPE`）
> **不会回滚已执行的第 1 条 `ZINCRBY`**。此外 `LeaderboardService.on` 先写 `ProcessedEventStore`（DB）再写 Redis，
> 若 DB 提交失败而 Redis 已累加，重试会再累加一次（at-least-once，存在重复计分窗口），该限制仍待后续收敛。

**已知限制（审计 2-9，2026-11-25）**：排行榜读写路径仍有两处不一致风险，本轮只做**如实记录**，未改实现：

| 风险 | 现状 | 影响 | 处置 |
| --- | --- | --- | --- |
| DB `processed_event` 与 Redis `ZINCRBY` 双写 | 两者在同一 `@Transactional` 内，Redis 抛异常会使 processed 记录一起回滚并重投（不会「标记了但没加分」）；反之若 Redis 成功、DB 提交失败，重投会重复计分 | 重复计分窗口 | outbox 至少一次语义的固有代价；ZSET 仅用于周/月/学期榜，总榜读账户表不受影响 |
| Redis 数据丢失后可重建 | ✅ **已实现（2026-11-25）**：`LeaderboardService.rebuild(window)` 以账本为准重算三个 ZSET | — | `LedgerRepository.findAllBetween(from,to)` 按 id 升序取流水 → `LeaderboardStore.resetFor(时间戳集合)` 只删会被重放到的 key → 逐条 `increment` 重放；入口 `POST /api/v1/admin/leaderboard/rebuild?days=200`（`/api/v1/admin/**` 已限定 ADMIN） |

> 结论：**周期榜属可重建的派生数据**，出现偏差时以声誉账本为准；总榜（ALL）直接读 `rep_reputation_account`，不受上述风险影响。


## 二、PO 决策与安全口径（2026-11-25 已收口）

### QA-02 课程问题可见性冲突（已被 2026-11-25 最终裁决取代）

> **更新（2026-11-25 晚）**：本文下方“保持公开可见”的初版结论已作废。
> PO 最终裁决为 **课程成员可见**：公开问题对所有访问者可见；课程问题仅管理员、作者、课程成员可见，
> 未加入者返回 `QA-3006`。代码 `QuestionQueryService.requireCanView()`、`QuestionController`、
> 前端 `QuestionDetailPage` 已按此实现。

- 初版（已作废）：PO 决策曾为“保持公开可见”，因此当时没有新增成员校验；
- 最终口径：以 [`v1-scope-decisions-2026-11-25.md`](v1-scope-decisions-2026-11-25.md) 决策 1 为准；
- 这是产品取舍，不是代码缺陷；当前代码已按最终裁决补齐 list/detail/search/related/bounty 校验与 viewer 缓存键。

### PRIV-01 课程成员名单匿名可读（已关闭）

`GET /api/v1/courses/*/members` 已从 `SecurityConfig` 的 `permitAll` 列表移除，落到 `anyRequest().authenticated()`：
匿名访问返回 401，登录用户可读。最终口径见 `v1-scope-decisions-2026-11-25.md` 决策 4。

### QA-03 删除问题与回答并发窗口

已改为作者删除时使用数据库条件 `UPDATE ... WHERE deleted_at IS NULL AND answer_count = 0`；管理者删除保持原逻辑。回归测试覆盖并发回答场景（PR #216 已合入）。

## 三、验证

| 范围 | 命令 | 结果 |
| --- | --- | --- |
| identity 三模块 | `mvn -o -pl co-identity/co-identity-domain,co-identity/co-identity-application,co-identity/co-identity-interfaces -am test -Djacoco.skip=true` | 当时基线 BUILD SUCCESS（domain 10、application 3）；当前 HEAD 为 domain 13、application 8 |
| qa + reputation 基础设施 | `mvn -o -pl co-qa/co-qa-infrastructure,co-reputation/co-reputation-infrastructure -am test -Djacoco.skip=true` | BUILD SUCCESS |
| bootstrap 主代码 | `mvn -o -pl co-bootstrap -am compile` | BUILD SUCCESS |
| 集成测试（含新增大小写/禁用用例） | CI `后端构建与验证` | 待 CI |

## 四、剩余动作

1. 在**当前 HEAD**上重跑 CI 全量 + Testcontainers，并在 SonarQube 机器复扫（`3750111` 的 PASSED 不能直接代表当前 277 单测）；
2. QA-02 已按 2026-11-25 最终决策“课程成员可见”关闭；PRIV-01 已按“名单仅登录可读”关闭；QA-03 已由 #216 修复；
3. 生产切换前轮换 `CO_FIELD_KEY`/`CO_FIELD_MAC_KEY` 并迁移已有 email 摘要。
