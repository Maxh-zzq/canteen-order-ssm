# canteen-order-web · 西工食堂前端

校园食堂网上订餐系统的 Web 前端，基于 **Vue 3 + Vite + TypeScript**，覆盖学生点餐、档口出餐看板和管理端后台。后端工程为同级目录 `../canteen-order`（Spring Boot，端口 `8083`）。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（`<script setup>` + Composition API） |
| 构建 | Vite 5.4、TypeScript 5.6、vue-tsc |
| UI | Element Plus 2.9（中文语言包、按需全量图标注册） |
| 状态 | Pinia 2.3 |
| 路由 | Vue Router 4.5（HTML5 History 模式） |
| 请求 | Axios 1.7（统一响应解包、401 单飞刷新、错误提示） |
| 图表 | ECharts 5.5 |
| 时间 | Day.js |

## 本地开发

前置：Node.js 20+（本项目在 Node 22 上验证），后端已在 `http://127.0.0.1:8083` 启动。

```bash
cd canteen-order-web
npm install
npm run dev
```

- 开发地址：`http://127.0.0.1:5173`
- Vite 已配置代理：
  - `/api` → `http://127.0.0.1:8083`
  - `/uploads` → `http://127.0.0.1:8083`
- 因此开发环境不存在跨域问题，前端请求统一走同源 `/api/v1`。

常用命令：

```bash
npm run dev         # 开发服务器（5173）
npm run type-check  # vue-tsc 类型检查
npm run build       # 类型检查 + 生产构建，产物在 dist/
npm run preview     # 本地预览生产构建
```

## 演示账号

| 角色 | 账号 | 密码 | 入口 |
| --- | --- | --- | --- |
| 学生 | `20230001` | `123456` | 今日点餐 / 购物车 / 我的订单 |
| 档口员工 | `staff01` | `123456` | 出餐看板 |
| 管理员 | `admin` | `123456` | 经营概览与后台管理 |

登录页提供三个演示账号的快捷填充。

## 页面与角色

| 角色 | 路由 | 说明 |
| --- | --- | --- |
| 学生 | `/menu` | 今日菜单、分类/档口筛选、搜索、加购 |
| 学生 | `/cart` | 数量调整、同档口校验、余额支付、取餐码弹窗 |
| 学生 | `/orders` | 状态筛选、倒计时、支付/取消、订单详情、评价 |
| 学生 | `/profile` | 资料维护、校园卡余额充值 |
| 员工 | `/staff/board` | 顶部展示“我的档口”，三列看板仅显示绑定档口订单，15 秒自动刷新 |
| 管理员 | `/admin/dashboard` | 今日经营指标、7/30 天销售趋势、热销菜品 |
| 管理员 | `/admin/dishes` | 菜品 CRUD、封面图上传、规格与加价、上下架 |
| 管理员 | `/admin/categories` | 分类 CRUD、排序、启停 |
| 管理员 | `/admin/stalls` | 档口 CRUD、营业时间、启停 |
| 管理员 | `/admin/menus` | 按日期发布菜单、调整库存 |
| 管理员 | `/admin/orders` | 全量订单、详情、退款、CSV 导出 |
| 管理员 | `/admin/users` | 用户查询、启用/禁用、为员工分配/收回多个档口 |
| 管理员 | `/admin/logs` | 操作日志审计 |
| 管理员 | `/admin/configs` | 业务参数配置 |

## 目录结构

```text
canteen-order-web/
├── public/                 # 静态资源（favicon）
├── src/
│   ├── api/                # Axios 实例与按领域拆分的接口模块
│   ├── components/         # BrandMark / DishCard / StatusTag / EmptyState
│   ├── layouts/            # StudentLayout（学生端）/ ConsoleLayout（控制台）
│   ├── router/             # 路由、角色守卫、首屏用户补水
│   ├── stores/             # Pinia：auth / cart
│   ├── styles/             # 设计 tokens 与全局样式
│   ├── types/              # 与后端 VO/DTO 对齐的 TS 类型
│   ├── utils/              # session、格式化、requestId、下载
│   └── views/
│       ├── LoginView.vue
│       ├── student/        # 点餐、购物车、订单、个人中心
│       ├── staff/          # 出餐看板
│       └── admin/          # 仪表盘与后台管理
├── deploy/nginx.conf.example
├── .env.development
├── .env.production
└── vite.config.ts
```

## 环境变量

| 变量 | 开发 | 生产 | 说明 |
| --- | --- | --- | --- |
| `VITE_API_BASE_URL` | `/api/v1` | `/api/v1` | 由 Vite / Nginx 反向代理到后端 |
| `VITE_APP_TITLE` | `西工食堂` | `西工食堂` | 页面标题 |

生产环境保持相对路径 `/api/v1`，由 Nginx 将 `/api` 反代到后端，避免把后端地址硬编码进构建产物。

## 企业级前后端分离部署（Nginx）

### 代码放在哪里

- **Nginx 不存放源码**，只托管 `npm run build` 生成的 `dist/` 静态文件。
- 源码存放在 Git：前后端可以是两个独立仓库，也可以是当前 monorepo 的两个目录。
  - `canteen-order/`：Spring Boot 源码，CI 产出 jar。
  - `canteen-order-web/`：Vue 源码，CI 产出 dist。
- CI/CD 负责“构建 → 发布产物”：`dist/` 发布到 Nginx 主机（或对象存储/CDN），jar 发布到应用服务器。
- 开发/测试/生产环境通过环境变量与 Nginx 配置区分，不把服务器密码提交到仓库。

### 发布步骤

```bash
cd canteen-order-web
npm ci
npm run build

# 将 dist 发布到 Nginx 主机
rsync -av --delete dist/ user@web-server:/var/www/canteen-order-web/
sudo nginx -t && sudo systemctl reload nginx
```

### Nginx 配置

完整示例见 [`deploy/nginx.conf.example`](deploy/nginx.conf.example)，核心点：

- `try_files $uri $uri/ /index.html`：支持 Vue Router History 模式。
- `/api/` 反代到后端 `8083`，保留 `/api/v1/...` 路径。
- `/uploads/` 同样反代到后端，菜品图片可直接显示。
- `index.html` 不缓存，带 hash 的 `assets/` 长缓存。
- 上传接口需要放开 `client_max_body_size`（示例为 10MB）。

如果 Nginx 与后端不在同一台机器，把 `upstream canteen_api` 的地址改为后端内网地址；不要把数据库或 Redis 端口暴露到公网。

## 前后端约定

- 接口前缀 `/api/v1`，响应体为 `Result<T>{ code, message, data, traceId, timestamp }`。
- 业务异常返回 **HTTP 200 + `code != 200`**，前端统一弹出 `message`，并抛出 `BizError`。
- 无 token 返回 HTTP 401，前端使用独立 Axios 实例做单飞 refresh，成功后重放原请求。
- 分页响应为 `PageResult<T>{ total, pages, pageNum, pageSize, list }`。
- 菜品封面返回 `/uploads/...` 相对地址，开发由 Vite 代理，生产由 Nginx 代理。
- 用户 `id` 为雪花 ID，后端以字符串序列化、前端 `UserVO.id` 使用 `string`，避免 JS Number 精度丢失。

## 档口数据权限

- 员工看板调用 `GET /staff/stalls` 展示“我的档口”；订单由后端按 `staff_stall` 绑定过滤，前端不承担权限判断。
- 管理员在用户管理中点击“档口权限”，弹窗勾选多个档口后调用 `PUT /admin/users/{id}/stalls` 整体保存；不勾选表示收回全部档口。
- 员工未绑定档口时看板显示警告且列表为空；跨档口操作由后端返回 `40206`。

## 已验证

- `npm run type-check` 通过。
- `npm run build` 通过，产物输出到 `dist/`。
- 管理员用户管理“档口权限”弹窗实测：3 个档口完整显示，`staff01` 默认勾选档口 1，保存后提示成功并关闭，无接口错误。
- 员工看板实测显示“我的档口 / 一食堂·家常菜”，订单范围由后端绑定过滤。
