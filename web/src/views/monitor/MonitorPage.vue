<template>
  <div class="monitor-page">
    <div class="page-header">
      <h3>系统监控</h3>
      <p class="page-desc">实时查看系统运行状态、设备在线率和告警统计</p>
      <el-button size="small" :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <!-- 1. 服务连接状态 -->
    <el-row :gutter="16" class="status-row">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="status-indicator" :class="data.serviceStatus?.mqtt ? 'online' : 'offline'">
            <span class="dot"></span>
            <span class="label">MQTT 消息服务</span>
          </div>
          <span class="status-text">{{ data.serviceStatus?.mqtt ? '已连接' : '未连接' }}</span>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="status-indicator" :class="data.serviceStatus?.database ? 'online' : 'offline'">
            <span class="dot"></span>
            <span class="label">数据库服务</span>
          </div>
          <span class="status-text">{{ data.serviceStatus?.database ? '已连接' : '未连接' }}</span>
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 设备在线率 -->
    <el-card class="section-card" v-loading="loading">
      <template #header>
        <div class="section-header">
          <el-icon size="18"><Monitor /></el-icon>
          <span>设备在线率</span>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value total">{{ data.deviceStats?.total || 0 }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value online">{{ data.deviceStats?.online || 0 }}</div>
            <div class="stat-label">在线设备</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value offline">{{ data.deviceStats?.offline || 0 }}</div>
            <div class="stat-label">离线设备</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value alarm">{{ data.deviceStats?.alarm || 0 }}</div>
            <div class="stat-label">告警设备</div>
          </div>
        </el-col>
      </el-row>
      <!-- 进度条 -->
      <div class="progress-section" v-if="data.deviceStats?.total > 0">
        <div class="progress-bar">
          <div class="bar-segment online-seg" :style="{ width: onlinePercent + '%' }"></div>
          <div class="bar-segment offline-seg" :style="{ width: offlinePercent + '%' }"></div>
          <div class="bar-segment alarm-seg" :style="{ width: alarmPercent + '%' }"></div>
        </div>
        <div class="progress-legend">
          <span class="legend-item"><span class="legend-dot online-dot"></span>在线 {{ onlinePercent }}%</span>
          <span class="legend-item"><span class="legend-dot offline-dot"></span>离线 {{ offlinePercent }}%</span>
          <span class="legend-item"><span class="legend-dot alarm-dot"></span>告警 {{ alarmPercent }}%</span>
        </div>
      </div>
    </el-card>

    <!-- 3. 告警统计 + 4. 系统概览（并排） -->
    <el-row :gutter="16">
      <!-- 告警统计 -->
      <el-col :span="12">
        <el-card class="section-card" v-loading="loading">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><WarningFilled /></el-icon>
              <span>最近 24 小时告警</span>
            </div>
          </template>
          <div class="alert-summary">
            <div class="alert-total">
              <span class="big-number">{{ data.alertStats?.total24h || 0 }}</span>
              <span class="big-unit">条</span>
            </div>
            <div class="alert-breakdown">
              <div class="alert-level critical">
                <span class="level-dot"></span>
                <span class="level-label">严重</span>
                <span class="level-count">{{ data.alertStats?.critical || 0 }}</span>
              </div>
              <div class="alert-level warning">
                <span class="level-dot"></span>
                <span class="level-label">警告</span>
                <span class="level-count">{{ data.alertStats?.warning || 0 }}</span>
              </div>
              <div class="alert-level info">
                <span class="level-dot"></span>
                <span class="level-label">提示</span>
                <span class="level-count">{{ data.alertStats?.info || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 系统数据概览 -->
      <el-col :span="12">
        <el-card class="section-card" v-loading="loading">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><DataAnalysis /></el-icon>
              <span>系统数据概览</span>
            </div>
          </template>
          <div class="overview-grid">
            <div class="overview-item">
              <el-icon size="24" color="#67C23A"><HomeFilled /></el-icon>
              <div class="overview-info">
                <div class="overview-value">{{ data.systemOverview?.greenhouses || 0 }}</div>
                <div class="overview-label">大棚</div>
              </div>
            </div>
            <div class="overview-item">
              <el-icon size="24" color="#409EFF"><Cpu /></el-icon>
              <div class="overview-info">
                <div class="overview-value">{{ data.systemOverview?.devices || 0 }}</div>
                <div class="overview-label">设备</div>
              </div>
            </div>
            <div class="overview-item">
              <el-icon size="24" color="#E6A23C"><UserFilled /></el-icon>
              <div class="overview-info">
                <div class="overview-value">{{ data.systemOverview?.users || 0 }}</div>
                <div class="overview-label">用户</div>
              </div>
            </div>
            <div class="overview-item">
              <el-icon size="24" color="#F56C6C"><SetUp /></el-icon>
              <div class="overview-info">
                <div class="overview-value">{{ data.systemOverview?.rules || 0 }}</div>
                <div class="overview-label">预警规则</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh, Monitor, WarningFilled, DataAnalysis, HomeFilled, Cpu, UserFilled, SetUp } from '@element-plus/icons-vue'
import { getMonitorOverview } from '@/api/monitor'

// ===== 数据 =====
const loading = ref(false)

const data = reactive({
  deviceStats: null,
  alertStats: null,
  serviceStatus: null,
  systemOverview: null
})

// ===== 计算属性 =====
const onlinePercent = computed(() => {
  const total = data.deviceStats?.total || 1
  return Math.round((data.deviceStats?.online || 0) / total * 100)
})
const offlinePercent = computed(() => {
  const total = data.deviceStats?.total || 1
  return Math.round((data.deviceStats?.offline || 0) / total * 100)
})
const alarmPercent = computed(() => {
  const total = data.deviceStats?.total || 1
  return Math.round((data.deviceStats?.alarm || 0) / total * 100)
})

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const res = await getMonitorOverview()
    const d = res.data
    if (d) {
      data.deviceStats = d.deviceStats
      data.alertStats = d.alertStats
      data.serviceStatus = d.serviceStatus
      data.systemOverview = d.systemOverview
    }
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.monitor-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.page-desc {
  flex: 1;
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 服务状态 */
.status-row {
  margin-bottom: 16px;
}

.status-card {
  text-align: center;
}

.status-card :deep(.el-card__body) {
  padding: 20px 16px;
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}

.status-indicator .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.status-indicator.online .dot {
  background: #67C23A;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
}

.status-indicator.offline .dot {
  background: #F56C6C;
  box-shadow: 0 0 6px rgba(245, 108, 108, 0.5);
}

.status-indicator .label {
  font-size: 14px;
  font-weight: 500;
}

.status-text {
  font-size: 13px;
  color: #909399;
}

/* 区块卡片 */
.section-card {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

/* 统计数值 */
.stat-item {
  text-align: center;
  padding: 12px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-value.total { color: #409EFF; }
.stat-value.online { color: #67C23A; }
.stat-value.offline { color: #909399; }
.stat-value.alarm { color: #F56C6C; }

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 进度条 */
.progress-section {
  margin-top: 16px;
}

.progress-bar {
  display: flex;
  height: 20px;
  border-radius: 10px;
  overflow: hidden;
  background: #f0f2f5;
}

.bar-segment {
  transition: width 0.5s ease;
}

.online-seg { background: #67C23A; }
.offline-seg { background: #909399; }
.alarm-seg { background: #F56C6C; }

.progress-legend {
  display: flex;
  gap: 20px;
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.online-dot { background: #67C23A; }
.offline-dot { background: #909399; }
.alarm-dot { background: #F56C6C; }

/* 告警统计 */
.alert-summary {
  display: flex;
  align-items: center;
  gap: 32px;
}

.alert-total {
  text-align: center;
  min-width: 80px;
}

.big-number {
  font-size: 42px;
  font-weight: 700;
  color: #303133;
}

.big-unit {
  font-size: 16px;
  color: #909399;
  margin-left: 4px;
}

.alert-breakdown {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-level {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.alert-level.critical .level-dot { background: #F56C6C; }
.alert-level.warning .level-dot { background: #E6A23C; }
.alert-level.info .level-dot { background: #409EFF; }

.level-label {
  width: 36px;
  color: #606266;
}

.level-count {
  font-weight: 600;
  color: #303133;
}

/* 系统概览网格 */
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
}

.overview-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.overview-label {
  font-size: 12px;
  color: #909399;
}
</style>
