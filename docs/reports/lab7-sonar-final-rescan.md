# Lab 7 · SonarQube 最终复扫证据（Gate PASSED）

> 执行人：房国栋（D，2177023844）　执行日期：2026-11-25 10:20（分析时间戳）
> 代码基线：`fix/audit-f-findings-fangguodong`（已合入 `develop` @ `3750111`，即含 #209/#216/#230/#232/#233 之后的最终代码）
> 工具：SonarQube Community Edition 9.9.8（build 100196），`sonarqube:lts-community` 容器，`http://localhost:9000`
> 结论：**Quality Gate PASSED**（Bugs 0 / Vulnerabilities 0 / Coverage 73.8% / 安全热点复核 100%）
> 本文替代 [`lab7-sonar-rescan.md`](lab7-sonar-rescan.md) 末尾「最终代码未再扫描」的待办状态。

---

## 一、为什么重扫

审计缺陷 **F-11** 指出：Lab 7 报告与截图对应的是 #209 之前的代码，文档里还并存 `70.3%/173` 与 `70.5%/174` 两套数字，且 `java:S5998` 是否消除没有实测证据。
本次在**最终代码**上重跑构建、覆盖率与 Sonar 扫描，并重截全部证据。

## 二、执行步骤（可复现）

```bash
# 1) 起 SonarQube 9.9.8（本机已存在旧实例时，另起全新实例避免历史 issue 干扰）
#    旧容器已 stop（未删除，数据仍在，可 docker start sonarqube 恢复）
docker stop sonarqube
docker run -d --name sonarqube-scan -p 9000:9000 \
  -v sonarqube-final-data:/opt/sonarqube/data \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:lts-community

# 2) 首次登录改管理密码 + 生成 token
curl -X POST -u admin:admin \
  "http://localhost:9000/api/users/change_password?login=admin&previousPassword=admin&password=<新密码>"
curl -u admin:<新密码> -X POST \
  "http://localhost:9000/api/user_tokens/generate?name=lab7-final"   # 记下 squ_xxx

# 3) 生成测试与 JaCoCo（跳过 Testcontainers）
cd backend && mvn -B clean verify -DskipITs -Djacoco.skip=false   # BUILD SUCCESS

# 4) 扫描（注意：sonar-maven-plugin 4.0.0.4121 认 sonar.login，不认 sonar.token）
mvn -B sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=<token>

# 5) 建门禁并绑定（首次需要；绑定后需再扫一次才评估）
curl -X POST -u admin:<新密码> "http://localhost:9000/api/qualitygates/create" -d "name=CampusOverflow Gate"
curl -X POST ... /api/qualitygates/create_condition -d "gateName=CampusOverflow Gate&metric=coverage&op=LT&error=70"
curl -X POST ... /api/qualitygates/create_condition -d "gateName=CampusOverflow Gate&metric=bugs&op=GT&error=0"
curl -X POST ... /api/qualitygates/create_condition -d "gateName=CampusOverflow Gate&metric=vulnerabilities&op=GT&error=0"
curl -X POST ... /api/qualitygates/select -d "gateName=CampusOverflow Gate&projectKey=campus-overflow"
```

## 三、结果

| 指标 | 目标 | 上次（#209 前） | **本次（最终代码）** | 结论 |
| --- | --- | --- | --- | --- |
| Quality Gate | PASSED | Failed（Bugs 1） | **PASSED** | ✅ |
| Bugs | 0 | 1（`java:S5998`） | **0** | ✅ |
| Vulnerabilities | 0 | 0 | **0** | ✅ |
| Security Hotspots | 全复核 | 3 未复核（Security Review E） | **0 待复核 / 100% 已复核**（A） | ✅ |
| Coverage | ≥ 70% | 70.5% | **73.8%** | ✅ |
| Lines to cover | — | 1,573 | **1,645**（未覆盖 386；行覆盖 76.5%，条件覆盖 67.5%） | — |
| Unit Tests | — | 174 | **219 / 0 失败 / 100% 通过** | ✅ |
| Duplications | — | — | 0.1%（2 个重复块） | ✅ |
| Code Smells | 非门禁项 | 75 | 75（维护性评级 A，技术债 1d 2h） | — |

门禁条件逐条（`api/qualitygates/project_status`）：

| 条件 | 阈值 | 实际 | 结果 |
| --- | --- | --- | --- |
| Overall Coverage | < 70% 判失败 | 73.8% | OK |
| Overall Bugs | > 0 判失败 | 0 | OK |
| Overall Vulnerabilities | > 0 判失败 | 0 | OK |
| New Code Coverage | < 80% 判失败 | 88.2% | OK |
| New Code Duplicated Lines | > 3% 判失败 | 0.0% | OK |
| New Code Maintainability / Reliability / Security Rating | 差于 A 判失败 | A / A / A | OK |

## 四、`java:S5998` 结论

`co-identity-domain/.../User.java` 的 EMAIL 正则按 #209「方案 A」改为不含重复分组的
`^[\w.+-]+@[\w.-]+$`，并配 `User.checkEmail` 的域名校验（不含 `..`、不以 `.` 开头/结尾、必须含 `.`）。
**本次扫描 Bugs = 0，`S5998` 已归零**，Lab 7 可以写 PASSED。

## 五、本次同时修掉的 3 个 Bug（扫描在合并 `develop` 后的代码上新暴露）

| 规则 | 位置 | 现象 | 处理 |
| --- | --- | --- | --- |
| `java:S2201` | `BountyService.latest` | `orElseThrow` 返回值被丢弃，只当存在性守卫 | 改为 `if (...isEmpty()) throw new NotFoundException(...)` |
| `java:S2201` | `DiscoveryQueryService.related` | 同上 | 同上 |
| `java:S3655` | `QuestionQueryService.list` | `lookup.value().isPresent()` 后又 `lookup.value().get()`，同一表达式重复求值 | 先取出 `Optional` 变量再 `isPresent/get` |

> 三处都是「守卫写法」而非业务缺陷，但确实是 Sonar 规则命中项；改后行为不变（单元测试全绿）。

## 六、3 个安全热点的复核结论

| 规则 | 位置 | 判定 | 理由（已写入 SonarQube 复核记录） |
| --- | --- | --- | --- |
| `java:S2068` | `DemoDataSeeder.PASSWORD` | **Safe** | 演示种子口令仅 `@Profile("demo")` 生效，`EnvironmentGuard` 拒绝 prod+demo，README 已公开该口令，不构成凭据泄露 |
| `java:S2077` | `OutboxRelay.count(...)` | **Safe（并已改代码）** | 原实现把 `maxAttempts` 拼进 SQL；已改为 `countWhere(sql, threshold)`：SQL 为字面量、阈值走 `?` 绑定参数，注入面消失 |
| `java:S2245` | `RedisQuestionListCache` TTL 抖动 | **Safe** | `ThreadLocalRandom` 仅用于 60+[0,15) 秒 TTL 抖动防雪崩，不参与令牌/密钥/ID 生成 |

## 七、截图（`docs/screenshots/lab7-sonar-final/`）

| 文件 | 内容 |
| --- | --- |
| [`01-dashboard-passed.png`](../screenshots/lab7-sonar-final/01-dashboard-passed.png) | Overview → **Overall Code**：QUALITY GATE **Passed**；Bugs 0、Vulnerabilities 0、Security Hotspots 0（100% reviewed）、Coverage 73.8%、219 单测 |
| [`02-coverage.png`](../screenshots/lab7-sonar-final/02-coverage.png) | Measures → Coverage：73.8%、Lines to cover 1,645、Uncovered 386、Line 76.5%、Condition 67.5%、Unit Tests 219 / 0 失败 |
| [`03-issues-bugs.png`](../screenshots/lab7-sonar-final/03-issues-bugs.png) | Issues 过滤 `types=BUG`：**0 issues**（Bug 0 / Vulnerability 0 / Code Smell 75） |
| [`04-quality-gate-conditions.png`](../screenshots/lab7-sonar-final/04-quality-gate-conditions.png) | `CampusOverflow Gate` 条件明细：New Code 6 条 + Overall `Bugs>0`/`Coverage<70%`/`Vulnerabilities>0` |
| [`05-security-hotspots.png`](../screenshots/lab7-sonar-final/05-security-hotspots.png) | Security Hotspots：**There are no Security Hotspots to review**，右上角 Reviewed **100%** |

> 旧截图（`lab7-sonar/` 3 Bugs、`lab7-sonar-rescan/` 1 Bug）保留作对照，不再作为验收证据。

## 八、环境说明（如实记录）

1. 为排除旧库历史 issue 干扰，本次用**同版本全新实例**（容器 `sonarqube-scan`，命名卷 `sonarqube-final-data`）扫描；旧容器 `sonarqube` 仅 `stop` 未删除，`docker start sonarqube` 即可恢复。
2. SonarQube 9.9 自带提示「you are running a version of SonarQube that is no longer active」与「Embedded database should be used for evaluation purposes only」——课程验收用本机实例，属预期提示，不影响门禁结论。
3. `sonar-maven-plugin 4.0.0.4121` 需用 `-Dsonar.login=` 传 token；若只传 `-Dsonar.token=` 会报 `Not authorized`。CI 的 `ci.yml` 阶段 6 已按 `-Dsonar.token` 写在 SonarCloud 语境下，接自建服务器时需同步该参数（见下）。

## 九、遗留（不影响本次结论）

- Code Smells 75 条（维护性评级 A）未逐条清理，属持续改进项。
- CI 阶段 6 的 token 参数与自建 SonarQube 不完全匹配：插件版本认 `sonar.login`，`ci.yml` 传的是 `sonar.token`。仓库当前未配置 `SONAR_TOKEN`，该阶段自动跳过，不影响 PR；若后续在 CI 启用自建 SonarQube，需把参数改成 `-Dsonar.login`（或在插件升级到识别 `sonar.token` 的版本后保持现状）。
