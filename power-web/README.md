# power-web

Vue 3 admin UI for **flowable-power**, talking to **power-gateway (`:8080`)**.

Root docs:

- [English README](../README.md) · [中文 README](../README.zh.md)

## Stack

| Area | Choice |
|------|--------|
| Build | Vite |
| Framework | Vue 3 + TypeScript |
| Router | Vue Router (dynamic menus) |
| State | Pinia |
| UI | Element Plus |
| HTTP | Axios |
| Styles | SCSS |

## Quick start

```bash
cd power-web
npm install
npm run dev
```

Open http://127.0.0.1:5173 (or the URL printed by Vite).

> Keep the `npm run dev` process running.  
> **Start gateway + backends first** (at least gateway + auth; workflow pages need power-workflow).

### Seed logins

Password for all: `admin123`.

| Username | Role | Use |
|----------|------|-----|
| `admin` | ADMIN | Full admin |
| `staff1` … `staff5` | STAFF | Start leave / business |
| `approver1` … `approver5` | APPROVER | Approve / countersign / claim leave |

Suggested path: `staff1` submits leave → `approver1` handles todo.

## Talking to the API

Dev does not need CORS setup:

- `.env.development`: `VITE_API_BASE_URL` empty
- `vite.config.ts` proxies `/auth`, `/system`, `/workflow` → `http://127.0.0.1:8080`

Production: set the gateway base URL in `.env.production`.

Unified envelope (same as backend `R<T>`):

```json
{ "code": 0, "message": "ok", "data": { } }
```

| Case | UI behavior |
|------|-------------|
| `code !== 0` | Error toast |
| HTTP `401` / auth business codes | Try refresh, else clear token → login |
| `20004` | Kicked by another login → login |

Treat snowflake IDs as **strings**, never `Number(...)`.

## Layout

```text
src/
  api/            # auth / user / workflow clients
  components/     # shared + workflow widgets
  layout/         # shell
  router/         # static + menu-driven routes
  stores/         # Pinia (auth)
  views/          # pages (match sys_menu.component)
  utils/          # token, permission, workflow helpers
  directives/     # v-perm
  types/          # TS types
  styles/         # theme
```

### Dynamic routes

1. Login → `GET /auth/me`, `GET /auth/menus/tree`
2. `router/menu.ts` converts menus to Vue Router records
3. Backend `component` like `workflow/leave/index` → `views/workflow/leave/index.vue`
4. Sidebar and routes share the same menu tree

**Add a page:** create under `views/` → configure menu → grant role → re-login.

### Permissions

- Directive: `v-perm="'workflow:task:handle'"`
- Must match backend `@authz.permit` and `sys_menu.perms`

### Workflow UI notes

| Page | Notes |
|------|-------|
| Leave | Always calls `POST /workflow/leave` |
| Instances | **标题** = start title; **类型** = process type; **发起业务** for expense / countersign |
| Tasks | Operator actions need `workflow:task:handle`; unclaim only when `canUnclaim` |
| CC picker | APPROVER + STAFF; operator pickers are APPROVER-only |

## Scripts

```bash
npm run dev       # local
npm run build     # production bundle
npm run preview   # preview build
```

## License

Same as the root project: [MIT](../LICENSE).
