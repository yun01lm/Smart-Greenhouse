<template>
  <div class="dashboard-page">
    <!-- 管理员：地区数据总览（含系统监控） -->
    <AdminDashboard v-if="isAdmin" />

    <!-- 农户/员工/专家：按大棚的数据总览 -->
    <template v-else>
      <!-- 传感器卡片 -->
      <SensorCards :data="realtimeData" />

      <!-- 中部：趋势图 + 健康评分 -->
      <div class="middle-row">
        <div class="chart-area">
          <TrendChart :history-data="historyData" />
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
import { getRealtimeData, getSensorHistory } from '@/api/sensor'
import { getHealthScore } from '@/api/health'
import { getAlerts, getUnreadAlertCount } from '@/api/alert'
import { getCurrentWeather } from '@/api/weather'
import realtimeClient from '@/utils/websocket'
import { Cloudy } from '@element-plus/icons-vue'

const props = defineProps({
  greenhouseId: { type: Number, default: 1 }
})

const authStore = useAuthStore()
const viewStore = useViewModeStore()
// R10：管理员进入棚主视角后按棚主版数据总览展示
const isAdmin = computed(() => authStore.isAdmin() && !viewStore.active)

const realtimeData = ref({})
const historyData = ref([])
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

let refreshTimer = null

async function loadAll() {
  try {
    const endTime = Date.now()
    const startTime = endTime - 24 * 3600 * 1000
    const [sensorRes, healthRes, alertRes, unreadRes, weatherRes, tempHistRes, humHistRes] = await Promise.allSettled([
      getRealtimeData(props.greenhouseId),
      getHealthScore(props.greenhouseId),
      getAlerts(props.greenhouseId, 1, 5),
      getUnreadAlertCount(props.greenhouseId),
      getCurrentWeather({ greenhouseId: props.greenhouseId }),
      getSensorHistory(props.greenhouseId, { sensorType: 'TEMPERATURE', startTime, endTime, interval: '1h' }),
      getSensorHistory(props.greenhouseId, { sensorType: 'HUMIDITY', startTime, endTime, interval: '1h' })
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
      unreadCount.value = unreadRes.value.data
    }
    if (weatherRes.status === 'fulfilled' && weatherRes.value?.data) {
      weatherData.value = weatherRes.value.data
    }
    if (tempHistRes.status === 'fulfilled' || humHistRes.status === 'fulfilled') {
      const temps = tempHistRes.status === 'fulfilled' ? tempHistRes.value?.data || [] : []
      const hums = humHistRes.status === 'fulfilled' ? humHistRes.value?.data || [] : []
      historyData.value = buildHistory(temps, hums)
    }
  } catch (e) {
    // 静默处理
  }
}

// WebSocket 实时数据更新
function setupWebSocket() {
  realtimeClient.disconnect()
  realtimeClient.connect(props.greenhouseId)
  realtimeClient.onMessage(`/topic/greenhouse/${props.greenhouseId}/realtime`, (data) => {
    if (data && data.sensorType) {
      const key = TYPE_TO_KEY[data.sensorType]
      if (key && data.value != null) {
        realtimeData.value = { ...realtimeData.value, [key]: data.value }
      }
    }
  })
}

// 大棚切换时重新加载
watch(() => props.greenhouseId, (newId) => {
  if (isAdmin.value) return
  loadAll()
  setupWebSocket()
})

onMounted(() => {
  if (isAdmin.value) return
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
