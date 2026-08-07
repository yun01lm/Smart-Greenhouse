<template>
  <div class="admin-dashboard">
    <!-- 地区选择 -->
    <el-card class="region-card" shadow="never">
      <div class="region-row">
        <div class="region-left">
          <div class="region-title">
            <el-icon size="16"><Location /></el-icon>
            <span>查看范围</span>
          </div>
          <RegionCascader v-model="regionPath" width="340px" @change="onRegionChange" />
        </div>
        <div class="region-right">
          <span v-if="regionText" class="region-text">
            <el-icon size="14"><MapLocation /></el-icon>{{ regionText }}
          </span>
          <el-button type="primary" :loading="loading" @click="loadOverview">查 询</el-button>
          <el-button plain @click="resetRegion">全部地区</el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon icon-greenhouse"><el-icon :size="24"><OfficeBuilding /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.greenhouseCount }}</div>
          <div class="stat-label">大棚总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-owner"><el-icon :size="24"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.ownerOnline }}<span class="stat-sub">/ {{ stats.ownerCount }}</span></div>
          <div class="stat-label">农户在线 / 总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-device"><el-icon :size="24"><Cpu /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.deviceOnline }}<span class="stat-sub">/ {{ stats.deviceTotal }}</span></div>
          <div class="stat-label">设备在线 / 总数（离线 {{ stats.deviceOffline }}）</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" :style="{ background: healthColor + '1f', color: healthColor }">
          <el-icon :size="24"><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value" :style="{ color: healthColor }">{{ health.score }}<span class="stat-sub"> 分</span></div>
          <div class="stat-label">地区健康评分（{{ health.level }}）</div>
        </div>
      </div>
    </div>

    <!-- 环境聚合 + 预警总览 -->
    <div class="row-gap">
      <el-row :gutter="16" class="equal-row">
        <el-col :span="12">
          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="section-header">
                <div class="header-left"><el-icon size="18"><DataLine /></el-icon><span>地区环境聚合</span></div>
                <span class="header-note">最新值平均</span>
              </div>
            </template>
            <div class="env-grid">
              <div v-for="item in envItems" :key="item.key" class="env-item">
                <div class="env-label">{{ item.label }}</div>
                <div class="env-value">{{ item.avg != null ? item.avg : '--' }}</div>
              </div>
            </div>
            <div class="env-note">采样大棚：{{ env.sampledGreenhouseCount }} 个</div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="section-header">
                <div class="header-left"><el-icon size="18"><WarningFilled /></el-icon><span>预警总览</span></div>
                <span class="header-note">累计</span>
              </div>
            </template>
            <div class="alert-overview">
              <div class="alert-total">
                <span class="big-number">{{ alerts.total }}</span>
                <span class="big-unit">条预警</span>
              </div>
              <div class="alert-levels">
                <div class="alert-level critical">
                  <span class="level-dot"></span>
                  <span class="level-label">严重</span>
                  <span class="level-count">{{ alerts.critical }}</span>
                </div>
                <div class="alert-level warning">
                  <span class="level-dot"></span>
                  <span class="level-label">警告</span>
                  <span class="level-count">{{ alerts.warning }}</span>
                </div>
                <div class="alert-level info">
                  <span class="level-dot"></span>
                  <span class="level-label">提示</span>
                  <span class="level-count">{{ alerts.info }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 最新预警 + 天气 -->
    <div class="row-gap">
      <el-row :gutter="16" class="equal-row">
        <el-col :span="15">
          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="section-header">
                <div class="header-left"><el-icon size="18"><BellFilled /></el-icon><span>最新预警</span></div>
                <span class="header-note">地区内全部农户</span>
              </div>
            </template>
            <el-table :data="latestAlerts" size="small" stripe empty-text="暂无预警">
              <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
              <el-table-column label="级别" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="levelTag(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="greenhouseName" label="大棚" width="130" show-overflow-tooltip />
              <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
              <el-table-column label="时间" width="160" align="center">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="9">
          <el-card class="section-card weather-card" shadow="never">
            <template #header>
              <div class="section-header">
                <div class="header-left"><el-icon size="18"><Cloudy /></el-icon><span>当前天气</span></div>
                <span class="header-note">{{ weatherLocation }}</span>
              </div>
            </template>
            <div v-if="weather" class="weather-info">
              <div class="weather-icon"><el-icon :size="34" color="#FF9800"><Sunny /></el-icon></div>
              <div class="weather-temp">{{ weather.temperature }}<span class="temp-unit">°C</span></div>
              <div class="weather-desc">{{ weather.weatherText }}</div>
              <div class="weather-details">
                <span class="wd-item"><el-icon size="13"><Pouring /></el-icon>湿度 {{ weather.humidity }}%</span>
                <span class="wd-item"><el-icon size="13"><WindPower /></el-icon>风速 {{ weather.windSpeed }}m/s</span>
              </div>
            </div>
            <el-empty v-else description="暂无天气数据" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 系统监控（已合并） -->
    <el-card class="section-card monitor-card" shadow="never">
      <template #header>
        <div class="section-header">
          <div class="header-left"><el-icon size="18"><Monitor /></el-icon><span>系统运行状态</span></div>
          <span class="header-note">原系统监控，已并入数据总览</span>
        </div>
      </template>
      <div class="monitor-grid">
        <div class="monitor-block">
          <div class="monitor-title"><el-icon size="14"><TrendCharts /></el-icon>设备在线率（全系统）</div>
          <div class="monitor-progress">
            <div class="bar">
              <div class="bar-online" :style="{ width: onlinePercent + '%' }"></div>
              <div class="bar-offline" :style="{ width: offlinePercent + '%' }"></div>
              <div class="bar-alarm" :style="{ width: alarmPercent + '%' }"></div>
            </div>
            <div class="bar-legend">
              <span><i class="lg-dot lg-online"></i>在线 {{ deviceStats.online }}</span>
              <span><i class="lg-dot lg-offline"></i>离线 {{ deviceStats.offline }}</span>
              <span><i class="lg-dot lg-alarm"></i>告警 {{ deviceStats.alarm }}</span>
              <span>共 {{ deviceStats.total }}</span>
            </div>
          </div>
        </div>
        <div class="monitor-block">
          <div class="monitor-title"><el-icon size="14"><Connection /></el-icon>服务连接状态</div>
          <div class="service-status">
            <span class="svc-item">
              <span :class="['svc-dot', serviceStatus.mqtt ? 'ok' : 'bad']"></span>
              <span class="svc-name">MQTT</span>
              <el-tag :type="serviceStatus.mqtt ? 'success' : 'danger'" size="small" effect="light">
                {{ serviceStatus.mqtt ? '正常' : '断开' }}
              </el-tag>
            </span>
            <span class="svc-item">
              <span :class="['svc-dot', serviceStatus.database ? 'ok' : 'bad']"></span>
              <span class="svc-name">数据库</span>
              <el-tag :type="serviceStatus.database ? 'success' : 'danger'" size="small" effect="light">
                {{ serviceStatus.database ? '正常' : '异常' }}
              </el-tag>
            </span>
          </div>
        </div>
        <div class="monitor-block">
          <div class="monitor-title"><el-icon size="14"><Coin /></el-icon>系统数据概览</div>
          <div class="overview-grid">
            <div class="ov-item"><span class="ov-value">{{ systemOverview.greenhouses }}</span><span class="ov-label">大棚</span></div>
            <div class="ov-item"><span class="ov-value">{{ systemOverview.devices }}</span><span class="ov-label">设备</span></div>
            <div class="ov-item"><span class="ov-value">{{ systemOverview.users }}</span><span class="ov-label">用户</span></div>
            <div class="ov-item"><span class="ov-value">{{ systemOverview.rules }}</span><span class="ov-label">预警规则</span></div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import RegionCascader from '@/components/RegionCascader.vue'
import { getAdminOverview } from '@/api/dashboard'
import {
  Location, MapLocation, OfficeBuilding, User, Cpu, Star,
  DataLine, WarningFilled, BellFilled, Cloudy, Sunny, Pouring, WindPower,
  Monitor, TrendCharts, Connection, Coin
} from '@element-plus/icons-vue'

const loading = ref(false)
const regionPath = ref([])
const regionText = ref('全部地区')
const overview = ref(null)

const stats = reactive({ greenhouseCount: 0, ownerCount: 0, ownerOnline: 0, deviceTotal: 0, deviceOnline: 0, deviceOffline: 0 })
const env = reactive({ sampledGreenhouseCount: 0, TEMPERATURE: null, HUMIDITY: null, LIGHT: null, CO2: null })
const alerts = reactive({ total: 0, critical: 0, warning: 0, info: 0 })
const health = reactive({ score: 0, level: '-' })
const latestAlerts = ref([])
const weather = ref(null)
const weatherLocation = ref('全部地区')
const deviceStats = reactive({ total: 0, online: 0, offline: 0, alarm: 0 })
const serviceStatus = reactive({ mqtt: false, database: false })
const systemOverview = reactive({ greenhouses: 0, devices: 0, users: 0, rules: 0 })

const envItems = computed(() => [
  { key: 'TEMPERATURE', label: '温度(℃)', avg: env.TEMPERATURE?.avg ?? null },
  { key: 'HUMIDITY', label: '湿度(%)', avg: env.HUMIDITY?.avg ?? null },
  { key: 'LIGHT', label: '光照(lx)', avg: env.LIGHT?.avg ?? null },
  { key: 'CO2', label: 'CO₂(ppm)', avg: env.CO2?.avg ?? null }
])

const healthColor = computed(() => {
  if (health.score >= 90) return '#67C23A'
  if (health.score >= 75) return '#409EFF'
  if (health.score >= 60) return '#E6A23C'
  return '#F56C6C'
})

const onlinePercent = computed(() => {
  const t = deviceStats.total || 1
  return Math.round((deviceStats.online / t) * 100)
})
const offlinePercent = computed(() => {
  const t = deviceStats.total || 1
  return Math.round((deviceStats.offline / t) * 100)
})
const alarmPercent = computed(() => {
  const t = deviceStats.total || 1
  return Math.round((deviceStats.alarm / t) * 100)
})

function regionParams() {
  const p = regionPath.value || []
  return {
    province: p[0] || undefined,
    city: p[1] || undefined,
    district: p[2] || undefined,
    town: p[3] || undefined,
    village: p[4] || undefined
  }
}

function regionLabel() {
  const p = regionPath.value || []
  if (p.length === 0) return '全部地区'
  return p.join(' / ')
}

function onRegionChange() {
  regionText.value = regionLabel()
}

function resetRegion() {
  regionPath.value = []
  regionText.value = '全部地区'
  loadOverview()
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await getAdminOverview(regionParams())
    const data = res.data || {}
    Object.assign(stats, data.stats || {})
    Object.assign(env, data.env || {})
    Object.assign(alerts, data.alerts || {})
    Object.assign(health, data.healthScore || {})
    latestAlerts.value = data.latestAlerts || []
    weather.value = data.weather || null
    const monitor = data.monitor || {}
    Object.assign(deviceStats, monitor.deviceStats || {})
    Object.assign(serviceStatus, monitor.serviceStatus || {})
    Object.assign(systemOverview, monitor.systemOverview || {})
    weatherLocation.value = regionLabel()
    regionText.value = regionLabel()
  } catch (e) {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

function levelTag(level) {
  return { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }[level] || 'info'
}

function levelLabel(level) {
  return { CRITICAL: '严重', WARNING: '警告', INFO: '提示' }[level] || level
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

loadOverview()
</script>

<style scoped>
.admin-dashboard {
  min-height: 100vh;
  padding: 0;
  background: linear-gradient(180deg, #f3f6fb 0%, #e9eef6 100%);
}

/* ===== 地区选择 ===== */
.region-card {
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.06);
}
.region-card :deep(.el-card__body) {
  padding: 14px 18px;
}
.region-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.region-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.region-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.region-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.region-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  margin-right: 4px;
}

/* ===== 统计卡片 ===== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.1);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-greenhouse { background: #ecf5ff; color: #409EFF; }
.icon-owner { background: #f0f9eb; color: #67C23A; }
.icon-device { background: #fdf6ec; color: #E6A23C; }
.stat-info { min-width: 0; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat-sub { font-size: 13px; font-weight: 400; color: #909399; }
.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 通用区块卡片 ===== */
.row-gap { margin-bottom: 16px; }
.section-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.06);
  margin-bottom: 16px;
}

/* ===== 同行卡片等高对齐 ===== */
.equal-row .el-col {
  display: flex;
}
.equal-row .section-card {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
}
.equal-row .section-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.equal-row .el-table {
  flex: 1;
}
.env-grid {
  flex: 1;
  align-content: center;
}
.alert-overview {
  flex: 1;
}
.weather-card :deep(.el-card__body) {
  justify-content: center;
}
.section-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #f0f2f5;
}
.section-card :deep(.el-card__body) { padding: 16px 18px; }
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.header-note { font-size: 12px; color: #c0c4cc; }

/* ===== 环境聚合 ===== */
.env-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.env-item {
  text-align: center;
  background: #f7f9fc;
  border-radius: 10px;
  padding: 14px 8px;
}
.env-label { font-size: 13px; color: #909399; }
.env-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}
.env-note { margin-top: 12px; font-size: 12px; color: #c0c4cc; }

/* ===== 预警总览 ===== */
.alert-overview {
  display: flex;
  align-items: center;
  gap: 36px;
  padding: 4px 0;
}
.alert-total {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.big-number {
  font-size: 44px;
  font-weight: 700;
  color: #F56C6C;
  font-variant-numeric: tabular-nums;
}
.big-unit { font-size: 13px; color: #909399; }
.alert-levels {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.alert-level {
  display: flex;
  align-items: center;
}
.level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 10px;
}
.alert-level.critical .level-dot { background: #F56C6C; }
.alert-level.warning .level-dot { background: #E6A23C; }
.alert-level.info .level-dot { background: #909399; }
.level-label { width: 40px; font-size: 13px; color: #606266; }
.level-count {
  margin-left: auto;
  min-width: 36px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  border-radius: 10px;
  padding: 2px 10px;
}
.alert-level.critical .level-count { background: #fde2e2; color: #F56C6C; }
.alert-level.warning .level-count { background: #fdf0e0; color: #E6A23C; }
.alert-level.info .level-count { background: #f0f2f5; color: #909399; }

/* ===== 天气 ===== */
.weather-card .weather-info { text-align: center; padding: 10px 0 4px; }
.weather-card :deep(.el-card__body) {
  background: linear-gradient(135deg, #eef7fd 0%, #f7fbff 100%);
}
.weather-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 152, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}
.weather-temp {
  font-size: 42px;
  font-weight: 700;
  color: #FF9800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.temp-unit { font-size: 18px; font-weight: 500; color: #FFB74D; }
.weather-desc { font-size: 15px; color: #606266; margin: 8px 0 10px; }
.weather-details {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}
.wd-item { display: inline-flex; align-items: center; gap: 4px; }

/* ===== 系统监控 ===== */
.monitor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.monitor-block {
  background: #f7f9fc;
  border-radius: 10px;
  padding: 16px;
}
.monitor-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
  margin-bottom: 14px;
}
.bar {
  display: flex;
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  background: #eef0f4;
}
.bar-online { background: linear-gradient(90deg, #67C23A, #95d475); }
.bar-offline { background: #d3d8e0; }
.bar-alarm { background: linear-gradient(90deg, #E6A23C, #f3c97c); }
.bar-legend {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #606266;
  flex-wrap: wrap;
}
.lg-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: 1px;
}
.lg-online { background: #67C23A; }
.lg-offline { background: #d3d8e0; }
.lg-alarm { background: #E6A23C; }
.service-status {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.svc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #303133;
}
.svc-dot { width: 10px; height: 10px; border-radius: 50%; }
.svc-dot.ok { background: #67C23A; }
.svc-dot.bad { background: #F56C6C; }
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.ov-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.ov-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
}
.ov-label { font-size: 12px; color: #909399; margin-top: 2px; }
.monitor-card { margin-bottom: 0; }
</style>
