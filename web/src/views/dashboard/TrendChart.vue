<template>
  <div class="trend-chart dashboard-card">
    <div class="chart-header">
      <h3>环境趋势与短期预测</h3>
      <span class="chart-note">近24小时 · 预测未来2小时</span>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  historyData: { type: Array, default: () => [] },
  forecastData: { type: Array, default: () => [] },
  allowMock: { type: Boolean, default: true }
})

const chartRef = ref(null)
let chart = null

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

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      data: ['温度 (°C)', '湿度 (%)', '预测温度 (°C)', '预测湿度 (%)'],
      textStyle: { color: '#a0aec0' },
      top: 0
    },
    grid: {
      left: '3%',
      right: '5%',
      bottom: '3%',
      top: '40px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '温度 (°C)',
        nameTextStyle: { color: '#FF9800' },
        axisLabel: { color: '#94a3b8' },
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      {
        type: 'value',
        name: '湿度 (%)',
        nameTextStyle: { color: '#2196F3' },
        axisLabel: { color: '#94a3b8' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '温度 (°C)',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#FF9800', width: 2 },
        itemStyle: { color: '#FF9800' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,152,0,0.3)' },
            { offset: 1, color: 'rgba(255,152,0,0.02)' }
          ])
        }
      },
      {
        name: '湿度 (%)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [],
        lineStyle: { color: '#2196F3', width: 2 },
        itemStyle: { color: '#2196F3' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(33,150,243,0.3)' },
            { offset: 1, color: 'rgba(33,150,243,0.02)' }
          ])
        }
      },
      {
        name: '预测温度 (°C)',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#FF9800', width: 2, type: 'dashed' },
        itemStyle: { color: '#FF9800' },
        symbol: 'circle',
        symbolSize: 5,
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#64748b', type: 'dashed' },
          label: { formatter: '现在', color: '#94a3b8', fontSize: 11, position: 'insideEndTop' },
          data: []
        }
      },
      {
        name: '预测湿度 (%)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [],
        lineStyle: { color: '#2196F3', width: 2, type: 'dashed' },
        itemStyle: { color: '#2196F3' },
        symbol: 'circle',
        symbolSize: 5
      }
    ]
  }

  chart.setOption(option)
}

function updateChart(data, forecast) {
  if (!chart) return
  const history = Array.isArray(data) ? data : []
  const future = Array.isArray(forecast) ? forecast : []

  if (history.length === 0) {
    if (!props.allowMock) {
      // 无数据且不允许模拟（专家未授权场景）：保持空白
      chart.setOption({
        xAxis: { data: [] },
        yAxis: [{ min: null, max: null }, { min: null, max: null }],
        series: [{ data: [] }, { data: [] }, { data: [] }, { data: [] }]
      })
      return
    }
    // 生成模拟历史数据
    const now = new Date()
    const times = []
    const temps = []
    const hums = []
    for (let i = 23; i >= 0; i--) {
      const t = new Date(now - i * 3600000)
      times.push(t.getHours() + ':00')
      temps.push(22 + Math.sin(i / 4) * 4 + Math.random() * 2)
      hums.push(65 + Math.cos(i / 4) * 8 + Math.random() * 4)
    }
    const tempRange = axisRange(temps)
    const humRange = axisRange(hums)
    chart.setOption({
      xAxis: { data: times },
      yAxis: [
        { min: tempRange?.min ?? null, max: tempRange?.max ?? null },
        { min: humRange?.min ?? null, max: humRange?.max ?? null }
      ],
      series: [
        { data: temps.map(v => +v.toFixed(1)) },
        { data: hums.map(v => +v.toFixed(1)) },
        { data: [] },
        { data: [] }
      ]
    })
    return
  }

  const times = [...history.map(d => d.time), ...future.map(d => d.time)]
  const tempHist = history.map(d => (d.temperature != null ? d.temperature : null))
  const humHist = history.map(d => (d.humidity != null ? d.humidity : null))
  const boundary = history.length - 1

  let tempForecast = []
  let humForecast = []
  if (future.length > 0) {
    tempForecast = [
      ...Array(Math.max(0, boundary)).fill(null),
      tempHist[boundary],
      ...future.map(d => (d.temperature != null ? d.temperature : null))
    ]
    humForecast = [
      ...Array(Math.max(0, boundary)).fill(null),
      humHist[boundary],
      ...future.map(d => (d.humidity != null ? d.humidity : null))
    ]
  }

  const tempRange = axisRange([...tempHist, ...tempForecast])
  const humRange = axisRange([...humHist, ...humForecast])

  chart.setOption({
    xAxis: { data: times },
    yAxis: [
      { min: tempRange?.min ?? null, max: tempRange?.max ?? null },
      { min: humRange?.min ?? null, max: humRange?.max ?? null }
    ],
    series: [
      { data: tempHist },
      { data: humHist },
      {
        data: tempForecast,
        markLine: future.length > 0 ? { data: [{ xAxis: boundary }] } : { data: [] }
      },
      { data: humForecast }
    ]
  })
}

watch(() => [props.historyData, props.forecastData], () => {
  updateChart(props.historyData, props.forecastData)
}, { deep: true })

onMounted(() => {
  initChart()
  updateChart(props.historyData, props.forecastData)

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
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin: 0;
}

.chart-note {
  font-size: 12px;
  color: #a0aec0;
}

.chart-body {
  width: 100%;
  height: 300px;
}
</style>