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

---
