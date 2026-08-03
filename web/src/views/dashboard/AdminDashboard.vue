<template>
  <div class="admin-dashboard">
    <!-- 地区选择 -->
    <el-card class="region-card" shadow="never">
      <div class="region-row">
        <span class="region-label">查看范围：</span>
        <RegionCascader v-model="regionPath" width="360px" @change="onRegionChange" />
        <el-button type="primary" style="margin-left: 12px" :loading="loading" @click="loadOverview">
          查询
        </el-button>
        <el-button style="margin-left: 8px" @click="resetRegion">全部地区</el-button>
        <span v-if="regionText" class="region-text">当前范围：{{ regionText }}</span>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #409EFF">{{ stats.greenhouseCount }}</div>
          <div class="stat-label">大棚数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #67C23A">
            {{ stats.ownerOnline }}<span class="stat-sub">/ {{ stats.ownerCount }}</span>
          </div>
          <div class="stat-label">农户在线 / 总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #E6A23C">
            {{ stats.deviceOnline }}<span class="stat-sub">/ {{ stats.deviceTotal }}</span>
          </div>
          <div class="stat-label">设备在线 / 总数（离线 {{ stats.deviceOffline }}）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: healthColor }">
            {{ health.score }}<span class="stat-sub"> 分</span>
          </div>
          <div class="stat-label">地区健康评分（{{ health.level }}）</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 环境聚合 + 预警总览 -->
    <el-row :gutter="16" class="middle-row">
      <el-col :span="12">
        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><DataLine /></el-icon>
              <span>地区环境聚合（最新值平均）</span>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col v-for="item in envItems" :key="item.key" :span="6">
              <div class="env-item">
                <div class="env-label">{{ item.label }}</div>
                <div class="env-value">{{ item.avg != null ? item.avg : '--' }}</div>
              </div>
            </el-col>
          </el-row>
          <div class="env-note">采样大棚：{{ env.sampledGreenhouseCount }} 个</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><WarningFilled /></el-icon>
              <span>预警总览（累计）</span>
            </div>
          </template>
          <div class="alert-overview">
            <div class="alert-total">
              <span class="big-number">{{ alerts.total }}</span>
              <span class="big-unit">条</span>
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

    <!-- 最新预警 + 天气 -->
    <el-row :gutter="16" class="bottom-row">
      <el-col :span="15">
        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><BellFilled /></el-icon>
              <span>最新预警（地区内全部农户）</span>
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
        <el-card class="section-card weather-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon size="18"><Cloudy /></el-icon>
              <span>当前天气（{{ weatherLocation }}）</span>
            </div>
          </template>
          <div v-if="weather" class="weather-info">
            <div class="weather-temp">{{ weather.temperature }}°C</div>
            <div class="weather-desc">{{ weather.weather || weather.description }}</div>
            <div class="weather-details">
              <span>湿度 {{ weather.humidity }}%</span>
              <span>风速 {{ weather.windSpeed }}m/s</span>
            </div>
          </div>
          <el-empty v-else description="暂无天气数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统监控（已合并） -->
    <el-card class="section-card monitor-card" shadow="hover">
      <template #header>
        <div class="section-header">
          <el-icon size="18"><Monitor /></el-icon>
          <span>系统运行状态（原系统监控，已并入数据总览）</span>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="monitor-block">
            <div class="monitor-title">设备在线率（全系统）</div>
            <div class="monitor-progress">
              <div class="bar">
                <div class="bar-online" :style="{ width: onlinePercent + '%' }"></div>
                <div class="bar-offline" :style="{ width: offlinePercent + '%' }"></div>
                <div class="bar-alarm" :style="{ width: alarmPercent + '%' }"></div>
              </div>
              <div class="bar-legend">
                <span>在线 {{ deviceStats.online }}</span>
                <span>离线 {{ deviceStats.offline }}</span>
                <span>告警 {{ deviceStats.alarm }}</span>
                <span>共 {{ deviceStats.total }}</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="monitor-block">
            <div class="monitor-title">服务连接状态</div>
            <div class="service-status">
              <span class="svc-item">
                <span :class="['svc-dot', serviceStatus.mqtt ? 'ok' : 'bad']"></span>
                MQTT {{ serviceStatus.mqtt ? '正常' : '断开' }}
              </span>
              <span class="svc-item">
                <span :class="['svc-dot', serviceStatus.database ? 'ok' : 'bad']"></span>
                数据库 {{ serviceStatus.database ? '正常' : '异常' }}
              </span>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="monitor-block">
            <div class="monitor-title">系统数据概览</div>
            <div class="overview-grid">
              <div class="ov-item"><span class="ov-value">{{ systemOverview.greenhouses }}</span><span class="ov-label">大棚</span></div>
              <div class="ov-item"><span class="ov-value">{{ systemOverview.devices }}</span><span class="ov-label">设备</span></div>
              <div class="ov-item"><span class="ov-value">{{ systemOverview.users }}</span><span class="ov-label">用户</span></div>
              <div class="ov-item"><span class="ov-value">{{ systemOverview.rules }}</span><span class="ov-label">预警规则</span></div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import RegionCascader from '@/components/RegionCascader.vue'
import { getAdminOverview } from '@/api/dashboard'
import { DataLine, WarningFilled, BellFilled, Cloudy, Monitor } from '@element-plus/icons-vue'

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
.admin-dashboard { padding: 0; }
.region-card { margin-bottom: 16px; }
.region-row { display: flex; align-items: center; }
.region-label { font-size: 14px; color: #606266; white-space: nowrap; }
.region-text { margin-left: 12px; font-size: 13px; color: #909399; }
.stat-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; }
.stat-sub { font-size: 14px; font-weight: 400; color: #909399; }
.stat-label { margin-top: 4px; font-size: 13px; color: #909399; }
.middle-row { margin-bottom: 16px; }
.section-card { margin-bottom: 16px; }
.section-header { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; }
.env-item { text-align: center; padding: 8px 0; }
.env-label { font-size: 13px; color: #909399; }
.env-value { font-size: 24px; font-weight: 700; color: #303133; margin-top: 4px; }
.env-note { margin-top: 8px; font-size: 12px; color: #c0c4cc; }
.alert-overview { display: flex; align-items: center; gap: 32px; padding: 8px 0; }
.big-number { font-size: 40px; font-weight: 700; color: #F56C6C; }
.big-unit { font-size: 14px; color: #909399; margin-left: 4px; }
.alert-levels { flex: 1; }
.alert-level { display: flex; align-items: center; margin-bottom: 10px; }
.level-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }
.alert-level.critical .level-dot { background: #F56C6C; }
.alert-level.warning .level-dot { background: #E6A23C; }
.alert-level.info .level-dot { background: #909399; }
.level-label { width: 40px; font-size: 13px; color: #606266; }
.level-count { font-size: 18px; font-weight: 600; margin-left: auto; }
.weather-card .weather-info { text-align: center; padding: 8px 0; }
.weather-temp { font-size: 40px; font-weight: 700; color: #FF9800; }
.weather-desc { font-size: 15px; color: #606266; margin: 6px 0; }
.weather-details { display: flex; justify-content: center; gap: 16px; font-size: 13px; color: #909399; }
.monitor-block { padding: 4px 0; }
.monitor-title { font-size: 13px; color: #909399; margin-bottom: 12px; }
.bar { display: flex; height: 14px; border-radius: 7px; overflow: hidden; background: #f0f2f5; }
.bar-online { background: #67C23A; }
.bar-offline { background: #c0c4cc; }
.bar-alarm { background: #E6A23C; }
.bar-legend { display: flex; gap: 12px; margin-top: 8px; font-size: 12px; color: #606266; }
.service-status { display: flex; flex-direction: column; gap: 12px; }
.svc-item { display: flex; align-items: center; font-size: 14px; color: #303133; }
.svc-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }
.svc-dot.ok { background: #67C23A; }
.svc-dot.bad { background: #F56C6C; }
.overview-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.ov-item { display: flex; flex-direction: column; align-items: center; }
.ov-value { font-size: 20px; font-weight: 600; color: #303133; }
.ov-label { font-size: 12px; color: #909399; margin-top: 2px; }
</style>
