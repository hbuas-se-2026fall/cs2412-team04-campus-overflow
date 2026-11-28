# 四个 domain 覆盖率证据（2026-11-27 重采；标题原为 v1.0.0，数据与 tag 内快照不同，见下方说明）

> 本地复核：青岑（QA）　日期：2026-11-27（基线 `5a74fb0` + 未提交管理页改动，JDK 21.0.10；原采集人陈明哲）
> 门槛：根 POM `domain.coverage.minimum=0.70`，四个 domain 模块各自 `check-domain-coverage` 通过
> 数据来源：全仓 `mvn verify -DskipITs -Djacoco.skip=false`（JDK 21）后生成的 JaCoCo HTML/CSV
> **本机路径含中文时** JaCoCo agent 默认写不进 `target/jacoco.exec`；本次显式使用
> `-Djacoco.destFile=E:/tmp/coverage-all-2.exec -Djacoco.dataFile=E:/tmp/coverage-all-2.exec` 生成报告。
> 因此 `check-domain-coverage` 本地仍会被跳过，阈值门禁以 CI（Linux、纯 ASCII 路径）为准。

## 结果

| 模块 | 行覆盖 | 行覆盖率 | 分支覆盖 | 分支覆盖率 |
| --- | ---: | ---: | ---: | ---: |
| `co-identity-domain` | 109 / 141 | **77.30%** | 54 / 80 | 67.50% |
| `co-qa-domain` | 199 / 241 | **82.57%** | 76 / 100 | 76.00% |
| `co-reputation-domain` | 136 / 171 | **79.53%** | 46 / 58 | 79.31% |
| `co-discovery-domain` | 73 / 93 | **78.49%** | 42 / 62 | 67.74% |

四个模块行覆盖率均 ≥ 70%（本地报告）；`check-domain-coverage` 因中文路径问题本地被跳过，最终以 CI 的
`mvn verify -DskipITs -Djacoco.skip=false` 结果为准。

## 文件

| 模块 | 截图（JaCoCo HTML） | CSV（复核用） |
| --- | --- | --- |
| Identity | [`coverage-identity.png`](coverage-identity.png) | [`coverage-identity.csv`](coverage-identity.csv) |
| Q&A | [`coverage-qa.png`](coverage-qa.png) | [`coverage-qa.csv`](coverage-qa.csv) |
| Reputation | [`coverage-reputation.png`](coverage-reputation.png) | [`coverage-reputation.csv`](coverage-reputation.csv) |
| Discovery | [`coverage-discovery.png`](coverage-discovery.png) | [`coverage-discovery.csv`](coverage-discovery.csv) |

> 截图是 `target/site/jacoco/index.html` 的原样页面；百分比按 CSV 的
> `LINE_COVERED / (LINE_COVERED + LINE_MISSED)` 计算，避免把页面里的 Missed/Total 当成覆盖率。

## 复现

```powershell
# JDK 21；路径含中文时显式指定 ASCII 的 exec 路径
mvn -B -f backend/pom.xml verify -DskipITs -Djacoco.skip=false `
  -Djacoco.destFile=E:/tmp/coverage-all.exec -Djacoco.dataFile=E:/tmp/coverage-all.exec
```

然后打开各模块报告：

```text
backend/co-qa/co-qa-domain/target/site/jacoco/index.html
backend/co-identity/co-identity-domain/target/site/jacoco/index.html
backend/co-reputation/co-reputation-domain/target/site/jacoco/index.html
backend/co-discovery/co-discovery-domain/target/site/jacoco/index.html
```

CI（Linux）上同一命令由「覆盖率门禁」阶段执行；本机若 JDK 25 + JaCoCo 0.8.12 不兼容，请改用 JDK 21 取证。
