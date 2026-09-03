# power-web

Vue 3 管理端前端，对接 **power-gateway（8080）**。

根仓库总览、后端启动与工作流说明：

- [English](../README.md) · [中文](../README.zh.md)

## 技术栈

| 类别 | 选型 | 作用 |
|------|------|------|
| 构建 | Vite | 开发热更新、打包 |
| 框架 | Vue 3 + TypeScript | 页面与组件 |
| 路由 | Vue Router | 地址栏跳转、动态菜单路由 |
| 状态 | Pinia | 登录用户、菜单等全局数据 |
| UI | Element Plus | 表格、表单、布局 |
| 请求 | Axios | 调用后端 API |
| 样式 | SCSS | 全局变量与紧凑主题 |

## 快速开始

```bash
cd power-web
npm install
npm run dev
```

浏览器打开 http://127.0.0.1:5173（或以终端 Vite 输出为准）。

> 只执行 `npm install` 不会启动页面；请保持 `npm run dev` 终端不关。  
> **请先启动网关与各后端**（至少 gateway + auth；工作流页还需 workflow）。

默认账号：`admin` / `admin123`（平台 WEB）。联调流程可用 `zhangsan`（发起）/ `lisi`（审批）。

## 与后端如何通信

开发环境不必单独配跨域：

- `.env.development` 里 `VITE_API_BASE_URL` 为空
- `vite.config.ts` 把 `/auth`、`/system`、`/workflow` 代理到 `http://127.0.0.1:8080`

生产在 `.env.production` 配置网关地址。

统一响应（与后端 `R<T>` 一致）：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code !== 0`：弹错误提示
- `401`：尝试 refreshToken 续期
- `20004`：被其他端顶号，跳转登录页

雪花 ID 按 **字符串** 处理，勿转成 Number。

## 目录说明

```text
src/
  api/            # 按模块封装接口（auth / user / workflow/...）
  components/     # 通用与工作流组件
  layout/         # 侧栏 + 顶栏布局
  router/         # 路由表 + 菜单转路由
  stores/         # Pinia（auth）
  views/          # 页面（与 sys_menu.component 路径对应）
  utils/          # Token、权限、工作流工具
  directives/     # v-perm 按钮级权限
  types/          # TS 类型
  styles/         # 全局样式变量
```

### 动态路由

1. 登录后请求 `GET /auth/me`、`GET /auth/menus/tree`
2. `router/menu.ts` 将菜单树转为 Vue Router 配置
3. 后端 `component` 如 `workflow/leave/index` → 加载 `views/workflow/leave/index.vue`
4. 侧栏与路由共用同一份 `menus`

**新增页面：**

1. 在 `views/` 下新建 `.vue`（路径与库表 `component` 一致）
2. 在「菜单管理」配置菜单（或改种子 SQL）
3. 给角色分配权限后重新登录

### 按钮权限

```vue
<el-button v-perm="'system:user:add'">新增</el-button>
```

与后端 `@PreAuthorize("@authz.permit('system:user:add')")` 使用同一权限码。

## 已实现页面

| 模块 | 路径（视图） | 说明 |
|------|--------------|------|
| 登录 / 个人中心 | `login` · `profile` | Token、资料、改密 |
| 用户 / 角色 / 菜单 | `system/*` | CRUD + 授权 |
| 流程分类 | `workflow/category` | CRUD |
| 流程定义 | `workflow/definition` | 部署、挂起、XML |
| 流程模型 | `workflow/model` | XML 草稿、部署（轻量版） |
| 流程实例 | `workflow/instance` | 我发起的 / 监控、详情抽屉 |
| 任务中心 | `workflow/task` | 待办已办、认领办理驳回转办 |
| 请假管理 | `workflow/leave` | 申请 + 结果详情 |

## 常用命令

```bash
npm run dev       # 开发
npm run build     # 类型检查 + 生产打包（输出 dist/）
npm run preview   # 预览打包结果
```
