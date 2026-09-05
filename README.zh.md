# flowable-power

[English](./README.md) | [中文](./README.zh.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Flowable](https://img.shields.io/badge/Flowable-7.1-blue.svg)](https://www.flowable.com/open-source)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

面向生产试用的 **工作流 / BPM 脚手架**：**Flowable Process 7** + **Spring Boot 3** + **Spring Cloud Gateway**，内置 JWT/RBAC，并配套 **Vue 3** 管理端。

适合需要「能直接跑通请假 / 报销 / 会签」的团队，而不是空白的 Boot 工程。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [模块一览](#模块一览)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [种子账号](#种子账号)
- [架构示意](#架构示意)
- [安全与约定](#安全与约定)
- [工作流](#工作流)
- [配置要点](#配置要点)
- [前端](#前端power-web)
- [数据库与升级](#数据库与升级)
- [常见问题](#常见问题)
- [贡献](#贡献)
- [License](#license)

---

## 功能特性

- **统一网关鉴权**：Gateway 校验 JWT，透传用户上下文
- **RBAC**：用户 / 角色 / 菜单 + `@PreAuthorize` / 前端 `v-perm`
- **多端登录**：`WEB` / `MOBILE` 隔离，可配置顶号（`code=20004`）
- **Flowable Process**：分类、模型、部署、实例、任务、流程图、撤销 / 终止 / 催办
- **业务闭环**：请假（候选人组）、费用报销（变量办理人 + 并行会签）、并行或签、串行会签
- **任务能力**：认领 / 取消认领、办理 / 驳回、转办 / 重新指派、委派、加签 / 减签、抄送
- **中间件封装**：Redis（会话 / 锁 / 缓存 / 限流）、RabbitMQ Outbox、OpenFeign
- **管理前端**：动态菜单、工作流全套页面、BPMN 查看

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
| 前端 | Vue 3 + Vite + Pinia + Element Plus + TypeScript | — |
| 本地中间件 | Docker Compose | — |

---

## 模块一览

```text
flowable-power/
├── docker/                 # Compose：MySQL / Redis / RabbitMQ / Nacos + init/upgrade SQL
├── power-common/           # 统一响应 R<T>、异常、常量、分页
├── power-middleware/       # Security/JWT、MP、Redis、MQ/Outbox、Feign、OpenAPI
├── power-gateway/          # :8080 网关鉴权与路由
├── power-auth/             # :8081 登录、RBAC、工作流身份查询
├── power-system/           # :8082 系统探针（Ping / Feign / Outbox）
├── power-workflow/         # :8083 Flowable + 请假 / 报销 / 会签 / 任务
└── power-web/              # :5173 Vue 3 管理端（开发时代理网关）
```

| 模块 | 端口 | 说明 |
|------|------|------|
| power-gateway | **8080** | 统一入口：`/auth/**`、`/system/**`、`/workflow/**` |
| power-auth | 8081 | 认证授权、用户角色菜单 |
| power-system | 8082 | 探针 / 示例 |
| power-workflow | 8083 | 流程引擎与工作流 API |
| power-web | 5173 | 管理端（Vite） |

```text
gateway ──► auth / system / workflow
auth / system / workflow ──► common + middleware
workflow ──Feign──► auth（角色编码 / 昵称）
```

---

## 环境要求

- **JDK 17+**（Spring Boot 3）
- Maven 3.8+
- Docker / Docker Compose
- Node.js 18+（前端）

---

## 快速开始

### 1. 启动中间件

```bash
cd docker
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d
```

MySQL 健康后会自动执行 [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql)（建库 + 表 + 种子）。该目录**只保留这一个全量初始化文件**。

已有库升级脚本见 [`docker/mysql/upgrade/`](docker/mysql/upgrade/README.md)；执行后重新登录以刷新权限。

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

默认 `local` Profile：不连 Nacos 发现，网关直连 `8081/8082/8083`。

### 4. 启动前端

```bash
cd power-web
npm install
npm run dev
```

浏览器打开 http://127.0.0.1:5173 。更多说明见 [`power-web/README.md`](power-web/README.md)。

### 5. 冒烟验证

```bash
curl -X POST http://127.0.0.1:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"platform\":\"WEB\"}"

curl http://127.0.0.1:8080/system/ping -H "X-Debug-Auth: local-only-change-me"
```

Swagger UI（Authorize 填 accessToken）：

| 服务 | URL |
|------|-----|
| Auth | http://127.0.0.1:8081/swagger-ui.html |
| System | http://127.0.0.1:8082/swagger-ui.html |
| Workflow | http://127.0.0.1:8083/swagger-ui.html |

---

## 种子账号

密码均为 **`admin123`**。

| 用户名 | 昵称（示例） | 角色 | 典型用途 |
|--------|--------------|------|----------|
| `admin` | 系统管理员 | ADMIN | 系统管理、部署、监控 |
| `approver1` … `approver5` | 王芳 / 李明 / … | APPROVER | 审批、会签、认领请假 |
| `staff1` … `staff5` | 张伟 / 刘娜 / … | STAFF | 仅发起请假 / 业务 |

**建议联调**

```text
staff1 → 请假管理 → 提交
approver1 → 任务中心 → 认领 / 办理
双方 → 流程实例 → 时间线 / 流程图
```

**报销 / 会签**

```text
staff1 → 流程实例 →「发起业务」
部门经理与会签人只能选 APPROVER
approver* → 任务中心办理
```

---

## 架构示意

```text
Browser (power-web :5173)
        │  /auth /system /workflow  (Vite 代理)
        ▼
power-gateway (:8080)
        │  JWT / 调试鉴权 / 路由
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

- `code = 0` 成功；失败返回业务码 + `message`
- 顶号：`code = 20004`（前端跳转登录）
- 鉴权失败 / Token 失效：前端清 Token 并跳转登录

### 权限码

格式：`模块:资源:动作`，如 `workflow:task:handle`。

- 后端：`@PreAuthorize("@authz.permit('…')")`
- 前端：`v-perm="'…'"`（与 `sys_menu.perms` 一致）

### Token

| 项 | 说明 |
|----|------|
| Access / Refresh | JWT；会话状态在 Redis |
| 平台 | 登录传 `WEB` 或 `MOBILE` |
| 多端 | `power.security.multi-platform-login-enabled` |
| 登出 | Access JTI 立即进黑名单 |

### 调试鉴权（仅 local / dev）

Header `X-Debug-Auth` 匹配配置时可跳过 JWT。**生产必须关闭。**

### Flowable 身份映射

| Flowable | 本系统 |
|----------|--------|
| `assignee` / `startUserId` | `sys_user.id` 的**字符串** |
| `candidateGroups` | `sys_role.role_code`（如 `APPROVER`） |
| 展示名 | 优先 `nickname` |

不同步 `ACT_ID_*`。雪花 ID 经 Jackson 序列化为**字符串**。

---

## 工作流

### 内置流程

| Key | 说明 |
|-----|------|
| `leave` | 候选人组 `APPROVER`；结束回写 `wf_leave.status` |
| `expense` | 变量办理人 → 并行会签 |
| `countersign-or` | 并行或签（一人通过**或**一票否决即结束多实例） |
| `countersign-seq` | 串行会签（按顺序；驳回一票否决） |

首次启动会 bootstrap 缺失定义；部分内置流程（如或签）在检测到旧完成条件时会**升版一次**。自行改 BPMN 后请在「流程定义 / 模型」重新部署。

### 正确发起方式

| 流程 | 正确入口 | 不要 |
|------|----------|------|
| 请假 | 「请假管理」或 `POST /workflow/leave` | 用通用 `instances/start` 伪造 leave |
| 报销 | 「发起业务」或 `POST /workflow/expense` | 缺经理 / 会签人 |
| 或签 / 串行会签 | 「发起业务」或 `POST /workflow/countersign/or\|seq` | 少于 2 名审批人 |

### 办理人规则

- 部门经理 / 会签 / 转办 / 委派 / 加签 / 重新指派：**仅审批人**（`APPROVER` 或 `ADMIN`）
- **STAFF** 可发起业务，可被抄送，但 UI 不能选为办理人
- 实例列表：**标题** = 发起时填写的 `title`；**类型** = 流程类型名

### 请假状态

| status | 含义 |
|--------|------|
| 1 | 审批中 |
| 2 | 通过 |
| 3 | 驳回 |
| 4 | 撤销 |

### 主要 API

经网关 `/workflow/**`，需登录 + 权限。完整表见英文 [README](./README.md#main-apis-gateway-workflow-login--permission)（中英结构一致），常用包括：

- 实例：`/workflow/instances/**`（含 `…/tasks` 活动任务、催办、终止）
- 任务：`/workflow/tasks/**`（办理 / 驳回 / 转办 / 指派 / 加签 / 减签 / 委派 / 抄送）
- 请假：`POST /workflow/leave`、`GET /workflow/leave/mine`
- 报销 / 会签：`POST /workflow/expense`、`/workflow/countersign/or|seq`

### 关键行为

| 能力 | 规则 |
|------|------|
| 撤销 | 仅发起人 + 运行中；请假 status → 4 |
| 驳回 | PREVIOUS / TO_NODE / TO_STARTER / TERMINATE；不可驳回到多实例节点 |
| 取消认领 | 仅存在候选人链路时允许（避免指定办理人任务变孤儿） |
| 减签 | 仅会签；只剩 1 个活动实例时禁止 |
| 或签否决 | `approved=false` 后清理其余多实例子任务 |
| Outbox | 完成 / 撤销 → `sys_outbox` → Exchange `power.workflow` |

---

## 配置要点

- Profile：`application.yml` + `application-local.yml` / `application-prod.yml`
- 环境变量：[`docker/.env.example`](docker/.env.example)
- `JWT_SECRET` 各服务一致；**prod 必填**
- 生产只暴露网关，关闭调试鉴权

---

## 前端（power-web）

| 项 | 说明 |
|----|------|
| 开发代理 | `/auth`、`/system`、`/workflow` → `8080` |
| 动态路由 | 菜单 `component` → `views/...` |
| 按钮权限 | `v-perm` 与后端同码 |

详见 [`power-web/README.md`](power-web/README.md)。

---

## 数据库与升级

| 路径 | 用途 |
|------|------|
| [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql) | Compose 首次启动全量 |
| [`docker/mysql/upgrade/`](docker/mysql/upgrade/README.md) | **已有库升级脚本** |

执行升级 SQL 后请**重新登录**刷新权限。

开发环境若要清空运行中的流程实例（慎用）：

```bash
mysql -uroot -proot < docker/mysql/upgrade/07_reset_runtime_and_users.sql
```

更干净的做法是删除 Compose MySQL 数据卷后重建。
---

## 常见问题

**登录后工作流页 403？**  
检查角色菜单权限；升级 SQL 后需重新登录。

**请假提示「请使用请假管理」？**  
必须走 `POST /workflow/leave` 或请假页，不能用通用发起伪造 leave。

**待办看不到请假？**  
候选组是 `APPROVER`，用 `approver1` 等登录后认领。

**取消认领后任务消失？**  
指定办理人 / 会签任务禁止取消认领，请用转办或实例详情「重新指派」。

**改了 BPMN 不生效？**  
在流程定义重新上传，或从模型部署。或签可能在启动时按内容自动升版一次。

**前端跨域？**  
开发靠 Vite 代理，先启动 gateway。

**初始化 SQL 没跑？**  
Compose 只在数据卷首次创建时执行；删卷后重建即可。

---

## 贡献

1. Fork 并从 `main` 拉特性分支
2. 变更尽量聚焦，遵守 `power-*` 模块边界
3. **禁止**用 `demo` 命名包 / API / 菜单（见 `.cursor/rules/no-demo-naming.mdc`）
4. 改表结构：同步更新 `docker/mysql/init/01-schema.sql`，并在 `docker/mysql/upgrade/` 增加升级脚本
5. PR 请写清改动说明与测试步骤（账号、接口、流程 Key）

---

## 明确不做 / 后置

- 多租户、独立 OAuth2 AS、Seata 强一致事务
- Flowable CMMN / DMN、同步 `ACT_ID_*`
- 分库分表、完整数据权限拦截器

---

## License

本项目基于 [MIT License](LICENSE) 开源。

Copyright (c) 2026 leexiangf
