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
        <div class="expert-region-actions">
          <span v-if="selectedGhName" class="expert-gh-name">当前：{{ selectedGhName }}</span>
          <el-button size="small" type="primary" plain @click="openApplyDialog">申请大棚权限</el-button>
          <el-button size="small" @click="openAuthList">我的申请</el-button>
        </div>
      </div>

      <!-- 专家未授权：整页空白（无卡片、无模拟曲线） -->
      <div v-if="isExpert && !expertLoading && expertGhOptions.length === 0" class="expert-blank">
        <div class="expert-blank-card">
          <el-icon :size="44" color="#94a3b8"><Lock /></el-icon>
          <p class="expert-blank-tip">您还没有被授权查看任何大棚数据</p>
          <div class="expert-blank-actions">
            <el-button type="primary" @click="openApplyDialog">申请大棚权限</el-button>
            <el-button @click="openAuthList">我的申请</el-button>
          </div>
          <div v-if="myAuths.length" class="my-auth-box">
            <div class="my-auth-title">我的申请记录</div>
            <el-table :data="myAuths" size="small" class="my-auth-table">
              <el-table-column prop="greenhouseName" label="大棚" min-width="140" />
              <el-table-column prop="reason" label="申请理由" min-width="160" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="authStatusTag(row.status)" size="small">{{ authStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="申请时间" width="150">
                <template #default="{ row }">{{ formatAuthTime(row.requestedAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>

      <template v-else-if="!isExpert || expertGhOptions.length > 0">
      <!-- 传感器卡片：有数据或加载中显示卡片网格；确认无数据时显示聚合空态 -->
      <SensorCards v-if="!realtimeReady || hasAnySensorData" :data="realtimeData" />
      <div v-else class="sensor-empty-card dashboard-card">
        <el-icon :size="36" color="#64748b"><DataLine /></el-icon>
        <p class="sensor-empty-text">当前大棚暂无传感器数据</p>
        <p class="sensor-empty-hint">请确认设备已接入并正常上报数据</p>
      </div>

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
          <TrendChart
            :history-data="historyData"
            :forecast-data="forecastData"
            :allow-mock="!isExpert"
            :history-range="historyRange"
            :metric-options="METRICS"
            :selected-metrics="selectedMetrics"
            @update:selected-metrics="onMetricsChange"
          />
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
              <div class="weather-loc">{{ weatherData.location }}</div>
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

    <!-- R28：专家申请大棚权限弹窗 -->
    <el-dialog append-to-body v-model="applyDialogVisible" title="申请大棚权限" width="520px">
      <el-form label-width="80px">
        <el-form-item label="选择大棚">
          <el-select v-model="applyForm.greenhouseId" filterable placeholder="按名称或地区搜索大棚" style="width: 100%">
            <el-option
              v-for="g in applyGreenhouses"
              :key="g.greenhouseId"
              :label="g.greenhouseName + (g.ownerName ? '（' + g.ownerName + '）' : '') + ' [' + [g.province, g.city, g.district, g.town, g.village].filter(Boolean).join('-') + ']'"
              :value="g.greenhouseId"
              :disabled="g.status !== 'NONE'"
            >
              <span>{{ g.greenhouseName }}</span>
              <el-tag v-if="g.status === 'PENDING'" size="small" type="warning" style="margin-left: 6px">待审批</el-tag>
              <el-tag v-else-if="g.status === 'APPROVED'" size="small" type="success" style="margin-left: 6px">已授权</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="请简要说明申请查看该大棚数据的原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applySubmitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- R28：我的申请记录弹窗 -->
    <el-dialog append-to-body v-model="authListDialogVisible" title="我的申请记录" width="720px">
      <el-table :data="myAuths" v-loading="myAuthsLoading" size="small">
        <el-table-column prop="greenhouseName" label="大棚" min-width="140" />
        <el-table-column prop="reason" label="申请理由" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="authStatusTag(row.status)" size="small">{{ authStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ formatAuthTime(row.requestedAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="authListDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
import { getApplyAvailable, requestAuthorization, getMyAuthorizations } from '@/api/expert'
import { ElMessage } from 'element-plus'
import realtimeClient from '@/utils/websocket'
import { Cloudy, Location, Lock, DataLine } from '@element-plus/icons-vue'

const props = defineProps({
  greenhouseId: { type: Number, default: 1 }
})

const authStore = useAuthStore()
const viewStore = useViewModeStore()
// R10：管理员进入棚主视角后按棚主版数据总览展示
const isAdmin = computed(() => authStore.isAdmin() && !viewStore.active)
const isExpert = computed(() => authStore.role() === 'EXPERT')

// ===== 专家授权大棚（R27） =====
// ===== R28：专家申请大棚权限 =====
const applyDialogVisible = ref(false)
const applySubmitting = ref(false)
const applyGreenhouses = ref([])
const applyForm = ref({ greenhouseId: null, reason: '' })
const myAuths = ref([])
const myAuthsLoading = ref(false)
const authListDialogVisible = ref(false)

function openApplyDialog() {
  applyDialogVisible.value = true
  applyForm.value = { greenhouseId: null, reason: '' }
  loadApplyAvailable()
}

async function loadApplyAvailable() {
  try {
    const res = await getApplyAvailable()
    applyGreenhouses.value = (res.data || []).filter(g => g.status !== 'APPROVED')
  } catch (e) { /* 拦截器统一处理 */ }
}

async function submitApply() {
  if (!applyForm.value.greenhouseId) { ElMessage.warning('请选择要申请的大棚'); return }
  applySubmitting.value = true
  try {
    const gh = applyGreenhouses.value.find(g => g.greenhouseId === applyForm.value.greenhouseId)
    await requestAuthorization({
      userId: gh ? gh.ownerId : null,
      greenhouseId: applyForm.value.greenhouseId,
      reason: applyForm.value.reason || ''
    })
    ElMessage.success('申请已提交，等待棚主审批')
    applyDialogVisible.value = false
    loadMyAuths()
    loadExpertGreenhouses()
  } catch (e) { /* 拦截器统一处理 */ } finally { applySubmitting.value = false }
}

function openAuthList() {
  authListDialogVisible.value = true
  loadMyAuths()
}

async function loadMyAuths() {
  myAuthsLoading.value = true
  try {
    const res = await getMyAuthorizations()
    myAuths.value = res.data || []
  } catch (e) { /* 拦截器统一处理 */ } finally { myAuthsLoading.value = false }
}

function authStatusLabel(s) {
  const m = { PENDING: '待审批', APPROVED: '已授权', REJECTED: '已拒绝', REVOKED: '已撤销', EXPIRED: '已过期' }
  return m[s] || s
}

function authStatusTag(s) {
  const m = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', REVOKED: 'info', EXPIRED: 'info' }
  return m[s] || 'info'
}

function formatAuthTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

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
  '7d': { ms: 7 * 24 * 3600 * 1000, interval: '1d' },
  '30d': { ms: 30 * 24 * 3600 * 1000, interval: '1d' }
}
const RANGE_LABEL = { '24h': '近24小时', '7d': '近7天', '30d': '近30天' }

// R29：趋势图可选指标（8 种，与后端 Device.SensorType 白名单一致）
const METRICS = [
  { type: 'TEMPERATURE', key: 'temperature', label: '温度', unit: '°C', color: '#FF9800', mockBase: 24, mockAmp: 4 },
  { type: 'HUMIDITY', key: 'humidity', label: '湿度', unit: '%', color: '#2196F3', mockBase: 65, mockAmp: 8 },
  { type: 'CO2', key: 'co2', label: 'CO₂', unit: 'ppm', color: '#4CAF50', mockBase: 450, mockAmp: 60 },
  { type: 'LIGHT', key: 'light', label: '光照', unit: 'lux', color: '#FFC107', mockBase: 25000, mockAmp: 8000 },
  { type: 'SOIL_TEMP', key: 'soilTemperature', label: '土壤温度', unit: '°C', color: '#795548', mockBase: 22, mockAmp: 2 },
  { type: 'SOIL_MOISTURE', key: 'soilHumidity', label: '土壤湿度', unit: '%', color: '#00BCD4', mockBase: 55, mockAmp: 10 },
  { type: 'SOIL_PH', key: 'soilPh', label: '土壤pH', unit: 'pH', color: '#9C27B0', mockBase: 6.5, mockAmp: 0.4 },
  { type: 'WIND_SPEED', key: 'windSpeed', label: '风速', unit: 'm/s', color: '#607D8B', mockBase: 2, mockAmp: 1 }
]

// 勾选显示的指标（默认温度+湿度；至少保留 1 项）
const selectedMetrics = ref(['TEMPERATURE', 'HUMIDITY'])

/** 生效中的大棚ID：专家用自选授权大棚，其他角色用全局选择器 */
const effectiveGreenhouseId = computed(() => (isExpert.value ? selectedGhId.value : props.greenhouseId))

const realtimeData = ref({})
const realtimeReady = ref(false)
/** 实时数据里是否已有任一传感器读数（用于无数据时聚合空态） */
const hasAnySensorData = computed(() =>
  Object.values(TYPE_TO_KEY).some(k => realtimeData.value[k] != null)
)
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
  SOIL_MOISTURE: 'soilHumidity',
  SOIL_PH: 'soilPh',
  WIND_SPEED: 'windSpeed'
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

/** 时间格式化：24h → HH:00；7d/30d → MM-DD（R29 日汇总粒度） */
function fmtHistoryTime(ts, range) {
  const d = new Date(ts)
  const pad = (n) => String(n).padStart(2, '0')
  if (range === '24h') return `${pad(d.getHours())}:00`
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 预测时间格式化：24h → HH:mm；7d/30d → MM-DD HH:mm */
function fmtForecastTime(ts, range) {
  const d = new Date(ts)
  const pad = (n) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return range === '24h' ? hm : `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${hm}`
}

/** 合并多指标历史点为趋势图需要的 [{ time, temperature, humidity, co2, ... }] */
function buildHistory(byType, range) {
  const map = new Map()
  for (const [type, points] of Object.entries(byType || {})) {
    const key = TYPE_TO_KEY[type]
    if (!key) continue
    for (const p of points || []) {
      const k = fmtHistoryTime(p.timestamp, range)
      if (!map.has(k)) map.set(k, { time: k })
      map.get(k)[key] = p.value
    }
  }
  return [...map.values()].sort((a, b) => (a.time < b.time ? -1 : 1))
}

/** 合并多指标预测点为 [{ time, temperature, humidity, co2, ... }] */
function buildForecast(byType, range) {
  const map = new Map()
  for (const [type, points] of Object.entries(byType || {})) {
    const key = TYPE_TO_KEY[type]
    if (!key) continue
    for (const p of points || []) {
      const k = fmtForecastTime(p.timestamp, range)
      if (!map.has(k)) map.set(k, { time: k })
      map.get(k)[key] = p.value
    }
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

/** 趋势图指标勾选变化：更新选中集合并重新拉取曲线数据（至少保留 1 项） */
function onMetricsChange(types) {
  if (!types || types.length === 0) return
  selectedMetrics.value = types
  loadAll()
}

let refreshTimer = null
/**
 * 加载竞态序号：30s 轮询 / 切时间范围 / 切指标都会触发 loadAll，
 * 若旧请求晚于新请求返回会覆盖新数据（表现为曲线"卡住/不显示/错乱"）。
 * 只接受最新一次请求的结果，其余丢弃。
 */
let loadSeq = 0

async function loadAll() {
  const seq = ++loadSeq
  try {
    const ghId = effectiveGreenhouseId.value
    if (!ghId) return
    const endTime = Date.now()
    const range = RANGE_CONFIG[historyRange.value] || RANGE_CONFIG['24h']
    const startTime = endTime - range.ms
    const metricTypes = selectedMetrics.value
    const historyPromises = metricTypes.map(type =>
      getSensorHistory(ghId, { sensorType: type, startTime, endTime, interval: range.interval })
    )
    const forecastPromises = metricTypes.map(type =>
      getSensorForecast(ghId, type, 4, 30)
    )
    const [sensorRes, healthRes, alertRes, unreadRes, weatherRes, ...curveResults] = await Promise.allSettled([
      getRealtimeData(ghId),
      getHealthScore(ghId),
      getAlerts(ghId, 1, 5),
      getUnreadAlertCount(ghId),
      getCurrentWeather({ greenhouseId: ghId }),
      ...historyPromises,
      ...forecastPromises
    ])

    // 期间已有更新的加载请求，本次结果全部丢弃（防竞态覆盖）
    if (seq !== loadSeq) return

    if (sensorRes.status === 'fulfilled' && sensorRes.value?.data) {
      realtimeData.value = flattenRealtime(sensorRes.value.data)
    }
    realtimeReady.value = true
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
    const histRes = curveResults.slice(0, metricTypes.length)
    const fcRes = curveResults.slice(metricTypes.length)
    const byTypeHist = {}
    const byTypeFc = {}
    metricTypes.forEach((type, i) => {
      if (histRes[i]?.status === 'fulfilled') byTypeHist[type] = histRes[i].value?.data || []
      if (fcRes[i]?.status === 'fulfilled') byTypeFc[type] = fcRes[i].value?.data?.points || []
    })
    if (Object.keys(byTypeHist).length) {
      historyData.value = buildHistory(byTypeHist, historyRange.value)
    }
    if (Object.keys(byTypeFc).length) {
      forecastData.value = buildForecast(byTypeFc, historyRange.value)
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
    loadMyAuths()
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
  loadSeq++ // 组件卸载后丢弃一切挂起响应
  realtimeClient.disconnect()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

/* 无数据聚合空态卡片 */
.sensor-empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 20px;
  text-align: center;
}
.sensor-empty-text {
  font-size: 15px;
  color: #a0aec0;
}
.sensor-empty-hint {
  font-size: 13px;
  color: #64748b;
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

.expert-region-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.expert-blank {
  min-height: 420px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 60px;
}

.expert-blank-card {
  width: 720px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px dashed rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  color: #e0e6ed;
}

.expert-blank-tip {
  margin: 12px 0 16px;
  font-size: 15px;
  color: #a0aec0;
}

.expert-blank-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

.my-auth-box {
  margin-top: 12px;
  text-align: left;
}

.my-auth-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 8px;
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
