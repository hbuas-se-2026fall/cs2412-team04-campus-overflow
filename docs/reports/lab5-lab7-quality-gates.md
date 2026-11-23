# Lab 5 前端工程化 + Lab 7 质量门禁（验收记录）

> 执行人：房国栋（D，2177023844）　日期：2026-11-19
> 对应实验：《软件工程》Lab 5「前端工程骨架（代码质量基础设施）」、Lab 7「测试、代码评审与质量门禁」

---

## 一、Lab 5 · 代码质量基础设施

原状态：`frontend/` 只有 `package.json` + `tsconfig.json` + `vite.config.ts`，**没有 ESLint / Prettier / EditorConfig / Husky**，`scripts` 里也只有 `dev`/`build`/`preview`/`typecheck`。

本次补齐（对应 Lab 5 Step 2 四项要求）：

| 要求 | 落地 | 文件 |
| --- | --- | --- |
| ESLint + @typescript-eslint + eslint-plugin-react-hooks | ESLint 9 flat config，含 `rules-of-hooks`（error）与 `exhaustive-deps`（warn） | `frontend/eslint.config.js` |
| Prettier 与 EditorConfig 统一格式 | Prettier 3 配置 + 忽略清单 + EditorConfig | `frontend/.prettierrc`、`.prettierignore`、`.editorconfig` |
| Husky + lint-staged pre-commit | 仓库根 `.husky/pre-commit` → `cd frontend && npx lint-staged` | `.husky/pre-commit`、根 `package.json`、`frontend/package.json` 的 `lint-staged` 段 |
| vite path alias `@/...` | `resolve.alias` + `tsconfig.paths` 双向对齐 | `frontend/vite.config.ts`、`frontend/tsconfig.json` |

同时按 Lab 5 Step 1 补 `noUncheckedIndexedAccess`（原只有 `strict`）：

```jsonc
"strict": true,
"noUncheckedIndexedAccess": true,   // 新增：数组/对象索引访问强制判空
"baseUrl": ".",
"paths": { "@/*": ["src/*"] }       // 新增：与 vite alias 对齐
```

### 1.1 新增 npm scripts

| 命令 | 作用 |
| --- | --- |
| `npm run lint` | ESLint 全量检查 |
| `npm run lint:fix` | 自动修可修项 |
| `npm run format` | Prettier 写入格式化 |
| `npm run format:check` | 仅检查不改动（CI 用） |
| `npm run typecheck` | `tsc --noEmit`（原有） |
| `npm run build` | `tsc -b && vite build`（原有） |

### 1.2 验证结果

```
$ cd frontend && npm run lint
✖ 3 problems (0 errors, 3 warnings)

$ npm run format:check
All matched files use Prettier code style!

$ npm run typecheck
（无输出 = 0 error）

$ npm run build
✓ built in 978ms   dist/assets/index-BlpapHnV.js  334.61 kB
```

3 个 warning 均为**建议级**，不阻塞：

| 文件 | 规则 | 说明 |
| --- | --- | --- |
| `auth.tsx:47`、`MarkdownPreview.tsx:73` | `react-refresh/only-export-components` | 文件同时导出组件与工具函数，影响 HMR 边界；不影响运行 |
| `BountyDialog.tsx:38` | `react-hooks/exhaustive-deps` | `useEffect` 依赖 `load`；当前写法是刻意的（仅在 questionId 变化时重载），加 `eslint-disable-next-line` 或补 `useCallback` 均可，留待重构 |

> `noUncheckedIndexedAccess` 开启后**全仓 0 新增错误**，说明既有代码没有越界索引隐患。

### 1.3 pre-commit 钩子实测（两轮）

**第一轮：故意提交不合规文件（未使用变量 + 错误格式）**

```
✖ prettier --write
↓ eslint --fix --no-warn-ignored

✖ eslint --fix --no-warn-ignored:
  src/__hook_test.tsx
  1:7  error  'unusedVar' is assigned a value but never used.
              Allowed unused vars must match /^_/u  @typescript-eslint/no-unused-vars

✖ 1 problem (1 error, 0 warnings)
husky - pre-commit script failed (code 1)
```

→ **提交被阻止**，且 lint-staged 自动把工作区还原（`Reverting to original state`），不会留下半成品。

**第二轮：合规文件**

```
✔ Done running tasks for staged files!
✔ Done staging changes from tasks!
```

→ **提交通过**，且文件被 Prettier 自动格式化后一起入库。

> 注：验证用的临时文件已删除，未留在仓库。

---

## 二、Lab 7 · 质量门禁与 SonarQube

### 2.1 已有基础（本次未改）

| 项 | 状态 | 位置 |
| --- | --- | --- |
| JaCoCo 采集与报告 | ✅ 已有 | `backend/pom.xml`：`prepare-agent` + `report`（`verify` 阶段产出 `jacoco.xml`） |
| 领域层覆盖率门禁 70% | ✅ 已有 | 4 个 `*-domain/pom.xml` 的 `check-domain-coverage`，阈值 `${domain.coverage.minimum}=0.70` |
| 架构适应度函数 | ✅ 已有 | `ArchitectureTest`（FF-1~FF-6），CI 阶段 3 |
| 集成测试硬门禁 | ✅ 已有 | CI 阶段 5，Testcontainers 起 MySQL + Redis |

实测覆盖率（2026-11-17 发布口径）：qa 83.12% / identity 75.74% / reputation 81.93% / discovery 78.26%，均 ≥ 0.70。

### 2.2 本次补齐

**① `sonar-project.properties`（仓库根）**

- 源码范围：`backend`（27 模块）+ `frontend/src`
- 排除：`target/`、`node_modules/`、`dist/`、`docs/`、`deploy/`、`scripts/`
- JaCoCo 报告接入：`sonar.coverage.jacoco.xmlReportPaths=backend/**/target/site/jacoco/jacoco.xml`
- 覆盖率排除：`co-bootstrap`（启动装配，无业务逻辑）、`*Application.java`、`*Config.java`、DTO、事件记录类
- 前端：`sonar.typescript.tsconfigPath=frontend/tsconfig.json`
- `sonar.qualitygate.wait=true`：门禁不通过则构建失败

**② `sonar-maven-plugin` 注册**

```xml
<sonar-maven-plugin.version>4.0.0.4121</sonar-maven-plugin.version>
...
<plugin>
  <groupId>org.sonarsource.scanner.maven</groupId>
  <artifactId>sonar-maven-plugin</artifactId>
  <version>${sonar-maven-plugin.version}</version>
</plugin>
```

**③ CI 两处增强（`.github/workflows/ci.yml`）**

| 位置 | 变更 |
| --- | --- |
| backend 阶段 6（新增） | `SonarQube 扫描` —— **未配置 `SONAR_TOKEN` 时自动跳过**，配置后启用并等待门禁 |
| backend 归档（新增） | `归档 JaCoCo 覆盖率报告` → `backend/**/target/site/jacoco/**` |
| frontend（新增两步） | `ESLint`（`npm run lint`）+ `Prettier 格式检查`（`npm run format:check`），排在 typecheck 之前 |

> 设计取舍：Sonar 步骤用 `if: env.SONAR_TOKEN != ''` 做条件。仓库没有 SonarQube 服务器时 CI 不红；一旦在仓库 Settings 里配好 `SONAR_TOKEN`（secret）与 `SONAR_HOST_URL`（variable），扫描自动生效。这样既不阻塞当前 PR，又留下了完整接入路径。

### 2.3 如何在本地跑一次 Sonar 扫描

```bash
# 1) 起 SonarQube（本机 Docker）
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community

# 2) 首次登录 http://localhost:9000 （admin/admin，按提示改密），
#    My Account → Security → Generate Tokens 生成 token

# 3) 生成测试与覆盖率报告
cd backend
mvn -B clean verify -DskipITs -Djacoco.skip=false

# 4) 推送扫描结果
mvn -B sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<你的token>

# 5) 在 SonarQube → Quality Gates 配置门禁
#    Coverage > 70%、Bugs = 0、Vulnerabilities = 0
```

> SonarQube 需要 ≥2 GB 可用内存；容器首次启动约 1~2 分钟。

---


### 2.4 同行评审清单

新增 [`docs/team/peer-review-checklist.md`](../team/peer-review-checklist.md)：作者自检 7 项 + 评审人 9 维清单（需求/架构/正确性/测试/安全/数据/可观测/文档/CI）+ 评论前缀约定 + Approve 条件 + 评审记录模板。

## 三、结论

| 项 | 状态 | 证据 |
| --- | --- | --- |
| Lab 5 · ESLint / Prettier / EditorConfig / Husky / alias / noUncheckedIndexedAccess | ✅ 全部落地 | 本文件 §1；`npm run lint`/`format:check`/`typecheck`/`build` 全绿 |
| Lab 5 · pre-commit 能拦截不合规提交 | ✅ 实测两轮 | §1.3 的失败与成功输出 |
| Lab 7 · JaCoCo 报告与 70% 门禁 | ✅ 既有 | 4 个 domain 模块的 `check-domain-coverage` |
| Lab 7 · 同行评审清单 | ✅ 已入库 | [`docs/team/peer-review-checklist.md`](../team/peer-review-checklist.md) |
| Lab 7 · SonarQube 配置与 CI 接入 | ✅ 已配置（凭 token 启用） | `sonar-project.properties`、`backend/pom.xml`、`ci.yml` 阶段 6 |
| Lab 7 · 真实 Dashboard 截图 + Quality Gate 跑通 | ✅ 已跑通（修复前 Failed → 复扫后剩 1 Bug，见 §4.6b） | `docs/screenshots/lab7-sonar/`（修复前）+ [`lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)（复扫） |
| Lab 7 · SonarQube 重扫截图 | ✅ **已执行（2026-11-23）** | 六张见 [`screenshots/lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)；结果见 §4.6b |

---

## 四、SonarQube 实测（2026-11-19 修复前基线）

> 上一版这里写的是「需环境支持、待补」；本次已在本机部署 SonarQube 并跑出真实结果，闭环。

### 4.1 环境

```bash
docker run -d --name sonarqube -p 9000:9000   -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:lts-community
# 版本 9.9.8.100196，状态 UP
```

生成 token：`POST /api/user_tokens/generate`（admin）。首次访问会强制改默认密码，已改为强密码。

### 4.2 执行

```bash
# 1) 产出 jacoco.xml（11 份）
cd backend && mvn -B clean verify -DskipITs -Djacoco.skip=false

# 2) 扫描
mvn -B -f backend/pom.xml sonar:sonar   -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
```

### 4.3 结果

| 指标 | 值 |
| --- | --- |
| ncloc | 8,261 |
| **Coverage** | **70.3%**（Line 72.9%，Condition 63.4%） |
| Unit Tests | 173 通过 / 0 失败 / 100% 成功 |
| **Bugs** | **3** |
| Vulnerabilities | 0 |
| Code Smells | 62 |
| Duplicated Lines | 未超阈值 |

**质量门禁 `CampusOverflow Gate`**（已设为项目门禁）条件：

| 范围 | 指标 | 运算符 | 阈值 | 实际 | 结果 |
| --- | --- | --- | --- | --- | --- |
| New Code | Coverage | LT | 80% | — | 通过（无新代码） |
| New Code | Duplicated Lines | GT | 3% | — | 通过 |
| New Code | Maintainability / Reliability / Security Rating | worse than | A | A | 通过 |
| New Code | Security Hotspots Reviewed | LT | 100% | — | 通过 |
| Overall | **Coverage** | LT | **70%** | **70.3%** | ✅ 通过 |
| Overall | **Bugs** | GT | **0** | **3** | ❌ **失败** |
| Overall | **Vulnerabilities** | GT | **0** | **0** | ✅ 通过 |

**门禁状态：Failed（1 条条件失败：3 Bugs > 0）** —— 这是真实结果，未做任何规避。

### 4.4 三个 Bug 的归属（不擅自修改）

按团队分工，这三个都落在队友负责的模块，只在本文登记并指派，未直接改代码：

| # | 位置 | Sonar 提示 | 严重度 | 归属 |
| --- | --- | --- | --- | --- |
| 1 | `co-identity-domain/.../User.java:24` | EMAIL 正则 `[\w.+-]+@[\w-]+(\.[\w-]+)+` 存在嵌套量词，大输入下可能灾难性回溯（可导致栈溢出） | MAJOR | 身份上下文（陈明哲/魏志国）<br>建议改为 `^[\w.+-]+@([\w-]+\.)+[\w-]+$` |
| 2 | `co-qa-application/.../QuestionCommandService.java:65` | `orElseThrow` 的返回值未被使用 | MAJOR | 问答应用层（青岑）<br>建议改为 `if (findCourse(id).isEmpty()) throw ...` |
| 3 | ~~`co-qa-infrastructure/.../RedisQuestionListCache.java:53`~~ | ~~`60 + nextInt(15)` 是 int 相加~~ | MINOR | ✅ **已于 2026-11-19 修复**（PR #197 `fix/qa-cache-ttl-long`，现为 `60L + ...`） |

> 三处都是**真实小缺陷**，修复成本极低（各 1 行）。第 3 个已随 PR #197 修复，重新扫描后 Bugs 预计降为 **2**；
> 余下 2 个（正则嵌套量词、`orElseThrow` 返回值）建议在 v1.1 首个 PR 一并处理，处理完门禁即可转绿。

### 4.5 截图

| 文件 | 内容 |
| --- | --- |
| [`screenshots/lab7-sonar/01-dashboard.png`](../screenshots/lab7-sonar/01-dashboard.png) | 项目 Dashboard，左上 `QUALITY GATE STATUS: Failed`，明示「3 Bugs is greater than 0」 |
| [`screenshots/lab7-sonar/02-coverage.png`](../screenshots/lab7-sonar/02-coverage.png) | Measures → Coverage：**70.3%**，Lines to cover 1,524，Unit Tests 173 全通过 |
| [`screenshots/lab7-sonar/03-bugs.png`](../screenshots/lab7-sonar/03-bugs.png) | Issues 过滤 `types=BUG`：3 条，与 §4.4 表一致 |
| [`screenshots/lab7-sonar/04-projects.png`](../screenshots/lab7-sonar/04-projects.png) | 项目列表 |
| [`screenshots/lab7-sonar/05-quality-gate-list.png`](../screenshots/lab7-sonar/05-quality-gate-list.png) | Quality Gates 列表：`CampusOverflow Gate` + 内置 `Sonar way` |
| [`screenshots/lab7-sonar/06-quality-gate-conditions.png`](../screenshots/lab7-sonar/06-quality-gate-conditions.png) | 门禁条件明细：New Code 6 条 + **Overall：Bugs>0、Coverage<70%** |

> 以上为 2026-11-19 **修复前**（Failed）的六张证据。PR #197/#200 修复后的**复扫结果与截图已完成**，见 §4.6b 与 [`screenshots/lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)。

### 4.6 过程中的两个坑（已修正配置）

1. **`sonar-project.properties` 在 Maven 多模块下不生效**：Maven 从「被构建 POM 所在目录」读取该文件，`mvn -f backend/pom.xml` 时每个子模块又各自找自己的目录，根目录/backend 目录的配置都读不到 →
   症状是项目 key 变成 `com.campusoverflow:campus-overflow`、jacoco 报 `No report imported`。
   **修正**：把 sonar 属性写进父 POM 的 `<properties>`，由模块继承。
2. **jacoco 报告路径在多模块下不能用相对通配**：`target/site/jacoco/jacoco.xml` 会被按各模块 basedir 解析而找不到 →
   改为在父 POM 里列全 11 个模块的显式路径（正斜杠）。改完后 Coverage 从 `49.5%`（未导入报告）变为 `70.3%`（真实值）。

### 4.6b 复扫（2026-11-23，随 #197 / #200 修复后）

修复落地后重跑 `mvn clean verify -DskipITs` + `sonar:sonar`，结果变化：

| 指标 | 修复前（11-19） | **复扫（11-23）** | 说明 |
| --- | --- | --- | --- |
| Bugs | 3 | **1** | 降 2 个：#197 修了 `RedisQuestionListCache` 的 int→long；#200 修了 `QuestionCommandService` 的 `orElseThrow` |
| Vulnerabilities | 0 | 0 | — |
| Coverage | 70.3% | **70.3%** | 不变 |
| Unit Tests | 173 | **173（100% 通过）** | 不变 |
| ncloc | 8,261 | 8,260 | 少 1 行（`orElseThrow` 改写所致） |
| **质量门禁** | Failed（3 Bugs） | **Failed（1 Bug）** | 仍失败，但只剩一项 |

**剩余 1 个 Bug（未消除，需真正的修复）**：

| 位置 | 规则 | 提示 | 归属 |
| --- | --- | --- | --- |
| `co-identity-domain/.../User.java:24` | `java:S5998` | Refactor this repetition that can lead to a stack overflow for large inputs. | 身份上下文（陈明哲/魏志国） |

当前代码（#200 之后）：

```java
Pattern.compile("^[\\w.+-]+@([\\w-]+\\.)+[\\w-]+$")
```

**#200 只是把嵌套从 `([\\w-]+\\.[\\w-]+)+` 挪到了 `([\\w-]+\\.)+`，内层 `+` 仍套在外层 `+` 里，所以 S5998 依然报。**
建议改成不含嵌套量词的形式，例如：

```java
// 外层用 *，且每次重复必须以字面量 '.' 开头（转义为 \\.），消除歧义
Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*$")
```

> 该改动属身份上下文，建议由模块负责人提交并复扫确认（S5998 的判定以 Sonar 实际结果为准）。

复扫截图见 [`screenshots/lab7-sonar-rescan/`](../screenshots/lab7-sonar-rescan/)：
`01-dashboard.png`（门禁仍 Failed，明示 1 Bug）、`02-coverage.png`（70.3%）、
`03-bugs.png`（**只剩 1 条，定位 User.java:24**）、`04~06`（项目与门禁配置）。

> 原 `screenshots/lab7-sonar/` 保留为 11-19 首次扫描的证据，两者对照可见修复效果。

---

### 4.7 覆盖率口径说明（为什么 70% 而不是更高）

- Sonar 统计全仓 8,261 行有效代码；`infrastructure`（JPA/Redis 适配器）与 `interfaces`（REST 控制器）**行覆盖率为 0**，
  因为这些层靠 Testcontainers 集成测试验证，而 jacoco 目前只对 surefire 单测挂 agent（`prepare-agent`），未对 failsafe 挂 `prepare-agent-integration`。
- 因此 Sonar 覆盖率的 `sonar.coverage.exclusions` 排除了 `**/infrastructure/**` 与 `**/interfaces/**`，
  **与仓库既有政策一致**：`jacoco` 的 70% 门禁本来就只作用于 4 个 `*-domain` 模块（`domain.coverage.minimum=0.70`）。
- 这是**口径对齐**，不是为过门禁而临时放宽：若要连适配器一起算，需先给 failsafe 接 `prepare-agent-integration`（建议 v1.1 做）。
