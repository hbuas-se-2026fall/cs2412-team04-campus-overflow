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

实测覆盖率（2026-11-17 发布口径）：qa 83.12% / identity 75.74% / reputation 80.72% / discovery 78.26%，均 ≥ 0.70。

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

## 三、结论

| 项 | 状态 | 证据 |
| --- | --- | --- |
| Lab 5 · ESLint / Prettier / EditorConfig / Husky / alias / noUncheckedIndexedAccess | ✅ 全部落地 | 本文件 §1；`npm run lint`/`format:check`/`typecheck`/`build` 全绿 |
| Lab 5 · pre-commit 能拦截不合规提交 | ✅ 实测两轮 | §1.3 的失败与成功输出 |
| Lab 7 · JaCoCo 报告与 70% 门禁 | ✅ 既有 | 4 个 domain 模块的 `check-domain-coverage` |
| Lab 7 · SonarQube 配置与 CI 接入 | ✅ 已配置（凭 token 启用） | `sonar-project.properties`、`backend/pom.xml`、`ci.yml` 阶段 6 |

**未完成项（需环境支持）**：在真实 SonarQube/SonarCloud 上跑出一次 Dashboard 截图并配置 Quality Gate。
本机未部署 SonarQube 服务器，故 CI 步骤做成"有 token 才跑"；拿到服务器地址后补一次扫描即可闭环。
