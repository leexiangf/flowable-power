# flowable-power

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

基于 **JDK 17 + Spring Boot 3 + Spring Cloud** 的多模块脚手架，内置 **JWT/RBAC 权限体系** 与 **Flowable 7 流程引擎**，并配套 **Vue 3 管理端**。

适合作为企业内部后台 / 审批类系统的起步工程：开箱可跑通登录、用户角色菜单、请假审批全链路。

---

## 功能特性

- **统一网关鉴权**：Spring Cloud Gateway 校验 JWT，透传用户上下文
- **RBAC 权限**：用户 / 角色 / 菜单 + `@PreAuthorize("@authz.permit('...')")` 按钮级鉴权
- **多端登录**：WEB / MOBILE 平台隔离，可配置顶号（业务码 `20004`）
- **Flowable Process**：分类、模型草稿、定义部署、实例、待办、流转图、撤销
- **业务闭环示例**：请假申请（候选人组审批）+ 费用报销演示（变量办理人 / 会签）
- **中间件封装**：Redis（会话 / 锁 / 缓存 / 限流）、RabbitMQ（Outbox 可靠投递）、OpenFeign
- **管理前端**：动态菜单路由、`v-perm` 权限指令、工作流全套页面

---

## 技术栈

| 类别 | 选型 | 版本（约） |
|------|------|------------|
| 语言 / 构建 | JDK 17、Maven 多模块 | — |
| 框架 | Spring Boot / Spring Cloud / SCA | 3.3.x / 2023.0.x / 2023.0.1.2 |
| 网关 | Spring Cloud Gateway | — |
| 注册配置 | Nacos（local 默认关闭发现） | 2.3.2（Compose） |
| 持久化 | MySQL 8 + MyBatis-Plus | 3.5.9 |
| 缓存 / MQ | Redis 7、RabbitMQ 3 | — |
| 安全 | Spring Security + JWT (jjwt) | 0.12.x |
| 流程引擎 | Flowable Process | **7.1.0** |
| API 文档 | SpringDoc OpenAPI | 2.6.0 |
| 前端 | Vue 3 + Vite + Pinia + Element Plus + TS | — |
| 本地中间件 | Docker Compose | — |

---

## 模块一览

```text
flowable-power/
├── docker/                 # Compose：MySQL / Redis / RabbitMQ / Nacos + 初始化 SQL
├── power-common/           # 统一响应 R<T>、异常、常量、分页模型
├── power-middleware/       # Security/JWT、MP、Redis、MQ/Outbox、Feign、OpenAPI
├── power-gateway/          # 8080 网关鉴权与路由
├── power-auth/             # 8081 登录、RBAC、工作流身份查询
├── power-system/           # 8082 业务示例（Ping / Feign / Outbox Demo）
├── power-workflow/         # 8083 Flowable + 请假 / 模型 / 任务
└── power-web/              # 5173 Vue3 管理端（开发时代理网关）
```

| 模块 | 端口 | 说明 |
|------|------|------|
| power-gateway | **8080** | 统一入口：`/auth/**`、`/system/**`、`/workflow/**` |
| power-auth | 8081 | 认证授权、用户角色菜单 |
| power-system | 8082 | 示例业务 |
| power-workflow | 8083 | 流程引擎与工作流 API |
| power-web | 5173 | 管理端（Vite） |

**依赖关系（简化）：**

```text
gateway ──► auth / system / workflow
auth / system / workflow ──► common + middleware
workflow ──Feign──► auth（角色编码 / 昵称）
```

---

## 环境要求

- **JDK 17+**（Boot 3 必须；本机若仍是 JDK 11，请先设置 `JAVA_HOME`）
- Maven 3.8+
- Docker / Docker Compose（跑中间件）
- Node.js 18+（仅前端）

---

## 快速开始

### 1. 启动中间件

```bash
cd docker
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d
```

等待 MySQL 健康后，会自动执行 [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql)（业务表 + 种子用户 / 菜单 / 角色）。

| 服务 | 端口 |
|------|------|
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672（管理台 15672） |
| Nacos | 8848 |

### 2. 编译后端

```bash
mvn -DskipTests clean install
```

### 3. 启动后端（各开一个终端）

```bash
mvn -pl power-auth spring-boot:run
mvn -pl power-system spring-boot:run
mvn -pl power-workflow spring-boot:run
mvn -pl power-gateway spring-boot:run
```

默认 `SPRING_PROFILES_ACTIVE=local`：

- 不连 Nacos 做服务发现，网关直连 `8081/8082/8083`
- 中间件地址指向 Compose 映射的 `127.0.0.1`（见各模块 `application-local.yml`）

### 4. 启动前端

```bash
cd power-web
npm install
npm run dev
```

浏览器打开 http://127.0.0.1:5173 。更多说明见 [`power-web/README.md`](power-web/README.md)。

### 5. 冒烟验证

```bash
# 登录（需带 platform）
curl -X POST http://127.0.0.1:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"platform\":\"WEB\"}"

# 调试跳过 Token（仅 local，Header 值见 application-local.yml）
curl http://127.0.0.1:8080/system/ping -H "X-Debug-Auth: local-only-change-me"
```

Swagger UI（各服务独立，Authorize 填 accessToken）：

- Auth：http://127.0.0.1:8081/swagger-ui.html
- System：http://127.0.0.1:8082/swagger-ui.html
- Workflow：http://127.0.0.1:8083/swagger-ui.html

---

## 种子账号

密码均为 `admin123`。

| 用户 | 角色 | 典型用途 |
|------|------|----------|
| `admin` | ADMIN | 系统管理、流程部署 / 监控 |
| `zhangsan` | STAFF | 发起请假、撤销自己的实例 |
| `lisi` | APPROVER | 待办认领、通过 / 驳回 / 转办 |

建议联调路径：

```text
zhangsan 提交请假 → lisi 任务中心办理 → 双方在「流程实例」查看时间线 / 流程图
```

---

## 架构示意

```text
Browser (power-web :5173)
        │  /auth /system /workflow  (Vite 代理)
        ▼
power-gateway (:8080)
        │  JWT 校验 / 调试鉴权 / 路由
        ├──────────────┬──────────────┐
        ▼              ▼              ▼
   power-auth     power-system   power-workflow
    (:8081)         (:8082)         (:8083)
        │                               │
        │◄──────── Feign 身份查询 ───────┤
        │                               │
        └──────── MySQL / Redis / RabbitMQ ──────┘
```

---

## 安全与约定

### 统一响应

```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "..." }
```

- `code = 0` 表示成功；业务失败返回明确业务码 + `message`
- 顶号：`code = 20004`，前端会跳转登录页

### 权限码

格式：`模块:资源:动作`，例如 `system:user:list`、`workflow:task:handle`。

- 后端：`@PreAuthorize("@authz.permit('workflow:task:handle')")`
- 前端：`v-perm="'workflow:task:handle'"`（与库表 `sys_menu.perms` 保持一致）

### Token

| 项 | 说明 |
|----|------|
| Access / Refresh | JWT；Refresh 与会话状态存 Redis |
| 平台 | 登录传 `WEB` 或 `MOBILE` |
| 多端 | `power.security.multi-platform-login-enabled`；关闭时同端再次登录会顶号 |
| 登出 | Access 立即进入 Redis 黑名单 |

### 调试鉴权（仅 local / dev）

Header：`X-Debug-Auth`，值与 `power.security.debug-auth-token` 一致时可跳过 JWT，注入配置中的调试用户（可带 `*` 通配权限）。**生产环境强制关闭。**

### Flowable 身份映射

| Flowable | 本系统 |
|----------|--------|
| `assignee` / `startUserId` | `sys_user.id` 的**字符串**，如 `"2"` |
| `candidateGroups` | `sys_role.role_code`，如 `APPROVER` |
| 展示名 | 优先 `nickname`，否则 `username` |

不同步 `ACT_ID_*`，由 auth 提供查询接口（登录即可）：

- `GET /auth/workflow/users/{userId}`
- `GET /auth/workflow/users/{userId}/roles`
- `GET /auth/workflow/roles/{roleCode}/users`

雪花 ID 经 Jackson 统一序列化为 **字符串**，避免前端精度丢失。

---

## 工作流能力

### 内置流程

| Key | 资源 | 说明 |
|-----|------|------|
| `leave` | `processes/leave.bpmn20.xml` | 候选人组 `APPROVER` 审批；结束监听回写 `wf_leave.status` |
| `expense` | `processes/expense.bpmn20.xml` | `${managerUserId}` 变量办理人 → 并行会签 |

首次启动若库中尚无对应定义，会 bootstrap 自动部署。**已部署环境不会自动覆盖 XML**；改 BPMN 后需重新部署。

### 请假状态

| status | 含义 |
|--------|------|
| 1 | 审批中 |
| 2 | 通过 |
| 3 | 驳回 |
| 4 | 撤销 |

### 主要 API（均经网关 `/workflow/**`，需登录 + 权限码）

<details>
<summary>点击展开：分类 / 模型 / 定义 / 实例 / 任务 / 请假</summary>

**分类** `workflow:category:*`

| 方法 | 路径 |
|------|------|
| CRUD / 分页 / 启用列表 | `/workflow/categories` … |

**模型** `workflow:model:*`（部署用 `workflow:definition:deploy`）

| 方法 | 路径 |
|------|------|
| 保存草稿（按 modelKey upsert） | `POST /workflow/models` |
| 分页 / 详情 / 删除 | `GET/DELETE /workflow/models…` |
| 部署到引擎 | `POST /workflow/models/{id}/deploy` |

**定义**

| 方法 | 路径 | 权限 |
|------|------|------|
| 上传 BPMN | `POST /workflow/definitions/deploy` | deploy |
| 最新版本列表 | `GET /workflow/definitions` | list |
| 可发起列表 | `GET /workflow/definitions/startable` | instance:start |
| 挂起 / 激活 | `POST …/suspend` · `…/activate` | suspend |
| BPMN XML | `GET …/xml` | list |

**实例**

| 方法 | 路径 | 权限 |
|------|------|------|
| 启动 | `POST /workflow/instances/start` | start |
| 我发起的 | `GET /workflow/instances/mine` | list |
| 监控列表 | `GET /workflow/instances` | monitor |
| 详情 / 时间线 / 高亮 / PNG | `GET …/{id}` 等 | list **或** monitor |
| 撤销（仅发起人） | `POST …/{id}/cancel` | list |

**任务**

| 方法 | 路径 | 权限 |
|------|------|------|
| 待办 / 已办 | `GET /workflow/tasks/todo` · `/done` | list |
| 认领 / 取消认领 | `POST …/claim` · `/unclaim` | handle |
| 办理 / 驳回 / 转办 | `POST …/complete` · `/reject` · `/transfer` | handle |

**请假**

| 方法 | 路径 | 权限 |
|------|------|------|
| 提交申请 | `POST /workflow/leave` | apply |
| 详情 | `GET /workflow/leave/{id}` | apply |

**演示**

| 方法 | 路径 |
|------|------|
| 发起报销演示 | `POST /workflow/demo/expense`（需 `workflow:instance:start`） |

</details>

### 关键行为摘要

| 能力 | 规则 |
|------|------|
| 撤销 | 仅运行中且 `startUserId` = 当前用户；请假 status → 4 |
| 驳回 | 有上一用户任务则退回；否则以 `approved=false` 完成 |
| 办理 | 默认写入 `approved=true`（Boolean） |
| 转办 | `setAssignee` 为目标 userId 字符串 |
| Outbox | 流程完成 / 撤销 → `sys_outbox` → Exchange `power.workflow` |

---

## 认证相关 API（节选）

经网关前缀 `/auth/**`：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | body: `{ username, password, platform }` |
| POST | `/auth/login/web` · `/mobile` | 便捷入口 |
| POST | `/auth/refresh` | 刷新 Token |
| POST | `/auth/logout` | 登出 |
| GET | `/auth/me` | 当前用户（含 roles / authorities） |
| PUT | `/auth/me/profile` · `/password` | 个人资料 / 改密 |
| GET | `/auth/menus/tree` | 当前用户侧栏菜单 |
| CRUD | `/auth/users` · `/roles` · `/menus` | 系统管理（对应 `system:*` 权限） |

白名单（无需 Token）：登录、刷新、登出。

---

## 配置要点

- Profile：`application.yml` 公共项；差异在 `application-local.yml` / `application-prod.yml`
- 环境变量与 Compose 对齐：见 [`docker/.env.example`](docker/.env.example)
- JWT：`JWT_SECRET` 三端一致；local 有开发默认值，**prod 必填**
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

### 生产启动

```bash
# Windows CMD 示例
set SPRING_PROFILES_ACTIVE=prod
# 并注入 MYSQL_* / REDIS_* / RABBIT_* / NACOS_ADDR / JWT_SECRET 等
```

生产环境请勿将业务端口直接暴露公网，仅暴露网关；关闭调试鉴权。

---

## 前端（power-web）

| 项 | 说明 |
|----|------|
| 开发代理 | `/auth`、`/system`、`/workflow` → `http://127.0.0.1:8080` |
| 动态路由 | 登录后拉菜单树，`component` 如 `system/user/index` → `views/system/user/index.vue` |
| 按钮权限 | `v-perm` 与后端 `@authz.permit` 同码 |
| 已实现页面 | 登录、个人中心、用户/角色/菜单、流程分类/定义/模型/实例/任务/请假 |

新增业务页：在 `views/` 建页面 → 菜单管理配置 `component` → 角色授权 → 重新登录。

---

## 中间件能力（middleware 模块）

| 能力 | 说明 |
|------|------|
| Redis 会话 | Token 黑名单、Refresh、踢人、用户版本号 |
| 分布式锁 | `RedisDistributedLock`（SET NX + Lua 释放） |
| 缓存 / 限流 | cache-aside、固定窗口限流 |
| RabbitMQ | 拓扑声明、手动 ACK 基类、消费幂等、重试 / DLX |
| Outbox | 本地消息表 + 调度投递（流程生命周期事件等） |
| MyBatis-Plus | 分页、逻辑删除、审计字段填充 |
| OpenAPI | 各服务 SpringDoc 自动配置 |

---

## 明确不做 / 后置

- 多租户、独立 OAuth2 Authorization Server、Seata 强一致分布式事务
- Flowable CMMN / DMN、同步 `ACT_ID_*`
- 分库分表、完整数据权限拦截器（可后续扩展）

---

## 常见问题

**Q: 登录后工作流页 403？**  
检查角色是否分配了对应 `workflow:*` 菜单权限；种子数据里 STAFF / APPROVER 权限不同。

**Q: 改了 BPMN 不生效？**  
引擎不会自动覆盖已部署定义，请在「流程定义」重新上传，或从「流程模型」部署新版本。

**Q: 待办里看不到任务？**  
请假节点候选组是 `APPROVER`，请用 `lisi`（或具备该角色的用户）登录；也可先「认领」再办理。

**Q: 前端请求跨域？**  
开发环境用 Vite 代理，无需配 CORS；务必先启动 gateway。

**Q: MySQL 初始化脚本没执行？**  
Compose 仅在数据卷首次创建时跑 `/docker-entrypoint-initdb.d`；若需重跑，删除卷后重新 `docker compose up -d`。

---

## License

本项目基于 [MIT License](LICENSE) 开源。

Copyright (c) 2026 leexiangf
