# 安全与质量审计修复记录（2026-11-24）

> 对应组员审计：SEC-01/02、SEC-03、SEC-04、QUAL-01、DOC-01、QA-02、QA-03、DOC-02、CACHE-02、PRIV-01、LEADER-01
> 分支：`fix/security-hardening-1124`；目标：develop（main 后续同步）

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

- `deploy/docker-compose.yml` 改为**生产**：默认 `prod`（不含 demo）、不映射 3306/6379、`DB_PASSWORD`/`MYSQL_ROOT_PASSWORD`/`CO_FIELD_KEY`/`CO_FIELD_MAC_KEY`/`GRAFANA_PASSWORD` 均为必填（`:?`）；
- 新增 `deploy/docker-compose.dev.yml`：本地开发专用，保留默认密码、端口映射和 demo；
- 新增 `deploy/.env.example`；README §5 改为「生产 `.env` + dev compose」两套命令；
- `EnvironmentGuard` 兜底拒绝 prod+demo/默认密钥/默认密码启动。

### QUAL-01 Sonar S5998

- 按方案 A 修改：`^[\\w.+-]+@[\\w.-]+$`，去掉所有重复分组；
- `User.checkEmail` 补域名校验：不含 `..`、不以 `.` 开头/结尾、必须含 `.`；
- `UserTest` 增加长 local part 与畸形域名用例。
- ⚠️ 需要 SonarQube 机器复扫确认 `S5998` 归零；在复扫前 Lab 7 的门禁事实仍是 Failed（1 Bug）。

### DOC-01 / DOC-02

- `03-quality.md`：数据截止 2026-11-24；明确“CI 门禁绿 ≠ Sonar 门禁绿”，补 `S5998` 风险行；
- `final/README.md`：03-quality 状态改为“已完成（2026-11-24 口径；Sonar Gate 仍 Failed 1 Bug）”；
- `release/v1.0.0.md`：已知问题补 `QUAL-01`；
- 文档中的 `@Size(max=254)` 改为真实的 **100**（注册 DTO 与领域 `checkEmail` 均为 100）；
- `AdminUserController.CreateStaffRequest` 仍需补 `@Size`（username 4–20、displayName 2–20、email ≤100、password 8–64、college ≤50）：当前只 import 了 `Size`，注解尚未加，领域层兜底校验仍然有效。

### CACHE-02 浏览量落库失败丢失

`RedisViewCounter.flush()`（PR #213 最终实现）：

- 用固定 `FLUSHING` key 原子摘出当前批次；
- 数据库批次写入使用事务，避免部分成功、部分失败；
- 失败时保留 `FLUSHING`，下一轮 `recoverFailedBatch()` 合并回 PENDING 后重试；
- 解析失败的条目记录日志并丢弃，不再让整批走异常路径。

### LEADER-01 排行榜三 ZSET 部分失败重复计分

`RedisLeaderboardStore.increment` 改为 **Lua 脚本一次原子更新** week/month/term 三个 ZSET 与 TTL：
任一失败则三个都不生效，DB 事务回滚后重试不会只给 week 多加一次。

## 二、需要 PO / 产品决策（未改代码）

### QA-02 课程问题可见性冲突

- 2026-11-24 已定为**课程内问题仅课程成员可见，公开问题保持公开**；
- 修复分支覆盖 list/detail/search/related/post/answer/comment/vote，并把 viewer 加入列表缓存 key；回归测试覆盖未登录、非成员、成员三种身份。
- `SecurityConfig` 仍允许匿名 GET，是为了让公开问题可被访客浏览；课程问题由应用层成员校验拦截。

### PRIV-01 课程成员名单匿名可读

`GET /api/v1/courses/*/members` 当前允许匿名，返回 `displayName/role/userId`。需 PO 确认名单是否公开；若不公开，要求登录 + 课程成员，并同步 Nginx 限制。

### QA-03 删除问题与回答并发窗口

已改为作者删除时使用数据库条件 `UPDATE ... WHERE deleted_at IS NULL AND answer_count = 0`；管理者删除保持原逻辑。回归测试覆盖并发回答场景（PR #216 已合入）。

## 三、验证

| 范围 | 命令 | 结果 |
| --- | --- | --- |
| identity 三模块 | `mvn -o -pl co-identity/co-identity-domain,co-identity/co-identity-application,co-identity/co-identity-interfaces -am test -Djacoco.skip=true` | BUILD SUCCESS（domain 10、application 3） |
| qa + reputation 基础设施 | `mvn -o -pl co-qa/co-qa-infrastructure,co-reputation/co-reputation-infrastructure -am test -Djacoco.skip=true` | BUILD SUCCESS |
| bootstrap 主代码 | `mvn -o -pl co-bootstrap -am compile` | BUILD SUCCESS |
| 集成测试（含新增大小写/禁用用例） | CI `后端构建与验证` | 待 CI |

## 四、剩余动作

1. CI 全量 + Testcontainers；若通过，合并 develop 并同步 main；
2. 在 SonarQube 机器复扫，确认 `S5998` 归零、Gate PASSED，补 `lab7-sonar-rescan` 截图；
3. PO 对 PRIV-01 拍板；QA-02 已按成员可见落地，QA-03 已修复；后续 PR 需同步更新本报告状态；
4. 生产切换前轮换 `CO_FIELD_KEY`/`CO_FIELD_MAC_KEY` 并迁移已有 email 摘要。
