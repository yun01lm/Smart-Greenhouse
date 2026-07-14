<template>
  <div class="sensor-cards">
    <div class="card-grid">
      <div v-for="sensor in sensors" :key="sensor.key" class="sensor-card dashboard-card">
        <div class="sensor-icon">
          <el-icon :size="28">
            <component :is="sensor.icon" />
          </el-icon>
        </div>
        <div class="sensor-info">
          <div class="sensor-label">{{ sensor.label }}</div>
          <div class="sensor-value">
            <span class="stat-number" :style="{ color: sensor.color }">{{ sensor.value }}</span>
            <span class="sensor-unit">{{ sensor.unit }}</span>
          </div>
          <div class="sensor-status">
            <el-tag :type="sensor.statusType" size="small">{{ sensor.statusText }}</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Sunny, WindPower, Cloudy, MostlyCloudy, Drizzling, Watermelon, Odometer
} from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Object, default: () => ({}) }
})

// 传感器配置
const sensorConfig = [
  { key: 'temperature', label: '空气温度', unit: '°C', icon: Sunny, color: '#FF9800', normal: [15, 30] },
  { key: 'humidity', label: '空气湿度', unit: '%', icon: Drizzling, color: '#2196F3', normal: [50, 85] },
  { key: 'co2', label: 'CO₂浓度', unit: 'ppm', icon: Cloudy, color: '#4CAF50', normal: [300, 800] },
  { key: 'light', label: '光照强度', unit: 'lux', icon: Sunny, color: '#FFC107', normal: [5000, 60000] },
  { key: 'soilTemperature', label: '土壤温度', unit: '°C', icon: Watermelon, color: '#795548', normal: [10, 28] },
  { key: 'soilHumidity', label: '土壤湿度', unit: '%', icon: MostlyCloudy, color: '#00BCD4', normal: [30, 80] },
  { key: 'soilPh', label: '土壤 pH', unit: '', icon: Odometer, color: '#9C27B0', normal: [5.5, 7.5] },
  { key: 'windSpeed', label: '风速', unit: 'm/s', icon: WindPower, color: '#607D8B', normal: [0, 5] }
]

const sensors = computed(() => {
  return sensorConfig.map(config => {
    const rawValue = props.data[config.key]
    const value = rawValue != null ? Number(rawValue).toFixed(1) : '--'
    const numVal = parseFloat(value)

    let statusType = 'success'
    let statusText = '正常'
    if (isNaN(numVal)) {
      statusType = 'info'
      statusText = '无数据'
    } else if (numVal < config.normal[0]) {
      statusType = 'warning'
      statusText = '偏低'
    } else if (numVal > config.normal[1]) {
      statusType = 'danger'
      statusText = '偏高'
    }

    return {
      ...config,
      value,
      statusType,
      statusText
    }
  })
})
</script>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.sensor-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.sensor-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.sensor-label {
  font-size: 13px;
  color: #a0aec0;
  margin-bottom: 4px;
}

.sensor-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
}

.sensor-unit {
  font-size: 14px;
  color: #a0aec0;
}

@media (max-width: 1400px) {
  .card-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1000px) {
  .card-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
