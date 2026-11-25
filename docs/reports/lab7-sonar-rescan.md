# Lab 7 · SonarQube 重扫证据与执行单

> 责任人：房国栋 / 陈明哲　状态：**已执行两轮复扫（2026-11-23 / 2026-11-24），但扫描的是 #209 之前的代码；#209 改为方案 A 后尚未复扫**
> 最近一次实测以 [`lab5-lab7-quality-gates.md`](lab5-lab7-quality-gates.md) §4.6b 为准：**Bugs 3 → 1**、Coverage 70.5%、Quality Gate **Failed**（`User.java` 的 `java:S5998`）。#209 已按方案 A 改代码，**最终代码未再扫描**；因此当前事实仍是 Failed，不能写成已修复。
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
| `02-coverage.png` | Measures → Coverage：**70.3% / 173 单测**（11-23 扫描；11-24 复扫为 70.5% / 174，截图未覆盖最终代码） |
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

### 5.2 第二轮复扫（2026-11-24，随 PR #207 二次修正正则后）

#207 把 EMAIL 正则改为「至少一段点域名」的写法：

```java
Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w-]+(\\.[\\w-]+)*$")
```

复扫结果（SonarQube 9.9.8，`2026-11-24 15:31` 分析）：

| 指标 | 11-23 | **11-24** |
| --- | --- | --- |
| Bugs | 1 | **1** |
| Coverage | 70.3% | **70.5%** |
| Unit Tests | 173 | **174** |
| Vulnerabilities | 0 | 0 |
| 门禁 | Failed | **Failed（仍卡 Bugs）** |

**`java:S5998` 仍然报，位置由 L24 移到 L28。**

原因：该规则把「**含 `+` 的分组再被 `*` 修饰**」也判定为重复歧义——`([\\w-]+` 在内层 `+`，`(\\.[\\w-]+)*` 在外层 `*`。
所以**只在「外层 `+` / 外层 `*` / 分组内顺序」之间反复挪写法，无法消除这条规则**。

#### 两个可选处置（建议模块负责人二选一）

**方案 A（已实施 2026-11-24）：改成不含任何重复分组**（语义变松，已补域名校验）

```java
Pattern.compile("^[\\w.+-]+@[\\w.-]+$")   // 单字符类，无分组
```

- 代价：`a@.b`、`a@b..c` 这类畸形域名也会通过；
- 建议配套：在 `Guard`/`User` 里补一条廉价校验（域名不含 `..`、不以 `.` 开头结尾），代码比正则更可读。

**方案 B：保留当前正则，在 SonarQube 里把该问题标记为 `Won't Fix` 并写明理由**

- 理由模板：`EMAIL` 长度上限是 **100**（`AuthController` 的 `@Size(max = 100)` 与
  `User.checkEmail` 的 `length() > 100` 双重约束），且仅用于注册/登录表单，
  输入规模受控（≤100 字符），S5998 描述的「大输入导致栈溢出」在本场景不构成实际风险；
- 效果：该 issue 从 Bugs 指标中移除，门禁即可 PASSED；
- 注意：这是**显式处置**，需在评审记录里写清，不能当作已修复。

> 两种方案都能让门禁转绿；**关键区别是「改代码」还是「改判定」**，需 PO/模块负责人在评审时定。

---

## 六、结论

重扫**已执行两轮**，但均对应 #209 之前的代码。最近结果 Coverage 70.5%、Vulnerabilities 0、**Bugs 1**，门禁 **Failed**。
#209 已按方案 A 把正则改为 `^[\w.+-]+@[\w.-]+$` 并补域名校验，但**最终代码尚未重新扫描**——
在拿到新的 SonarQube 截图前，Lab 7 的结论只能是「已定位、代码已改、待复扫」，不能写 PASSED。
