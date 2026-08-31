# flowable-power

JDK 17 + Spring Boot 3 + Spring Cloud 脚手架。方案见 [`docs/`](docs/README.md)。

## 模块

| 模块 | 端口 | 说明 |
|------|------|------|
| power-common | - | 统一响应、异常、常量 |
| power-middleware | - | Security/JWT、MP、Redis、MQ、Outbox、Feign |
| power-gateway | 8080 | 网关鉴权 / 调试 Header |
| power-auth | 8081 | 登录、RBAC |
| power-system | 8082 | 业务示例（`@PreAuthorize` / Feign / Outbox） |

## 快速开始

> 需要 **JDK 17+**。本机若默认仍是 JDK 11，请先设置：
> `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`

### 1. 启动中间件

```bash
cd docker
copy .env.example .env   # 首次：与 Compose / 本地应用默认值对齐
docker compose up -d
```

等待 MySQL 健康后（会自动执行 `docker/mysql/init/01-schema.sql`）。

### 2. 编译

```bash
mvn -DskipTests clean install
```

### 3. 启动服务（各开一个终端）

```bash
mvn -pl power-auth spring-boot:run
mvn -pl power-system spring-boot:run
mvn -pl power-gateway spring-boot:run
```

默认 `SPRING_PROFILES_ACTIVE=local`（也可不设，application.yml 默认 local）：

- 不连 Nacos，网关直连 `8081/8082`
- 中间件地址默认指向 Compose 映射的 `127.0.0.1`（见 `application-local.yml` / `docker/.env.example`）

生产：

```bash
set SPRING_PROFILES_ACTIVE=prod
# 并注入 MYSQL_* / REDIS_* / RABBIT_* / NACOS_ADDR / JWT_SECRET 等（见各模块 application-prod.yml）
```

### 4. 验证

登录（默认账号 `admin` / `admin123`）：

```bash
curl -X POST http://127.0.0.1:8080/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

调试跳过 Token：

```bash
curl http://127.0.0.1:8080/system/ping -H "X-Debug-Auth: local-only-change-me"
```

Swagger：`http://127.0.0.1:8081/swagger-ui.html`、`http://127.0.0.1:8082/swagger-ui.html`

## 配置要点

- Profile：`application.yml` 仅公共项；差异在 `application-local.yml` / `application-prod.yml`
- 环境变量与 Compose 对齐：见 [`docker/.env.example`](docker/.env.example)
- JWT 密钥：`JWT_SECRET`（三端一致）；local 有开发默认值，prod 必填
- 调试鉴权：仅 local 开启；Header `X-Debug-Auth`
- 多数据源：`power.datasource.multi-enabled=false`（默认关）
- 异常源位置：local `include-source=true`，prod 关闭

### 应用 ↔ Compose 变量对照（local）

| 应用变量 | Compose / .env | 本地默认 |
|----------|----------------|----------|
| `MYSQL_HOST` / `MYSQL_PORT` | 映射 `3306` | `127.0.0.1` / `3306` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | root / `MYSQL_ROOT_PASSWORD` | `root` / `root` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 映射 `6379`，默认无密 | `127.0.0.1` / `6379` / 空 |
| `RABBIT_HOST` / `RABBIT_PORT` | 映射 `5672` | `127.0.0.1` / `5672` |
| `RABBIT_USER` / `RABBIT_PASSWORD` | `RABBITMQ_DEFAULT_USER/PASS` | `guest` / `guest` |
| `NACOS_ADDR` | `8848` | `127.0.0.1:8848`（local 默认不启用发现） |