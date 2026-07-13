# 智慧大棚AIoT系统 — 开发总日志

> **规则**：只追加，不修改，不删除。每条记录永久保留。

---

## 2026-07-12

### 步骤1：创建项目骨架 | ✅ 完成

- **时间**：02:55
- **操作**：
  - 创建 Maven 多模块项目结构（父项目 smart-greenhouse + common + backend）
  - 配置 Spring Boot 3.3.5 + Java 21
  - 创建 common 模块：ApiResponse（统一响应）、PageResult（分页）、BusinessException（业务异常）、ErrorCode（8大类40+错误码枚举）
  - 创建 backend 模块：启动类 SmartGreenhouseApplication、application.yml
  - 配置所有 Maven 依赖（Spring Boot、Spring AI 1.0.9、Spring Security、JPA、WebSocket、MySQL、InfluxDB、MQTT、JWT、OkHttp、Lombok）
  - 创建 .gitignore
  - Git 初始化并首次 commit（10个文件，499行）
- **结果**：项目骨架就绪，可以开始写业务代码
- **文件清单**：
  - `pom.xml`（父POM）
  - `common/pom.xml`
  - `common/src/main/java/com/greenhouse/common/ApiResponse.java`
  - `common/src/main/java/com/greenhouse/common/PageResult.java`
  - `common/src/main/java/com/greenhouse/common/BusinessException.java`
  - `common/src/main/java/com/greenhouse/common/ErrorCode.java`
  - `backend/pom.xml`
  - `backend/src/main/java/com/greenhouse/SmartGreenhouseApplication.java`
  - `backend/src/main/resources/application.yml`
  - `.gitignore`

### 步骤2：Docker 开发环境 | ✅ 完成

- **时间**：03:10
- **操作**：
  - 创建 `docker-compose.yml`：4个服务（MySQL 8.0 / InfluxDB 2.7 / Mosquitto 2.x / Chroma），含健康检查和数据卷持久化
  - 创建 `mosquitto.conf`：开发阶段允许匿名连接，含 WebSocket MQTT（端口9001）
  - 创建 `.env.example`：环境变量模板，含所有第三方API占位符
  - 创建 `application-dev.yml`：Spring Boot 完整开发环境配置（MySQL/InfluxDB/Chroma/MQTT/JWT/所有第三方API）
- **结果**：一条 `docker-compose up -d` 命令即可启动全部基础设施
- **用户确认**：MQTT 匿名连接后期改认证只需改配置+重启，不影响代码
- **文件清单**：
  - `docker-compose.yml`
  - `mosquitto.conf`
  - `.env.example`
  - `backend/src/main/resources/application-dev.yml`

### 步骤3：Spring Boot 骨架（安全+JWT+异常处理） | ✅ 完成

- **时间**：03:20
- **操作**：
  - `SecurityConfig.java`：无状态JWT认证，关闭CSRF/Session，白名单路径（注册/登录/WebSocket/健康检查/Swagger/OPTIONS），其余全部需认证
  - `JwtTokenProvider.java`：JWT 生成（含userId/username/role）、解析、验证，密钥从配置读取
  - `JwtAuthenticationFilter.java`：从 Authorization: Bearer <token> 提取Token，验证后设置SecurityContext
  - `UserDetailsServiceImpl.java`：临时内存用户实现，预置admin账号，步骤4将改为数据库查询
  - `GlobalExceptionHandler.java`：统一拦截 BusinessException / @Valid校验失败 / BadCredentialsException / AccessDeniedException / 未知异常，全部返回ApiResponse格式
  - `WebSocketConfig.java`：STOMP基础配置，端点 /ws/connect，后续步骤9完善
  - `@RequireGreenhouseAccess` / `@RequireFunction`：自定义权限注解（空壳），步骤7实现AOP切面
- **结果**：Spring Boot 安全骨架就绪，JWT认证链路完整
- **文件清单**：
  - `backend/.../config/SecurityConfig.java`
  - `backend/.../config/WebSocketConfig.java`
  - `backend/.../security/JwtTokenProvider.java`
  - `backend/.../security/JwtAuthenticationFilter.java`
  - `backend/.../security/UserDetailsServiceImpl.java`
  - `backend/.../exception/GlobalExceptionHandler.java`
  - `backend/.../security/annotations/RequireGreenhouseAccess.java`
  - `backend/.../security/annotations/RequireFunction.java`

### 步骤4：C1 用户认证模块 | ✅ 完成

- **时间**：03:30
- **操作**：
  - `User.java`：JPA实体，四种角色(ADMIN/OWNER/WORKER/EXPERT)，BCrypt密码，员工单归属(ownerId)，专家在线状态(expertStatus)
  - `UserRepository.java`：JPA Repository，支持用户名/手机号唯一性校验，按角色/棚主/在线状态查询
  - `AuthController.java`：3个API端点 — POST /api/v1/auth/register（注册）、POST /api/v1/auth/login（登录）、GET /api/v1/auth/profile（获取个人信息）
  - `AuthService.java`：注册（校验角色/用户名/手机号唯一性、员工必须指定棚主、BCrypt加密、注册成功直接返回Token）、登录（Spring Security认证+生成JWT+检查账号状态）
  - 4个DTO：RegisterRequest（含@Valid校验）、LoginRequest、LoginResponse（含Token+用户信息）、UserProfileResponse
  - `UserDetailsServiceImpl.java`：**已从临时内存版本改为数据库版本**，通过UserRepository加载用户
- **结果**：用户认证链路完整（注册→BCrypt加密→数据库存储→登录→JWT生成→过滤器验证→SecurityContext）
- **文件清单**：
  - `backend/.../entity/User.java`
  - `backend/.../repository/UserRepository.java`
  - `backend/.../module/auth/controller/AuthController.java`
  - `backend/.../module/auth/service/AuthService.java`
  - `backend/.../module/auth/dto/RegisterRequest.java`
  - `backend/.../module/auth/dto/LoginRequest.java`
  - `backend/.../module/auth/dto/LoginResponse.java`
  - `backend/.../module/auth/dto/UserProfileResponse.java`
  - `backend/.../security/UserDetailsServiceImpl.java`（修改）

### 步骤5：C3 大棚管理 + C20 地区管理 | ✅ 完成

- **时间**：03:45
- **操作**：
  - `Greenhouse.java`：JPA实体，字段包含名称、位置、作物类型、棚主ID(ownerId)、五级地址（省/市/区/镇/村）、创建时间
  - `GreenhouseRepository.java`：JPA Repository，支持按棚主查询、按棚主计数、地区统计查询（按省/市/区/镇/村分组统计大棚数量）
  - `GreenhouseController.java`：6个API端点 — GET /api/v1/greenhouses（列表，按角色过滤）、GET /api/v1/greenhouses/{id}（详情）、POST /api/v1/greenhouses（创建）、PUT /api/v1/greenhouses/{id}（更新）、DELETE /api/v1/greenhouses/{id}（删除）、GET /api/v1/greenhouses/regions（地区统计）
  - `GreenhouseService.java`：核心业务 — 按角色过滤大棚列表（ADMIN看全部、OWNER看自己的、WORKER看所属OWNER的）、棚主大棚数量限制校验、所有权校验、五级地区统计
  - 3个DTO：GreenhouseRequest（@Valid校验，名称/位置必填）、GreenhouseResponse（含作物类型和地区信息）、RegionStatsResponse（地区统计数据结构）
- **结果**：大棚管理完整闭环，支持五级地区钻取统计，权限分级过滤
- **文件清单**：
  - `backend/.../entity/Greenhouse.java`
  - `backend/.../repository/GreenhouseRepository.java`
  - `backend/.../module/greenhouse/controller/GreenhouseController.java`
  - `backend/.../module/greenhouse/service/GreenhouseService.java`
  - `backend/.../module/greenhouse/dto/GreenhouseRequest.java`
  - `backend/.../module/greenhouse/dto/GreenhouseResponse.java`
  - `backend/.../module/greenhouse/dto/RegionStatsResponse.java`

### 步骤6：C2 设备管理 + C19 设备分组 | ✅ 完成

- **时间**：04:10
- **操作**：
  - `Device.java`：JPA实体，字段包含名称、设备编号(deviceSn)、设备类型(SENSOR/CONTROLLER)、传感器子类型(8种枚举)、状态(ONLINE/OFFLINE/ALARM)、大棚归属、MQTT topic自动生成、安装位置、上次数据时间/值
  - `DeviceGroup.java`：JPA实体，使用 @ElementCollection 存储组内设备ID列表（device_group_members关联表），支持设备多分组归属
  - `DeviceRepository.java`：支持按大棚/设备类型/状态筛选查询、设备统计（总数/按类型分组统计/按状态统计）、批量查询
  - `DeviceGroupRepository.java`：按大棚查询分组、名称唯一性校验
  - `DeviceController.java`：5个API端点 — GET（列表，可选type/status参数筛选）、GET/{id}（详情）、POST（创建）、PUT/{id}（更新）、DELETE/{id}（删除）
  - `DeviceGroupController.java`：7个API端点 — GET（列表）、GET/{id}（详情）、POST（创建）、PUT/{id}（更新）、POST/{groupId}/devices/{deviceId}（添加设备）、DELETE/{groupId}/devices/{deviceId}（移除设备）、DELETE/{groupId}（删除分组）
  - `DeviceService.java`：核心业务 — 设备创建（大棚归属校验、数量上限50、编号/名称唯一性、传感器类型必填校验）、列表查询（按角色过滤+按类型/状态筛选）、更新（deviceSn和deviceType创建后不可修改）、删除
  - `DeviceGroupService.java`：核心业务 — 分组创建（上限20、名称唯一性、设备合法性校验防跨大棚）、更新、设备添加/移除、删除
  - 4个DTO：DeviceRequest（@Valid校验）、DeviceResponse（含MQTT topic和状态信息）、DeviceGroupRequest（@Size(max=50)）、DeviceGroupResponse（含deviceCount计算字段）
- **结果**：设备管理和分组功能完整，API路径遵循 RESTful 风格（`/api/v1/greenhouses/{greenhouseId}/devices` 和 `/api/v1/greenhouses/{greenhouseId}/device-groups`），设备归属严格校验，防跨大棚操作
- **文件清单**：
  - `backend/.../entity/Device.java`
  - `backend/.../entity/DeviceGroup.java`
  - `backend/.../repository/DeviceRepository.java`
  - `backend/.../repository/DeviceGroupRepository.java`
  - `backend/.../module/device/controller/DeviceController.java`
  - `backend/.../module/device/controller/DeviceGroupController.java`
  - `backend/.../module/device/service/DeviceService.java`
  - `backend/.../module/device/service/DeviceGroupService.java`
  - `backend/.../module/device/dto/DeviceRequest.java`
  - `backend/.../module/device/dto/DeviceResponse.java`
  - `backend/.../module/device/dto/DeviceGroupRequest.java`
  - `backend/.../module/device/dto/DeviceGroupResponse.java`

### 步骤7：C18 多角色权限模块 + AOP切面 | ✅ 完成

- **时间**：04:40
- **操作**：
  - `EmployeePermission.java`：JPA实体，对应 DB 第8号表，6个功能权限布尔字段（canViewData/canControlDevice/canDiagnose/canAskExpert/canViewAlerts/canViewHistory）
  - `EmployeePermissionRepository.java`：支持按员工ID/棚主ID/大棚ID查询权限，按员工+大棚组合删除
  - `PermissionController.java`：棚主端 5个API端点 — POST /api/v1/owner/employees（添加/邀请员工）、GET（员工列表）、GET/{id}/permissions（查看权限）、PUT/{id}/permissions（更新权限）、DELETE/{id}（删除员工）
  - `WorkerPermissionController.java`：员工端 2个API端点 — GET /api/v1/worker/permissions（查看自己权限）、GET /api/v1/worker/greenhouses（可访问大棚列表）
  - `PermissionService.java`：核心业务 — 添加员工（用户名/手机号查找、角色校验、单归属校验、首次绑定ownerId、权限记录创建）、权限更新（6个字段按非空更新）、删除员工（删除权限+解除归属）、员工端查询
  - `PermissionAspect.java`：AOP切面实现 — `@RequireGreenhouseAccess` 校验大棚访问权限（ADMIN放行/OWNER检查归属/WORKER查employee_permissions/EXPERT暂拒绝）、`@RequireFunction` 校验功能权限（ADMIN+OWNER放行/WORKER按6个功能标识逐一检查）、greenhouseId自动提取（支持@PathVariable/@RequestParam）
  - `UserRepository.java`：新增 `findByRoleAndOwnerId` 和 `countByRoleAndOwnerId` 方法
  - `GreenhouseService.java`：WORKER角色从返回空列表改为从 employee_permissions 查询被授权的大棚
  - `DeviceService.java`：WORKER角色从拒绝改为检查 employee_permissions 表
  - `pom.xml`：显式添加 `spring-boot-starter-aop` 依赖确保切面正常工作
- **结果**：多角色权限体系完整闭环 — 棚主可邀请员工并精细分配6项功能权限，员工端可查看自己被授权的大棚和权限，AOP切面自动拦截校验，之前步骤中 WORKER 返回空列表/拒绝的问题全部修复
- **文件清单**：
  - `backend/.../entity/EmployeePermission.java`
  - `backend/.../repository/EmployeePermissionRepository.java`
  - `backend/.../module/permission/controller/PermissionController.java`
  - `backend/.../module/permission/controller/WorkerPermissionController.java`
  - `backend/.../module/permission/service/PermissionService.java`
  - `backend/.../module/permission/dto/AddEmployeeRequest.java`
  - `backend/.../module/permission/dto/EmployeeResponse.java`
  - `backend/.../module/permission/dto/UpdatePermissionRequest.java`
  - `backend/.../module/permission/dto/PermissionResponse.java`
  - `backend/.../security/aop/PermissionAspect.java`
  - `backend/.../repository/UserRepository.java`（修改）
  - `backend/.../module/greenhouse/service/GreenhouseService.java`（修改）
  - `backend/.../module/device/service/DeviceService.java`（修改）
  - `backend/pom.xml`（修改）

### 步骤8：C4 MQTT消费者 + C5 时序数据 | ✅ 完成

- **时间**：05:10
- **操作**：
  - `MqttConfig.java`：MQTT 客户端配置，支持用户名/密码认证（可选）、自动重连、cleanSession，从 application.yml 读取 broker/clientId
  - `InfluxDbConfig.java`：InfluxDB 2.x 客户端配置，创建 InfluxDBClient/WriteApiBlocking/QueryApi 三个 Bean，从 application.yml 读取 url/token/org/bucket
  - `MqttSubscriber.java`：MQTT 消息订阅器 — @PostConstruct 启动时订阅 `greenhouse/+/device/+` 通配符主题，接收 ESP32 JSON 数据（greenhouseId/deviceId/sensorType/value/timestamp），调用 SensorDataService 写入 InfluxDB + 更新 Device 状态为 ONLINE
  - `SensorDataService.java`：时序数据核心服务 — writeData（写入 InfluxDB point）、updateDeviceStatus（更新设备在线状态和最后读数）、getRealtimeData（Flux 查询最近5分钟 last()）、getHistoryData（时间范围 + aggregateWindow 均值聚合）、getCompareData（多设备同时段对比）、getAggregateData（mean/max/min/last/count 五维统计）、exportCsv（生成 CSV 字符串）
  - `InfluxDbConfigHelper.java`：辅助组件，@Value 注入 influxdb.org/bucket，避免与 InfluxDbConfig 循环依赖
  - `SensorController.java`：5个API端点 — GET /api/v1/sensors/realtime（实时数据）、POST /api/v1/sensors/history（历史数据+聚合）、POST /api/v1/sensors/compare（多组对比）、GET /api/v1/sensors/aggregate（聚合统计）、GET /api/v1/sensors/export（CSV导出）
  - 6个DTO：SensorDataPoint（通用数据点）、SensorRealtimeResponse（按类型分组）、SensorHistoryRequest（@Valid校验）、SensorCompareResponse（含DeviceSeries子类）、SensorAggregateResponse（五维统计）、CompareRequest
- **结果**：感知→存储→查询整条链路打通 — ESP32 通过 MQTT 上报数据 → InfluxDB 存储 → API 查询（实时/历史/对比/统计/导出），设备在线状态自动更新
- **文件清单**：
  - `backend/.../config/MqttConfig.java`
  - `backend/.../config/InfluxDbConfig.java`
  - `backend/.../module/mqtt/MqttSubscriber.java`
  - `backend/.../module/sensor/service/SensorDataService.java`
  - `backend/.../module/sensor/service/InfluxDbConfigHelper.java`
  - `backend/.../module/sensor/controller/SensorController.java`
  - `backend/.../module/sensor/dto/SensorDataPoint.java`
  - `backend/.../module/sensor/dto/SensorRealtimeResponse.java`
  - `backend/.../module/sensor/dto/SensorHistoryRequest.java`
  - `backend/.../module/sensor/dto/SensorCompareResponse.java`
  - `backend/.../module/sensor/dto/SensorAggregateResponse.java`
  - `backend/.../module/sensor/dto/CompareRequest.java`

### 步骤9：C11 WebSocket 实时推送 | ✅ 完成

- **时间**：05:35
- **操作**：
  - `StompAuthInterceptor.java`：STOMP CONNECT 阶段拦截器 — 从 Header 提取 `Authorization: Bearer <token>`，调用 JwtTokenProvider 校验，解析 userId/username/role 存入 StompPrincipal，校验失败抛出异常拒绝连接
  - `WebSocketConfig.java`：重写 — 注入 StompAuthInterceptor，注册到 `configureClientInboundChannel`；连接端点 `/ws/connect` 允许跨域；消息代理 `/topic` + `/queue`；应用前缀 `/app`；用户前缀 `/user`
  - `RealtimePushService.java`：统一推送服务 — `pushSensorData()`（传感器数据 → `/topic/greenhouse/{id}/realtime`）、`pushDeviceStatus()`（设备状态变更 → `/topic/device/{id}/status`）、`pushAlert()`（预警 → `/topic/greenhouse/{id}/alerts`），全部通过 SimpMessagingTemplate.convertAndSend()
  - `MqttSubscriber.java`：修改 — MQTT 收到数据后，在写入 InfluxDB + 更新设备状态之后，调用 `pushService.pushSensorData()` 实时推送到前端
  - `SensorDataService.java`：修改 — `updateDeviceStatus()` 改为返回设备名称（String），供推送消息使用
  - 3个消息 DTO：RealtimeMessage（SENSOR_DATA）、DeviceStatusMessage（DEVICE_STATUS）、AlertPushMessage（ALERT，为步骤10准备）
- **结果**：WebSocket 实时推送链路完整 — 前端 STOMP 连接 `/ws/connect` → JWT 认证 → 订阅主题 → MQTT 数据到达后自动推送到前端，无需轮询
- **文件清单**：
  - `backend/.../module/websocket/handler/StompAuthInterceptor.java`
  - `backend/.../module/websocket/service/RealtimePushService.java`
  - `backend/.../module/websocket/dto/RealtimeMessage.java`
  - `backend/.../module/websocket/dto/DeviceStatusMessage.java`
  - `backend/.../module/websocket/dto/AlertPushMessage.java`
  - `backend/.../config/WebSocketConfig.java`（修改）
  - `backend/.../module/mqtt/MqttSubscriber.java`（修改）
  - `backend/.../module/sensor/service/SensorDataService.java`（修改）

### 步骤10：C7 设备控制 + C12 场景联动 | ✅ 完成

- **时间**：06:05
- **操作**：
  - `Scene.java`：JPA实体，对应 DB 第11号表 — 场景名称、描述、触发条件JSON（Phase 2用）、动作列表JSON、所属大棚、启用状态
  - `ControlLog.java`：JPA实体，对应 DB 第14号表 — 操作人ID、设备ID、动作(ON/OFF)、来源(MANUAL/SCENE/ALERT)、场景ID、成功/失败原因
  - `SceneRepository.java`：按大棚查询/按启用状态查询/名称唯一性校验/数量统计
  - `ControlLogRepository.java`：按设备/用户/场景查询日志，支持分页
  - `ControlService.java`：设备控制核心 — 校验设备类型(CONTROLLER)/在线状态/权限（OWNER/WORKER需canControlDevice）、MQTT下发指令到 `greenhouse/{ghId}/device/{deviceSn}/command`、自动记录控制日志、更新设备状态
  - `SceneService.java`：场景联动核心 — CRUD + 手动执行（逐一调用ControlService控制每个设备，标记source=SCENE）、设备归属校验（禁止跨大棚）、控制器类型校验
  - `ControlController.java`：2个端点 — POST /api/v1/control/actuator（控制设备）、GET /api/v1/control/logs（查询日志）
  - `SceneController.java`：5个端点 — GET /api/v1/control/scenes（列表）、POST（创建）、PUT/{id}（更新）、DELETE/{id}（删除）、POST/{id}/execute（执行）
  - 4个DTO：ControlRequest（@Valid校验）、ControlLogResponse（含username/deviceName）、SceneRequest（含SceneAction子类，@NotEmpty校验）、SceneResponse（含SceneActionInfo子类）
- **结果**：设备控制链路完整 — 用户API → MQTT下发 → ESP32执行 → 日志记录。场景联动支持一键批量控制多个设备。Phase 1 全部 10 步业务代码完成！
- **文件清单**：
  - `backend/.../entity/Scene.java`
  - `backend/.../entity/ControlLog.java`
  - `backend/.../repository/SceneRepository.java`
  - `backend/.../repository/ControlLogRepository.java`
  - `backend/.../module/control/service/ControlService.java`
  - `backend/.../module/control/service/SceneService.java`
  - `backend/.../module/control/controller/ControlController.java`
  - `backend/.../module/control/controller/SceneController.java`
  - `backend/.../module/control/dto/ControlRequest.java`
  - `backend/.../module/control/dto/ControlLogResponse.java`
  - `backend/.../module/control/dto/SceneRequest.java`
  - `backend/.../module/control/dto/SceneResponse.java`

### 步骤11：集成验证 + 推送 GitHub | ✅ 完成

- **时间**：06:30
- **操作**：
  - 整体文件清单审查：73 个 Java 文件，包结构符合 AI 开发规则文档第 3.1 节规范
  - API 路径一致性检查：所有 35+ 端点均使用 `/api/v1/` 前缀 + kebab-case + RESTful 风格，与 API 端点清单一致
  - ErrorCode 完整性检查：所有引用的错误码均在 ErrorCode 枚举中定义，无遗漏
  - Git log 审查：18 次提交，每步 feat + docs 配对，历史清晰可追溯
  - 推送到 GitHub：`yun01lm/Smart-Greenhouse`，main 分支，18 次提交全部推送成功
- **结果**：Phase 1（后端骨架 + 核心业务模块）全部完成并推送 GitHub
- **Phase 1 总结**：
  - Java 文件：73 个（含 4 个 common 模块）
  - API 端点：35+
  - 实体类：7 个（User/Greenhouse/Device/DeviceGroup/EmployeePermission/Scene/ControlLog）
  - 配置类：4 个（Security/WebSocket/MQTT/InfluxDB）
  - 安全模块：JWT + AOP 切面 + STOMP 认证
  - 数据链路：MQTT → InfluxDB 存储 + WebSocket 推送
  - 控制链路：API → MQTT 下发 → ESP32 执行 → 日志记录
- **仓库地址**：https://github.com/yun01lm/Smart-Greenhouse

---

## 2026-07-13

### 步骤12：C6 预警引擎 + C21 自定义阈值 | ✅ 完成

- **时间**：07:30
- **操作**：
  - `AlertRule.java`：JPA实体，对应 DB 第9号表 — 4种规则类型(THRESHOLD/TREND/COMPOSITE/WEATHER)、3级告警(INFO/WARNING/CRITICAL)、条件JSON、关联场景
  - `Alert.java`：JPA实体，对应 DB 第10号表 — 告警级别/标题/内容/传感器数值/已读状态
  - `UserAlertThreshold.java`：JPA实体，对应 DB 第25号表 — 用户自定义min/max阈值、传感器类型
  - 3个Repository：AlertRuleRepository（按大棚+传感器类型查询启用规则）、AlertRepository（分页+未读统计）、UserAlertThresholdRepository（按用户+大棚+传感器查询）
  - `AlertEngine.java`：预警引擎核心 — MQTT 数据到达后调用 `check()`，比对系统规则（THRESHOLD 类型解析 JSON 中的 min/max）和用户自定义阈值，超限则生成 Alert 记录并通过 `pushService.pushAlert()` 推送 WebSocket
  - `AlertRuleService.java`：规则管理 — 创建/列表/更新/删除，权限校验（OWNER 只能操作自己大棚的规则），数量上限 50 条
  - `AlertThresholdService.java`：自定义阈值管理 — 设置（创建或更新同传感器类型的阈值）/列表/删除
  - `AlertController.java`：9个端点 — GET /api/v1/alerts（告警列表分页，可选 level 筛选）、PUT /api/v1/alerts/{id}/read（标记已读）、GET/POST/PUT/DELETE /api/v1/alerts/rules（规则 CRUD）、GET/POST/PUT/DELETE /api/v1/alerts/thresholds（阈值 CRUD）
  - 5个DTO：AlertRuleRequest（@Valid校验）、AlertRuleResponse（fromEntity转换）、AlertResponse（含greenhouseName）、ThresholdRequest、ThresholdResponse
  - `MqttSubscriber.java`：修改 — 在 WebSocket 推送之后新增 `alertEngine.check()` 调用
- **结果**：预警检测闭环完成 — MQTT 数据 → AlertEngine 比对规则/阈值 → 超限生成 Alert → WebSocket 实时推送告警。系统从"被动查看数据"升级为"主动告警通知"
- **文件清单**：
  - `backend/.../entity/AlertRule.java`
  - `backend/.../entity/Alert.java`
  - `backend/.../entity/UserAlertThreshold.java`
  - `backend/.../repository/AlertRuleRepository.java`
  - `backend/.../repository/AlertRepository.java`
  - `backend/.../repository/UserAlertThresholdRepository.java`
  - `backend/.../module/alert/service/AlertEngine.java`
  - `backend/.../module/alert/service/AlertRuleService.java`
  - `backend/.../module/alert/service/AlertThresholdService.java`
  - `backend/.../module/alert/controller/AlertController.java`
  - `backend/.../module/alert/dto/AlertRuleRequest.java`
  - `backend/.../module/alert/dto/AlertRuleResponse.java`
  - `backend/.../module/alert/dto/AlertResponse.java`
  - `backend/.../module/alert/dto/ThresholdRequest.java`
  - `backend/.../module/alert/dto/ThresholdResponse.java`
  - `backend/.../module/mqtt/MqttSubscriber.java`（修改）

---
