<template>
  <div class="trend-chart dashboard-card">
    <div class="chart-header">
      <h3>环境趋势与短期预测</h3>
      <div class="chart-header-right">
        <el-checkbox-group v-model="selected" class="metric-checks">
          <el-checkbox
            v-for="m in props.metricOptions"
            :key="m.type"
            :value="m.type"
            size="small"
            class="metric-check"
          >{{ m.label }}</el-checkbox>
        </el-checkbox-group>
        <span class="chart-note">{{ RANGE_LABEL[props.historyRange] || '近24小时' }} · 预测未来2小时</span>
      </div>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
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

const chartRef = ref(null)
let chart = null

/** 勾选状态：受控于父级，至少保留 1 项 */
const selected = computed({
  get: () => props.selectedMetrics,
  set: (v) => {
    if (!v || v.length === 0) return
    emit('update:selectedMetrics', v)
  }
})

/** 当前选中的指标元信息 */
function selectedMeta() {
  return props.metricOptions.filter(m => selected.value.includes(m.type))
}

/** 计算 y 轴刻度范围：以数据 min/max 收缩并留少量内边距，放大曲线波动 */
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

/** 按单位构建 Y 轴（同类单位共轴，位置左右交替 + 偏移） */
function buildAxes(metrics) {
  const units = [...new Set((props.metricOptions || []).map(m => m.unit))]
  const axes = []
  const unitIndex = {}
  units.forEach((unit, i) => {
    unitIndex[unit] = i
    axes.push({
      unit,
      position: i % 2 === 0 ? 'left' : 'right',
      offset: Math.floor(i / 2) * 55
    })
  })
  return { axes, unitIndex }
}

function buildOption() {
  const metrics = selectedMeta()
  const history = Array.isArray(props.historyData) ? props.historyData : []
  const future = Array.isArray(props.forecastData) ? props.forecastData : []
  const { axes, unitIndex } = buildAxes(metrics)

  const legendData = metrics.flatMap(m => [`${m.label} (${m.unit})`, `预测${m.label} (${m.unit})`])
  const times = [...history.map(d => d.time), ...future.map(d => d.time)]
  const boundary = history.length - 1
  const hasForecast = future.length > 0

  const series = []
  const axisValues = axes.map(() => [])
  for (const m of metrics) {
    const key = m.key
    const histVals = history.map(d => (d[key] != null ? d[key] : null))
    let fcVals = []
    if (hasForecast) {
      fcVals = [
        ...Array(Math.max(0, boundary)).fill(null),
        histVals[boundary],
        ...future.map(d => (d[key] != null ? d[key] : null))
      ]
    }
    const yi = unitIndex[m.unit]
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
      markLine: hasForecast
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

  const yAxis = axes.map((a, i) => {
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
  })

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      data: legendData,
      textStyle: { color: '#a0aec0' },
      top: 0,
      type: 'scroll'
    },
    grid: {
      left: '3%',
      right: '8%',
      bottom: '3%',
      top: '52px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: times,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis,
    series
  }
}

function renderEmpty() {
  chart.setOption({
    xAxis: { data: [] },
    yAxis: [],
    series: []
  })
}

/** 无数据时生成模拟曲线（按范围生成对应天数/小时数） */
function buildMock(metrics, axes, unitIndex) {
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
    const yi = unitIndex[m.unit]
    for (const p of points) axisValues[yi].push(p[m.key])
  }
  return { points, axisValues }
}

function render() {
  if (!chart) return
  const metrics = selectedMeta()
  const history = Array.isArray(props.historyData) ? props.historyData : []

  if (history.length === 0) {
    if (!props.allowMock || metrics.length === 0) {
      renderEmpty()
      return
    }
    const { axes, unitIndex } = buildAxes(metrics)
    const { points, axisValues } = buildMock(metrics, axes, unitIndex)
    const times = points.map(p => p.time)
    const option = buildOption()
    // 用模拟数据填充序列
    const filled = option.series.map(s => {
      const meta = metrics.find(m => `${m.label} (${m.unit})` === s.name || `预测${m.label} (${m.unit})` === s.name)
      const isForecast = s.name.startsWith('预测')
      const vals = meta ? points.map(p => (p[meta.key] != null ? p[meta.key] : null)) : []
      return { ...s, data: isForecast ? [] : vals, markLine: { data: [] } }
    })
    chart.setOption({
      ...option,
      xAxis: { data: times },
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
      series: filled
    })
    return
  }

  chart.setOption(buildOption(), { notMerge: true })
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
  gap: 2px 8px;
}

.metric-check {
  --el-checkbox-font-size: 12px;
  margin-right: 0;
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