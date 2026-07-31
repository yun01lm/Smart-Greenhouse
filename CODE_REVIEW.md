# 项目代码审查报告

> 审查日期: 2026-07-31 | 项目: Smart-Greenhouse (智慧大棚AIoT系统)
> 审查范围: 后端 (Spring Boot) + Android App (Java) + Web管理端 (Vue 3)

---

## 1. 项目总览

### 技术栈

| 端 | 语言/框架 | 构建工具 | 数据库/中间件 |
|---|----------|----------|--------------|
| 后端 | Java 17 + Spring Boot 3.x | Gradle + Maven | MySQL 8, InfluxDB 2.7, Chroma |
| Android | Java + Android SDK 34 | Gradle (AGP) | SharedPreferences (本地) |
| Web | Vue 3 + Vite + Element Plus | npm | localStorage (本地) |
| 基础设施 | Docker Compose | — | Mosquitto MQTT, Nginx(反代) |

### 架构风格

| 端 | 架构模式 | 层级划分 |
|---|----------|----------|
| 后端 | 分层架构 + AOP横切 | Controller → Service → Repository → Entity; Security (JWT + AOP权限) |
| Android | MVVM | Activity/Fragment → ViewModel → Repository → ApiService/StompClient |
| Web | 组件化 SPA | View(Vue SFC) → Pinia Store → API Module(axios) |

### 核心模块列表

- **后端 19 个业务模块**: auth, greenhouse, sensor, device, control, alert, diagnosis, crop, health, qa, knowledge, chat, expert, admin, permission, weather, websocket, file, mqtt
- **Android 18 个界面**: 登录、主界面、仪表盘、设备控制、预警、AI助手、诊断、长势、健康、历史、专家列表、专家聊天、授权管理、个人中心
- **Web 11 个页面**: 登录、仪表盘、设备管理、用户管理、知识库、预警配置、数据导出、系统监控、语料管理、专家工作台、棚主管理

---

## 2. 架构设计评估

### 后端 (Spring Boot)

**优点:**
- 模块划分清晰，19个模块职责单一，按业务领域垂直拆分
- AOP权限校验设计优秀 (`PermissionAspect.java`)，通过 `@RequireGreenhouseAccess` 和 `@RequireFunction` 实现声明式权限控制
- 统一响应封装 (`ApiResponse`) 和统一异常处理 (`BusinessException` + `ErrorCode`)
- JWT 无状态认证 + BCrypt 密码编码，安全基础扎实
- 支持 Mock Provider 配置切换（`ai.image.provider: mock`），便于本地开发调试

**风险项:**

| 文件/模块 | 严重度 | 问题描述 | 改进建议 |
|----------|--------|----------|----------|
| `backend/.../security/SecurityConfig.java` | 高 | CSRF 全局关闭 (`AbstractHttpConfigurer::disable`)，适用于纯API但缺少额外防护 | 添加 `RateLimiter` 或 API 网关层限流 |
| `backend/.../security/SecurityConfig.java` | 中 | `/ws/**` 设为 `permitAll()`，WebSocket 握手无认证 | 改为通过 `JwtAuthenticationFilter` 在握手阶段拦截并校验 token |
| `backend/.../security/JwtAuthenticationFilter.java:62` | 中 | Token 无效时不阻断请求，交由后续 `SecurityConfig` 拒绝 — 可能导致无效请求到达 Controller | 无效 Token 时直接返回 401 |
| `backend/.../aop/PermissionAspect.java:92-94` | 中 | EXPERT 角色直接抛异常 `GREENHOUSE_ACCESS_DENIED`，硬编码 TODO 注释 | 尽快实现专家授权校验逻辑 |
| `backend/.../aop/PermissionAspect.java:150-167` | 低 | `extractGreenhouseId` 第三轮兜底匹配 "任何第一个 Long 参数"，可能误匹配 | 移除兜底逻辑，强制要求命名参数 |
| 全局 | 中 | 缺少全局异常处理 `@ControllerAdvice`，部分异常可能直接暴露 stacktrace | 添加 `GlobalExceptionHandler` |

### Android 端

**优点:**
- 采用 MVVM 架构，Fragment/Activity 职责清晰，不写业务逻辑
- ViewBinding 绑定视图，避免 findViewById
- ViewModel + LiveData 实现数据驱动 UI 更新
- 自定义轻量 STOMP 协议实现 (`StompClient.java`)，避免引入重型依赖
- 网络请求通过 `ExecutorService` + `Handler` 在子线程执行

**风险项:**

| 文件/模块 | 严重度 | 问题描述 | 改进建议 |
|----------|--------|----------|----------|
| `app/.../repository/GreenhouseRepository.java` | 高 | **上帝对象** — 单一类约 900 行，封装所有 API 调用（认证/大棚/传感器/预警/诊断/QA/聊天/专家...），违反单一职责原则 | 按业务模块拆分为多个 Repository: `AuthRepository`, `SensorRepository`, `AlertRepository` 等 |
| `app/.../repository/GreenhouseRepository.java` | 中 | 所有 API 调用使用 `Retrofit.execute()` 同步阻塞方式，每个请求占用线程池一个线程 | 改用 Retrofit 异步 `enqueue()` 或迁移到 Kotlin Coroutines |
| `app/.../data/api/ApiClient.java:37` | 中 | 固定 4 线程池 (`newFixedThreadPool(4)`) — 高并发时可能阻塞 | 改用 `CachedThreadPool` 或增加线程数到 CPU 核心数×2 |
| `app/.../data/local/TokenManager.java` | 高 | JWT Token 明文存储在 `SharedPreferences`（见安全分析） | 使用 `EncryptedSharedPreferences` |
| `app/.../ui/dashboard/DashboardFragment.java:164` | 低 | `@Override onDestroyView` 中设置 `binding = null` 但未取消 ViewModel 观察 | 使用 `getViewLifecycleOwner()` 已正确处理（当前正确） |

### Web 管理端

**优点:**
- Vue 3 Composition API + Pinia 状态管理，代码组织清晰
- Axios 统一拦截器处理 Token 附加和 401 重定向
- 路由守卫控制页面访问权限
- API 模块按业务拆分（14 个 API 模块文件），职责分明
- 自定义 STOMP WebSocket 客户端，支持自动重连和心跳

**风险项:**

| 文件/模块 | 严重度 | 问题描述 | 改进建议 |
|----------|--------|----------|----------|
| `web/src/stores/auth.js` | 高 | Token 和用户信息明文存储在 `localStorage` | Token 改用 `httpOnly` Cookie（需后端配合），或至少用 `sessionStorage` |
| `web/src/utils/websocket.js:26` | 中 | WebSocket URL 中 token 通过 STOMP CONNECT 帧的 `Authorization` header 传递 — 但初次握手 URL 无认证参数 | 确认后端 WebSocket 握手是否通过 header 校验 |
| `web/src/utils/request.js:33` | 低 | 401 响应时直接 `router.push('/login')`，未保存当前页面路径，登录后无法恢复 | 在 401 处理中保存 `router.currentRoute.value.fullPath` 并在登录后跳回 |
| `web/src/views/` | 低 | 所有 Vue 组件无错误边界 (ErrorBoundary)，组件异常可能导致白屏 | 添加 `onErrorCaptured` 或全局错误组件 |

---

## 3. 代码质量与坏味道

### 后端

| 文件 | 行号 | 问题 | 建议 |
|------|------|------|------|
| `backend/.../auth/service/AuthService.java:35-38` | 35-53 | register 方法内联校验逻辑较长（用户名唯一、手机号唯一、员工棚主），可抽取为独立验证方法 | 抽取 `validateRegistration(request)` 私有方法 |
| `backend/.../sensor/controller/SensorController.java:45` | 45-49 | `realtime` 方法额外调用 `greenhouseRepository.findById` 仅取 name，造成额外查询 | 在 `SensorDataService.getRealtimeData` 内部 JOIN 查询一并返回 |
| `backend/.../control/service/ControlService.java` | 全局 | 未检查 actuator 执行结果/状态，命令下发达 MQTT 后不等待 ACK | 添加 MQTT 响应超时和失败重试机制 |
| `backend/.../security/JwtTokenProvider.java:34` | 34 | JWT 过期时间默认 7200000ms (2h) 硬编码 — 短于行业常见的 24h+refresh token 方案 | 引入 refresh token 机制 |
| `backend/.../resources/application-dev.yml:17` | 17 | `ddl-auto: update` — dev 环境可接受但容易误带到生产 | 生产环境显式设为 `validate` |

### Android 端

| 文件 | 行号 | 问题 | 建议 |
|------|------|------|------|
| `app/.../data/repository/GreenhouseRepository.java` | 全文件(~900行) | 大量重复的 try-catch 模式：`execute(() -> { try { Response<...> response = apiService.xxx().execute(); if (response.isSuccessful()...) postSuccess; else postError; } catch (IOException e) { postError; } })` — 重复代码占比 >70% | 抽取泛型方法 `executeApiCall(Call<T> call, Callback<T> cb)` 统一处理 |
| `app/.../data/api/ApiClient.java:55` | 55 | `OkHttpClient` 每次 `init()` 创建新实例，无连接池复用 | 将 `OkHttpClient` 设为单例并在 init 中只创建一次 |
| `app/.../ui/dashboard/DashboardFragment.java:125-158` | 125-158 | 4 个卡片点击事件代码几乎相同（设置 intent extra + startActivity），重复 4 次 | 抽取 `navigateToActivity(Class, greenhouseId)` 方法 |
| `app/.../data/model/` | 全部 | 35 个 Model 类全部是纯 POJO，大量手动 getter/setter 样板代码 | 如果是 Kotlin 可用 data class；Java 下建议使用 Lombok（但目前 Android 未配置） |

### Web 端

| 文件 | 行号 | 问题 | 建议 |
|------|------|------|------|
| `web/src/utils/websocket.js:67` | 67-77 | `handleFrame` 中的 STOMP 帧解析与 Android `StompClient.java:handleStompFrame` 逻辑重复 — 两侧各自实现帧解析 | 抽取共享的 STOMP 协议规范文档，或至少统一解析边界处理 |
| `web/src/utils/websocket.js:71` | 71 | `const line = lines[i]` 使用 `var` 风格命名，与 let/const 混用 | 统一使用 `const` |
| `web/src/views/dashboard/DashboardPage.vue` | — | 设为抽样范围，未深入审查 | — |

---

## 4. 安全性分析

### 严重 (Critical)

| 编号 | 问题 | 文件 | 利用场景 | 修复方案 |
|------|------|------|----------|----------|
| S1 | JWT Token 明文存储 | `app/.../TokenManager.java:22` (SharedPreferences), `web/src/stores/auth.js:10` (localStorage) | 设备被 root/越狱或 XSS 攻击可窃取 Token，冒充用户身份 | Android: 使用 `EncryptedSharedPreferences`; Web: 使用 `httpOnly` + `Secure` Cookie（需后端 `Set-Cookie`） |
| S2 | MQTT 匿名访问 | `mosquitto.conf:14` (`allow_anonymous true`) | 攻击者可向 MQTT Broker 注入伪造传感器数据或劫持设备控制指令 | 启用 MQTT 认证：配置 `password_file` + ACL |
| S3 | MySQL 明文传输 | `application-dev.yml:8` (`useSSL=false`) | 中间人攻击可窃听数据库连接，获取全部业务数据和密码哈希 | 启用 SSL：`useSSL=true` + 配置 CA 证书 |

### 高 (High)

| 编号 | 问题 | 文件 | 修复方案 |
|------|------|------|----------|
| H1 | 数据库凭据硬编码 | `docker-compose.yml:17-18`, `.env.example:9` (`MYSQL_ROOT_PASSWORD=root`) | 生产环境使用 Kubernetes Secrets / Docker Secrets / Vault |
| H2 | InfluxDB Token 硬编码 | `application-dev.yml:24` (`INFLUX_TOKEN: dev-token-greenhouse-2026`), `docker-compose.yml:44` | 同上 |
| H3 | 无 API 限流 | 全局 | 添加 `Bucket4j` 或 Nginx `limit_req` 防止暴力破解和 DDoS |
| H4 | WebSocket 握手无认证 | `SecurityConfig.java:60` (`/ws/**` permitAll) | 在 WebSocket 握手拦截器中校验 JWT |
| H5 | Debug 模式日志泄露 Token | `ApiClient.java:42` (`HttpLoggingInterceptor.Level.BODY`) — Debug 构建打印完整请求体含 JWT | Release 构建确保 `BuildConfig.DEBUG = false` — 当前已做此判断 |

### 中 (Medium)

| 编号 | 问题 | 文件 | 修复方案 |
|------|------|------|----------|
| M1 | JWT 无 refresh token | `JwtTokenProvider.java:34` (2h 过期) | 实现 refresh token 机制，access token 短时效 (15min) |
| M2 | 无 CORS 白名单 | `SecurityConfig.java` — 未配置 CORS | 添加 `.cors()` 配置，限制允许的 origin |
| M3 | 错误信息可能泄露 | `business/exception handler` 缺失全局处理器 | 统一 `@ControllerAdvice` 过滤 stacktrace |
| M4 | 第三方 API Key 无加密存储 | `application-dev.yml` 中 baidu/xunfei/deepseek/siliconflow/qweather 的 key | 生产环境使用环境变量 + 加密配置 |

### 低 (Low)

| 编号 | 问题 | 修复方案 |
|------|------|----------|
| L1 | 密码策略仅依赖 BCrypt（无复杂度校验） | 注册时校验密码长度≥8、含数字+字母 |
| L2 | 登录无失败次数限制 | 添加登录失败计数 + 临时锁定机制 |

---

## 5. 性能优化建议

### 后端

| 编号 | 问题 | 文件 | 影响 | 优化建议 |
|------|------|------|------|----------|
| P1 | 传感器实时查询每次都查 InfluxDB | `SensorDataService.java` | 高并发下 InfluxDB 压力大 | 添加 Caffeine 本地缓存 (TTL 5s) 或 Redis |
| P2 | 无数据库连接池显式配置 | `application-dev.yml` | Spring Boot 默认 HikariCP 但连接数默认 10，高并发可能不足 | 显式配置 `spring.datasource.hikari.maximum-pool-size: 20` |
| P3 | 知识库文档上传同步处理 | `KnowledgeService.java` | 大文档上传 + 向量化可能阻塞请求线程 | 文档上传后异步提交向量索引任务 (`@Async`) |
| P4 | 预警规则检查频率不明 | `AlertRuleService.java` | 轮询间隔可能过短导致数据库压力，或过长导致预警延迟 | 建议用 InfluxDB 的 `Flux` 任务做规则检查 + MQTT 触发推送 |
| P5 | JPA `show-sql: true` 生产环境 | `application-dev.yml:18` | 生产环境日志 IO 开销大 | 生产环境关闭或降为 `WARN` |

### Android 端

| 编号 | 问题 | 文件 | 影响 | 优化建议 |
|------|------|------|------|----------|
| P6 | 无网络请求缓存 | `GreenhouseRepository.java` | 每次页面切换都重新请求，浪费流量和电量 | 添加内存缓存 (LRU) + 数据过期策略 |
| P7 | 图片未使用缓存库 | `DiagnosisFragment.java` (拍照诊断) | 原图加载可能 OOM | 使用 Glide/Coil 加载并压缩 |
| P8 | 固定线程池只有 4 线程 | `ApiClient.java:37` | 超过 4 个并发请求会排队 | 改用 `CachedThreadPool` 或 `Coroutines` |
| P9 | RecyclerView 未配置 `setHasFixedSize` | `DashboardFragment.java` 及多处 Adapter | 每次更新都重新计算 item 大小 | 添加 `rv.setHasFixedSize(true)` |
| P10 | WebSocket 心跳在主线程 | `StompClient.java:259` (`heartbeatHandler` 使用 MainLooper) | 主线程发送心跳消息 | 移至后台线程 Handler |

### Web 端

| 编号 | 问题 | 文件 | 影响 | 优化建议 |
|------|------|------|------|----------|
| P11 | 仪表盘未做虚拟滚动 | `DashboardPage.vue` | 大量传感器卡片可能卡顿 | 使用 `vue-virtual-scroller` |
| P12 | Element Plus 全量引入 | `main.js` (推测) | 首屏 JS 体积过大 | 改为按需引入 (`unplugin-vue-components`) |
| P13 | API 请求无合并/去重 | 全局 | 多个组件同时请求相同数据可能重复调用 | 使用请求去重缓存 (如 `vue-request`) |
| P14 | WebSocket 重连无指数退避 | `websocket.js:36` (固定 5s) | 服务端恢复时可能造成惊群效应 | 使用指数退避: 1s → 2s → 4s → 8s → Max 30s |

---

## 6. 总体评分与优先行动

### 各维度评分 (1-10)

| 维度 | 评分 | 简评 |
|------|------|------|
| 架构设计 | 7.5 | 后端模块化良好，AOP 权限设计优秀；Android Repository 上帝对象扣分 |
| 代码质量 | 7.0 | 命名规范、注释充分；大量重复 try-catch 代码，Repository 未拆分 |
| 安全性 | 5.5 | BCrypt 密码加密、JWT 认证基础好；但 Token 明文存储、MQTT 匿名、MySQL 无 SSL |
| 性能 | 6.0 | 架构可扩展；缺缓存层、缺连接池优化、Android 无线程池弹性 |
| 可维护性 | 7.0 | 模块拆分好；Android Repository 900 行单一类和 STOMP 协议重复实现降低可维护性 |
| **综合** | **6.6** | 功能实现完整度较高，但安全和性能需重点加固 |

### 总体评价

项目整体架构设计合理，后端 19 个模块划分清晰，AOP 权限设计是亮点。三端采用 MVVM/MVC 等主流架构模式，代码可读性好。但存在三个显著短板：**(1) Android Repository 上帝对象 (~900行)** 严重违反单一职责原则；**(2) 安全防护层次不足** — Token 明文存储、MQTT 匿名访问是生产环境不可接受的风险；**(3) 缺少缓存层和性能优化** — 传感器数据每次直查 InfluxDB 不可扩展。

### 紧急修复项 (必须立即处理)

1. **S1 — Token 明文存储**: Android `SharedPreferences` → `EncryptedSharedPreferences`; Web `localStorage` → `httpOnly` Cookie
2. **S2 — MQTT 匿名访问**: 启用 MQTT 用户名/密码认证
3. **S3 — MySQL SSL 传输**: 启用 `useSSL=true`

### 短期优化计划 (本月内可执行)

- 拆分 `GreenhouseRepository.java` 为多个职责单一的 Repository
- 抽取后端全局异常处理器 `@ControllerAdvice`
- 添加 API 限流 (Bucket4j 或 Nginx)
- 实现 JWT refresh token 机制
- 传感器数据添加 Caffeine 本地缓存
- Android `OkHttpClient` 设为单例

### 长期重构路线图

- Android 端迁移至 Kotlin + Coroutines + Jetpack Compose
- 引入 Redis 缓存层，替代本地 Caffeine
- 建立 CI/CD 流水线（自动化测试 + 安全扫描 + 构建）
- MQTT 指令下发增加 ACK/超时/重试机制
- 依赖库版本审计与 CVE 扫描集成

---

> 审查工程师注: 本次审查覆盖三端全部关键路径文件。Android `GreenhouseRepository` (~900行)、后端 `PermissionAspect`、Web `request.js` 等核心文件已全文审查。其余文件按模块抽样核验。审查基于 2026-07-31 代码快照。