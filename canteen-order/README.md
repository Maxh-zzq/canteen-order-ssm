# 校园食堂网上订餐系统（canteen-order）

面向校园食堂的「在线点餐 + 到店取餐」后端服务，按企业单体应用标准实现，覆盖 **学生点餐 / 档口出餐 / 管理后台** 三条业务线。

- 统一 REST 返回、全局异常处理、TraceId 链路追踪
- JWT 双 Token（Access 30 分钟 / Refresh 7 天）+ Redis 会话撤销
- MySQL 持久化 + Redis 缓存/购物车/库存预扣/幂等/延迟队列
- 订单状态机、取餐码核销、余额支付与模拟网关回调、退款
- 菜品/菜单/库存管理、评价、营业统计、CSV 导出、操作日志
- Flyway 自动建表与种子数据，首次启动即可用

---

## 1. 技术栈

| 领域 | 选型 | 版本 |
|---|---|---|
| JDK | JDK 11（编译、运行统一） | 11.0.23 |
| 构建 | Maven | 3.9.x |
| 框架 | Spring Boot | 2.7.18 |
| Web | Spring MVC + Validation | 随 Boot |
| 安全 | Spring Security + jjwt | 5.7 / 0.11.5 |
| 持久层 | MyBatis-Plus | 3.5.3.2 |
| 数据库 | MySQL | 8.0.x |
| 连接池 | HikariCP | 随 Boot |
| 缓存 | Redis + Lettuce | 7.x |
| 迁移 | Flyway（含 flyway-mysql 模块） | 9.22.3 |
| 文档 | springdoc-openapi | 1.6.15 |
| 工具 | Lombok / commons-lang3 | 随 Boot |

> JDK 11 决定了 Spring Boot 只能用 2.7.x（Boot 3.x 要求 JDK 17+），因此没有采用 Boot 3。

---

## 2. 目录结构

```
canteen-order
├── docs/技术栈与实施计划.md
├── src/main/java/com/xatu/canteen
│   ├── CanteenApplication.java
│   ├── common/       Result、ResultCode、BizException、全局异常、TraceId、缓存、锁、限流、操作日志
│   ├── config/       Security、MyBatis-Plus、WebMvc、Jackson
│   ├── security/     JWT Provider/Filter、LoginUser、鉴权失败处理
│   ├── entity/       16 张表实体（含 staff_stall 档口绑定）
│   ├── mapper/       BaseMapper + 库存/统计自定义 SQL
│   ├── dto/ vo/      入参与出参
│   ├── service/      认证、菜品、菜单、库存、购物车、订单、支付、评价、统计、文件、后台
│   ├── controller/   11 个控制器，约 48 个接口
│   ├── job/          订单超时、每日菜单初始化
│   └── init/         启动时补齐演示账号与今日菜单
└── src/main/resources
    ├── application.yml        默认连接 182.92.130.2
    ├── application-local.yml  本机 MySQL 联调
    └── db/migration           V1 建表 / V2 种子数据 / V3 员工档口绑定
```

---

## 3. 快速启动

### 3.1 前置条件

- JDK 11，`JAVA_HOME` 指向 JDK 11
- Maven 3.6+
- MySQL 8 可连接，账号具备建库/建表权限
- Redis 可连接（本项目不设密码时留空即可）

### 3.2 打包

```powershell
cd "G:\Workspace\My Project\canteen-order-ssm\canteen-order"
$env:JAVA_HOME = "E:\Java_learing\JDK11"
& "G:\Maven\apache-maven-3.9.15\bin\mvn.cmd" clean package -DskipTests
```

产物：`target/canteen-order.jar`

### 3.3 启动

```powershell
# 默认配置：8083 端口，MySQL/Redis 均连 182.92.130.2
java -jar target\canteen-order.jar

# 本机 MySQL 联调：MySQL 走 127.0.0.1，Redis 仍连 182.92.130.2，端口仍为 8083
java -jar target\canteen-order.jar --spring.profiles.active=local

# 端口被占用时临时覆盖
java -jar target\canteen-order.jar --server.port=8084
```

首次启动 Flyway 自动执行 `V1__init_schema.sql`、`V2__seed_data.sql`、`V3__staff_stall.sql`，随后 `DataInitializer` 补齐演示账号和当天菜单。已有表结构时 Flyway 会校验版本并跳过。

### 3.4 访问入口

| 入口 | 地址 |
|---|---|
| Swagger UI | `http://localhost:8083/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8083/v3/api-docs` |
| 健康检查 | `http://localhost:8083/actuator/health` |
| 上传图片 | `http://localhost:8083/uploads/...` |

---

## 4. 服务器配置

`src/main/resources/application.yml` 默认连接：

| 项 | 值 |
|---|---|
| MySQL | `182.92.130.2:3306`，库 `canteen_order`，`root / 123456` |
| Redis | `182.92.130.2:6379`，无密码，DB 0 |
| 端口 | **8083** |

所有关键项都支持环境变量覆盖（未设置时使用上面的默认值），便于开发/测试/生产分离：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8083` | 服务端口 |
| `DB_URL` | 服务器 `canteen_order` 连接串 | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | 数据库账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` | `182.92.130.2` / `6379` / `0` | Redis |
| `JWT_SECRET` | 内置演示密钥 | 生产必须替换 |
| `PAY_SECRET` | 内置演示密钥 | 模拟支付签名密钥 |
| `CORS_ALLOWED_ORIGINS` | `*` | 前端域名，多个用逗号分隔 |
| `UPLOAD_DIR` / `LOG_FILE` | `./uploads` / `./logs/canteen-order.log` | 上传目录 / 日志文件 |

> 也兼容 Spring Boot 标准变量：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_REDIS_HOST` 等。
> 前后端分离部署时，建议把 `CORS_ALLOWED_ORIGINS` 收敛到前端实际域名，例如 `https://canteen.example.com`。

### 4.1 MySQL 远程访问授权（必须）

如果启动时报：

```
ERROR 1045 (28000): Access denied for user 'root'@'x.x.x.x'
```

说明 MySQL 只允许 `root@localhost` 登录。请在服务器上以管理员身份执行：

```sql
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456';
ALTER USER 'root'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

并确认 `my.cnf` / `mysqld.cnf` 中 `bind-address` 不是仅 `127.0.0.1`，云服务器安全组已放行 3306（建议只对办公出口 IP 放行）。

验证：

```powershell
& "E:\ee\MySQL\Install Directory\bin\mysql.exe" -h 182.92.130.2 -uroot -p123456 -e "SELECT @@version, CURRENT_USER();"
```

---

## 5. 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `123456` | ADMIN | 系统管理员：菜品/菜单/档口/用户/档口权限/日志/配置/统计/退款 |
| `staff01` | `123456` | STAFF | 一食堂员工：仅处理绑定档口（默认一食堂·家常菜）的接单、出餐、取餐核销 |
| `20230001` | `123456` | STUDENT | 学生（张同学），初始余额 100.00 |

密码由启动器用 BCrypt 现场生成，数据库中不存明文。

---

## 6. 核心接口

统一前缀 `/api/v1`，返回 `Result<T>{ code, message, data, traceId, timestamp }`。

| 模块 | 接口 |
|---|---|
| 认证 | `POST /auth/login`、`POST /auth/refresh`、`POST /auth/logout`、`GET /auth/captcha` |
| 用户 | `GET /users/me`、`PUT /users/me`、`GET /users/me/balance`、`POST /users/me/recharge` |
| 菜品 | `GET /categories`、`GET /dishes`、`GET /dishes/{id}`、`GET /dishes/{id}/reviews`、`GET /menus/today`、`GET /stalls` |
| 购物车 | `GET /cart`、`POST /cart/items`、`PUT /cart/items/{id}`、`DELETE /cart/items/{id}`、`DELETE /cart` |
| 订单 | `POST /orders`、`GET /orders`、`GET /orders/{orderNo}`、`POST /orders/{orderNo}/cancel` |
| 档口 | `GET /staff/orders`、`GET /staff/stalls`、`POST /staff/orders/{orderNo}/accept`、`.../ready`、`.../pickup?pickupCode=` |
| 支付 | `POST /payments/balance/{orderNo}`、`POST /payments/mock/callback`、`POST /payments/{orderNo}/refund` |
| 评价 | `POST /reviews` |
| 后台 | `GET /admin/categories`、`GET /admin/stalls`、`GET /admin/dishes`、`GET /admin/dishes/{id}`、`/admin/menus/publish`、`/admin/menus/{id}/stock`、`/admin/files/upload`、`/admin/users`、`GET/PUT /admin/users/{id}/stalls`、`/admin/logs`、`/admin/configs`、`/admin/stats/*`、`/admin/orders/export` |

权限：

- 无 Token 访问受保护接口 → HTTP 401
- 学生访问 `/admin/**` → HTTP 403
- 业务异常返回 HTTP 200 + 业务码（如 40205 取餐码错误），系统异常返回 HTTP 500

### 6.1 档口数据权限（STAFF）

采用“角色权限 + 数据域 + 资源归属”三层校验，不依赖前端隐藏：

1. **角色**：`STAFF` 只能访问 `/staff/**`；`ADMIN` 可访问全部后台接口。
2. **数据域**：`staff_stall(user_id, stall_id, status, effective_from, effective_to)` 维护员工可管理的档口，一个员工可绑定多个档口。`StaffStallService.activeStallIds()` 只取 `status=1` 且处于生效期的绑定。
3. **资源归属**：订单列表只返回绑定档口的订单；订单详情、接单、出餐、核销在状态流转前校验订单 `stall_id` 是否在当前员工有效绑定集合内。

行为约定：

| 场景 | 结果 |
|---|---|
| `STAFF` 已有绑定 | 只能看到/操作绑定档口订单 |
| `STAFF` 无任何绑定 | 列表为空；接单/详情等返回 `20202 当前员工未绑定档口` |
| `STAFF` 操作非绑定档口 | 返回 `40206 无权操作该档口订单` |
| `ADMIN` | 不受档口绑定限制，全量查看与操作 |

管理员接口：`PUT /admin/users/{id}/stalls`，请求体 `{ "stallIds": [1,2] }`；传空数组表示收回全部档口，仅允许给 `STAFF` 分配。绑定关系保留历史行，解绑时将 `status` 置 0，重新绑定复用原行。

---

## 7. 关键设计

### 7.1 订单状态机

```
10 待支付 ──支付──> 20 已支付 ──接单──> 30 备餐中 ──出餐──> 40 可取餐 ──核销──> 50 已完成
   │                    │
   │取消/超时            │退款
   ▼                    ▼
 60 已取消            70 已退款
```

所有流转统一走 `OrderService.transit()`：白名单校验 + `@Version` 乐观锁 + 写 `order_status_log`。

### 7.2 防超卖

1. 下单前 Redis Lua 原子预扣 `canteen:stock:menu:{menuId}`；
2. DB 执行 `UPDATE daily_menu SET stock_left = stock_left - n WHERE stock_left >= n`；
3. DB 扣减失败 → 回滚 Redis 并抛「库存不足」；
4. 取消/超时/退款 → DB 与 Redis 双向回补。

### 7.3 幂等

- 下单：`requestId` + Redis `SETNX`（30 分钟）+ 表唯一索引 `uk_user_request(user_id, request_id)`，重复提交返回同一订单。
- 支付回调：`payment_record.trade_no` 唯一索引，重复回调直接返回 `success`。
- 退款：`refund_record.refund_no` 唯一。

### 7.4 缓存

| Key | TTL | 说明 |
|---|---|---|
| `canteen:dish:category` | 30 分钟 | 分类列表 |
| `canteen:dish:detail:{id}` | 30 分钟 + 抖动 | 菜品详情 |
| `canteen:dish:null:{id}` | 60 秒 | 空值缓存防穿透 |
| `canteen:menu:today:{date}` | 60 秒 | 今日菜单（库存实时叠加） |
| `canteen:cart:{userId}` | 7 天 | 购物车 Hash |
| `canteen:auth:refresh:{userId}` | 7 天 | Refresh Token |
| `canteen:auth:black:{jti}` | 剩余有效期 | 登出黑名单 |
| `canteen:order:idem:{userId}:{requestId}` | 30 分钟 | 下单幂等 |
| `canteen:order:delay` | — | ZSet 延迟队列（15 分钟未支付取消） |

Redis 故障时降级：缓存未命中、库存走 DB、购物车走 DB、鉴权跳过会话校验；Refresh 接口 fail-closed。

---

## 8. 已验证链路（服务器 MySQL 8.0.46 + Redis 7.0.11，端口 8083）

- 服务器库 `canteen_order` 已完成 Flyway V1/V2/V3，16 张表 + 种子数据就绪
- 档口数据权限：`staff01` 仅见绑定档口 1；跨档口接单/详情返回 40206；无绑定列表为空且接单返回 20202；管理员全量可见，调整绑定后员工列表即时生效
- 登录 / 刷新轮换（旧 Refresh 返回 401）/ 登出黑名单（旧 Access 返回 401）
- CORS 预检通过（`Origin: http://localhost:5173` 返回对应 Allow-Origin 与 Allow-Credentials）
- 今日菜单 10 个菜品、库存与销量正确
- 加购（含规格加价）→ 下单 → 重复 `requestId` 返回同一订单 → 余额支付 → 接单 → 出餐 → 取餐码核销 → 评价
- 错误取餐码返回 40205，重复核销返回 40202
- 取消未支付订单，库存 57 → 59 正确回补
- 支付后管理员退款，余额 43.50 → 57.00，订单变「已退款」
- 后台新建菜品 → 发布今日菜单 → 学生端可见并可下单
- 图片上传、静态访问、CSV 导出（UTF-8 BOM，中文表头正常）
- 未登录 401、学生访问后台 403、统计概览/趋势/热销排行
- Redis 缓存二次命中正常（修复了列表反序列化泛型擦除问题）

---

## 9. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `Access denied for user 'root'@'...'` | MySQL 未授权远程 root | 执行 4.1 的 SQL，检查 bind-address/安全组 |
| `Unsupported Database: MySQL 8.0` | Flyway 缺 MySQL 模块 | 已引入 `flyway-mysql`，勿单独降级 Flyway |
| 8083 启动失败 | 端口被其他进程占用 | `--server.port=8084` 临时覆盖，或结束占用进程 |
| Redis 连接超时 | 服务器未放行 6379 / Redis 未启动 | 安全组放行，`redis-cli -h 182.92.130.2 ping` 应返回 PONG |
| 中文日志乱码 | Windows 控制台编码 | 不影响文件日志，Linux 部署无此问题 |
| 首次启动较慢 | Flyway 建表 + 初始化菜单 | 正常，看日志出现 `Started CanteenApplication` 即可 |
