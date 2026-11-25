# 四个 domain 覆盖率证据（v1.0.0）

> 采集人：陈明哲（E）　日期：2026-11-25（最新 CI 工件重采）
> 门槛：根 POM `domain.coverage.minimum=0.70`，四个 domain 模块各自 `check-domain-coverage` 通过
> 数据来源：全仓 `mvn verify -DskipITs -Djacoco.skip=false`（JDK 21）后生成的 JaCoCo HTML/CSV

## 结果

| 模块 | 行覆盖 | 行覆盖率 | 分支覆盖 | 分支覆盖率 |
| --- | ---: | ---: | ---: | ---: |
| `co-identity-domain` | 109 / 141 | **77.30%** | 54 / 80 | 67.50% |
| `co-qa-domain` | 194 / 237 | **81.86%** | 71 / 94 | 75.53% |
| `co-reputation-domain` | 136 / 166 | **81.93%** | 46 / 56 | 82.14% |
| `co-discovery-domain` | 73 / 93 | **78.49%** | 42 / 62 | 67.74% |

四个模块行覆盖率均 ≥ 70%，CI 覆盖率门禁阶段（`mvn verify -DskipITs -Djacoco.skip=false`，全量模块）通过。

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
# JDK 21
mvn -B -f backend/pom.xml verify -DskipITs -Djacoco.skip=false
```

然后打开各模块报告：

```text
backend/co-qa/co-qa-domain/target/site/jacoco/index.html
backend/co-identity/co-identity-domain/target/site/jacoco/index.html
backend/co-reputation/co-reputation-domain/target/site/jacoco/index.html
backend/co-discovery/co-discovery-domain/target/site/jacoco/index.html
```

CI（Linux）上同一命令由「覆盖率门禁」阶段执行；本机若 JDK 25 + JaCoCo 0.8.12 不兼容，请改用 JDK 21 取证。
