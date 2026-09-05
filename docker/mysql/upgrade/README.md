# MySQL upgrade scripts

These scripts are for **existing** databases that were created from an older seed.

Fresh Docker Compose installs only need:

[`../init/01-schema.sql`](../init/01-schema.sql)

That init directory must stay a **single** full bootstrap file (Compose runs it once on first volume create).

## Apply (existing DB)

```bash
# from repo root, adjust user/password as needed
mysql -uroot -proot < docker/mysql/upgrade/03_m4_upgrade.sql
mysql -uroot -proot < docker/mysql/upgrade/04_m5_upgrade.sql
mysql -uroot -proot < docker/mysql/upgrade/05_rename_demo_to_probe.sql
mysql -uroot -proot < docker/mysql/upgrade/06_staff_task_handle.sql
mysql -uroot -proot < docker/mysql/upgrade/08_approver_urge_leave_list.sql
```

Optional (dev only — clears Flowable runtime/history; then re-seed users from `init/01-schema.sql` user section or recreate the volume):

```bash
mysql -uroot -proot < docker/mysql/upgrade/07_reset_runtime_and_users.sql
```

After permission upgrades, **re-login** so JWT authorities refresh.

## Index

| File | Purpose |
|------|---------|
| `03_m4_upgrade.sql` | CC / urge tables + related menus |
| `04_m5_upgrade.sql` | Add-sign permission |
| `05_rename_demo_to_probe.sql` | Rename demo → probe menus/perms |
| `06_staff_task_handle.sql` | STAFF gets `workflow:task:handle` |
| `07_reset_runtime_and_users.sql` | Clear Flowable instance/history (+ biz runtime tables) |
| `08_approver_urge_leave_list.sql` | APPROVER: urge + my-leave list |
