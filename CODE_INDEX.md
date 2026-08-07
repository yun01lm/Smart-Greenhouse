# Smart-Greenhouse 代码索引

> 自动生成于 2026-07-31 | 基于代码实际内容，非文档描述

---

## 项目总览

| 组件 | 技术栈 | 位置 |
|------|--------|------|
| 后端 | Spring Boot 3.x + JPA + MySQL | backend/ |
| Android App | Java + Retrofit + STOMP | app/ |
| Web 管理端 | Vue 3 + Vite + Axios | web/ |
| 共享库 | Java Common Lib | common/ |
| 模拟器 | Python | simulator/ |
| 工具脚本 | Python + SQL | tools/ |
| 消息队列 | Mosquitto MQTT | mosquitto.conf |
| 容器化 | Docker Compose | docker-compose.yml |

---

## 一、后端模块 (Spring Boot) — 19 个业务模块

### 1. auth — 用户认证 (/api/v1/auth)
- POST /register — 注册
- POST /login — 登录（返回 JWT token）
- GET /profile — 获取当前用户信息
- PUT /password — 修改密码（R16：旧密码验证+复杂度校验，Web/App 全端通用）

### 2. greenhouse — 大棚管理 (/api/v1/greenhouses)
- GET / — 大棚列表（按角色过滤）
- GET /{id} — 大棚详情
- POST / — 创建大棚（棚主）
- PUT /{id} — 更新大棚
- DELETE /{id} — 删除大棚
- GET /regions — 地区分布统计

### 3. sensor — 传感器数据 (/api/v1/sensors)
- GET /realtime?greenhouseId= — 实时传感器数据
- POST /history?greenhouseId= — 历史数据（时间范围 + 聚合）
- POST /compare — 多设备数据对比
- GET /aggregate — 大棚聚合统计
- GET /export — CSV 导出

### 4. device — 设备管理 (/api/v1/greenhouses/{greenhouseId}/devices)
- GET / — 设备列表
- GET /{deviceId} — 设备详情
- POST / — 添加设备
- PUT /{deviceId} — 更新设备
- DELETE /{deviceId} — 删除设备
- 设备分组: /device-groups — CRUD + 添加/移除设备

### 5. control — 设备控制 (/api/v1/control)
- POST /actuator — 执行器控制
- GET /logs — 控制日志
- 场景联动: /scenes — CRUD + execute 一键执行

### 6. alert — 预警系统 (/api/v1/alerts)
- GET / — 预警列表
- PUT /{id}/read — 标记已读
- GET /unread-count — 未读计数
- 规则: /rules — CRUD
- 阈值: /thresholds — CRUD

### 7. diagnosis — 病虫害诊断 (/api/v1/diagnosis)
- POST /recognize — 图像识别诊断
- GET /records — 诊断记录列表

### 8. crop — 作物周期 (/api/v1/crop-cycles)
- CRUD + GET /{id}/timeline 时间线

### 9. health — 健康评估 (/api/v1/health)
- GET /score — 健康评分
- GET /history — 评分历史
- GET /detail/{id} — 评分详情

### 10. qa — AI 智能问答 (/api/v1/qa)
- POST /ask — 文本问答（RAG 检索增强）
- POST /ask/voice — 语音问答
- GET /records — 问答记录
- 服务: RagQaService, EmbeddingService, ChromaRetrievalService, VoiceQaService
- Embedding: EmbeddingProvider 策略接口 → SiliconFlowEmbeddingProvider（真实，bge-m3 1024维，分批32条+429退避重试）/ MockEmbeddingProvider（默认兜底）；provider 由 .env.local 的 AI_EMBEDDING_PROVIDER 控制（mock|siliconflow）
- 检索: ChromaRetrievalService 相似度阈值兜底（greenhouse.ai.rag.min-similarity，默认0.3，低于阈值过滤，全部过滤降级通用知识）

### 11. knowledge — 知识库 (/api/v1/knowledge)
- GET /documents — 文档列表
- GET /categories — 分类列表（含文档数统计）
- GET /categories/managed — 分类管理列表（正式分类 CRUD，含 docCount/description）
- POST /categories/managed — 新建分类（名称唯一）
- PUT /categories/managed/{id} — 编辑分类（重命名级联文档 + Chroma 元数据）
- DELETE /categories/managed/{id} — 删除分类（有文档时拒绝）
- POST /documents — 上传文档（ID 优先复用回收池最小 ID）
- POST /index — 触发向量化索引（幂等，先清旧向量）
- PUT /documents/{id} — 更新文档标记信息（编号/标题/分类/简介）
- DELETE /documents/{id} — 删除文档（ID 入回收池）
- POST /test — 问答测试
- 服务: KnowledgeService（allocateDocumentId/ensureCategoryRegistered/renameDocumentCategory）, KnowledgeCategoryService

### 12. chat — 专家聊天 (/api/v1/chat)
- POST /conversations — 创建会话
- GET /conversations — 会话列表
- GET /conversations/{id}/messages — 消息列表
- POST /messages — 发送消息
- POST /snapshot — 创建快照
- PUT /conversations/{id}/close — 关闭会话
- GET /unread — 未读计数

### 13. expert — 专家管理 (/api/v1/experts + /api/v1/expert)
- GET /api/v1/experts — 专家列表
- POST /api/v1/expert/authorize/request — 请求授权
- GET /api/v1/expert/authorize/pending — 待处理授权
- PUT /api/v1/expert/authorize/{id}/approve — 批准授权
- PUT /api/v1/expert/authorize/{id}/reject — 拒绝授权
- PUT /api/v1/expert/authorize/{id}/revoke — 撤销授权
- GET /api/v1/expert/authorize/active — 已激活授权
- GET /api/v1/expert/authorize/history — 授权历史
- PUT /api/v1/expert/status — 切换在线状态
- 专家登录/登出自动置在线/离线（R9：AuthService + ExpertPresenceListener WebSocket 兜底）
- 会话列表角色前缀 Bug 修复（R9：getCurrentUserRole 去 ROLE_ 前缀，专家侧会话列表不再为空）

### 14. admin — 管理员功能 (/api/v1/admin)
- /users — 用户 CRUD（R16：POST 新增用户，初始密码123456，ADMIN上限3个；PUT /users/{id}/password 管理员改密，需验证绑定手机号）
- /roles — 角色统计
- /alerts — 预警规则 CRUD + 阈值管理
- /corpus — 语料 CRUD + 方言类型
- /corpus/{id}/audio — 语料音频流式播放（R11：ADMIN 鉴权，在线播放；前端经 axios Blob 懒加载）
- /experts — 专家列表 + 在线切换 + 授权记录 + 统计
- /experts/conversations — 咨询记录分页查询（R9：按专家/用户关键词/时间筛选）
- /experts/conversations/{id}/messages — 对话消息明细（R9）
- /experts/conversations/export — 咨询记录导出 Excel（R9）
- /monitor/overview — 系统监控概览
- /owners — 棚主列表（R10：关键词搜索/五级地区筛选/分页/regionText 地区列）
- /owners/{id}/greenhouses — 棚主名下大棚详情
- 预警规则/阈值接口支持 ADMIN 携带 ownerId 代查（R10，非 ADMIN 拒绝）
- /report 导出接口支持 ADMIN 携带 ownerId 代查（R10，非 ADMIN 仍按 OWNER/WORKER 归属校验）
- /report — 多类型数据导出（ADMIN 系统级，前端不暴露；农户/技术员导出走 /api/v1/report，见模块 20）

### 15. permission — 权限管理 (/api/v1/owner/employees + /api/v1/worker)
- 棚主视角: POST/GET/DELETE 员工 + GET/PUT 权限
- 员工视角: GET /worker/permissions + GET /worker/greenhouses

### 16. weather — 天气服务 (/api/v1/weather)
- GET /current — 当前天气（和风天气 API）
- GET /forecast — 天气预报

### 17. websocket — 实时推送
- STOMP over WebSocket 认证拦截
- 实时数据推送、预警推送、设备状态推送
- ExpertPresenceListener（R9）：专家 WebSocket 连接/断开自动置在线/离线

### 18. file — 文件服务
- 文件上传/存储（图片、文档等）

### 19. mqtt — MQTT 集成
- Mosquitto MQTT 客户端

### 20. report — 数据导出（OWNER/WORKER 专用，R8 新增）
- GET /sensors — 导出传感器历史数据（Excel）
- GET /alerts — 导出预警记录（Excel）
- GET /controls — 导出设备控制日志（Excel）
- GET /health — 导出健康评分记录（Excel）
- 权限：SecurityConfig 限定 OWNER/WORKER；ReportAccessService 按大棚归属校验（OWNER 本人大棚 / WORKER 被授权大棚）

### 安全层
- JwtTokenProvider: JWT 令牌生成/验证
- JwtAuthenticationFilter: 认证过滤器
- PermissionAspect: 权限切面（AOP — @RequireFunction, @RequireGreenhouseAccess）
- AuditAspect: 审计日志切面

---

## 二、Android App — 18 个界面

| 界面 | 类名 | 功能 |
|------|------|------|
| 登录 | LoginActivity | 用户登录 |
| 主界面 | MainActivity | 底部导航容器 |
| 数据总览 | DashboardFragment | 实时仪表盘 |
| 设备控制 | ControlFragment | 执行器控制 |
| 预警列表 | AlertFragment | 预警查看 |
| 预警详情 | AlertDetailActivity | 预警详细信息 |
| 阈值设置 | ThresholdSettingsActivity | 自定义阈值 |
| AI 助手 | AiAssistantFragment | 智能问答 |
| 病虫害诊断 | DiagnosisFragment | 拍照诊断 |
| 诊断结果 | DiagnosisResultActivity | 诊断结果展示 |
| 长势评估 | GrowthActivity | 长势评估 |
| 健康评分 | HealthActivity | 健康详情 |
| 历史数据 | HistoryActivity | 历史图表 |
| 专家列表 | ExpertListActivity | 专家浏览 |
| 专家聊天 | ChatActivity | 实时聊天 |
| 授权管理 | AuthorizationActivity | 数据授权 |
| 个人中心 | ProfileFragment | 个人信息（R16：新增修改密码入口） |

---

## 三、Web 管理端 — 10 个页面

| 路由 | 页面 | 功能 |
|------|------|------|
| /login | Login.vue | 登录 |
| /dashboard | DashboardPage.vue | 数据总览（管理员地区总览 AdminDashboard.vue / 农户端传感器卡片+预警+图表+健康） |
| /devices | DevicePage.vue | 设备管理与分组 |
| /users | UserPage.vue | 用户管理与角色概览（R16：新增用户/编辑改密/顶栏改密） |
| /knowledge | KnowledgePage.vue | 知识库文档管理（上传/分类管理/ID复用/编辑标记信息/向量化/问答测试） |
| /alerts | AlertRulePage.vue | 预警规则配置 |
| /export | ReportPage.vue | 多类型数据导出（OWNER/WORKER） |
| /monitor | MonitorPage.vue | 系统监控 |
| /corpus | CorpusPage.vue | 语料管理（R11：用途说明/上传/列表/音频播放/删除，方言选项动态合并） |
| /expert | ExpertPage.vue | 专家工作台（在线状态 + 咨询记录查询/明细/导出） |
| /owner | OwnerPage.vue | 棚主管理（搜索/地区筛选/进入棚主视角） |
| 视角切换 | stores/viewMode.js | 棚主视角状态（R10：管理员进入棚主视角查看其系统，一键切回；菜单/路由按有效角色 OWNER 放行） |

---

## 四、基础设施

- docker-compose.yml: MySQL + Mosquitto + Chroma 等服务编排
- mosquitto.conf: MQTT Broker 配置
- simulator/device_simulator.py: 设备模拟器
- tools/sensor_simulator.py: 传感器数据生成
- tools/init_seed_data.sql: 数据库初始化种子数据
