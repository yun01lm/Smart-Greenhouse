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
              <div class="weather-desc">{{ weatherData.weather || weatherData.description }}</div>
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
import { getRealtimeData } from '@/api/sensor'
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

let refreshTimer = null

async function loadAll() {
  try {
    const [sensorRes, healthRes, alertRes, unreadRes, weatherRes] = await Promise.allSettled([
      getRealtimeData(props.greenhouseId),
      getHealthScore(props.greenhouseId),
      getAlerts(props.greenhouseId, 1, 5),
      getUnreadAlertCount(props.greenhouseId),
      getCurrentWeather({ greenhouseId: props.greenhouseId })
    ])

    if (sensorRes.status === 'fulfilled' && sensorRes.value?.data) {
      realtimeData.value = sensorRes.value.data
    }
    if (healthRes.status === 'fulfilled' && healthRes.value?.data) {
      healthData.value = healthRes.value.data
    }
    if (alertRes.status === 'fulfilled' && alertRes.value?.data) {
      alerts.value = alertRes.value.data.records || alertRes.value.data || []
    }
    if (unreadRes.status === 'fulfilled' && unreadRes.value?.data != null) {
      unreadCount.value = unreadRes.value.data
    }
    if (weatherRes.status === 'fulfilled' && weatherRes.value?.data) {
      weatherData.value = weatherRes.value.data
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
    if (data) {
      realtimeData.value = { ...realtimeData.value, ...data }
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
  margin: 8px 0;
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
