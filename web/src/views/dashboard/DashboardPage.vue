<template>
  <div class="dashboard-page">
    <!-- 管理员：地区数据总览（含系统监控） -->
    <AdminDashboard v-if="isAdmin" />

    <!-- 农户/员工/专家：按大棚的数据总览 -->
    <template v-else>
      <!-- 专家（R27）：授权大棚地区级联选择 -->
      <div v-if="isExpert" class="expert-region-row">
        <div class="expert-region-inner">
          <el-icon :size="16"><Location /></el-icon>
          <span class="expert-region-label">查看大棚</span>
          <el-cascader
            v-if="expertGhOptions.length"
            v-model="expertGhPath"
            :options="expertGhOptions"
            placeholder="选择已授权大棚（按地区分类）"
            clearable
            style="width: 420px"
            @change="onExpertGhChange"
          />
          <span v-else-if="expertLoading" class="expert-loading-text">加载授权大棚中...</span>
        </div>
        <span v-if="selectedGhName" class="expert-gh-name">当前：{{ selectedGhName }}</span>
      </div>

      <!-- 专家未授权：整页空白（无卡片、无模拟曲线） -->
      <div v-if="isExpert && !expertLoading && expertGhOptions.length === 0" class="expert-blank"></div>

      <template v-else-if="!isExpert || expertGhOptions.length > 0">
      <!-- 传感器卡片 -->
      <SensorCards :data="realtimeData" />

      <!-- 历史数据时间范围（R27） -->
      <div v-if="!isAdmin" class="range-toolbar">
        <el-radio-group v-model="historyRange" size="small" @change="onRangeChange">
          <el-radio-button value="24h">近24小时</el-radio-button>
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d">近30天</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 中部：趋势图 + 健康评分 -->
      <div class="middle-row">
        <div class="chart-area">
          <TrendChart :history-data="historyData" :forecast-data="forecastData" :allow-mock="!isExpert" />
        </div>
        <div class="score-area">
          <HealthScore :data="healthData" />
        </div>
      </div>

      <!-- 底部：预警列表 + 天气 -->
      <div class="bottom-row">
        <div class="alert-area">
          <AlertList :alerts="alerts" :unread-count="unreadCount" />
        </div>
        <div class="weather-area">
          <div class="weather-card dashboard-card">
            <h3>当前天气</h3>
            <div v-if="weatherData" class="weather-info">
              <div class="weather-temp">{{ weatherData.temperature }}°C</div>
              <div class="weather-desc">{{ weatherData.weatherText }}</div>
              <div class="weather-loc">📍 {{ weatherData.location }}</div>
              <div class="weather-details">
                <span>湿度 {{ weatherData.humidity }}%</span>
                <span>风速 {{ weatherData.windSpeed }}m/s</span>
              </div>
            </div>
            <div v-else class="weather-empty">
              <el-icon :size="32" color="#a0aec0"><Cloudy /></el-icon>
              <p>暂无天气数据</p>
            </div>
          </div>
        </div>
      </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useViewModeStore } from '@/stores/viewMode'
import AdminDashboard from './AdminDashboard.vue'
import SensorCards from './SensorCards.vue'
import TrendChart from './TrendChart.vue'
import HealthScore from './HealthScore.vue'
import AlertList from './AlertList.vue'
import { getRealtimeData, getSensorHistory, getSensorForecast } from '@/api/sensor'
import { getHealthScore } from '@/api/health'
import { getAlerts, getUnreadAlertCount } from '@/api/alert'
import { getCurrentWeather } from '@/api/weather'
import { getGreenhouses } from '@/api/greenhouse'
import realtimeClient from '@/utils/websocket'
import { Cloudy, Location } from '@element-plus/icons-vue'

const props = defineProps({
  greenhouseId: { type: Number, default: 1 }
})

const authStore = useAuthStore()
const viewStore = useViewModeStore()
// R10：管理员进入棚主视角后按棚主版数据总览展示
const isAdmin = computed(() => authStore.isAdmin() && !viewStore.active)
const isExpert = computed(() => authStore.role() === 'EXPERT')

// ===== 专家授权大棚（R27） =====
const expertLoading = ref(false)
const expertGreenhouses = ref([])
const expertGhOptions = ref([])
const expertGhPath = ref([])
const selectedGhId = ref(null)
const selectedGhName = ref('')

// 历史数据时间范围（R27）
const historyRange = ref('24h')
const RANGE_CONFIG = {
  '24h': { ms: 24 * 3600 * 1000, interval: '1h' },
  '7d': { ms: 7 * 24 * 3600 * 1000, interval: '6h' },
  '30d': { ms: 30 * 24 * 3600 * 1000, interval: '1d' }
}

/** 生效中的大棚ID：专家用自选授权大棚，其他角色用全局选择器 */
const effectiveGreenhouseId = computed(() => (isExpert.value ? selectedGhId.value : props.greenhouseId))

const realtimeData = ref({})
const historyData = ref([])
const forecastData = ref([])
const healthData = ref(null)
const alerts = ref([])
const unreadCount = ref(0)
const weatherData = ref(null)

// 后端传感器类型 → 前端卡片字段（SensorCards 期望扁平结构）
const TYPE_TO_KEY = {
  TEMPERATURE: 'temperature',
  HUMIDITY: 'humidity',
  CO2: 'co2',
  LIGHT: 'light',
  SOIL_TEMP: 'soilTemperature',
  SOIL_MOISTURE: 'soilHumidity'
}

/** 把后端 dataByType 嵌套结构拍平为 SensorCards 需要的扁平字段 */
function flattenRealtime(data) {
  if (!data) return {}
  const flat = { greenhouseId: data.greenhouseId, greenhouseName: data.greenhouseName }
  const byType = data.dataByType || {}
  for (const [type, points] of Object.entries(byType)) {
    const key = TYPE_TO_KEY[type]
    if (key && points && points.length > 0 && points[0].value != null) {
      flat[key] = points[0].value
    }
  }
  return flat
}

/** 合并温度/湿度历史点为趋势图需要的 [{ time, temperature, humidity }] */
function buildHistory(tempPoints, humPoints) {
  const map = new Map()
  const hour = (ts) => {
    const d = new Date(ts)
    return `${String(d.getHours()).padStart(2, '0')}:00`
  }
  for (const p of tempPoints || []) {
    const k = hour(p.timestamp)
    map.set(k, { time: k, temperature: p.value, humidity: null })
  }
  for (const p of humPoints || []) {
    const k = hour(p.timestamp)
    if (map.has(k)) {
      map.get(k).humidity = p.value
    } else {
      map.set(k, { time: k, temperature: null, humidity: p.value })
    }
  }
  return [...map.values()].sort((a, b) => (a.time < b.time ? -1 : 1))
}

/** 合并温度/湿度预测点为 [{ time, temperature, humidity }]（保留分钟粒度） */
function buildForecast(tempPoints, humPoints) {
  const map = new Map()
  const fmt = (ts) => {
    const d = new Date(ts)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  for (const p of tempPoints || []) {
    const k = fmt(p.timestamp)
    map.set(k, { time: k, temperature: p.value, humidity: null })
  }
  for (const p of humPoints || []) {
    const k = fmt(p.timestamp)
    if (map.has(k)) map.get(k).humidity = p.value
    else map.set(k, { time: k, temperature: null, humidity: p.value })
  }
  return [...map.values()].sort((a, b) => (a.time < b.time ? -1 : 1))
}

function findOrCreate(children, value, label) {
  let node = children.find(n => n.value === value)
  if (!node) {
    node = { value, label, children: [] }
    children.push(node)
  }
  return node
}

/** 把授权大棚按 省→市→县→乡镇→村→棚主→大棚 构建级联选项 */
function buildExpertOptions(list) {
  const root = []
  for (const gh of list) {
    const p = findOrCreate(root, gh.province || '未知省份', gh.province || '未知省份')
    const c1 = findOrCreate(p.children, p.value + '|' + (gh.city || '未知城市'), gh.city || '未知城市')
    const d = findOrCreate(c1.children, c1.value + '|' + (gh.district || '未知区县'), gh.district || '未知区县')
    const t = findOrCreate(d.children, d.value + '|' + (gh.town || '未知乡镇'), gh.town || '未知乡镇')
    const v = findOrCreate(t.children, t.value + '|' + (gh.village || '未知村'), gh.village || '未知村')
    const owner = findOrCreate(v.children, v.value + '|' + (gh.ownerName || '未知棚主'), gh.ownerName || '未知棚主')
    owner.children.push({ value: gh.id, label: gh.name })
  }
  return root
}

/** 取第一个叶子节点的级联路径（默认选中） */
function firstLeafPath(options) {
  const path = []
  let level = options
  while (level && level.length) {
    const first = level[0]
    path.push(first.value)
    level = first.children
  }
  return path
}

function onExpertGhChange(path) {
  const ghId = path && path.length ? Number(path[path.length - 1]) : null
  selectedGhId.value = ghId
  selectedGhName.value = ghId
    ? (expertGreenhouses.value.find(g => g.id === ghId)?.name || '')
    : ''
}

async function loadExpertGreenhouses() {
  expertLoading.value = true
  try {
    const res = await getGreenhouses()
    expertGreenhouses.value = res.data || []
    expertGhOptions.value = buildExpertOptions(expertGreenhouses.value)
    if (expertGhOptions.value.length) {
      expertGhPath.value = firstLeafPath(expertGhOptions.value)
      const ghId = Number(expertGhPath.value[expertGhPath.value.length - 1])
      selectedGhId.value = ghId
      selectedGhName.value = expertGreenhouses.value.find(g => g.id === ghId)?.name || ''
    }
  } catch (e) {
    // 拦截器统一处理
  } finally {
    expertLoading.value = false
  }
}

function onRangeChange() {
  loadAll()
}

let refreshTimer = null

async function loadAll() {
  try {
    const ghId = effectiveGreenhouseId.value
    if (!ghId) return
    const endTime = Date.now()
    const range = RANGE_CONFIG[historyRange.value] || RANGE_CONFIG['24h']
    const startTime = endTime - range.ms
    const [sensorRes, healthRes, alertRes, unreadRes, weatherRes, tempHistRes, humHistRes, tempForecastRes, humForecastRes] = await Promise.allSettled([
      getRealtimeData(ghId),
      getHealthScore(ghId),
      getAlerts(ghId, 1, 5),
      getUnreadAlertCount(ghId),
      getCurrentWeather({ greenhouseId: ghId }),
      getSensorHistory(ghId, { sensorType: 'TEMPERATURE', startTime, endTime, interval: range.interval }),
      getSensorHistory(ghId, { sensorType: 'HUMIDITY', startTime, endTime, interval: range.interval }),
      getSensorForecast(ghId, 'TEMPERATURE', 4, 30),
      getSensorForecast(ghId, 'HUMIDITY', 4, 30)
    ])

    if (sensorRes.status === 'fulfilled' && sensorRes.value?.data) {
      realtimeData.value = flattenRealtime(sensorRes.value.data)
    }
    if (healthRes.status === 'fulfilled' && healthRes.value?.data) {
      healthData.value = healthRes.value.data
    }
    if (alertRes.status === 'fulfilled' && alertRes.value?.data) {
      const d = alertRes.value.data
      alerts.value = d.records || d.list || []
    }
    if (unreadRes.status === 'fulfilled' && unreadRes.value?.data != null) {
      // 后端返回 { count: N }，兼容纯数字两种形态
      const uc = unreadRes.value.data
      unreadCount.value = typeof uc === 'number' ? uc : (uc && uc.count) || 0
    }
    if (weatherRes.status === 'fulfilled' && weatherRes.value?.data) {
      weatherData.value = weatherRes.value.data
    }
    if (tempHistRes.status === 'fulfilled' || humHistRes.status === 'fulfilled') {
      const temps = tempHistRes.status === 'fulfilled' ? tempHistRes.value?.data || [] : []
      const hums = humHistRes.status === 'fulfilled' ? humHistRes.value?.data || [] : []
      historyData.value = buildHistory(temps, hums)
    }
    if (tempForecastRes.status === 'fulfilled' || humForecastRes.status === 'fulfilled') {
      const fTemps = tempForecastRes.status === 'fulfilled' ? tempForecastRes.value?.data?.points || [] : []
      const fHums = humForecastRes.status === 'fulfilled' ? humForecastRes.value?.data?.points || [] : []
      forecastData.value = buildForecast(fTemps, fHums)
    }
  } catch (e) {
    // 静默处理
  }
}

// WebSocket 实时数据更新
function setupWebSocket() {
  const ghId = effectiveGreenhouseId.value
  if (!ghId) return
  realtimeClient.disconnect()
  realtimeClient.connect(ghId)
  realtimeClient.onMessage(`/topic/greenhouse/${ghId}/realtime`, (data) => {
    if (data && data.sensorType) {
      const key = TYPE_TO_KEY[data.sensorType]
      if (key && data.value != null) {
        realtimeData.value = { ...realtimeData.value, [key]: data.value }
      }
    }
  })
}

// 大棚切换时重新加载
watch(() => effectiveGreenhouseId.value, (newId) => {
  if (isAdmin.value || !newId) return
  loadAll()
  setupWebSocket()
})

onMounted(async () => {
  if (isAdmin.value) return
  if (isExpert.value) {
    await loadExpertGreenhouses()
    if (!selectedGhId.value) return // 未授权：空白
  }
  loadAll()
  setupWebSocket()
  // 30 秒轮询作为兜底
  refreshTimer = setInterval(loadAll, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  realtimeClient.disconnect()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.expert-region-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 10px 16px;
  margin-bottom: 16px;
}

.expert-region-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
}

.expert-region-label {
  font-size: 13px;
  color: #e0e6ed;
}

.expert-loading-text {
  font-size: 13px;
  color: #94a3b8;
}

.expert-gh-name {
  font-size: 13px;
  color: #94a3b8;
}

.expert-blank {
  min-height: 420px;
}

.range-toolbar {
  margin: 16px 0 4px;
  display: flex;
  justify-content: flex-end;
}

.middle-row {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.chart-area {
  flex: 1;
}

.score-area {
  width: 320px;
  flex-shrink: 0;
}

.bottom-row {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.alert-area {
  flex: 1;
}

.weather-area {
  width: 320px;
  flex-shrink: 0;
}

.weather-card {
  padding: 20px;
}

.weather-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 16px;
}

.weather-temp {
  font-size: 42px;
  font-weight: 700;
  color: #FF9800;
  font-family: 'DIN', monospace;
}

.weather-desc {
  font-size: 16px;
  color: #e0e6ed;
  margin: 8px 0 2px;
}

.weather-loc {
  font-size: 12px;
  color: #a0aec0;
  margin: 0 0 8px;
}

.weather-details {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #a0aec0;
}

.weather-empty {
  text-align: center;
  padding: 24px;
  color: #a0aec0;
}

.weather-empty p {
  margin-top: 8px;
  font-size: 14px;
}
</style>
