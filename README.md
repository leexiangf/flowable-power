# flowable-power

[English](./README.md) | [中文](./README.zh.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Flowable + Spring Boot 3 + Spring Cloud scaffolding** with JWT/RBAC, API gateway, and a Vue 3 admin UI. Spin up login, user/role/menu management, and a full **leave-approval BPMN workflow** in minutes.

Built for teams who need a practical **Java workflow / BPM starter** (Flowable Process 7) instead of a blank Spring Boot repo.

---

## Features

- **API Gateway auth** — Spring Cloud Gateway validates JWT and forwards user context
- **RBAC** — users / roles / menus + `@PreAuthorize("@authz.permit('...')")`
- **Multi-platform login** — WEB / MOBILE isolation, optional kick-out (`code=20004`)
- **Flowable Process** — categories, model drafts, deploy, instances, tasks, diagrams, cancel
- **Business samples** — leave request (candidate group) + expense demo (assignee variable / countersign)
- **Middleware kit** — Redis (session / lock / cache / rate limit), RabbitMQ Outbox, OpenFeign
- **Admin frontend** — dynamic menu routes, `v-perm` directive, full workflow pages

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
| Frontend | Vue 3 + Vite + Pinia + Element Plus + TS | — |
| Local infra | Docker Compose | — |

---

## Modules

```text
flowable-power/
├── docker/                 # Compose: MySQL / Redis / RabbitMQ / Nacos + init SQL
├── power-common/           # Unified R<T>, errors, constants, paging
├── power-middleware/       # Security/JWT, MP, Redis, MQ/Outbox, Feign, OpenAPI
├── power-gateway/          # :8080 gateway auth & routing
├── power-auth/             # :8081 login, RBAC, workflow identity APIs
├── power-system/           # :8082 demo business (Ping / Feign / Outbox)
├── power-workflow/         # :8083 Flowable + leave / model / tasks
└── power-web/              # :5173 Vue 3 admin (proxies gateway in dev)
```

| Module | Port | Role |
|--------|------|------|
| power-gateway | **8080** | Entry: `/auth/**`, `/system/**`, `/workflow/**` |
| power-auth | 8081 | Auth & RBAC |
| power-system | 8082 | Sample APIs |
| power-workflow | 8083 | Flowable & workflow APIs |
| power-web | 5173 | Admin UI (Vite) |

```text
gateway ──► auth / system / workflow
auth / system / workflow ──► common + middleware
workflow ──Feign──► auth (role codes / display names)
```

---

## Requirements

- **JDK 17+** (required by Boot 3)
- Maven 3.8+
- Docker / Docker Compose
- Node.js 18+ (frontend only)

---

## Quick start

### 1. Infrastructure

```bash
cd docker
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d
```

After MySQL is healthy, [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql) runs automatically (schema + seed users / menus / roles).

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

- No Nacos discovery; gateway points to `8081/8082/8083`
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

Swagger UI (Authorize with accessToken):

- Auth: http://127.0.0.1:8081/swagger-ui.html
- System: http://127.0.0.1:8082/swagger-ui.html
- Workflow: http://127.0.0.1:8083/swagger-ui.html

---

## Seed accounts

Password for all: `admin123`.

| User | Role | Typical use |
|------|------|-------------|
| `admin` | ADMIN | Admin, deploy, monitor |
| `zhangsan` | STAFF | Start leave, cancel own instance |
| `lisi` | APPROVER | Claim / complete / reject / transfer |

Suggested path:

```text
zhangsan applies leave → lisi handles todo → both check instance timeline / diagram
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

## Security conventions

### Unified response

```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "..." }
```

- `code = 0` success; business failures return a clear code + `message`
- Kick-out: `code = 20004` (frontend redirects to login)

### Permission codes

Pattern: `module:resource:action`, e.g. `system:user:list`, `workflow:task:handle`.

- Backend: `@PreAuthorize("@authz.permit('workflow:task:handle')")`
- Frontend: `v-perm="'workflow:task:handle'"` (same as `sys_menu.perms`)

### Tokens

| Item | Notes |
|------|-------|
| Access / Refresh | JWT; Refresh & session state in Redis |
| Platform | Pass `WEB` or `MOBILE` on login |
| Multi-login | `power.security.multi-platform-login-enabled`; if off, re-login kicks the same platform |
| Logout | Access JTI blacklisted in Redis immediately |

### Debug auth (local / dev only)

Header `X-Debug-Auth` matching `power.security.debug-auth-token` skips JWT and injects a configured debug user (may include `*`). **Must be disabled in production.**

### Flowable identity mapping

| Flowable | This project |
|----------|--------------|
| `assignee` / `startUserId` | `sys_user.id` as **string**, e.g. `"2"` |
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

On first boot, missing definitions are deployed from classpath. **Existing deployments are not overwritten** — re-deploy after BPMN changes.

### Leave status

| status | Meaning |
|--------|---------|
| 1 | Approving |
| 2 | Approved |
| 3 | Rejected |
| 4 | Cancelled |

### Main APIs (via gateway `/workflow/**`, login + permission required)

<details>
<summary>Expand: categories / models / definitions / instances / tasks / leave</summary>

**Categories** `workflow:category:*` — CRUD under `/workflow/categories`

**Models** `workflow:model:*` (deploy needs `workflow:definition:deploy`)

| Method | Path |
|--------|------|
| Save draft (upsert by `modelKey`) | `POST /workflow/models` |
| Page / detail / delete | `GET/DELETE /workflow/models…` |
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
| Start | `POST /workflow/instances/start` | start |
| Mine | `GET /workflow/instances/mine` | list |
| Monitor | `GET /workflow/instances` | monitor |
| Detail / timeline / highlight / PNG | `GET …/{id}` … | list **or** monitor |
| Cancel (starter only) | `POST …/{id}/cancel` | list |

**Tasks**

| Method | Path | Perm |
|--------|------|------|
| Todo / done | `GET /workflow/tasks/todo` · `/done` | list |
| Claim / unclaim | `POST …/claim` · `/unclaim` | handle |
| Complete / reject / transfer | `POST …/complete` · `/reject` · `/transfer` | handle |

**Leave**

| Method | Path | Perm |
|--------|------|------|
| Apply | `POST /workflow/leave` | apply |
| Detail | `GET /workflow/leave/{id}` | apply |

**Demo**

| Method | Path |
|--------|------|
| Expense demo | `POST /workflow/demo/expense` (`workflow:instance:start`) |

</details>

### Behavior summary

| Capability | Rule |
|------------|------|
| Cancel | Running instance & `startUserId` = current user; leave status → 4 |
| Reject | Move back to previous user task if any; else complete with `approved=false` |
| Complete | Defaults to `approved=true` (Boolean) |
| Transfer | `setAssignee` to target userId string |
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
| Pages | Login, profile, user/role/menu, workflow category/definition/model/instance/task/leave |

Add a page: create under `views/` → configure menu → assign role → re-login.

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
Ensure the role has the matching `workflow:*` menu permissions (STAFF vs APPROVER differ in seed data).

**BPMN changes have no effect?**  
Engine does not auto-overwrite deployed definitions — re-upload under Definitions or deploy from Models.

**No todos?**  
Leave uses candidate group `APPROVER` — log in as `lisi` (or another APPROVER), claim, then complete.

**CORS in the browser?**  
Dev uses the Vite proxy; start the gateway first.

**Init SQL did not run?**  
Compose runs `/docker-entrypoint-initdb.d` only on first volume create. Remove the volume and `docker compose up -d` again if needed.

---

## License

Released under the [MIT License](LICENSE).

Copyright (c) 2026 leexiangf
