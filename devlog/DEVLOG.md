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

---
