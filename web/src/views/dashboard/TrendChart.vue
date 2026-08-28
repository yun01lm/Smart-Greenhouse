<template>
  <div class="trend-chart dashboard-card">
    <div class="chart-header">
      <h3>环境趋势与短期预测</h3>
      <div class="chart-header-right">
        <!-- 自定义图例/指标勾选：颜色点 + 名称（顺序固定为定义顺序，不受勾选顺序影响） -->
        <div class="metric-checks">
          <el-checkbox
            v-for="(m, idx) in props.metricOptions"
            :key="m.type"
            :model-value="selected.includes(m.type)"
            :disabled="!selected.includes(m.type) && selected.length >= MAX_METRICS"
            size="small"
            class="metric-check"
            @change="(val) => onToggle(m, idx, val)"
          >
            <span class="metric-dot" :style="{ background: m.color }"></span>
            <span class="metric-name">{{ m.label }}</span>
          </el-checkbox>
        </div>
        <span class="chart-note">{{ RANGE_LABEL[props.historyRange] || '近24小时' }} · {{ hasForecast ? '预测未来2小时' : '暂无预测数据' }}</span>
      </div>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const props = defineProps({
  historyData: { type: Array, default: () => [] },
  forecastData: { type: Array, default: () => [] },
  allowMock: { type: Boolean, default: true },
  historyRange: { type: String, default: '24h' },
  metricOptions: { type: Array, default: () => [] },
  selectedMetrics: { type: Array, default: () => ['TEMPERATURE', 'HUMIDITY'] }
})

const emit = defineEmits(['update:selectedMetrics'])

const RANGE_LABEL = { '24h': '近24小时', '7d': '近7天', '30d': '近30天' }
/** 同时最多显示的指标数（多轴可读性上限） */
const MAX_METRICS = 4

const chartRef = ref(null)
let chart = null

// ===== 结构缓存：用于增量渲染判定 =====
let lastStructureKey = ''
let mockCache = null   // 模拟数据缓存（key 变化时重建，避免每 30s 随机重生成）
let mockCacheKey = ''

/**
 * 选择状态以组件内部 ref 为准（同步更新，竞态免疫）：
 * 快速连点时父级 prop 回传存在延迟，若直接读 prop 会导致前几次点击丢失。
 * pendingEmit 记录自己发出的最新值：prop 回传与它一致时忽略（避免旧 prop 覆盖新状态），
 * 不一致时才认为来自外部（如父级重置）并同步进来。
 */
const selRef = ref([...(props.selectedMetrics || [])])
let pendingEmit = null

watch(() => props.selectedMetrics, (v) => {
  const next = (v || []).join(',')
  if (pendingEmit === next) {
    pendingEmit = null
    return
  }
  const cur = selRef.value.join(',')
  if (cur !== next) selRef.value = [...(v || [])]
})

/** 是否勾选了某指标（数组顺序无意义，按定义顺序展示） */
const selected = computed(() => selRef.value)

const hasForecast = computed(() => (props.forecastData || []).length > 0)

/** 勾选切换：限制最多 MAX_METRICS 项、至少保留 1 项，顺序始终按定义顺序 */
function onToggle(metric, idx, val) {
  const set = new Set(selRef.value)
  if (val) {
    if (set.size >= MAX_METRICS) {
      ElMessage.warning(`最多同时显示 ${MAX_METRICS} 个指标`)
      return
    }
    set.add(metric.type)
  } else {
    if (set.size <= 1) {
      ElMessage.warning('至少保留一个指标')
      return
    }
    set.delete(metric.type)
  }
  const ordered = props.metricOptions.filter(m => set.has(m.type)).map(m => m.type)
  selRef.value = ordered
  pendingEmit = ordered.join(',')
  emit('update:selectedMetrics', ordered)
}

/** 当前选中的指标元信息（始终按 metricOptions 定义顺序） */
function selectedMeta() {
  const set = new Set(selected.value)
  return props.metricOptions.filter(m => set.has(m.type))
}

/** 计算 y 轴刻度范围 */
function axisRange(values) {
  const all = (values || []).filter(v => v != null && !Number.isNaN(v))
  if (all.length === 0) return null
  const min = Math.min(...all)
  const max = Math.max(...all)
  let pad = (max - min) * 0.15
  if (pad < 0.5) pad = 0.5
  return { min: +(min - pad).toFixed(2), max: +(max + pad).toFixed(2) }
}

function areaGradient(color) {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: color + '33' },
    { offset: 1, color: color + '05' }
  ])
}

/**
 * 构建 Y 轴布局：轴位置由指标在 metricOptions 定义表中的固定序号决定
 * （第 1 个恒左、第 2 个恒右、第 3 个左偏移、第 4 个右偏移），
 * 与勾选顺序、勾选组合无关 —— 修复"轴标识左右跳变"。
 */
function buildAxes(metrics) {
  const axes = metrics.map(m => {
    const defIdx = Math.max(0, props.metricOptions.findIndex(o => o.type === m.type))
    return {
      unit: m.unit,
      metricType: m.type,
      position: defIdx % 2 === 0 ? 'left' : 'right',
      offset: Math.floor(defIdx / 2) * 50
    }
  })
  const unitIndex = {}
  axes.forEach((a, i) => {
    unitIndex[a.metricType] = i
  })
  return { axes, unitIndex }
}

/** 组装系列（历史 + 预测虚线）与轴数据 */
function assemble(metrics, history, future) {
  const { axes, unitIndex } = buildAxes(metrics)
  const boundary = history.length - 1
  const hasFc = future.length > 0

  const series = []
  const axisValues = axes.map(() => [])
  for (const m of metrics) {
    const key = m.key
    const histVals = history.map(d => (d[key] != null ? d[key] : null))
    let fcVals = []
    if (hasFc) {
      fcVals = [
        ...Array(Math.max(0, boundary)).fill(null),
        histVals[boundary],
        ...future.map(d => (d[key] != null ? d[key] : null))
      ]
    }
    const yi = unitIndex[m.type]
    axisValues[yi].push(...histVals.filter(v => v != null), ...fcVals.filter(v => v != null))
    series.push({
      name: `${m.label} (${m.unit})`,
      type: 'line',
      smooth: true,
      yAxisIndex: yi,
      data: histVals,
      lineStyle: { color: m.color, width: 2 },
      itemStyle: { color: m.color },
      areaStyle: { color: areaGradient(m.color) }
    })
    series.push({
      name: `预测${m.label} (${m.unit})`,
      type: 'line',
      smooth: true,
      yAxisIndex: yi,
      data: fcVals,
      lineStyle: { color: m.color, width: 2, type: 'dashed' },
      itemStyle: { color: m.color },
      symbol: 'circle',
      symbolSize: 5,
      markLine: hasFc
        ? {
            silent: true,
            symbol: 'none',
            lineStyle: { color: '#64748b', type: 'dashed' },
            label: { formatter: '现在', color: '#94a3b8', fontSize: 11, position: 'insideEndTop' },
            data: [{ xAxis: boundary }]
          }
        : { data: [] }
    })
  }
  return { axes, unitIndex, series, axisValues }
}

/** 全量渲染（指标组合 / 时间范围 / 预测有无 / 空态 等结构变化时） */
function fullRender(history, future, metrics) {
  if (!chart) return
  if (history.length === 0) {
    chart.setOption({ xAxis: { data: [] }, yAxis: [], series: [] }, { notMerge: true })
    return
  }
  const times = [...history.map(d => d.time), ...future.map(d => d.time)]
  const { axes, series, axisValues } = assemble(metrics, history, future)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: { show: false },
    grid: {
      left: '4%',
      right: '10%',
      bottom: '3%',
      top: '16px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: times,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: axes.map((a, i) => {
      const r = axisRange(axisValues[i])
      return {
        type: 'value',
        name: a.unit,
        nameTextStyle: { color: '#94a3b8' },
        position: a.position,
        offset: a.offset,
        axisLabel: { color: '#94a3b8' },
        splitLine: i < 2 ? { lineStyle: { color: '#1e293b' } } : { show: false },
        min: r?.min ?? null,
        max: r?.max ?? null
      }
    }),
    series
  }, { notMerge: true })
}

/** 增量渲染：仅数据更新（轮询/范围切换）时合并更新，不重置图例与轴状态 */
function dataRender(history, future, metrics) {
  if (!chart) return
  const times = [...history.map(d => d.time), ...future.map(d => d.time)]
  const { axes, series, axisValues } = assemble(metrics, history, future)
  chart.setOption({
    xAxis: { data: times },
    yAxis: axes.map((a, i) => {
      const r = axisRange(axisValues[i])
      return { min: r?.min ?? null, max: r?.max ?? null }
    }),
    series: series.map(s => ({ name: s.name, data: s.data, markLine: s.markLine }))
  }, { notMerge: false })
}

/** 模拟数据（缓存：同一结构 key 下不重新随机生成，曲线保持稳定） */
function getMock(metrics) {
  const key = `${props.historyRange}|${metrics.map(m => m.type).join(',')}`
  if (mockCache && mockCacheKey === key) return mockCache

  const { axes, unitIndex } = buildAxes(metrics)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const is24h = props.historyRange === '24h'
  const count = props.historyRange === '30d' ? 30 : props.historyRange === '7d' ? 7 : 24
  const stepMs = is24h ? 3600000 : 86400000
  const points = []
  for (let i = count - 1; i >= 0; i--) {
    const t = new Date(now - i * stepMs)
    const time = is24h
      ? `${pad(t.getHours())}:00`
      : `${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
    const p = { time }
    for (const m of metrics) {
      const wave = Math.sin(i / 4 + m.type.length) * m.mockAmp * 0.6
      const noise = (Math.random() - 0.5) * m.mockAmp * 0.4
      p[m.key] = +(m.mockBase + wave + noise).toFixed(1)
    }
    points.push(p)
  }
  const axisValues = axes.map(() => [])
  for (const m of metrics) {
    const yi = unitIndex[m.type]
    for (const p of points) axisValues[yi].push(p[m.key])
  }
  mockCache = { points, axisValues }
  mockCacheKey = key
  return mockCache
}

/** 当前渲染的结构 key：指标组合 / 时间范围 / 预测有无 / 数据有无 */
function structureKey(metrics, history, future) {
  return [
    metrics.map(m => m.type).join(','),
    props.historyRange,
    future.length > 0 ? 'fc' : 'nofc',
    history.length === 0 ? 'empty' : 'data'
  ].join('|')
}

function render() {
  if (!chart) return
  const metrics = selectedMeta()
  let history = Array.isArray(props.historyData) ? props.historyData : []
  const future = Array.isArray(props.forecastData) ? props.forecastData : []

  // 无真实数据：允许模拟时用缓存模拟数据填充
  if (history.length === 0 && props.allowMock && metrics.length > 0) {
    const mock = getMock(metrics)
    history = mock.points
    // 模拟曲线不画预测段，保持"现在"线不出现
    const key = structureKey(metrics, history, [])
    if (lastStructureKey !== key) {
      mockRender(metrics, mock)
      lastStructureKey = key
    }
    return
  }

  const key = structureKey(metrics, history, future)
  if (lastStructureKey !== key) {
    fullRender(history, future, metrics)
    lastStructureKey = key
  } else {
    dataRender(history, future, metrics)
  }
}

/** 模拟数据全量渲染（无预测段） */
function mockRender(metrics, mock) {
  if (!chart) return
  const times = mock.points.map(p => p.time)
  const { axes, unitIndex } = buildAxes(metrics)
  const series = []
  for (const m of metrics) {
    const yi = unitIndex[m.type]
    const vals = mock.points.map(p => (p[m.key] != null ? p[m.key] : null))
    series.push({
      name: `${m.label} (${m.unit})`,
      type: 'line',
      smooth: true,
      yAxisIndex: yi,
      data: vals,
      lineStyle: { color: m.color, width: 2 },
      itemStyle: { color: m.color },
      areaStyle: { color: areaGradient(m.color) }
    })
  }
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: { show: false },
    grid: {
      left: '4%',
      right: '10%',
      bottom: '3%',
      top: '16px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: times,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: axes.map((a, i) => {
      const r = axisRange(mock.axisValues[i])
      return {
        type: 'value',
        name: a.unit,
        nameTextStyle: { color: '#94a3b8' },
        position: a.position,
        offset: a.offset,
        axisLabel: { color: '#94a3b8' },
        splitLine: i < 2 ? { lineStyle: { color: '#1e293b' } } : { show: false },
        min: r?.min ?? null,
        max: r?.max ?? null
      }
    }),
    series
  }, { notMerge: true })
}

watch(
  () => [props.historyData, props.forecastData, props.historyRange, props.selectedMetrics, props.metricOptions],
  () => render(),
  { deep: true }
)

onMounted(() => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  render()

  const handleResize = () => chart?.resize()
  window.addEventListener('resize', handleResize)
  onUnmounted(() => window.removeEventListener('resize', handleResize))
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart {
  padding: 20px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin: 0;
  white-space: nowrap;
}

.chart-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.metric-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
}

.metric-check {
  --el-checkbox-font-size: 12px;
  margin-right: 0;
}

/* 颜色点：让勾选行同时充当图例（颜色与曲线一致） */
.metric-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin: 0 4px 0 2px;
  vertical-align: 1px;
}

.metric-name {
  color: #cbd5e1;
}

.chart-note {
  font-size: 12px;
  color: #a0aec0;
  white-space: nowrap;
}

.chart-body {
  width: 100%;
  height: 300px;
}
</style>
