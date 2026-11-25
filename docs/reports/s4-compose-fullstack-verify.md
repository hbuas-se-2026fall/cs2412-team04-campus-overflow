# Sprint 4 · Docker 全栈部署验证（T4-01 补证）

> 执行人：房国栋（D，2177023844）　日期：2026-11-18
> 目的：补齐 `sprint-4.md` T4-01 最后一条「干净环境 `docker compose ... up -d --build` 按 README 走通」
> 证据：本文件 + `s4-compose-web.png`（经 80 端口访问）+ `s4-compose-search.png`

---

## 一、验证范围

与既有 `s4-clean-env-deploy-verify.md` 的区别：那次只起 MySQL + Redis（中间件），
本次是**完整全栈** —— 通过 `docker compose up -d --build` 构建并运行 **backend（Spring Boot）+ web（Nginx）**，
即 README §5「Docker 一键部署」真正走的那条路径。

```bash
# 生产 compose 需要 deploy/.env 注入 DB_PASSWORD / MYSQL_ROOT_PASSWORD / CO_FIELD_KEY / CO_FIELD_MAC_KEY
cp deploy/.env.example deploy/.env   # 填入真实值
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

> 修订（2026-11-25，审计 F-09）：原版命令 `docker compose -f deploy/docker-compose.yml up -d --build`
> 缺少 `--env-file`；当前生产 compose 对 `DB_PASSWORD` 等变量强制必填（`:?`），不带 env 会直接失败。

## 二、容器状态

| 容器 | 镜像 | 状态 | 端口 |
| --- | --- | --- | --- |
| co-mysql | mysql:8.0.39 | Up (healthy) | 3306（本机映射 3307，见 §五） |
| co-redis | redis:7-alpine | Up (healthy) | 6379 |
| co-backend | campus-overflow-backend:latest | **Up (healthy)** | 8080 |
| co-web | campus-overflow-web:latest | Up | 80 |

实测输出：

```
NAMES        STATUS                    PORTS
co-mysql     Up (healthy)              0.0.0.0:3307->3306/tcp
co-backend   Up (healthy)              0.0.0.0:8080->8080/tcp
co-web       Up                        0.0.0.0:80->80/tcp
co-redis     Up (healthy)              0.0.0.0:6379->6379/tcp
```

## 三、端到端验证

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 后端健康 | `curl http://localhost:8080/actuator/health` | `{"status":"UP","groups":["liveness","readiness"]}` |
| 前端可达 | `curl -o /dev/null -w "%{http_code}" http://localhost` | `200` |
| **Nginx 反代后端** | `curl "http://localhost/api/v1/questions?page=1&size=2"` | 返回问题列表 JSON（含 3 条演示数据） |
| 课程接口 | `curl http://localhost/api/v1/courses` | `[{"id":1,"code":"CS2001","name":"数据结构",...}]` |
| 排行榜 | `curl "http://localhost/api/v1/leaderboard?period=all"` | 小红 35 / 小明 10 / 系统管理员 0 |
| 登录链路 | 页内 `POST /api/v1/auth/login`（2023001 / Campus2026） | `200` |
| 首页渲染 | 浏览器打开 `http://localhost/` | 问题列表正常，见 `s4-compose-web.png` |
| 搜索页 | 浏览器打开 `http://localhost/search?q=红黑树` | 命中 + 相关标签侧栏，见 `s4-compose-search.png` |

> 关键点：以上所有请求都走 **80 端口**，证明 Nginx → backend → MySQL/Redis 三层链路在容器网络内是通的。

## 四、过程中发现并修复的问题

### 4.1 `deploy/backend.Dockerfile` 的 COPY 通配符无法匹配（**真实缺陷**）

**现象**：镜像构建成功、容器启动后立即退出，日志只有一行：

```
Error: Unable to access jarfile app.jar
```

**排查**：进入镜像查看 `/app/` 是空目录：

```bash
docker run --rm --entrypoint sh campus-overflow-backend:latest -c "ls -la /app/"
# total 8   → 空
```

再看 Dockerfile 原来写的是：

```dockerfile
COPY --from=build /src/co-bootstrap/target/co-bootstrap-*.jar app.jar
```

而 `co-bootstrap/pom.xml` 里有 `<finalName>campus-overflow</finalName>`，
实际产物是 `campus-overflow.jar` —— `co-bootstrap-*.jar` 这个通配符**匹配不到任何文件**。

Docker 的 `COPY` 通配符匹配不到时**不报错**，只是不复制（静默失败），所以构建阶段看起来全部成功。

**修复**：

```dockerfile
COPY --from=build /src/co-bootstrap/target/campus-overflow.jar app.jar
```

> 这是本次验证发现的真实缺陷：此前没有人跑过完整 compose 构建，只起过中间件，所以一直没暴露。

### 4.2 缺失 `.dockerignore` 导致构建上下文过大

首次构建没有 `.dockerignore`，会把 `frontend/node_modules`、各模块 `target/` 一并作为上下文
发给 daemon（仓库约 226 MB）。已补 `.dockerignore` 排除
`node_modules / dist / target / .git / *.mp4 / *.webm` 等。

> 注意：`.dockerignore` 只影响**发送给 daemon 的上下文**，不影响构建阶段内部 `mvn package` 生成的
> `target/`，所以排除 `target` 是安全的（Dockerfile 的 `COPY --from=build` 读的是构建阶段容器内的文件）。

### 4.3 Maven 依赖下载中断（环境问题，非代码问题）

首次构建在 `co-identity-interfaces` 阶段失败：

```
Could not transfer artifact org.apache.tomcat.embed:tomcat-embed-core:jar:10.1.40
  ... Premature end of Content-Length delimited message body
      (expected: 3,625,673; received: 515,621)
```

网络把 3.6 MB 的 jar 截断在 515 KB。处置：把本机 `~/.m2` 挂进构建阶段复用已有依赖
（仅本机验证使用，**不写入仓库**）：

```yaml
# /tmp/compose-m2.yml（临时覆盖文件，不入库）
services:
  backend:
    volumes:
      - C:/Users/PC/.m2:/root/.m2
```

正式 CI 不受影响 —— 它有自己的依赖缓存且网络条件不同。

## 五、本机端口差异（沿用既有做法）

本机已装 `MySQL80` 服务占用 3306，故验证时用覆盖文件把容器映射到 3307：

```yaml
# deploy/docker-compose.local.yml（已在 .gitignore，不入库）
services:
  mysql:
    ports: !override
      - "3307:3306"
```

> 修订（2026-11-25，审计 F-09）：`deploy/docker-compose.local.yml` 是当时的临时覆盖文件，现已不在仓库；
> 本地开发改用 `deploy/docker-compose.dev.yml`（默认端口 3306、含 demo 数据），见 README §2.2。

对应后端 `DB_PORT=3307`。仓库默认仍是 3306，`README.md` §2.2 已写明两种做法。

> 踩坑提示：`ports` 在 compose 多文件合并时是**追加**而非替换，必须写 `!override`，
> 否则会 3306 与 3307 同时暴露，3306 仍然冲突。

## 六、结论

`docker compose -f deploy/docker-compose.yml up -d --build` 一键部署路径**已走通**：

- 四容器全部启动，backend 与 MySQL/Redis 均 healthy；
- Nginx 80 端口可访问前端，并成功反代 `/api/**` 到后端；
- 登录、问题列表、课程、排行榜接口在容器网络内正常工作；
- 页面渲染正常（首页 + 搜索页截图见本目录）。

`sprint-4.md` T4-01 最后一条可以勾选。同时 `deploy/backend.Dockerfile` 的 COPY 通配符问题已修复。

## 七、复现步骤

```bash
# 0) 准备环境变量（当前生产 compose 必填）
cp deploy/.env.example deploy/.env   # 填入 DB_PASSWORD / MYSQL_ROOT_PASSWORD / CO_FIELD_KEY / CO_FIELD_MAC_KEY

# 1) 起中间件（本地开发可直接用 docker-compose.dev.yml，无需手写端口覆盖）
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d mysql redis

# 2) 构建并启动全栈
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build

# 3) 验证
curl http://localhost/actuator/health    # {"status":"UP",...}（经 Nginx 白名单）
curl -o /dev/null -w "%{http_code}\n" http://localhost   # 200
curl "http://localhost/api/v1/questions?page=1&size=2"   # 问题列表 JSON

# 4) 浏览器打开 http://localhost/
```

> 修订（2026-11-25，审计 F-09）：生产 compose 不再把后端 8080 暴露到 host，健康检查改走
> `http://localhost/actuator/health`；本地开发请用 `deploy/docker-compose.dev.yml`。
