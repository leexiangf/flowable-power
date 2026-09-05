# flowable-power

[English](./README.md) | [中文](./README.zh.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Flowable](https://img.shields.io/badge/Flowable-7.1-blue.svg)](https://www.flowable.com/open-source)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

**Production-oriented scaffold** for workflow / BPM apps: **Flowable Process 7** + **Spring Boot 3** + **Spring Cloud Gateway**, with JWT/RBAC and a **Vue 3** admin UI.

Use it when you need a runnable leave / expense / countersign stack—not an empty Boot skeleton.

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Modules](#modules)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Seed accounts](#seed-accounts)
- [Architecture](#architecture)
- [Security](#security)
- [Workflow](#workflow)
- [Configuration](#configuration)
- [Frontend](#frontend-power-web)
- [Database & upgrades](#database--upgrades)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **API Gateway auth** — Spring Cloud Gateway validates JWT and forwards identity headers
- **RBAC** — users / roles / menus + `@PreAuthorize("@authz.permit('...')")` / frontend `v-perm`
- **Multi-platform login** — `WEB` / `MOBILE` isolation; optional kick-out (`code=20004`)
- **Flowable Process** — categories, model drafts, deploy, instances, tasks, diagrams, cancel / terminate / urge
- **Business flows** — leave (candidate group), expense (variable assignee + parallel countersign), parallel or-sign, sequential countersign
- **Task operations** — claim / unclaim, complete / reject, transfer / assign, delegate, add-sign / reduce-sign, CC
- **Middleware kit** — Redis (session / lock / cache / rate limit), RabbitMQ Outbox, OpenFeign
- **Admin UI** — dynamic menus, workflow pages, BPMN viewer / modeler hooks

---

## Tech stack

| Area | Choice | Version (approx.) |
|------|--------|-------------------|
| Language / build | JDK 17, Maven multi-module | — |
| Framework | Spring Boot / Spring Cloud / SCA | 3.3.x / 2023.0.x / 2023.0.1.2 |
| Gateway | Spring Cloud Gateway | — |
| Registry / config | Nacos (discovery off in `local`) | 2.3.2 (Compose) |
| Persistence | MySQL 8 + MyBatis-Plus | 3.5.9 |
| Cache / MQ | Redis 7, RabbitMQ 3 | — |
| Security | Spring Security + JWT (jjwt) | 0.12.x |
| Workflow engine | Flowable Process | **7.1.0** |
| API docs | SpringDoc OpenAPI | 2.6.0 |
| Frontend | Vue 3 + Vite + Pinia + Element Plus + TypeScript | — |
| Local infra | Docker Compose | — |

---

## Modules

```text
flowable-power/
├── docker/                 # Compose: MySQL / Redis / RabbitMQ / Nacos + init/upgrade SQL
├── power-common/           # Unified R<T>, errors, constants, paging
├── power-middleware/       # Security/JWT, MP, Redis, MQ/Outbox, Feign, OpenAPI
├── power-gateway/          # :8080 gateway auth & routing
├── power-auth/             # :8081 login, RBAC, workflow identity APIs
├── power-system/           # :8082 system probe (Ping / Feign / Outbox)
├── power-workflow/         # :8083 Flowable + leave / expense / countersign / tasks
└── power-web/              # :5173 Vue 3 admin (proxies gateway in dev)
```

| Module | Port | Role |
|--------|------|------|
| power-gateway | **8080** | Entry: `/auth/**`, `/system/**`, `/workflow/**` |
| power-auth | 8081 | Auth & RBAC |
| power-system | 8082 | Probe / sample APIs |
| power-workflow | 8083 | Flowable & workflow APIs |
| power-web | 5173 | Admin UI (Vite) |

```text
gateway ──► auth / system / workflow
auth / system / workflow ──► common + middleware
workflow ──Feign──► auth (role codes / display names)
```

---

## Requirements

- **JDK 17+** (Spring Boot 3)
- Maven 3.8+
- Docker / Docker Compose
- Node.js 18+ (frontend)

---

## Quick start

### 1. Infrastructure

```bash
cd docker
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d
```

When MySQL becomes healthy, [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql) runs once (create DB + schema + seed). Keep **only that one** full init file under `docker/mysql/init/`.

Existing DB upgrades live under [`docker/mysql/upgrade/`](docker/mysql/upgrade/README.md); re-login after applying.

| Service | Port |
|---------|------|
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 (UI 15672) |
| Nacos | 8848 |

### 2. Build backend

```bash
mvn -DskipTests clean install
```

### 3. Run services (one terminal each)

```bash
mvn -pl power-auth spring-boot:run
mvn -pl power-system spring-boot:run
mvn -pl power-workflow spring-boot:run
mvn -pl power-gateway spring-boot:run
```

Default profile `local`:

- No Nacos discovery; gateway routes to `8081` / `8082` / `8083`
- Middleware hosts default to `127.0.0.1` (see `application-local.yml`)

### 4. Frontend

```bash
cd power-web
npm install
npm run dev
```

Open http://127.0.0.1:5173 — see [`power-web/README.md`](power-web/README.md).

### 5. Smoke test

```bash
curl -X POST http://127.0.0.1:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"platform\":\"WEB\"}"

# Debug auth (local only; token from application-local.yml)
curl http://127.0.0.1:8080/system/ping -H "X-Debug-Auth: local-only-change-me"
```

Swagger UI (Authorize with `accessToken`):

| Service | URL |
|---------|-----|
| Auth | http://127.0.0.1:8081/swagger-ui.html |
| System | http://127.0.0.1:8082/swagger-ui.html |
| Workflow | http://127.0.0.1:8083/swagger-ui.html |

---

## Seed accounts

Password for **all** users: `admin123`.

| Username | Nickname (sample) | Role | Typical use |
|----------|-------------------|------|-------------|
| `admin` | 系统管理员 | ADMIN | Admin, deploy, monitor |
| `approver1` … `approver5` | 王芳 / 李明 / … | APPROVER | Approve, countersign, claim leave |
| `staff1` … `staff5` | 张伟 / 刘娜 / … | STAFF | Start leave / business only |

**Suggested path**

```text
staff1 → Leave Management → submit
approver1 → Task Center → claim / complete
both → Process Instances → timeline / diagram
```

**Expense / countersign**

```text
staff1 → Process Instances →「发起业务」
pick manager + countersigners from APPROVER only
approver* → Task Center → handle
```

---

## Architecture

```text
Browser (power-web :5173)
        │  /auth /system /workflow  (Vite proxy)
        ▼
power-gateway (:8080)
        │  JWT / debug auth / routes
        ├──────────────┬──────────────┐
        ▼              ▼              ▼
   power-auth     power-system   power-workflow
    (:8081)         (:8082)         (:8083)
        │                               │
        │◄──────── Feign identity ──────┤
        │                               │
        └──────── MySQL / Redis / RabbitMQ ──────┘
```

---

## Security

### Unified response

```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "..." }
```

- `code = 0` success; failures return a business `code` + `message`
- Kick-out: `code = 20004` (UI redirects to login)
- Auth expired / invalid: frontend clears tokens and redirects

### Permission codes

Pattern: `module:resource:action`, e.g. `system:user:list`, `workflow:task:handle`.

- Backend: `@PreAuthorize("@authz.permit('workflow:task:handle')")`
- Frontend: `v-perm="'workflow:task:handle'"` (same as `sys_menu.perms`)

### Tokens

| Item | Notes |
|------|-------|
| Access / Refresh | JWT; refresh & session state in Redis |
| Platform | Pass `WEB` or `MOBILE` on login |
| Multi-login | `power.security.multi-platform-login-enabled`; if off, re-login kicks the same platform |
| Logout | Access JTI blacklisted in Redis immediately |

### Debug auth (local / dev only)

Header `X-Debug-Auth` matching `power.security.debug-auth-token` skips JWT and injects a configured debug user (may include `*`). **Disable in production.**

### Flowable identity mapping

| Flowable | This project |
|----------|--------------|
| `assignee` / `startUserId` | `sys_user.id` as **string**, e.g. `"7"` |
| `candidateGroups` | `sys_role.role_code`, e.g. `APPROVER` |
| Display name | `nickname`, else `username` |

`ACT_ID_*` is **not** synced. Auth exposes (authenticated):

- `GET /auth/workflow/users/{userId}`
- `GET /auth/workflow/users/{userId}/roles`
- `GET /auth/workflow/roles/{roleCode}/users`

Snowflake IDs are serialized as **strings** via Jackson to avoid JS precision loss.

---

## Workflow

### Built-in processes

| Key | Resource | Notes |
|-----|----------|-------|
| `leave` | `processes/leave.bpmn20.xml` | Candidate group `APPROVER`; end listener updates `wf_leave.status` |
| `expense` | `processes/expense.bpmn20.xml` | `${managerUserId}` then parallel countersign |
| `countersign-or` | `processes/countersign-or.bpmn20.xml` | Parallel or-sign (one approve **or** one reject ends MI) |
| `countersign-seq` | `processes/countersign-seq.bpmn20.xml` | Sequential countersign (ordered; veto on reject) |

On first boot, missing definitions are deployed from classpath. Some built-ins (e.g. or-sign) **refresh once** when an old completion condition is detected. After you edit BPMN yourself, re-deploy under **Definitions** or **Models**.

### How to start each process

| Process | Correct entry | Do **not** |
|---------|---------------|------------|
| Leave | UI **请假管理** or `POST /workflow/leave` | Generic `POST /workflow/instances/start` with key `leave` |
| Expense | UI **发起业务** or `POST /workflow/expense` | Start without `managerUserId` / countersigners |
| Or / seq countersign | UI **发起业务** or `POST /workflow/countersign/or\|seq` | Fewer than 2 approvers |

Generic `POST /workflow/instances/start` remains for custom keys. For built-in keys it enforces the same variable / operator rules (and **blocks** forged leave starts).

### Operator rules

- **Approvers only** for manager / countersign / transfer / delegate / add-sign / re-assign targets (`APPROVER` or `ADMIN`)
- **STAFF** can start processes and appear in **CC**, but cannot be picked as task operators in the UI
- Instance list: **标题** = user-entered `title` variable; **类型** = process type label

### Leave status

| status | Meaning |
|--------|---------|
| 1 | Approving |
| 2 | Approved |
| 3 | Rejected |
| 4 | Cancelled |

### Main APIs (gateway `/workflow/**`, login + permission)

<details>
<summary>Expand: categories / models / definitions / instances / tasks / leave / expense</summary>

**Categories** `workflow:category:*` — CRUD under `/workflow/categories`

**Models** `workflow:model:*` (deploy needs `workflow:definition:deploy`)

| Method | Path |
|--------|------|
| Save draft (upsert by `modelKey`) | `POST /workflow/models` |
| Page / detail / delete | `GET` / `DELETE /workflow/models…` |
| Deploy | `POST /workflow/models/{id}/deploy` |

**Definitions**

| Method | Path | Perm |
|--------|------|------|
| Upload BPMN | `POST /workflow/definitions/deploy` | deploy |
| Latest list | `GET /workflow/definitions` | list |
| Startable list | `GET /workflow/definitions/startable` | instance:start |
| Suspend / activate | `POST …/suspend` · `…/activate` | suspend |
| BPMN XML | `GET …/xml` | list |

**Instances**

| Method | Path | Perm |
|--------|------|------|
| Start (generic) | `POST /workflow/instances/start` | start |
| Mine | `GET /workflow/instances/mine` | list |
| Monitor | `GET /workflow/instances` | monitor |
| Detail / timeline / highlight / PNG | `GET …/{id}` … | list **or** monitor **or** CC |
| Active tasks | `GET …/{id}/tasks` | same as detail |
| Cancel (starter) | `POST …/{id}/cancel` | list |
| Terminate / suspend / activate / urge | `POST …/{id}/terminate` … | matching perms |

**Tasks**

| Method | Path | Perm |
|--------|------|------|
| Todo / done / CC | `GET /workflow/tasks/todo` · `/done` · `/cc` | list / cc |
| Claim / unclaim | `POST …/claim` · `/unclaim` | handle |
| Complete / reject / transfer / assign | `POST …/complete` · `/reject` · `/transfer` · `/assign` | handle |
| Rejectable nodes | `GET …/{taskId}/rejectable-nodes` | handle |
| Delegate / resolve | `POST …/delegate` · `/resolve` | delegate |
| Add-sign / reduce-sign | `POST …/add-sign` · `/reduce-sign` | addsign |

**Leave**

| Method | Path | Perm |
|--------|------|------|
| Apply | `POST /workflow/leave` | apply |
| My list | `GET /workflow/leave/mine` | list |
| Detail | `GET /workflow/leave/{id}` | apply |

**Expense / countersign**

| Method | Path | Perm |
|--------|------|------|
| Expense | `POST /workflow/expense` | instance:start |
| Or-sign / sequential | `POST /workflow/countersign/or` · `/countersign/seq` | instance:start |

</details>

### Behavior summary

| Capability | Rule |
|------------|------|
| Cancel | Running instance & `startUserId` = current user; leave status → 4 |
| Reject | `strategy`: PREVIOUS / TO_NODE / TO_STARTER / TERMINATE; MI targets excluded from TO_NODE list |
| Complete | Defaults to `approved=true` (Boolean); BEFORE add-sign returns without advancing |
| Unclaim | Only when candidate links exist (no orphaning variable-assignee tasks) |
| Add-sign | BEFORE returns to owner; AFTER advances via add-signer |
| Reduce-sign | MI only; refused when only one active instance remains |
| Transfer / assign | Target must be an operator role (`APPROVER` / `ADMIN`) |
| Or-sign veto | One reject sets `approved=false` and ends remaining MI siblings |
| Outbox | completed / cancelled → `sys_outbox` → Exchange `power.workflow` |

---

## Auth APIs (selected)

Gateway prefix `/auth/**`:

| Method | Path | Notes |
|--------|------|-------|
| POST | `/auth/login` | `{ username, password, platform }` |
| POST | `/auth/login/web` · `/mobile` | Shortcuts |
| POST | `/auth/refresh` | Refresh tokens |
| POST | `/auth/logout` | Logout |
| GET | `/auth/me` | Current user (roles / authorities) |
| PUT | `/auth/me/profile` · `/password` | Profile / password |
| GET | `/auth/menus/tree` | Sidebar menus |
| CRUD | `/auth/users` · `/roles` · `/menus` | Admin (`system:*`) |

Whitelist (no token): login, refresh, logout.

---

## Configuration

- Profiles: shared `application.yml`; diffs in `application-local.yml` / `application-prod.yml`
- Env vars aligned with Compose: [`docker/.env.example`](docker/.env.example)
- JWT: `JWT_SECRET` must match across services; local has a dev default, **prod requires it**
- Multi-datasource: `power.datasource.multi-enabled=false` by default
- Exception source location: on in local, off in prod

### App ↔ Compose (local)

| App var | Compose / .env | Local default |
|---------|----------------|---------------|
| `MYSQL_HOST` / `MYSQL_PORT` | mapped `3306` | `127.0.0.1` / `3306` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | root / `MYSQL_ROOT_PASSWORD` | `root` / `root` |
| `REDIS_*` | mapped `6379`, no password by default | `127.0.0.1` / `6379` / empty |
| `RABBIT_*` | mapped `5672` | `127.0.0.1` / `5672` / `guest` |
| `NACOS_ADDR` | `8848` | `127.0.0.1:8848` (discovery off in local) |

### Production

```bash
export SPRING_PROFILES_ACTIVE=prod
# inject MYSQL_* / REDIS_* / RABBIT_* / NACOS_ADDR / JWT_SECRET …
```

Expose only the gateway publicly; keep debug auth off.

---

## Frontend (power-web)

| Item | Notes |
|------|-------|
| Dev proxy | `/auth`, `/system`, `/workflow` → `http://127.0.0.1:8080` |
| Dynamic routes | Menu `component` e.g. `system/user/index` → `views/system/user/index.vue` |
| Button perms | `v-perm` matches `@authz.permit` |
| Pages | Login, profile, user/role/menu, workflow category / definition / model / instance / task / leave |

Add a page: create under `views/` → configure menu → assign role → re-login.

---

## Database & upgrades

| Path | Use |
|------|-----|
| [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql) | Compose first boot (full schema + seed) |
| [`docker/mysql/upgrade/`](docker/mysql/upgrade/README.md) | **Upgrade scripts** for existing databases |

After applying an upgrade SQL, **re-login** so JWT authorities refresh.

To wipe runtime instances in a **dev** DB (then recreate volume or re-apply seed carefully):

```bash
mysql -uroot -proot < docker/mysql/upgrade/07_reset_runtime_and_users.sql
```

Prefer recreating the Compose MySQL volume for a clean slate.
---

## Middleware capabilities

| Capability | Notes |
|------------|-------|
| Redis sessions | Blacklist, refresh, kick, user version |
| Distributed lock | `RedisDistributedLock` (SET NX + Lua unlock) |
| Cache / rate limit | cache-aside, fixed-window limiter |
| RabbitMQ | Topology, manual-ack base, idempotency, retry / DLX |
| Outbox | Local outbox table + dispatcher |
| MyBatis-Plus | Paging, logic delete, audit fill |
| OpenAPI | SpringDoc auto-config per service |

---

## Out of scope (for now)

- Multi-tenancy, standalone OAuth2 AS, Seata strong XA
- Flowable CMMN / DMN, syncing `ACT_ID_*`
- Sharding / full data-scope interceptors (extend later)
---

## FAQ

**Workflow pages return 403 after login?**  
Ensure the role has matching `workflow:*` menu permissions. STAFF and APPROVER differ in seed data. Re-login after SQL upgrades.

**Leave submit says “use Leave Management”?**  
You must call `POST /workflow/leave` (or the Leave UI). Generic instance start cannot forge leave processes.

**No todos for leave?**  
Leave uses candidate group `APPROVER` — log in as `approver1` (etc.), then **claim** / complete.

**Cancelled claim made a task disappear?**  
Variable-assignee / MI tasks cannot unclaim. Use **transfer** / **re-assign** from instance detail instead.

**BPMN changes have no effect?**  
Re-upload under Definitions or deploy from Models. Or-sign may auto-refresh once on boot when an outdated completion condition is detected.

**CORS in the browser?**  
Dev uses the Vite proxy; start the gateway first.

**Init SQL did not run?**  
Compose runs `/docker-entrypoint-initdb.d` only on first volume create. Remove the volume and `docker compose up -d` again if needed.

---

## Contributing

1. Fork and create a feature branch from `main`
2. Keep changes focused; follow existing module boundaries (`power-*`)
3. Do **not** name packages/APIs/menus with `demo` (see `.cursor/rules/no-demo-naming.mdc`)
4. For schema changes: update `docker/mysql/init/01-schema.sql` **and** add an upgrade script under `docker/mysql/upgrade/`
5. Open a PR with a short summary and test notes (accounts used, APIs hit)

Bug reports: include service logs, request path, username/role, and process definition key when relevant.

---

## License

Released under the [MIT License](LICENSE).

Copyright (c) 2026 leexiangf
