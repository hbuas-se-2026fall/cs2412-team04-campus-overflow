# Lab 7 · SonarQube 重扫证据与执行单

> 责任人：房国栋 / 陈明哲　状态：**配置、CI 接入、3 个 Bug 修复（PR #197/#200）已完成；重扫截图待有 SonarQube 的机器执行**
> 关联：`sonar-project.properties`、`backend/pom.xml`（sonar-maven-plugin）、`.github/workflows/ci.yml` 阶段 6
> 同行评审清单：[`docs/team/peer-review-checklist.md`](../team/peer-review-checklist.md)

## 一、现状

| 项 | 状态 | 证据 |
| --- | --- | --- |
| `sonar-project.properties` | ✅ 已入库 | 项目 key `campus-overflow`，源码/排除/JaCoCo/TS 配置完整 |
| `sonar-maven-plugin` | ✅ 已注册 | `pom.xml` 属性 `sonar-maven-plugin.version=4.0.0.4121` |
| CI 阶段 6 | ✅ 已接入（无 token 自动跳过） | `ci.yml`：`if: env.SONAR_TOKEN != ''`，配置 secret 后自动扫描并等待门禁 |
| 3 个 Sonar Bug | ✅ 已修复 | #197 缓存 TTL、#200 正则与 `orElseThrow`；重扫后 Bugs 应为 0 |
| 本机 SonarQube | ⬜ 无 | 本机无 Docker/WSL2，未安装 SonarQube 服务 |
| 重扫截图 | ⬜ 待执行 | 需在装 SonarQube 的机器跑一次，截 Quality Gate 页 |

## 二、执行步骤（在有 Docker 的机器）

```bash
# 1) 启动 SonarQube LTS（首次约 1–2 分钟，≥2 GB 内存）
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community

# 2) 浏览器 http://localhost:9000
#    admin/admin 登录并按提示改密
#    My Account → Security → Generate Tokens，生成 token

# 3) 生成测试与 JaCoCo（全量、跳过 Testcontainers 以节省时间）
cd backend
mvn -B clean verify -DskipITs -Djacoco.skip=false

# 4) 推送扫描（token 用上一步生成的）
mvn -B -f backend/pom.xml sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<token>

# 5) Quality Gate 配置
#    Coverage > 70%、Bugs = 0、Vulnerabilities = 0
#    等待 sonar.qualitygate.wait=true 返回 PASSED
```

## 三、要截什么图

1. `http://localhost:9000/dashboard?id=campus-overflow`：**Quality Gate passed**、Coverage、Bugs/Vulnerabilities 汇总；
2. `http://localhost:9000/component_measures?id=campus-overflow&metric=coverage`：四个 domain 的覆盖率明细；
3. 控制台 `BUILD SUCCESS` + `QUALITY GATE STATUS: PASSED` 一行。

建议保存为：

```text
docs/final/screenshots/sonar/sonar-quality-gate.png
docs/final/screenshots/sonar/sonar-coverage.png
docs/final/screenshots/sonar/sonar-console.txt
```

## 四、CI 侧启用方式

在 GitHub 仓库 Settings：

- Secrets：`SONAR_TOKEN`；
- Variables：`SONAR_HOST_URL`（如 `http://<sonar-host>:9000`）。

配置后 `后端构建与验证` 的阶段 6 会自动执行，并在门禁失败时让 CI 变红；未配置时保持跳过，不阻塞现有 PR。

## 五、验收

- [ ] SonarQube Quality Gate 为 **PASSED**
- [ ] Coverage ≥ 70%（四个 domain 与总体均达标）
- [ ] Bugs = 0、Vulnerabilities = 0
- [ ] 截图/控制台归档到 `docs/final/screenshots/sonar/`
- [ ] 在 `docs/reports/lab5-lab7-quality-gates.md` §三 勾选
