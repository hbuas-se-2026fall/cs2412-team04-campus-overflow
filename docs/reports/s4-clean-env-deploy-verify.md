CampusOverflow 干净环境部署验证（T4-01 最后一项）
========================================================
执行人：房国栋（2177023844）
日期：2026-11-18
机器：Windows 11 build 26200 / 27.8 GB RAM

## 1. 环境
Docker Desktop : 29.9.3 (build 5457199)，WSL2 后端
JDK            : 21.0.10 (JBR)
Maven          : 3.9.11
镜像源         : docker.1ms.run（USTC/网易/腾讯云均已停服，实测 http=000）

## 2. 镜像拉取
$ docker pull docker.1ms.run/library/redis:7-alpine   -> Downloaded
$ docker pull docker.1ms.run/library/mysql:8.0.39     -> Downloaded
$ docker tag docker.1ms.run/library/redis:7-alpine redis:7-alpine
$ docker tag docker.1ms.run/library/mysql:8.0.39 mysql:8.0.39

## 3. 容器启动
本机 3306 被已装的 MySQL80 服务占用，当时用 override 文件把容器端口映射到 3307
（deploy/docker-compose.local.yml，未提交）。

$ docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.local.yml up -d mysql redis

> 修订（2026-11-25，审计 F-09）：当时的 `deploy/docker-compose.local.yml` 临时覆盖文件现已不在仓库；
> 本地开发改用 `deploy/docker-compose.dev.yml`（默认 3306、含 demo 数据，容器名 `co-mysql-dev` / `co-redis-dev`）。
> 当前生产 compose 已强制要求 `--env-file deploy/.env`（DB_PASSWORD 等变量 `:?` 必填），不带 env 会失败。

NAME       IMAGE            COMMAND                   SERVICE   CREATED         STATUS                   PORTS
co-mysql   mysql:8.0.39     "docker-entrypoint.s…"   mysql     5 minutes ago   Up 5 minutes (healthy)   0.0.0.0:3307->3306/tcp, [::]:3307->3306/tcp
co-redis   redis:7-alpine   "docker-entrypoint.s…"   redis     7 minutes ago   Up 7 minutes (healthy)   0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp

## 4. 容器内连通性
$ docker exec co-mysql mysql -uroot -p*** -e 'SELECT VERSION(); SHOW DATABASES;'
  version = 8.0.39
  databases = campus_overflow, information_schema, mysql, performance_schema, sys
$ docker exec co-redis redis-cli ping      -> PONG
$ docker exec co-redis redis-cli INFO server | grep redis_version -> 7.4.12

## 5. 后端起服务
$ mvn -DskipTests -Djacoco.skip=true -pl co-bootstrap -am install   -> BUILD SUCCESS (79 MB jar)
$ DB_PORT=3307 SPRING_PROFILES_ACTIVE=demo java -jar co-bootstrap/target/campus-overflow.jar
  -> Tomcat started on port 8080
  -> Started CampusOverflowApplication in 12.306 seconds

## 6. 接口与数据验证
$ curl http://localhost:8080/actuator/health
  {"status":"UP","groups":["liveness","readiness"]}

$ curl 'http://localhost:8080/api/v1/questions?page=1&size=3'
  {"items":[{"id":3,"title":"MySQL 的 ngram 全文索引是如何支持中文检索的？",...}]}
  ^ demo profile 写入的演示数据已可读，说明 Flyway 迁移与 MySQL 连接正常

$ curl http://localhost:8080/api/v1/courses
  [{"id":1,"code":"CS2001","name":"数据结构","term":"2026-秋","teacherId":2,"teacherName":"王老师","myRole":null}]

## 结论
按 README 的 compose 方式可拉起 MySQL + Redis，后端启动并完成 Flyway 迁移，
health = UP，演示数据可读。T4-01 的「干净环境部署验证」满足。

## 备注
1. 镜像源 USTC / 网易 / 腾讯云在 2026-11-18 实测全部不可用，建议团队改用 docker.1ms.run。
2. 本机装有 MySQL80 占用 3306，验证时用 3307 映射；如需完全复现 README，先 net stop MySQL80。
