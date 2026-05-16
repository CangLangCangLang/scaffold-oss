# Runbook

本文档记录本项目后续如何运行、启动和测试。

> 业务开发者更关心"功能怎么用"请看 [`FEATURES.md`](./FEATURES.md)；
> 想了解"接下来还能加什么通用能力"请看 [`ROADMAP.md`](./ROADMAP.md)。

## 项目结构

```
project/
├── backend/             Spring Boot 4 后端（多模块 Maven 工程）
├── frontend/            Vue 3 + Vite + TypeScript 前端
├── docker-compose.yml   一键拉起 mysql / redis / backend / frontend
├── scripts/             开发辅助脚本（PowerShell）
└── docs/                运维与设计文档
```

## 环境要求

- JDK 17+，本地已验证 JDK 21 可用
- Maven 3.9+
- Node 20 LTS（根目录 `.nvmrc` 已固定为 `20`）
- npm 10+
- MySQL 8，数据库名默认 `project`
- Redis 7，默认密码 `123456`
- 可选：Docker Desktop

## 配置文件

首次启动前复制示例环境变量：

```powershell
Copy-Item .env.example .env
```

常用配置项：

- `SERVER_PORT`：后端端口，默认 `9080`
- `FRONTEND_PORT`：前端 Nginx 端口，默认 `9081`
- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`：MySQL 连接
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`：Redis 连接
- `JWT_SECRET`：JWT 密钥，生产环境必须替换（长度 ≥ 64 字节）
- `LIQUIBASE_ENABLED`：是否启用 Liquibase，默认 `true`
- `SPRING_PROFILES_ACTIVE`：后端 Spring profile，默认 `druid`
- `VITE_APP_BASE_API`：前端 axios 基础前缀，默认 `/dev-api`

## 本地启动

确认 MySQL 和 Redis 已启动后，启动后端：

```powershell
cd backend
mvn clean install '-Dmaven.test.skip=true'
java -jar scaffold-admin\target\scaffold-admin.jar
```

启动前端（Vue 3）：

```powershell
cd frontend
npm install
npm run dev
```

默认监听 `http://localhost:9081`。Vite 会把以 `/dev-api` 开头的请求代理到 `http://localhost:9080`，可在 `frontend/.env.development` 中通过 `VITE_BACKEND_ORIGIN` 调整后端地址。

也可以用脚本同时启动后端 + 前端：

```powershell
.\scripts\start-dev.ps1
```

如需同时拉起 MySQL / Redis 容器：

```powershell
.\scripts\start-dev.ps1 -WithInfra
```

如需启用 Quartz JDBC 持久化：

```powershell
.\scripts\start-dev.ps1 -UseQuartzJdbc
```

## 前端质量与构建

```powershell
cd frontend
npm run lint              # ESLint + Prettier
npm run type-check        # vue-tsc 严格模式
npm run build             # 生产构建，产物位于 frontend/dist
npm run build:report      # 生成 dist/stats.html 体积分析
npm run test              # Vitest 单元测试
npm run test:coverage     # 生成 coverage/ 报告
npm run test:e2e:install  # 首次执行前安装 Playwright 浏览器
npm run test:e2e          # Playwright 端到端烟测（自动启动 preview）
npm run gen:openapi       # 拉取后端 /v3/api-docs 生成 src/types/openapi.d.ts
```

CI（`.github/workflows/ci.yml`）会执行 lint → type-check → unit test → build；PR 还会触发 Playwright 烟测。

## Docker Compose 启动

```powershell
Copy-Item .env.example .env
docker compose up --build
```

默认访问地址：

- 前端（Nginx + Vue 3）：`http://localhost:8081`
- 后端：`http://localhost:8080`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

只启动基础设施：

```powershell
docker compose up -d mysql redis
```

## 数据库迁移

项目使用 Liquibase 管理数据库初始化和后续变更。

主 changelog：

```text
backend/scaffold-admin/src/main/resources/db/changelog/db.changelog-master.yml
```

初始化 SQL：

```text
backend/sql/scaffold_20260320.sql
backend/sql/quartz.sql
```

说明：

- 新数据库启动后由 Liquibase 自动初始化基础表和 Quartz 表
- 已初始化过的数据库通过前置条件将 baseline 标记为已执行，避免破坏现有数据
- 新增表结构变更时，优先新增 Liquibase changeset，不要直接修改历史 SQL

## 测试与验证

后端构建：

```powershell
cd backend
mvn clean install '-Dmaven.test.skip=true'
```

后端单元测试：

```powershell
cd backend
mvn -pl scaffold-admin -am test
```

前端构建：

```powershell
cd frontend
npm ci --no-audit --no-fund
npm run build
```

前端单元测试与 E2E：

```powershell
cd frontend
npm run test           # Vitest 单元测试
npm run test:coverage  # 覆盖率报告 -> coverage/
# 端到端烟测（首次执行需安装浏览器二进制）
npm run test:e2e:install
npm run test:e2e
```

品牌残留扫描：

```powershell
python scripts\check-brand.py
```

接口连通性验证：

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/captchaImage" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:8081/dev-api/captchaImage" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -UseBasicParsing
```

Liquibase 验证：

```powershell
python -c "import pymysql; c=pymysql.connect(host='localhost',user='root',password='password',database='project'); cur=c.cursor(); cur.execute('select count(*) from DATABASECHANGELOG'); print(cur.fetchone()[0]); c.close()"
```

启动期 Liquibase changelog 校验（CI / 灰度强烈建议开启）：

```powershell
$env:LIQUIBASE_VALIDATE_ON_STARTUP="true"
java -jar backend\scaffold-admin\target\scaffold-admin.jar
```

## 常见问题

### PowerShell 中 Maven 参数报错

PowerShell 需要给 `-Dmaven.test.skip=true` 加引号：

```powershell
mvn clean install '-Dmaven.test.skip=true'
```

### `frontend-next/` 残留目录

Vue 3 重写工程历史上叫 `frontend-next/`，已合并到 `frontend/`。如果在仓库根目录看到一个空的 `frontend-next/` 文件夹（IDE 的 TypeScript 服务可能锁住该目录），可重启 IDE 后手动删除，或忽略它（已加入 `.gitignore`）。

### Docker 命令不可用

如果提示 `docker` 不是可识别命令，请先安装并启动 Docker Desktop，再执行 Docker Compose 命令。

## 可观测性

后端默认开启 Spring Boot Actuator，并暴露 Prometheus 抓取端点：

| 端点 | 用途 |
| --- | --- |
| `/actuator/health` | 存活/就绪探针，已配置 `liveness` / `readiness` + `scaffoldModules` 子组件 |
| `/actuator/info`   | 应用元信息 |
| `/actuator/metrics` | Micrometer 指标列表 |
| `/actuator/prometheus` | Prometheus 抓取端点（含 JVM / HTTP / DataSource / Redis / `scaffold.business.rows`） |
| `/actuator/loggers` | 动态调整日志级别（需鉴权） |
| `/actuator/scaffold-modules` | 当前已启用业务模块清单（`name` / `version` / `description` / `enabled`） |
| `/actuator/health/scaffoldModules` | 同上但走 health endpoint，便于 K8s probe 链 |

启动时附加 `--spring.profiles.active=druid,json` 将切换为结构化 JSON 日志（带 `traceId`、`app` 字段），方便 ELK / Loki 摄取。

### Q-3 慢请求 / 错误请求录入 + 告警 inbox

`scaffold-framework` 自带 `HttpRequestRecorder` Filter — 默认开启，超过 `app.observability.slow-ms`（默认 1000ms）或 5xx 响应自动落 `sys_slow_request` 表（含 traceId / username / clientIp / reason / exceptionMsg）；4xx 默认不录入，可通过 `app.observability.record-client-error=true` 开启。

`SlowApiAlertJob`（`sys_job 9001`，cron `0 */5 * * * ?`）每 5 分钟扫一次窗口（默认 10 min）内的 `alerted=0` 记录，按 reason 分组发 inbox（`observability.slow_request` / `observability.server_error`）给 `app.observability.alert-recipients`（默认 admin，逗号分隔多人）。inbox 模块未启用时仅落表 + 标 alerted（防死循环）。

后台前端访问菜单 9001 "可观测性"：
- 9002 慢请求列表 — 列表 + filter / purge / 立即扫描 / 单删
- 9003 业务指标 — Prometheus 文本解析展示 `scaffold.business.rows` Gauge + JVM + HTTP TOP10
- 9004 健康检查 — `/actuator/health` components + `/actuator/scaffold-modules` 模块清单

E2E：`backend/scripts/verify-observability.ps1`（10 步）。

## 限流与幂等

| 注解 | 作用 | 关键参数 |
| --- | --- | --- |
| `@Idempotent` | Redis 分布式幂等 | `key`(SpEL)、`expire` |
| `@RateLimit`  | Redis Lua 固定窗口限流 | `count`、`period`、`limitType=DEFAULT/IP/USER` |
| `RedisLockTemplate` | 分布式锁工具 | `tryAcquire` / `runWithLock` |

示例：

```java
@RateLimit(count = 20, period = 60, limitType = RateLimit.LimitType.IP, message = "请求过于频繁")
@PostMapping("/captcha")
public R<?> captcha() { ... }
```

## 文件存储

通过 `file.storage.type` 选择实现：

- `local`（默认）：写入 `${scaffold.profile}` 或 `file.storage.local-root`
- `s3`：S3 / MinIO / 阿里云 OSS / 腾讯云 COS 等兼容服务，参数：

```yaml
file:
  storage:
    type: s3
    s3:
      endpoint: http://minio:9000
      region: us-east-1
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
      bucket: scaffold
      path-style: true
      public-url: https://cdn.example.com/scaffold
```

业务侧只需注入 `FileStorageService`，存储实现完全透明。

## 生产环境建议

- 设置强随机 `JWT_SECRET`，并保证长度 ≥ 64 字节
- 关闭 Druid 控制台或设置强密码（参见 `DRUID_STAT_VIEW_ENABLED`）
- 使用 `SPRING_PROFILES_ACTIVE=druid,prod,json`，开启结构化日志便于聚合
- 根据部署模式选择是否启用 `quartz-jdbc`
- 文件上传切换到 `file.storage.type=s3`，并启用 CDN
- 配置 `cors.allowed-origins` 仅允许实际使用的前端域名
- 部署时打开 `LIQUIBASE_VALIDATE_ON_STARTUP=true`，让启动期主动校验 changelog
- 使用裁剪后的运行时镜像：`docker build -f backend/Dockerfile -t scaffold:latest .`（已使用 jlink 自定义运行时，体积 ≈ 200MB）

## 结构化错误码

后端统一定义在 `com.scaffold.common.constant.BizCode`，每个枚举值含三要素：

- `httpStatus`：与 `HttpStatus` 兼容（401/403/409/429/500/...）
- `errorKey`：稳定字符串（如 `BIZ_RATE_LIMITED`），前端基于此做提示/跳转，比 code 与文案更稳定
- `defaultMessage`：默认中文文案，i18n 缺失时回退

抛错时统一使用 `BizException`：

```java
if (!isAllowed) throw new BizException(BizCode.RATE_LIMITED);
if (notFound)  throw new BizException(BizCode.RESOURCE_NOT_FOUND, "用户 " + id + " 不存在");
```

`GlobalExceptionHandler` 会查 `messages_<locale>.properties` 中以 `errorKey` 为 key 的文案；前端 `frontend/src/utils/errorCode.ts` 的 `ERROR_KEY_I18N` 与之一一对应，配合 Vue I18n 实现一处错误码、双端可读。

新增错误码流程：

1. 在 `BizCode` 增加枚举值
2. 在 `messages.properties` / `messages_en_US.properties` 增加同名 key 的文案
3. 在前端 `src/locales/zh-CN.ts` / `en-US.ts` 增加 `errors.biz.xxx`
4. 在 `frontend/src/utils/errorCode.ts` 的 `ERROR_KEY_I18N` 增加映射

## 字段脱敏（@SensitiveLog）

针对响应 DTO 与操作日志参数，提供 Jackson 注解级别脱敏：

```java
public class UserVO {
    @SensitiveLog(strategy = SensitiveStrategy.MOBILE)
    private String phone;

    @SensitiveLog(strategy = SensitiveStrategy.ID_CARD)
    private String idCard;

    @SensitiveLog(strategy = SensitiveStrategy.CUSTOM, prefixKeep = 3, suffixKeep = 4)
    private String token;
}
```

可选策略：`DEFAULT / CHINESE_NAME / MOBILE / FIXED_PHONE / EMAIL / ID_CARD / BANK_CARD / ADDRESS / PASSWORD / CUSTOM`。

## 缓存抽象（CacheTemplate）

注入 `CacheTemplate` 即可获得 get-or-load 语义、缓存击穿（Redis SET NX EX 串行化回源）与缓存穿透（NULL 占位）防御：

```java
User user = cacheTemplate.getOrLoad(
        "user:" + id,
        Duration.ofMinutes(10).toSeconds(), TimeUnit.SECONDS,
        () -> userMapper.selectById(id));
```

并发场景：未持锁的请求最长等待 `lockTtlSeconds`，期间持锁线程写完缓存后立刻返回；超时则降级回源。生产建议 `lockTtlSeconds` 略大于 loader 的 P99。

## API 版本路由（@ApiVersion）

`@ApiVersion(n)` 标在 Controller 类或方法上，与请求路径中的 `/v{n}/` 协同：

```java
@RestController
@RequestMapping("/v{version}/order")
@ApiVersion(1)
public class OrderController {
    @GetMapping("list") public R<?> listV1() { ... }

    @ApiVersion(2)
    @GetMapping("list") public R<?> listV2() { ... }
}
```

客户端请求 `/v2/order/list` 命中 v2，请求 `/v1/order/list` 命中 v1；高于声明版本的请求自动落到最高匹配版本（向上兼容）。便于负载逐步升版、A/B 切流。

## Quartz 集群部署专节

启用前提：

- 数据库开启 `application-quartz-jdbc.yml`（`spring.profiles.active=...,quartz-jdbc`）
- 使用同一份 `QRTZ_*` 表（已包含在 `backend/sql/quartz.sql` 与 Liquibase changelog）
- 节点之间 **时钟必须同步**（建议 NTP / chrony，时差 < 5s）

集群核心参数（已在 `application-quartz-jdbc.yml` 中默认配置好）：

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `org.quartz.scheduler.instanceName` | `ScaffoldScheduler` | 集群名，必须所有节点一致 |
| `org.quartz.scheduler.instanceId` | `AUTO` | 让每个节点自动生成唯一 ID |
| `org.quartz.jobStore.isClustered` | `true` | 启用集群模式 |
| `org.quartz.jobStore.clusterCheckinInterval` | `15000` | 心跳间隔（ms），过短会增加 DB 压力 |
| `org.quartz.jobStore.misfireThreshold` | `12000` | 任务漏触发判定阈值 |
| `QUARTZ_THREAD_COUNT` | `20` | 单节点工作线程数，按 CPU/任务数调整 |

部署清单：

1. 准备至少 2 个对等节点（同一 Spring profile、同一 `JWT_SECRET`、同一数据库与 Redis）
2. 各节点启动命令与单机一致；调度器读取 DB 行锁选主，节点宕机后由其他节点自动接管未完成的 JobDetail
3. 在反向代理（Nginx / SLB）上做无状态轮询，对 HTTP API 使用 Redis 共享 token，支持随机命中任一节点
4. 监控：
   - Actuator `/actuator/quartz`（Boot 已自动暴露 health 和 metrics）
   - 自定义 SQL：`SELECT INSTANCE_NAME, LAST_CHECKIN_TIME FROM QRTZ_SCHEDULER_STATE`，长时间未心跳的实例需排查
   - Prometheus：`spring.quartz.jobs.executed`, `spring.quartz.jobs.errored` 等指标
5. 滚动发布：每次只下线一个节点；重启前可执行 `Scheduler#standby()` 让其停止接受新任务，等待当前 Job 完成后退出
6. 数据库要求：`QRTZ_LOCKS` 表行锁竞争激烈，建议 RDS 启用合适的 InnoDB 行锁监控；高频任务（< 1s）不建议走 Quartz，转推到 Redis Stream/MQ
7. 故障排查清单：
   - 任务不触发：查 `QRTZ_FIRED_TRIGGERS`、`QRTZ_TRIGGERS.NEXT_FIRE_TIME`
   - 重复触发：检查节点时间是否漂移、`isClustered` 是否所有节点都为 true
   - 启动失败：检查 changelog 中 Quartz 表是否同版本（`org.quartz.jobStore.tablePrefix=QRTZ_`）

切换到单机模式：在 `application-quartz-jdbc.yml` 设置 `org.quartz.jobStore.isClustered=false`，**仅** 用于调试，生产禁用。

## WebSocket / SSE 推送总线

在线消息能力以 STOMP over WebSocket 实现，多实例通过 Redis Pub/Sub 做 fan-out，无需 sticky session。

后端组件：

- `WebSocketStompConfig` - 注册 `/ws` 端点（SimpleBroker：`/queue`, `/topic`），用户前缀 `/user`
- `JwtHandshakeInterceptor` - 握手时校验 `?token=` 或 `Authorization`，复用 `TokenService`
- `AuthHandshakeHandler` + `StompPrincipal` - 给会话绑定登录用户名，使 `convertAndSendToUser` 精确路由
- `MessagePublisher` 接口 + `RedisMessageBus` 实现 - 发到 Redis 频道，每个节点订阅该频道并通过本节点的 `SimpMessagingTemplate` 投递
- REST：`POST /system/message/user`、`POST /system/message/topic`（需要 `system:message:push` 权限）

业务侧使用：

```java
@Autowired MessagePublisher publisher;

publisher.toUser("alice", "order.shipped", Map.of("orderId", 123));
publisher.toTopic("system.broadcast", "maintenance", Map.of("eta", "2026-05-10 02:00"));
```

前端：

- `frontend/src/utils/websocket.ts` 提供 `WebSocketBus`，自动重连、自动重订阅、握手时携带 token
- `frontend/src/stores/notification.ts` Pinia store 缓存最近 100 条消息、未读计数
- TopBar 上的铃铛即时显示连接状态、未读数、消息列表

开发模式下 Vite 已配置 `/ws` 反向代理到 `VITE_BACKEND_ORIGIN`；生产 Nginx 需要：

```nginx
location /ws {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 600s;
    proxy_send_timeout 600s;
}
```

环境变量：

- `WEBSOCKET_ALLOWED_ORIGINS`：握手 Origin 白名单，生产建议显式指定（默认 `*`）
- `WEBSOCKET_BUS_CHANNEL`：Redis Pub/Sub 频道（默认 `scaffold:ws:bus`）

如需 SSE 备选通道（兼容老旧浏览器），可在 STOMP 端点上加 `.withSockJS()` 或额外 `EventSource` 控制器。本仓库默认使用纯 WebSocket，包体最小。

## OAuth2 / OIDC SSO（Authorization Code）

- 仅作客户端：脚手架自身不实现 IDP；只要对方支持标准 OAuth2 / OIDC，就能配置接入。
- **完全可选**：不在 `spring.security.oauth2.client.registration.*` 配置 IDP 时，前端登录页不显示按钮，后端不挂载 oauth2Login filter，相当于该能力关闭。
- 关键路径：`/oauth2/authorization/<id>` 入口、`/login/oauth2/code/<id>` 回调、`/sso/callback` 前端落 token、`/sso/providers` 列出按钮。
- 表：`sys_user_external_identity`，唯一键 `(provider, subject)`。
- 自动开户：`sso.auto-provision=true` 时按 IDP 返回的 `email` 找本地账号，找不到就用 IDP 的 `preferred_username` 自动建用户（默认 disabled，需管理员审核启用）。
- 详见 [`FEATURES.md` §11](./FEATURES.md#11-oauth2--oidc-sso)。

## 离线消息盒（Message Inbox，可插拔模块）

- 是脚手架第一个**可插拔业务模块**：`backend/scaffold-module-inbox` + `frontend/src/modules/inbox`。删整目录即下线。
- 与 WebSocket 推送总线协同：业务调用 `MessagePublisher.toUser(...)` 时**先入库**再 fan-out，用户上线 / 重连后由前端拉未读 + ACK。
- 表：`sys_message_inbox`，`(scope, target, message_id)` 唯一去重。
- REST：`/system/inbox/unread`、`/system/inbox/unread-count`、`/system/inbox/{id}/ack`、`/system/inbox/ack-all`。
- 清理任务：默认 `0 30 3 * * ?` 每天 03:30 跑，TTL 7 天 / 过期后保留 30 天物理删除，全部可配置。
- 关停：`app.module.inbox.enabled=false`（保留 jar）；卸载：删模块目录 + admin 依赖 + 主 changelog include；如需清表运行 `message_inbox-uninstall.sql`。
- 详见 [`FEATURES.md` §12](./FEATURES.md#12-离线消息盒)。

## 可插拔模块（Modular Loading）

- 后端发现：`/actuator/scaffold-modules`（需在 `management.endpoints.web.exposure.include` 中包含 `scaffold-modules`，默认已加）。
- 启动日志会一次性打印加载的所有模块及版本。
- 命令速查：

| 操作 | 命令 |
|------|------|
| 看启用了哪些模块 | `curl -u admin:... http://host/actuator/scaffold-modules` |
| 关停某模块 | 设置 `app.module.<name>.enabled=false` 后重启 |
| 检查 framework 没有泄露依赖 | 注释掉 admin pom 中模块依赖，`mvn -pl scaffold-admin -am -DskipTests compile` 应仍成功 |

- 创建新模块的最小骨架与样板见 [`FEATURES.md` §13](./FEATURES.md#13-可插拔模块加载约定-b-1b-2b-3)。

## 操作审计（@AuditLog / sys_audit_log）

- 注解 + 切面落 `sys_audit_log` 表；与 `@Log` / `sys_oper_log` 互补，关键写操作建议两个都挂。
- 全文档见 [`FEATURES.md` §14](./FEATURES.md#14-操作审计-auditlog--sys_audit_log)。
- 命令速查：

| 操作 | 命令 |
|------|------|
| 看某用户最近操作 | `SELECT * FROM sys_audit_log WHERE actor='alice' ORDER BY id DESC LIMIT 50;` |
| 看某资源被改了几次 | `SELECT * FROM sys_audit_log WHERE resource_type='user' AND resource_id='12' ORDER BY id;` |
| 列出失败的关键操作 | `SELECT * FROM sys_audit_log WHERE status=1 AND module LIKE 'system.%' ORDER BY id DESC;` |
| 清理 180 天前 | `DELETE /system/audit/log/older?retainDays=180`（前端列表页"清理旧数据"按钮） |
| 关闭审计（紧急回滚） | 暂时把目标方法上的 `@AuditLog` 注释掉重启；表保留以便后期重启 |
| 容量评估 | 单条平均 ~2-4KB（含 before/after），10万条约 200-400MB。生产建议加月分区或归档冷数据 |

- 安全约束：审计是合规底线，**不要**把它做成可被业务关闭的开关；要降级也只关业务方法上的注解，不关基础设施。
- 写入异常默认吞掉 + warn 日志，不阻断业务；想要"审计失败 = 业务失败"的高合规模式，改 `SysAuditLogServiceImpl.record()` 抛出异常即可（默认不开）。

## 工作流模块（Flowable 8）

- 全文档见 [`FEATURES.md` §15](./FEATURES.md#15-工作流模块m-1可插拔-flowable-8)，增强第一批见 [§15.1–15.3](./FEATURES.md#工作流增强已合入)，第二批见 [§15.5–15.7](./FEATURES.md#155-任务级动态表单task-form)。
- 命令速查：

| 操作 | 命令 |
|------|------|
| 看工作流是否启用 | `curl -u admin:... http://host/actuator/scaffold-modules` 看 `workflow` 是否在列表 |
| 临时关停模块 | 设置 `app.module.workflow.enabled=false` 重启；ACT_* 表保留 |
| 永久卸载 | 跑 `scaffold-module-workflow/.../sql/workflow-uninstall.sql`，再删 jar 依赖 / 模块目录 |
| 看正在运行的实例 | `SELECT ID_, NAME_, BUSINESS_KEY_, START_USER_ID_, START_TIME_ FROM ACT_RU_EXECUTION WHERE PARENT_ID_ IS NULL;` |
| 看待办任务总数 | `SELECT COUNT(*) FROM ACT_RU_TASK;` |
| 看部署列表 | `SELECT ID_, NAME_, DEPLOY_TIME_ FROM ACT_RE_DEPLOYMENT ORDER BY DEPLOY_TIME_ DESC;` |
| 看实例运行时态（active/completed/rejected） | `curl -H "Authorization: Bearer $TOKEN" http://host/workflow/process/instances/{id}/state` |
| 看实例时间轴 | `curl -H "Authorization: Bearer $TOKEN" http://host/workflow/process/instances/{id}/timeline` |
| 看启用中的启动表单 schema | `SELECT id, version, enabled FROM wf_form_schema WHERE process_definition_key='leave' AND activity_id='__START__' ORDER BY version DESC;` |
| 看启用中的任务表单 schema | `SELECT id, version, enabled FROM wf_form_schema WHERE process_definition_key='leave' AND activity_id='Task_Approve' ORDER BY version DESC;` |
| 看某实例所有前加签子任务 | `SELECT t.ID_, t.NAME_, t.ASSIGNEE_ FROM ACT_RU_TASK t JOIN ACT_RU_TASK_VAR v ON v.TASK_ID_=t.ID_ WHERE v.NAME_='scaffoldPreSignOriginTaskId' AND t.PROC_INST_ID_=...;` |
| 强制把某任务从前加签阻塞中解除 | `DELETE FROM ACT_RU_TASK_VAR WHERE NAME_='scaffoldBlockedByTaskIds' AND TASK_ID_=...;`（运营兜底，仅用于"前加签子任务被人为删表 / 卡死"时的回滚）|
| 调用接口撤销前加签 | `curl -X DELETE -H "Authorization: Bearer $TOKEN" http://host/workflow/task/{childTaskId}/add-sign-before`（仅本子任务发起人或 admin 可撤；deleteTask + 摘父 scaffoldBlockedByTaskIds + history 打 cancelled=true）|
| 实例分页查询（admin 视角） | `curl -H "Authorization: Bearer $TOKEN" "http://host/workflow/process/instances?processDefinitionKey=leave&status=running&pageNum=1&pageSize=20"`（非 admin 自动按 startedBy=current 过滤）|
| 强制把退回标记清掉 | `DELETE FROM ACT_RU_VARIABLE WHERE NAME_='scaffoldRejectedActivityIds' AND PROC_INST_ID_=...;` |
| 强制清理无主任务 | （慎）`DELETE FROM ACT_RU_TASK WHERE ASSIGNEE_ IS NULL AND CREATE_TIME_ < ...;` 只在引擎 bug 复盘时使用 |
| E2E 回归（全部增强） | `powershell -ExecutionPolicy Bypass -File backend/scripts/verify-workflow-enhancements.ps1`（要求 mysql + redis + scaffold-admin 起着；覆盖前加签 / 撤销 / 时间轴 / 实例分页）|

- 数据库初始化：首次启动 Flowable 引擎自动建 30+ 张 `ACT_*` / `FLW_*` 表（`flowable.database-schema-update=true`）。生产稳定后建议改 false 让运维主动 DDL。
- 表单 schema 表 `wf_form_schema`：项目自有表，由 Liquibase 管理（changeset `workflow-20260506-form-schema`），不进入 Flowable 引擎管理。每次保存 version+1，旧版本自动 enabled=0；如需"完全回退到 v3"，更新对应行的 `enabled=1` 同时把后续版本置 0 即可。activity_id 取 `__START__` 表示启动表单，取 BPMN userTask id（如 `Task_Approve`）表示任务表单。
- 前加签元数据：靠 task local var 而非新建表。
  - `scaffoldPreSignOriginTaskId`（子任务 local var）：指向被前加签的原任务 id。
  - `scaffoldBlockedByTaskIds`（原任务 local var，List）：被哪些子任务阻塞；列表为空时变量自动清除，原任务即可继续提交。
  - 流程级 `scaffoldAddSignBeforeHistory`（runtime / history variable）：仅作时间轴数据源 + 撤销标记（追加 `cancelled=true / cancelledBy / cancelledAt`），不影响阻塞判定。
- 撤销前加签兜底（极端情况：API 撤销失败 / 历史数据脏，需要 DBA 直改）：
  ```sql
  -- 替换 t-child 为子任务 id，t-parent 为父任务 id
  DELETE FROM ACT_RU_TASK WHERE ID_ = 't-child';
  DELETE FROM ACT_RU_VARIABLE WHERE TASK_ID_ = 't-parent' AND NAME_ = 'scaffoldBlockedByTaskIds';
  ```
  正常流程优先走 `DELETE /workflow/task/{childTaskId}/add-sign-before`，兜底 SQL 只在引擎报错时使用。
- 历史归档：`flowable.history-level=audit`（默认）保留任务级历史，能查询"已办"。改 `full` 会同时保留所有变量历史，存储倍增；`activity` / `none` 减历史但牺牲可追溯性。
- 推送联动：任务到达 / 完成 / 抄送 / 后加签 / 前加签时 `TaskNotifyEventListener` + `WorkflowFacade` 自动推送站内信（`workflow.task.created/completed/cc/addsign/addsign.before`）。如要关闭推送，让 `MessagePublisher` 不可注入即可（不需要改工作流模块代码）。
- 容量评估：单审批流程实例 ~5 张表写入约 1KB；100 万实例约 1GB。`ACT_HI_*` 历史表是大头，长期运行建议按月归档/分区。`wf_form_schema` 一般不超过几百行（按流程定义数量计）。
- 时间轴常见问题：
  - **缺 `process.end` / `task.complete` 比预期少 1**：通常是流程仍有 active task；用 `state` 端点确认还有活动节点即可；只有 `historicProcessInstance.endTime != null` 时才有 `process.end`。
  - **`task.addsign.before` 数量异常**：检查 `scaffoldAddSignBeforeHistory` 是否被业务侧手动 `removeVariable`；时间轴只反映该变量的当前值，删了就消失。
  - **任务批注 (`task.comment`) 缺失**：Flowable 默认不存批注（只在 `addComment` 时落 `ACT_HI_COMMENT`）；如果某些客户端走了不传 comment 的 complete，时间轴看不到内容。

## 数据级权限（@DataScope）

- 全文档见 [`FEATURES.md` §16](./FEATURES.md#16-数据级权限data-scope-已就绪--默认接入审计列表)。三步快速接入指南：domain 继承 BaseEntity → service 加 `@DataScope(deptAlias=, userAlias=)` → mapper.xml 加 LEFT JOIN + `${params.dataScope}` 占位。
- 命令速查：

| 操作 | 命令 |
|------|------|
| 给某角色查实际数据范围 | `SELECT role_id, role_name, data_scope FROM sys_role WHERE role_id = 100;`（参考 `DATA_SCOPE_*` 常量：1=全部 / 2=自定义 / 3=本部门 / 4=本部门及以下 / 5=仅本人）|
| 看某自定义范围角色绑定的部门 | `SELECT * FROM sys_role_dept WHERE role_id = 100;` |
| 用脚本端到端验证（admin / 部门角色 / 仅本人三种）| `powershell -ExecutionPolicy Bypass -File backend/scripts/verify-data-scope.ps1` |
| 给某用户加最强 admin 角色（绕过过滤）| 把用户绑到 `role_id=1`（脚手架默认 super admin）；`SysUser.isAdmin()` 走短路逻辑 |
| 给老业务表补 actor_dept_id 回填 | 参考 `sys_audit_log_dept_id_backfill.sql`：`UPDATE biz a LEFT JOIN sys_user u ON u.user_name = a.creator SET a.creator_dept_id = u.dept_id WHERE a.creator_dept_id IS NULL;` |
| 临时禁用某接口的数据过滤 | 把该 service 上的 `@DataScope` 注解注释掉重启（不要在生产临时切，要走变更流程）|

- 注意事项：
  - **mapper.xml 必须用 `${params.dataScope}` 而不是 `#{...}`**：JDBC 参数化会把 SQL 当字符串转义。这与 RuoYi 体系约定一致。
  - **第一参数必须是 BaseEntity 子类**：切面只看 `joinPoint.getArgs()[0].getParams()`。控制层用散参时，要么改成 query 对象，要么自己包一层 wrapper。
  - **多角色取并集**：用户绑多个角色时数据范围合并，命中"全部"短路。配置时不必担心冲突。
  - **历史数据隔离**：新接入数据权限后，老数据 `xxx_dept_id IS NULL` 默认查不到；写一次 backfill changeset 就能让"按部门"范围立刻生效。

## CMS 内容管理（M-3 模块）

- 全文档见 [`FEATURES.md` §17](./FEATURES.md#17-cms-内容管理m-3可插拔模块)。模块开关 `app.module.cms.enabled` 默认 true；删 `scaffold-module-cms` 目录与 admin/pom 依赖即彻底下线。
- 命令速查：

| 操作 | 命令 |
|------|------|
| E2E 端到端回归（栏目 + 4 篇文章打满状态机六分支 + 公开 API + 软删过滤） | `powershell -ExecutionPolicy Bypass -File backend/scripts/verify-cms.ps1` |
| 单测 | `mvn -pl scaffold-module-cms -am test "-Dtest=ArticleServiceTest,ChannelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` |
| 看当前文章状态分布 | `SELECT status, COUNT(*) FROM cms_article WHERE del_flag='0' GROUP BY status;` |
| 找某文章的所有审计事件 | `SELECT module, action, comment, status, created_at FROM sys_audit_log WHERE module LIKE 'cms.%' AND resource_id = '<articleId>' ORDER BY created_at DESC LIMIT 50;` |
| 把误状态强制改回 DRAFT 兜底 | `UPDATE cms_article SET status='DRAFT', updated_at=NOW() WHERE id=<id>;`（同时 `UPDATE` 关联文章保留 `published_at`，不会丢失首次发布时间）|
| 公开 API 烟测 | `Invoke-RestMethod -Uri "http://localhost:8080/cms/public/channels"` / `Invoke-RestMethod -Uri "http://localhost:8080/cms/public/articles?pageSize=5"` |
| 调整图片上传上限 | application.yml 加 `app.module.cms.upload.max-size-mb: 20`（默认 10）；扩展名白名单同 `app.module.cms.upload.allowed-extensions: jpg,png,webp,gif` |
| 卸载（清表 + 清菜单）| 跑 `backend/scaffold-module-cms/src/main/resources/db/changelog/sql/cms_uninstall.sql` |

- 富文本图片落到 `${ScaffoldConfig.profile}/cms/image/yyyyMM/<uuid>.<ext>`，对外通过框架已注册的 `/profile/**` 静态前缀直接访问；不需要改 nginx。
- 公开门户 API（`/cms/public/**`）走 `@Anonymous`，由 `PermitAllUrlProperties` 启动时扫到自动加白名单，不必改 SecurityConfig。
- 工作流接入：当前 `CmsWorkflowAdapter` 默认空实现；用 `app.module.cms.workflow.enabled=true` 开桥模块即接入 Flowable 真审批流（详见下面 M-4 章节 / FEATURES §17.5 / §17.9）。
- bundle 优化：访问"文章编辑"页时才下载 `vendor-wangeditor-*.js`（约 807KB / gzip 282KB）；不进编辑页主 bundle 不被拖大。

## CMS × Workflow 联动桥（M-4）+ CMS × Inbox 通知桥（M-5）

- 全文档见 [`FEATURES.md` §17.9](./FEATURES.md#179-m-4-cms--workflow-联动桥) / [§17.10](./FEATURES.md#1710-m-5-cms--inbox-通知桥)。
- 两个独立桥模块：
  - **M-4 `scaffold-module-cms-workflow`**：默认**关**，需 `app.module.cms.workflow.enabled=true` 显式开启。开后 CMS submit 走 Flowable 真审批流，关后 CMS 退回 M-3 自闭环状态机。
  - **M-5 `scaffold-module-cms-inbox`**：默认**开**，可用 `app.module.cms.inbox.enabled=false` 关闭。开后 CMS 状态变更（发布 / 驳回 / 下线）自动给作者发站内信 `cms.article.{published,rejected,unpublished}`。
- 命令速查：

| 操作 | 命令 |
|------|------|
| E2E 端到端回归（M-4 + M-5 联动，覆盖审批通过 / 驳回 / 下线 / 直接 approve cancelInstance） | `powershell -ExecutionPolicy Bypass -File backend/scripts/verify-cms-workflow.ps1` |
| 单测 M-4 桥（adapter 启动流程 + onApprove cancel + onReject cancel） | `mvn -pl scaffold-module-cms-workflow -am test "-Dtest=WorkflowAwareCmsAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false"` |
| 单测 M-4 CMS 回写（onWorkflowApprove / onWorkflowReject / submit adapter 接管路径） | `mvn -pl scaffold-module-cms -am test "-Dtest=ArticleServiceWorkflowCallbackTest" "-Dsurefire.failIfNoSpecifiedTests=false"` |
| 单测 M-5 桥（10 用例 a/b/c/d 触发表 + 容错） | `mvn -pl scaffold-module-cms-inbox -am test "-Dtest=ArticleStatusInboxListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` |
| 临时关 M-4 桥（CMS 退回自闭环）| application.yml 设 `app.module.cms.workflow.enabled: false`，重启后端；脚本会自动检测并走 CMS 直 approve/reject 路径 |
| 临时关 M-5 桥（不影响 CMS 状态机）| application.yml 设 `app.module.cms.inbox.enabled: false`，重启后端 |
| 看走过 workflow 的文章 | `SELECT id, title, status, process_instance_id FROM cms_article WHERE process_instance_id IS NOT NULL AND del_flag='0';` |
| 看 cms.article.* 类型的最新站内信 | `SELECT id, recipient, type, payload, created_at FROM message_inbox WHERE type LIKE 'cms.article.%' ORDER BY id DESC LIMIT 20;` |
| 看 BPMN 部署版本 | `Invoke-RestMethod -Uri "http://localhost:8080/workflow/process/definitions?defKey=cms_article_review" -Headers @{Authorization="Bearer $token"}` |
| 永久卸载 M-4 桥 | admin/pom.xml 删 `scaffold-module-cms-workflow` + 删 `backend/scaffold-module-cms-workflow/`；CMS 自动退回 M-3 自闭环（`process_instance_id` 列保留无害）|
| 永久卸载 M-5 桥 | admin/pom.xml 删 `scaffold-module-cms-inbox` + 删 `backend/scaffold-module-cms-inbox/`；CMS 仍发 `ArticleStatusChangedEvent`，仅无人订阅 |

- 桥模块独立可拆 / 互不影响：M-4 关 + M-5 开是合法配置（CMS 自闭环但仍发通知）；M-4 开 + M-5 关也合法（走真审批但不发通知）。
- M-5 触发条件特殊：需 `actor != author` 才发，否则视为"自己操作自己"跳过——E2E 脚本 provision `cms_author / Test@1234` 用户作为作者、admin 作为审批人来满足这个条件。
- M-4 自带 BPMN（`cms_article_review.bpmn20.xml`）启动时按 key+md5 智能部署：缺失或变更则部署新版，已有同 md5 则 skip；若运维想自定义流程，在 workflow 设计器里发一版同 key 的覆盖即可，脚手架不再回写。

## Swagger 查看 CMS API（M-3 / M-4 / M-5）

- 浏览器打开：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 顶部下拉切到 **`cms` (CMS 内容管理（M-3）)** 分组（默认分组 `default` 只含脚手架自带的"测试模块"）
- 6 个 Tag 分组：
  - `CMS 文章管理（后台）` —— `/cms/article` CRUD（5 个端点）
  - `CMS 文章状态机` —— `/cms/article/{id}/{action}` 6 个流转动作（submit / approve / reject / publish / unpublish / back-to-draft）
  - `CMS 栏目管理（后台）` —— `/cms/channel` CRUD + tree（6 个端点）
  - `CMS 标签字典` —— `/cms/tag` CRUD（5 个端点）
  - `CMS 富文本图片上传` —— `/cms/upload/image`（1 个端点）
  - `CMS 公开门户（匿名）` —— `/cms/public/**` 3 个端点（无需 token）
- 拿 OpenAPI JSON：`Invoke-RestMethod "http://localhost:8080/v3/api-docs/cms"`（可喂给 Postman / 自动化测试 / 客户端代码生成器）
- 对方法签名变动敏感的话，建议把 `/v3/api-docs/cms` 输出 commit 进仓库做 diff（脚手架本身不强制）。

## Inbox 全页面 / 批量操作（保养向 P2）

- 路由：`/system/message/inbox`（顶栏铃铛 popover 底部 → 「查看全部消息」一键跳转）
- 过滤维度：状态（未读 / 已读 / 已过期）、消息类型 LIKE、创建时间区间
- 批量动作：`POST /system/inbox/ack-batch` / `DELETE /system/inbox/batch`，**严格按当前登录用户限定**（payload 里的 `username` 字段被忽略，后端从 `SecurityUtils.getUsername()` 取）
- 默认排序：`status IN (0,1)`（未读+已读）→ `created_at DESC, id DESC`
- 索引：`idx_target_status_created (target, status, created_at)`（自动通过 Liquibase changelog 落地，重复执行时由 `preConditions` 保证幂等）

| 场景 | 速查 |
|------|------|
| 后端单测（16 用例覆盖 page / ackBatch / removeBatch / removeOne + 安全） | `mvn -pl scaffold-module-inbox -am test "-Dtest=MessageInboxServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` |
| Swagger 查 Inbox API | `http://localhost:8080/swagger-ui.html` 顶部切 `inbox (Inbox 收件箱)` 分组；OpenAPI JSON：`Invoke-RestMethod "http://localhost:8080/v3/api-docs/inbox"` |
| 看某用户最近收件箱 | `SELECT id, type, status, created_at FROM message_inbox WHERE scope='USER' AND target='admin' ORDER BY created_at DESC LIMIT 20;` |
| 验证跨用户隔离 | 用 a 用户登录在 InboxList 把 b 用户的消息 id 拼到 batch payload 里，应返回 `count=0`（service 层 `markBatchReadByIds` / `deleteBatchByIds` 都强制 `target=#{username}`） |

## 前端 i18n 切换（保养向 P5）

- 内置语言包：`frontend/src/locales/{zh-CN,en-US}.ts`（通用） + 各模块 `index.ts` 内嵌 namespace（loader 启动时 `mergeLocaleMessage` 合并到 i18n 全局）
- 切换：登录后顶栏齿轮 → 「语言」下拉，选项保留在 `localStorage[app.locale]`，刷新页面后续保持；或 SDK / 自动化场景下 `i18n.global.locale.value = 'en-US'`
- 三模块覆盖度（CMS / Inbox / Workflow）：用户可见硬编码中文 = **0 处**（已通过 `rg '[\u4e00-\u9fff]' --type vue` 在三模块下校验过；唯一残留是源码注释 / 第三方运行时控件如 wangEditor / form-create-designer / bpmn-js 的菜单文案——这些不走 vue-i18n）
- 缺 key 排查：开发模式 vue-i18n 会打印 `[intlify] Not found '<key>' key in '<lang>' locale messages`，建议在 dev console grep `intlify` 看新加的 .vue 是否漏接

| 场景 | 速查 |
|------|------|
| 全局 lint（保证新加 .vue 不带硬编码中文） | `npm run lint` —— ESLint `vue/no-deprecated-filter` + 通用规则 |
| TypeScript 检查（防止 i18n key 在 sub-namespace / scalar 间冲突） | `npm run type-check` —— TS1117 会在两端起同名 key 时直接报错 |
| 加新 i18n key | 在对应模块 `frontend/src/modules/<m>/index.ts` 的 `locales['zh-CN']` 与 `locales['en-US']` 同时加；若是跨模块通用，加到 `frontend/src/locales/{zh-CN,en-US}.ts` 的 `common.*` 下 |
| 后端模块 BPMN 内的任务名（如 `cms_article_review.bpmn20.xml` 里的"提交审批"） | **不走 vue-i18n**——是 Flowable 引擎数据；切换前端语言不会影响。如需多语言可以在 BPMN 设计器里维护多个 key（如 `cms_article_review_zh`、`cms_article_review_en`）并在 `WorkflowAwareCmsAdapter` 里按当前 locale 选择 |

## 通用文件上传（M-10 Pre-Phase）

- 接口：`POST /system/upload/file`（鉴权 `system:upload:file`，默认 admin 通配自动通过；其它角色给菜单 `5025` 即可）
- 入参 multipart/form-data：`file`（必填）+ `bucket`（可选，默认 `common`；约定多级路径如 `cms/image` / `form/file`）
- 返回 `{ url, originalFilename, size, bucket }`，url 形如 `/profile/<bucket>/yyyyMM/<uuid>.<ext>`
- 默认白名单：图片 + 常见办公文档 + zip（jpg/jpeg/png/gif/webp/bmp/svg/pdf/doc/docx/xls/xlsx/ppt/pptx/txt/csv/zip）
- 默认大小上限 10MB；可通过 `app.upload.max-size-mb` 改全局，或调用方传 `UploadOptions` 覆盖
- 底层 `FileStorageService`（`file.storage.type=local` / `s3`）由 framework 装配，业务侧无感切换

| 场景 | 速查 |
|------|------|
| 业务模块自定义白名单 | 注入 `UploadStorageService`，调 `save(file, "form/file", UploadOptions.exts(Set.of("png","pdf")))` |
| CMS 复用（兼容老代码） | `CmsUploadController` 已切到 `UploadStorageService`，bucket=`cms/image`，老 URL 形态 `/profile/cms/image/...` 保持 |
| 单测覆盖 | `mvn -pl scaffold-framework -am test "-Dtest=DefaultUploadStorageServiceTest"`（7 用例：null/empty / 非法 bucket / 白名单 / 大小限 / objectKey 拼接 / UploadOptions 覆盖 / 大小写归一化） |

## 表单引擎（M-10）

- 后端模块：`scaffold-module-form`，关停 `app.module.form.enabled=false`
- 双表：`form_template`（id / form_key / name / category / schema_json / version / status / del_flag / 标准 4 列；索引 `form_key+version` / `status` / `category`）+ `form_submission`（id / template_id / template_key / template_version / submitter / submitter_name / status / data / 时间；索引 `template_id+create_time` / `submitter+create_time` / `template_key`）
- 状态机：DRAFT → PUBLISHED → ARCHIVED；草稿编辑原地改；已发布编辑自动派生 `version+1` 的新草稿（不破坏在线版本）；发布会自动归档同 `formKey` 下其他 PUBLISHED 版本
- 横向越权防线：列表 / 详情 service 层强制 `submitter=current`，admin 看全量
- 前端：5 页 + 6 widget；vendor-form-create-designer chunk（~1MB / 340KB gzip）仅 `TemplateDesign` 入口懒加载，TemplateList / FormFill / SubmissionList 不污染
- E2E：`backend/scripts/verify-form.ps1`（12 步：登录 / CRUD / 状态门 / 版本派生 / 提交 / 列表详情 / 通用上传白名单）

| 场景 | 速查 |
|------|------|
| 跑后端 26 单测 | `mvn -pl scaffold-module-form -am test`（FormTemplateServiceTest 16 + FormSubmissionServiceTest 10） |
| 跑 E2E（要求后端 8080 起着） | `pwsh backend/scripts/verify-form.ps1` |
| Swagger 查 form API | `http://localhost:8080/swagger-ui.html` 顶部切 `form (通用表单引擎 M-10)` 分组；OpenAPI JSON：`Invoke-RestMethod "http://localhost:8080/v3/api-docs/form"` |
| 调试 — 看某模板所有版本 | `SELECT id, form_key, version, status, published_at FROM form_template WHERE form_key='leave_application' ORDER BY version DESC;` |
| 调试 — 看某用户最近提交 | `SELECT id, template_key, template_version, status, create_time FROM form_submission WHERE submitter='admin' ORDER BY create_time DESC LIMIT 20;` |
| 卸载模块 | admin/pom.xml 删 `scaffold-module-form` 依赖 + 跑 `backend/scaffold-module-form/src/main/resources/db/changelog/sql/form_uninstall.sql` |
| 6 个 widget 在 schema 里怎么用 | rule 里 `type: 'FormUserPicker'` / `'FormDeptPicker'` / `'FormDictSelect'` / `'FormCascaderSelect'` / `'FormDynamicTable'` / `'FormDetailSubForm'`；props 依各 widget 定义（见 `frontend/src/modules/form/widgets/*.vue`） |

## 文件中心（M-6）

- 后端模块：`scaffold-module-file`，关停 `app.module.file.enabled=false`
- 四张表：`sys_file`（主表 + bucket / refCount / del_flag / delete_time）/ `sys_file_folder`（用户级树 + path 唯一）/ `sys_file_share`（token / 过期 / 一次性 / BCrypt 密码 / visits）/ `sys_file_ref`（跨模块引用计数明细，`(file_id, ref_module, ref_type, ref_id)` 唯一）
- 上传：走 framework `UploadStorageService`（默认白名单 + 10MB 上限）。专用入口 `POST /file/file/upload`，通用入口 `POST /system/upload/file`（M-10 Pre-Phase）
- 删除（两阶段）：默认软删 → 30 天后由 quartz 任务（`fileCleanupJob.purge` / `sys_job` id 6025，cron `0 0 3 * * ?`）清磁盘 + 删 DB；管理员可调 `DELETE /file/file/purge/{id}` 立即清回收站，或 `POST /file/file/purge-now?retainDays=N` 手工触发整批清理（`retainDays=0` 立即清全部）
- 鉴权下载：`GET /file/download/{id}`（需 `file:file:download`），local 模式流式输出 + path traversal 防护，S3 / OSS 模式自动 302 redirect
- 分享访问：`GET /file/share/access/{token}`（`@Anonymous`，无需登录），五重校验（status / expireAt / oneTime visits / 密码 BCrypt / 文件未删）。一次性 token 在第一次访问后 status 转 2（已用尽）
- 引用计数：业务模块（CMS / form / wf 等）`@Autowired FileRefService` 调 `attach(fileId, "cms", "article", id)` / `detach(...)`，service 内部对 `sys_file_ref` 与 `sys_file.ref_count` 原子同步；`ref_count > 0` 时软删 / 硬删全部被服务层拒绝
- 权限模型：纯 perm — `file:list` 看全量，`file:list:mine` 仅看自己；service 不强制，所有过滤在 controller 层根据 `SecurityUtils.hasPermi` 分支
- 前端：modules/file 三页（`/file/mine` / `/file/all` / `/file/share`）+ 三个复用组件（FilePicker / FilePreview / ShareDialog）re-export 给 CMS / form 等业务模块复用

| 场景 | 速查 |
|------|------|
| 跑后端 33 单测 | `mvn -pl scaffold-module-file -am test`（FileServiceTest 11 + FolderServiceTest 8 + ShareServiceTest 10 + FileRefServiceTest 4） |
| 跑 E2E（要求后端 8080 起着） | `pwsh backend/scripts/verify-file.ps1`（12 步：登录 / 通用上传 / 文件中心专用上传 / 列表过滤 / 文件夹建移动 / 鉴权下载 / 永久 + 一次性 + 密码三组分享 / 软删 / 立即清盘 / 手动触发清理任务） |
| 调试 — 看某用户最近上传 | `SELECT id, name, bucket, size_bytes, ref_count, del_flag, create_time FROM sys_file WHERE create_by='admin' ORDER BY id DESC LIMIT 20;` |
| 调试 — 找软删超 30 天的待清理 | `SELECT id, name, ref_count, delete_time FROM sys_file WHERE del_flag='2' AND ref_count=0 AND delete_time < DATE_SUB(NOW(), INTERVAL 30 DAY);` |
| 调试 — 看某文件被谁引用 | `SELECT * FROM sys_file_ref WHERE file_id = ?` 或 `GET /file/file/{id}/refs` |
| 调试 — 看某 token 是否还有效 | `SELECT id, file_id, status, expire_at, one_time, visits, create_by FROM sys_file_share WHERE token = ?` |
| 业务模块挂引用 | `@Autowired FileRefService refService;` 后 `refService.attach(fileId, "your-module", "your-type", id);` 文章 / 表单删除时同步 `detach(...)` |
| 卸载模块 | admin/pom.xml 删 `scaffold-module-file` 依赖 + 跑 `backend/scaffold-module-file/src/main/resources/db/changelog/sql/file_uninstall.sql`（删菜单 6001-6030 + 删 4 张表）；磁盘文件需另外手工清 |
| Swagger 查 file API | `http://localhost:8080/swagger-ui.html` 顶部切 `file` 分组（如已配 group），或直接看 `/v3/api-docs` 内 `/file/**` |

## 报表中心（M-8）

- 后端模块：`scaffold-module-report`，关停 `app.module.report.enabled=false`
- 五张表：`sys_report_template`（SQL 模板，`row_limit` / `timeout_ms` / `perm_key` 三闸）/ `sys_report_run_log`（运行历史；status：0 成功 / 1 失败 / 2 超时）/ `sys_report_dashboard` + `sys_report_dashboard_card`（看板 + 卡片，整批替换语义）/ `sys_report_datasource`（外部 JDBC，密码字段 `password_enc` AES-256-CBC，`ENC(...)` 前缀）
- 安全（三层防御）：
  - `ReportSqlGuard.ensureSelectOnly(sql)` — 模板保存 + 运行 + 校验入口必经；先剥离行注释 / 块注释 / 字符串字面量再正则；只允许 `SELECT` / `WITH` 起头；禁多语句（注入分号）/ DDL / DML / `OUTFILE` / `LOAD_FILE` / `@@` 系统变量
  - `ReportParamBinder.bind(template, params)` — `${name}` → `?`，缺失参数显式 `ServiceException`；杜绝字符串拼接
  - `ReportRunner.execute(...)` — `clamp` rowLimit ≤ 全局 10000、timeout ≤ 30000ms；`statement.setQueryTimeout(...)` + `statement.setMaxRows(...)` 双闸；耗时 > timeout/2 自动 WARN 慢日志
- 数据源管理（`ReportDataSourceManager`）：
  - id=0 为应用主库 → 直接返回 Spring 注入的 `DataSource`
  - id>0 为外部源 → 懒建 Druid + 缓存（`ConcurrentHashMap`），编辑 / 删除时自动 `invalidate(id)` + 释放老池
  - 密码：`Aes256Util.encrypt(masterKey, plain)` 加密落库；列表 / 详情接口返 `passwordMask`（`ENC(...)` 字面）；编辑接口 `password=null` 不动 / `""` 清空 / 非空重新加密
  - 测连接：`/report/datasource/test` 用临时 Druid 实例 + `Connection.isValid(2s)` 做存活探测，不入主缓存
- 即席 SQL（一次性查询）：调 `/report/run` 时不传 `templateId`、传 `sql` + 可选 `datasourceId` + `params`；要求 `report:template:add` 权限（管理员级）
- 模板二次权限：`perm_key` 留空 = 仅登录可用；填了 = 在 `report:template:run` 之外再叠加自身 perm 校验。看板 view 同理叠 `report:dashboard:view` + 自身 perm
- 导出：`POST /report/run/export?format=csv|xlsx` 走 `responseType: 'blob'`；CSV 带 UTF-8 BOM 兼容 Excel；xlsx 走 POI `SXSSFWorkbook` 流式（默认 100 行 keep-in-memory，超大数据集不会 OOM）
- `ReportRunLogCleanupJob`：`sys_job` id 7025（cron `0 0 4 * * ?`，参数 `90`）每天凌晨 4 点清 90 天前运行日志；备用入口 `POST /report/run/log/purge-now?days=N`
- 前端：`modules/report` 七个路由
  - `/report/template` ReportList：模板分页 + CRUD
  - `/report/template/:id` ReportEdit：SQL 编辑器 + 校验联调
  - `/report/run/:id` ReportRun：参数表单 + 表格预览 + CSV / xlsx 导出
  - `/report/dashboard` DashboardList：看板列表
  - `/report/dashboard/:id/:mode` DashboardView：view 模式自动跑全部卡片；edit 模式增删卡片、配图表类型 / 宽高 / configJson / paramJson
  - `/report/datasource` DataSourceList：外部数据源 CRUD + 测连接
  - `/report/log` RunLogList：运行日志（按模板 / 状态过滤；`Purge 90d+` 一键触发）
- ECharts 懒加载：`vendor-charts` chunk（约 1.05 MB / gzip 348 KB）由 `vite.config.ts` `manualChunks` 单独切；`EchartsCard.vue` 用 `await import('echarts')`，`DashboardView` 用 `defineAsyncComponent` 引；只在第一次渲染图表卡片时下载，模板编辑 / 列表 / 数据源等场景不污染主包

| 场景 | 速查 |
|------|------|
| 跑后端 51 单测 | `mvn -pl scaffold-module-report -am test`（SqlGuard 16 + ParamBinder 5 + Runner 8 + Template 6 + DataSource 9 + Dashboard 7） |
| 跑 E2E（要求后端 8080 起着） | `pwsh backend/scripts/verify-report.ps1`（14 步：登录 / 模板 CRUD / SqlGuard 三组拒 / 运行 / 行数截断 / 缺参数 / CSV+xlsx 导出 / 看板 CRUD + 卡片整批替换 / 运行日志列表 / 清理任务） |
| 调试 — 看最近运行日志 | `SELECT id, template_code, status, row_count, cost_ms, error_msg, create_time FROM sys_report_run_log ORDER BY id DESC LIMIT 20;` |
| 调试 — 看慢查询 | `SELECT * FROM sys_report_run_log WHERE cost_ms > 5000 ORDER BY cost_ms DESC LIMIT 20;` |
| 调试 — 看模板用了哪个数据源 | `SELECT id, code, name, datasource_id FROM sys_report_template WHERE status='0';` |
| 调试 — 看某看板的卡片 | `SELECT id, dashboard_id, template_id, title, chart_type, pos_w, pos_h FROM sys_report_dashboard_card WHERE dashboard_id = ? ORDER BY order_num;` |
| 配置外部数据源 | UI: `/report/datasource` → 新建 → 选 type / 填 URL / username / password（明文，后端 AES 加密）→ 测连接通过后保存。Mysql 默认驱动 `com.mysql.cj.jdbc.Driver`；其它数据库需手填 driverClass |
| 模板里写参数化 SQL | `SELECT * FROM t WHERE id >= ${minId} AND status = ${status}`；同时在 paramSchema 里声明：`[{"name":"minId","type":"number","required":true,"default":1},{"name":"status","type":"string","options":[{"label":"启用","value":"0"}]}]` |
| AES 主密钥配置 | `application.yml` 加 `report.datasource.aes-master-key: <16+ 位字符串>`（生产请走 vault / k8s secret）；不配会用默认弱口令并 WARN |
| Swagger 查 report API | `http://localhost:8080/swagger-ui.html` 顶部切 `report` 分组（如已配 group），或直接看 `/v3/api-docs` 内 `/report/**` |
| 卸载模块 | admin/pom.xml 删 `scaffold-module-report` 依赖 + 跑 `backend/scaffold-module-report/src/main/resources/db/changelog/sql/report_uninstall.sql`（删菜单 7001-7030 + 删 sys_job 7025 + 删 5 张表）|

