# Lab 1 · 开发环境版本汇总（角色卡 E3）

> 团队：TeamX　项目：CampusOverflow　对应实验：Lab 1 团队组建与协作工具链（Sprint 0）
> 对应提交物：Lab 1【环境与前置准备】——Git 2.40+、JDK 21、Node.js 20+、Docker Desktop
> 负责人：**房国栋（Dev-2 前端与验证）**　验收标准：**5 份齐，且每份含 `mvn clean test` 结果，不能只截版本号**
> 采集口径：同一仓库版本（`main`）、同一命令、同一周内完成；截图必须带终端提示符与执行路径

---

## 1. 采集方法（不要手工复制版本号）

仓库内提供两条等价脚本，运行后直接打印一行 Markdown，粘贴进第 2 节表格即可：

```powershell
# Windows (PowerShell)
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev-env/collect-env.ps1 -Name 房国栋 -GitHub 2177023844
```

```bash
# Linux / macOS / Git Bash
bash scripts/dev-env/collect-env.sh 房国栋 2177023844
```

脚本会依次探测 `java`、`mvn`、`node`、`docker`、`git`，缺失的项输出 `not installed`，不会中途退出。

> ⚠️ 踩坑记录：`.ps1` 脚本刻意写成**纯 ASCII**。Windows PowerShell 5.1 在没有 BOM 的情况下按 ANSI（GBK）读取 `.ps1`，脚本里一旦出现中文就会解析失败（`意外的标记`）。
> 如果你要改成中文提示，必须把文件另存为 **UTF-8 with BOM**；用 `pwsh` 7 则不受影响。

采集完成后同时截图（同一终端窗口，先跑脚本再跑测试，一张图两件事）：

```bash
cd backend
mvn clean test          # 结果决定第 3 节怎么填，红也要截，见 3.2
```

---

## 2. 环境版本汇总表

| 成员 | GitHub | OS | JDK | Maven | Node.js | Docker | 采集日期 | env 截图 | mvn 测试证据 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 房国栋 | `2177023844` | Windows 11（`10.0.26200`） | 21.0.10（JBR-21.0.10+1-1163.110） | 3.9.11 | v24.18.1 / npm 11.16.0 | **29.9.3**（本机实测，Engine running） | 2026-11-18 | ✅ [`env-fangguodong.png`](env/env-fangguodong.png)（10-29 首采）+ ✅ [`env-fangguodong-docker.png`](env/env-fangguodong-docker.png)（11-18 Docker 二采） | ✅ [`screenshots/mvn-test-fangguodong.png`](screenshots/mvn-test-fangguodong.png)（2026-11-18 全绿）+ 日志 [`logs/mvn-test-fangguodong-20261118.log`](logs/mvn-test-fangguodong-20261118.log) |
| 魏志国 | `2123345397-star` | Windows 11（`10.0.26200`）Git Bash（MINGW64） | ⚠️ 25.0.2（达标线 21；本机为更高版本，CI 固定 JDK 21） | 3.9.13 | v24.21.0 / npm 11.19.0 | 29.9.2（Engine running） | 2026-11-16 | ✅ [`env-weizhiguo.png`](env/env-weizhiguo.png) | ✅ [`logs/mvn-test-weizhiguo.log`](logs/mvn-test-weizhiguo.log) + [`screenshots/mvn-test-weizhiguo.png`](screenshots/mvn-test-weizhiguo.png) |
| 陈明哲 | `cmz666-BOT` | Windows 11（`10.0.26200`） | 21.0.10 | 3.9.9 | v24.21.0 / npm 11.19.0 | ⬜ 未安装（见 §6；全栈 compose 由房国栋机器验证） | 2026-11-16 | ✅ [`env-chenmingzhe.png`](env/env-chenmingzhe.png) | ✅ [`mvn-test-chenmingzhe.png`](screenshots/mvn-test-chenmingzhe.png)（历史 CI run 45742927110 / `62332d2`：264 单测 + FlowIT 4 = **268**；`P5` 修复分支本地 277 单测 / 总口径 281，新的 CI run 待补；204 为更早批次）+ [`logs/mvn-test-chenmingzhe.log`](logs/mvn-test-chenmingzhe.log) |
| 刘俊琒 | `Liujunfeng520` | Microsoft Windows 10.0.19045 | 21.0.11（java/javac 同版本） | 3.9.11 | v24.20.0 / npm 11.19.0 | ⬜ 未安装（见 §6） | 2026-11-17 | ✅ [`env-liujunfeng.png`](env/env-liujunfeng.png) | ✅ [`mvn-test-liujunfeng.png`](screenshots/mvn-test-liujunfeng.png) + [`logs/mvn-test-liujunfeng.log`](logs/mvn-test-liujunfeng.log) |
| 青岑 | `qing963666` | Microsoft Windows 10.0.26200 | 21.0.10 | 3.9.2 | v26.13.0 / npm 11.20.0 | 29.9.2 | 2026-11-09 | ✅ [`env-qingcen.png`](env/env-qingcen.png) | ✅ [`screenshots/mvn-test-qingcen.png`](screenshots/mvn-test-qingcen.png) |

图例：✅ 达标　⚠️ 版本不符但已记录　⬜ 待采集

**达标线（Lab 1 明确要求）**

| 工具 | 要求 | 判定方式 |
| --- | --- | --- |
| Git | 2.40+ | `git --version` 主版本号 ≥ 2.40 |
| JDK | 21 | `java -version` 显示 `21.x`；`javac -version` 与 `java` 必须同版本 |
| Maven | 3.9+ | `mvn -v` 首行 `Apache Maven 3.9.x` |
| Node.js | 20+ | `node -v` ≥ v20 |
| Docker | Desktop 可运行 | `docker version` 能连上 daemon（`docker info` 不报错） |

> JDK 必须 21：后端 `pom.xml` 以 Java 21 编译，用 17 或 8 会在 `mvn clean install` 阶段报 `invalid target release: 21`。

---

## 3. `mvn clean test` 取证（E3 的关键一半）

### 3.1 当前团队状态（2026-10-28 复查）

后端按 Lab 4 的 TDD 策略**先落了测试、领域实现留桩**（`co-qa-domain` 中的 `TODO(S2)`，负责人青岑）。因此 `backend/` 整体 `mvn clean test` 现阶段**预期为红**。这不是环境问题，交付时必须把"红的原因"和"绿的部分"一起给出，否则老师会误判为环境没配好。

**实测数据（2026-10-29，房国栋，JDK 21 / Maven 3.9.11）**：

| 项目 | 结果 |
| --- | --- |
| 构建结果 | `BUILD FAILURE`，停在 `co-qa-domain` 的 surefire |
| 测试统计 | `Tests run: 18, Failures: 2, Errors: 13`（3 通过） |
| 失败根因 | `java.lang.UnsupportedOperationException: TODO(S2)：MarkdownBody.ofQuestion 尚未实现` |
| 失败用例分布 | `QuestionTest$Posting` 1、`$Accepting` 5、`$EditingAndDeleting` 3、`AnswerTest` 3、`VoteAndCommentTest` 3 |
| 已通过模块 | `co-shared-kernel` 2 个、`co-identity-domain` 9 个（含 `CourseTest` 3 个） |
| 原始日志 | [`logs/mvn-test-fangguodong.log`](logs/mvn-test-fangguodong.log)（79 KB） |

### 3.2 交付格式：红 + 绿 两张证据

**(a) 整体结果（红，说明原因）**

```bash
cd backend
mvn clean test -DfailIfNoTests=false 2>&1 | tee ../docs/lab1-submission/logs/mvn-test-$(whoami).log
```

截图中要能看到：`BUILD FAILURE`、失败的测试类，以及失败原因是 `UnsupportedOperationException` 或 `TODO(S2)` 桩未实现——而不是 `Could not resolve dependencies` 或 `No compiler is provided`（那才是环境问题）。

**(b) 已实现模块（绿，证明环境可用）**

```bash
cd backend
mvn -pl co-shared-kernel -am install -DskipTests     # 先装依赖
mvn -pl co-shared-kernel test                        # 单独跑，应全绿
```

> 踩坑记录：`mvn -pl co-qa test` 只会构建聚合 POM、不会进叶子模块；正确做法是先 `-pl <叶子模块> -am install -DskipTests`，再进该模块 `mvn test`。

### 3.3 结果登记表

| 成员 | 整体 `mvn clean test` | 失败模块 / 原因 | 已实现模块 `mvn test` | 日志 / 截图 |
| --- | --- | --- | --- | --- |
| 房国栋 | ❌ 红（预期） | `co-qa-domain`：`TODO(S2)：MarkdownBody.ofQuestion 尚未实现`；18 个用例中 2 失败 13 错误 | ✅ `co-shared-kernel` 2、`co-identity-domain` 9 全绿 | [`logs/mvn-test-fangguodong.log`](logs/mvn-test-fangguodong.log) |
| 魏志国 | ❌ 红（预期） | `co-discovery-domain`：`TODO(S3)：SearchQuery.terms / RelatedQuestionPolicy 尚未实现`（S3 未开工的 TDD 桩）；1 失败 2 错误 | ✅ shared-kernel 2、identity 9、qa-domain 42、qa-application 32、reputation 36，共 121 条全绿 | [`logs/mvn-test-weizhiguo.log`](logs/mvn-test-weizhiguo.log)、[`screenshots/mvn-test-weizhiguo.png`](screenshots/mvn-test-weizhiguo.png) |
| 陈明哲 | ✅ 全绿（CI 全量） | 无 | ✅ CI 全量 268 用例（264 单测 + FlowIT 4，run 45742927110）；`P5` 修复分支本地 277 单测，新的 CI run 待补 | [`logs/mvn-test-chenmingzhe.log`](logs/mvn-test-chenmingzhe.log) + [`screenshots/mvn-test-chenmingzhe.png`](screenshots/mvn-test-chenmingzhe.png) |
| 刘俊琒 | ✅ 全绿（S4 收口后） | 无 | ✅ 27 个模块、165 个用例全绿 | [`logs/mvn-test-liujunfeng.log`](logs/mvn-test-liujunfeng.log) + [`screenshots/mvn-test-liujunfeng.png`](screenshots/mvn-test-liujunfeng.png) |
| 青岑 | ✅ QA 两模块绿（交付时 42+32） | 无 | ✅ 交付时 `co-qa-domain` 42、`co-qa-application` 32；2026-11-27 当前为 44、56 | [`screenshots/mvn-test-qingcen.png`](screenshots/mvn-test-qingcen.png) |

> 上表是 **Lab1 交付时快照**，不是当前状态：`TODO(S2)`/`TODO(S3)` 桩已实现，全仓 2026-11-27 为 281 单测 + 5 FlowIT，见 [`final-test-report.md`](../qa/final-test-report.md)。保留历史失败口径仅用于说明当时的红/绿证据。

---

## 4. 截图命名与存放

**环境证据统一放在 [`env/`](env/) 子目录**（目录说明见 [`env/README.md`](env/README.md)）：

| 文件 | 内容 | 要求 |
| --- | --- | --- |
| `env/<拼音>.png`（别名 `env-<拼音>.png`） | 版本采集脚本输出 | 终端提示符、命令与输出同屏 |
| `env/<拼音>.txt` | 采集脚本打印的 Markdown 行 | 可直接恢复空白的汇总表 |
| `screenshots/mvn-test-<拼音>.png` | `mvn clean test` 结果 | 含 `BUILD FAILURE` / `BUILD SUCCESS` 行 |
| `screenshots/mvn-test-green-<拼音>.png` | 已实现模块 `mvn test` 全绿 | 含 `Tests run: N, Failures: 0` |

已归档：`env/env-fangguodong.png`、`env/env-fangguodong.txt`。
其余四人补齐后同样放进 `env/`，并把第 3 节表格改成 ✅。

---

## 5. E3 关闭条件（逐条打勾才算交付）

- [x] **5/5** 份 `env-<拼音>.png` 齐备（房国栋、魏志国、陈明哲、刘俊琒、青岑）
- [x] **5/5** 份 `mvn-test-*` 齐备（房国栋/刘俊琒/青岑有本机全绿截图；魏志国 121 条；陈明哲为 CI 全量截图+日志（历史 CI 268 / 合入 P5 后本地 277），属环境例外已记录）
- [x] 至少 1 份"已实现模块全绿"证据：房国栋 `screenshots/mvn-test-fangguodong.png`——27 模块 165 用例全绿
- [x] 本页第 2、3 节表格无空白 ⬜（陈明哲 Docker「未安装」+ 刘俊琒 Docker 例外已在 §6 登记；全栈部署由房国栋机器完成）
- [x] 版本差异（如有）已写入下方"已知差异"并说明处置

> 2026-11-18 复核：房国栋的 `mvn clean test` 已由"10-29 红（TODO 桩未实现）"变为**全绿**
> （27 模块 / 165 用例 / Failures 0 / Errors 0 / BUILD SUCCESS），截图见
> [`screenshots/mvn-test-fangguodong.png`](screenshots/mvn-test-fangguodong.png)，
> 原始日志 [`logs/mvn-test-fangguodong-20261118.log`](logs/mvn-test-fangguodong-20261118.log)。
> 截图由该真实日志渲染，任何人重跑 `cd backend && mvn -B clean test` 可复现。
> E3 剩余缺口：**仅剩 Docker 一列**——陈明哲「未安装」、刘俊琒标记为例外（见 §6），其余五列（OS/JDK/Maven/Node/截图/测试证据）已 5/5 齐备。

> Docker 状态（2026-11-18）：房国栋本机已装 Docker Desktop 29.9.3（WSL2 后端），`docker info` 正常。
> **干净环境部署验证已完成**：`docker compose up -d mysql redis` 两容器 `healthy`，后端启动 
> `Started CampusOverflowApplication in 12.3s`、`/actuator/health` = `UP`、演示数据可读。
> 证据见 [`docs/reports/s4-clean-env-deploy-verify.md`](../reports/s4-clean-env-deploy-verify.md)。
> 注意：USTC / 网易 / 腾讯云镜像源在 2026-11-18 实测全部不可用，请改用 `docker.1ms.run`。

## 6. 已知差异与处置

| 成员 | 差异 | 处置 | 状态 |
| --- | --- | --- | --- |
| 房国栋 | `git` 未加入系统 PATH（使用 `D:\Program Files\Git\cmd\git.exe`） | 已加入 PATH；本文档命令仍保留绝对路径兜底 | ⚠️ 已记录 |
| 房国栋 | 命令行 `java` 默认指向 JDK **1.8.0_481**（系统 PATH 里 Oracle `javapath` 排在前面），但 IDEA/JBR 的 21 可用；Maven 实际已用 21（编译日志 `release 21` 且无 `invalid target release`） | 截图前执行 `$env:JAVA_HOME="E:\IntelliJ IDEA 2025.3.3\jbr"; $env:Path="$env:JAVA_HOME\bin;"+$env:Path`；长期方案见文末"PATH 修正" | ✅ 已解决（临时） |
| 房国栋 | Docker Desktop：10-29 记录为"未安装"，11-16 由青岑代跑取值，11-18 本人机器装好并实测 | `docker --version` → `Docker version 29.9.3, build 5457199`；`docker info --format '{{.ServerVersion}}'` → `29.9.3`；`docker info` 的 `Registry Mirrors` 为 `https://docker.1ms.run`（中科大 / 网易 / 腾讯云三家已停服，实测 http=000）；二采截图见 [`env/env-fangguodong-docker.png`](env/env-fangguodong-docker.png)；WSL2 后端（`wsl --install --no-distribution` + 重启后可运行） | ✅ 已完成（2026-11-18 本机实测） |
| 魏志国 | 本机 JDK 为 25.0.2（达标线 21） | 高于达标线、可编译运行；JaCoCo 0.8.12 agent 不支持 JDK 25，本地取证用 `-Djacoco.skip=true`（CI 用 JDK 21 正常出覆盖率）；不再为此降级本机环境 | ⚠️ 已记录 |
| 刘俊琒 | 系统 PATH 里 `java` 指向 `...\Oracle\Java\javapath` 转发存根（本机已无对应 JDK，执行无输出）；`mvn` 完全未入 PATH（Maven 由 IDEA 内置，在 `D:\ideal\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin`）；`C:\Program Files\nodejs` 条目被截断成 `gram Files\nodejs\`（丢了 `C:\Pro`），导致终端里 `node` 找不到 | 采集前在同一窗口临时修正（命令见 [`env/env-liujunfeng.txt`](env/env-liujunfeng.txt) §0）；长期方案同本文末「PATH 修正」，把三条写进系统变量 | ⚠️ 已记录（2026-11-17） |
| 刘俊琒 | Docker Desktop 未安装（`C:\Program Files\Docker` 不存在） | 本机不参与容器化冒烟与镜像构建；Docker 相关验证由房国栋/青岑代跑，与本表房国栋 2026-10-29 同一处置 | ⚠️ 已记录 |

| 陈明哲 | Docker Desktop 未安装（WSL2/Hyper-V 未启用；本机不参与容器化验证） | 全栈 compose 由房国栋机器完成（[`s4-compose-fullstack-verify.md`](../reports/s4-compose-fullstack-verify.md)）；本机用 jar + MySQL/Redis 完成干净库全流程冒烟 | ⚠️ 已记录（2026-11-19） |
### PATH 修正（长期，一次到位）

1. `Win + R` → `sysdm.cpl` → 高级 → 环境变量；
2. 系统变量 `Path` 中把 `E:\IntelliJ IDEA 2025.3.3\jbr\bin` 移到最前，或删除 `Oracle\Java\javapath`、`java8path` 两条；
3. 新建系统变量 `JAVA_HOME` = `E:\IntelliJ IDEA 2025.3.3\jbr`；
4. 重开终端，`java -version` 应为 `21.x`。

---

## 7. 与其他提交物的关系

- 与角色卡 **E4 身份绑定核查** 配套：[`identity-audit.md`](identity-audit.md)（版本对了、人也要对得上）。
- 与 Lab 1 总索引：[`README.md`](README.md)。
