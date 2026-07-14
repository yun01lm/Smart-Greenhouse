<template>
  <div class="trend-chart dashboard-card">
    <div class="chart-header">
      <h3>环境趋势 (近24小时)</h3>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  historyData: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

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
      data: ['温度 (°C)', '湿度 (%)'],
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
      }
    ]
  }

  chart.setOption(option)
}

function updateChart(data) {
  if (!chart) return
  if (!data || data.length === 0) {
    // 生成模拟历史数据
    const now = new Date()
    const times = []
    const temps = []
    const hums = []
    for (let i = 23; i >= 0; i--) {
      const t = new Date(now - i * 3600000)
      times.push(t.getHours() + ':00')
      temps.push((22 + Math.sin(i / 4) * 4 + Math.random() * 2).toFixed(1))
      hums.push((65 + Math.cos(i / 4) * 8 + Math.random() * 4).toFixed(1))
    }
    chart.setOption({
      xAxis: { data: times },
      series: [
        { data: temps.map(Number) },
        { data: hums.map(Number) }
      ]
    })
  } else {
    const times = data.map(d => d.time)
    const temps = data.map(d => d.temperature)
    const hums = data.map(d => d.humidity)
    chart.setOption({
      xAxis: { data: times },
      series: [
        { data: temps },
        { data: hums }
      ]
    })
  }
}

watch(() => props.historyData, updateChart, { deep: true })

onMounted(() => {
  initChart()
  updateChart(props.historyData)

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

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 12px;
}

.chart-body {
  width: 100%;
  height: 300px;
}
</style>
