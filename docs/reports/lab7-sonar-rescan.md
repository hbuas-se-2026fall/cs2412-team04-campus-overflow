# Lab 7 · SonarQube 重扫证据与执行单

> 责任人：房国栋 / 陈明哲　状态：**✅ 已执行完毕（2026-11-23）**——配置、CI 接入、Bug 修复、本机 SonarQube 重扫与六张截图全部完成
> 实测结果以 [`lab5-lab7-quality-gates.md`](lab5-lab7-quality-gates.md) §4.6b 为准：**Bugs 3 → 1**、Coverage 70.3%、Quality Gate 仍 Failed（剩 `User.java:24` 的 `java:S5998`）
> 关联：`sonar-project.properties`、`backend/pom.xml`（sonar-maven-plugin）、`.github/workflows/ci.yml` 阶段 6
> 同行评审清单：[`docs/team/peer-review-checklist.md`](../team/peer-review-checklist.md)

## 一、现状

| 项 | 状态 | 证据 |
| --- | --- | --- |
| `sonar-project.properties` | ✅ 已入库 | 项目 key `campus-overflow`，源码/排除/JaCoCo/TS 配置完整 |
| `sonar-maven-plugin` | ✅ 已注册 | `pom.xml` 属性 `sonar-maven-plugin.version=4.0.0.4121` |
| CI 阶段 6 | ✅ 已接入（无 token 自动跳过） | `ci.yml`：`if: env.SONAR_TOKEN != ''`，配置 secret 后自动扫描并等待门禁 |
| 3 个 Sonar Bug | 🟡 **修复 2 个，剩 1 个** | #197 缓存 TTL（int→long）✅、#200 `orElseThrow` ✅；**`User.java:24` 的 `java:S5998` 未被消除**（#200 只把嵌套位置从 `([\\w-]+\\.[\\w-]+)+` 挪到 `([\\w-]+\\.)+`） |
| 本机 SonarQube | ✅ 已部署并跑完 | Docker `sonarqube:lts-community` 9.9.8.100196，`http://localhost:9000` |
| 重扫截图 | ✅ 已归档（六张） | [`screenshots/lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)：dashboard / coverage / bugs / projects / gate-list / gate-conditions |

## 二、实际执行记录（2026-11-23，本机）

命令与下面一致，已全部跑通：

```bash
# 1) 启动 SonarQube LTS（本机 9.9.8.100196，启动约 30 秒）
docker run -d --name sonarqube -p 9000:9000   -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:lts-community

# 2) 取 token（首次访问强制改默认密码，已改为强密码）
curl -u admin:admin -X POST "http://localhost:9000/api/user_tokens/generate" -d "name=ci-local"

# 3) 生成测试与 JaCoCo（跳过 Testcontainers）
cd backend && mvn -B clean verify -DskipITs -Djacoco.skip=false   # BUILD SUCCESS, 11 份 jacoco.xml

# 4) 推送扫描
mvn -B -f backend/pom.xml sonar:sonar   -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>

# 5) 门禁：CampusOverflow Gate
#    Overall: Coverage < 70% / Bugs > 0 / Vulnerabilities > 0
#    New Code: 沿用 Clean as You Code 默认 6 条
curl -X POST ".../api/qualitygates/create" -d "name=CampusOverflow Gate"
curl -X POST ".../api/qualitygates/create_condition" -d "gateName=...&metric=coverage&op=LT&error=70"
curl -X POST ".../api/qualitygates/select" -d "gateName=...&projectKey=campus-overflow"
```

> 门禁绑定只对下一次分析生效，绑定后需**再扫一次**才会显示条件评估结果。

### 2.1 两个必须知道的配置坑（已在仓库修好）

| 坑 | 症状 | 处理 |
| --- | --- | --- |
| `sonar-project.properties` 在 Maven 多模块下不生效 | 项目 key 变成 `com.campusoverflow:campus-overflow`、jacoco 报 `No report imported` | 属性写进父 POM `<properties>`，由模块继承 |
| jacoco 报告路径不能用相对通配 | `target/site/jacoco/jacoco.xml` 按各模块 basedir 解析找不到 | 父 POM 里列全 11 个模块的显式路径（正斜杠） |

改完覆盖率从 `49.5%`（报告未导入）变为 `70.3%`（真实值）。

## 三、已归档截图（六张）

目录：[`docs/screenshots/lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)

| 文件 | 内容 |
| --- | --- |
| `01-dashboard.png` | 项目 Dashboard：`QUALITY GATE STATUS: Failed`，明示「1 Bugs is greater than 0」 |
| `02-coverage.png` | Measures → Coverage：**70.3%**、Lines to cover 1,524、Unit Tests 173 全通过 |
| `03-bugs.png` | Issues 过滤 `types=BUG`：**仅 1 条**，定位 `co-identity/.../User.java L24` |
| `04-projects.png` | 项目列表 |
| `05-quality-gate-list.png` | Quality Gates：`CampusOverflow Gate` + 内置 `Sonar way` |
| `06-quality-gate-conditions.png` | 门禁条件明细：New Code 6 条 + Overall `Bugs>0`/`Coverage<70%`/`Vulnerabilities>0` |

> 修复前的对照证据在 [`screenshots/lab7-sonar/`](../screenshots/lab7-sonar/)（2026-11-19，3 Bugs）。

## 四、CI 侧启用方式

在 GitHub 仓库 Settings：

- Secrets：`SONAR_TOKEN`；
- Variables：`SONAR_HOST_URL`（如 `http://<sonar-host>:9000`）。

配置后 `后端构建与验证` 的阶段 6 会自动执行，并在门禁失败时让 CI 变红；未配置时保持跳过，不阻塞现有 PR。

## 五、验收（2026-11-23 实际结果）

| 验收项 | 目标 | 实际 | 结论 |
| --- | --- | --- | --- |
| SonarQube Quality Gate | PASSED | **Failed**（1 条条件未过） | 🟡 未达标，见下 |
| Coverage | ≥ 70% | **70.3%** | ✅ |
| Bugs | = 0 | **1** | ❌ 剩 `java:S5998` |
| Vulnerabilities | = 0 | **0** | ✅ |
| 截图归档 | 已归档 | 六张，`screenshots/lab7-sonar-rescan/` | ✅ |
| `lab5-lab7-quality-gates.md` §三 勾选 | 已勾 | 已勾，并新增「重扫截图 ✅ 已执行」一行 | ✅ |

### 5.1 唯一未达标项与建议

`co-identity/co-identity-domain/src/main/java/com/campusoverflow/identity/domain/User.java:24`
规则 `java:S5998`（Regex repetition that can lead to stack overflow）。

当前写法（#200 之后）：

```java
Pattern.compile("^[\\w.+-]+@([\\w-]+\\.)+[\\w-]+$")
```

**#200 只是把嵌套从 `([\\w-]+\\.[\\w-]+)+` 挪到了 `([\\w-]+\\.)+`，内层 `+` 仍套在外层 `+` 里**，规则依旧命中。

建议改成外层 `*`、每次重复以字面量 `.` 开头：

```java
Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*$")
```

> 属身份上下文（陈明哲/魏志国），由模块负责人提交并复扫确认；改完后 Bugs 归零，门禁即可 PASSED。

---

## 六、结论

重扫**已执行**，证据齐备。三大指标里 Coverage（70.3%）与 Vulnerabilities（0）达标，
Bugs 由 3 降到 1，**门禁仍 Failed，原因是 #200 的修复未真正消除 `S5998`**——
这是本轮最有价值的发现：如果只看「#200 已修复」的说明而不复扫，会误以为门禁已绿。
