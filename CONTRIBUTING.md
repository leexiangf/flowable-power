# Contributing to flowable-power

Thanks for helping improve this scaffold.

## Ground rules

- Prefer small, focused PRs over large mixed changes.
- Respect module boundaries: `power-common` → `power-middleware` → services; UI in `power-web`.
- **No `demo` naming** for packages, APIs, menus, MQ, or UI copy. Prefer domain names (`leave`, `expense`, `countersign`, `probe`). See `.cursor/rules/no-demo-naming.mdc`.
- Do not commit secrets (`.env`, private keys, real JWT secrets).

## Development setup

Follow the [Quick start](./README.md#quick-start) in the root README (Compose + four Spring Boot apps + Vite).

Default seed password: `admin123` (`staff1` / `approver1` / `admin`).

## Database changes

1. Update the Compose full init file: [`docker/mysql/init/01-schema.sql`](docker/mysql/init/01-schema.sql)
2. Add a numbered upgrade script under [`docker/mysql/upgrade/`](docker/mysql/upgrade/README.md) for **existing** databases
3. Document the script in `docker/mysql/upgrade/README.md`

## Workflow / BPMN

- Built-in BPMN lives under `power-workflow/src/main/resources/processes/`
- Identity: `assignee` = `sys_user.id` string; `candidateGroups` = `role_code`
- Operator selections (manager, countersign, transfer, …) must stay **APPROVER/ADMIN**, not STAFF
- Leave must start via `POST /workflow/leave` (or `startFromBusiness`), never a forged generic start

## Pull requests

Please include:

1. **Summary** — what and why
2. **Test plan** — accounts used, APIs/UI paths, process keys
3. Notes on SQL upgrades or BPMN redeploy if needed

## Code style

- Java 17, existing Spring / Lombok patterns
- Vue 3 + TypeScript; match nearby components
- Prefer clear error messages over silent failures

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
