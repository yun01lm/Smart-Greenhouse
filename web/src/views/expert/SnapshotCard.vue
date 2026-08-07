<template>
  <div class="snapshot-card">
    <div class="snap-title">
      <el-icon><Odometer /></el-icon>
      <span>环境快照</span>
      <span v-if="snapshot?.greenhouseName" class="snap-gh">{{ snapshot.greenhouseName }}</span>
    </div>
    <div v-if="items.length" class="snap-grid">
      <div v-for="item in items" :key="item.label" class="snap-item">
        <div class="snap-label">{{ item.label }}</div>
        <div class="snap-value">{{ item.value }}<span class="snap-unit">{{ item.unit }}</span></div>
      </div>
    </div>
    <div v-else class="snap-empty">快照数据为空</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Odometer } from '@element-plus/icons-vue'

const props = defineProps({
  snapshot: { type: Object, default: null }
})

const TYPE_META = {
  TEMPERATURE: { label: '空气温度', unit: '°C' },
  HUMIDITY: { label: '空气湿度', unit: '%' },
  CO2: { label: 'CO₂', unit: 'ppm' },
  LIGHT: { label: '光照', unit: 'lux' },
  SOIL_TEMP: { label: '土壤温度', unit: '°C' },
  SOIL_MOISTURE: { label: '土壤湿度', unit: '%' },
  SOIL_PH: { label: '土壤pH', unit: '' },
  WIND_SPEED: { label: '风速', unit: 'm/s' }
}

const items = computed(() => {
  const byType = props.snapshot?.dataByType || {}
  const list = []
  for (const [type, points] of Object.entries(byType)) {
    const meta = TYPE_META[type]
    if (meta && points && points.length > 0 && points[0].value != null) {
      list.push({ label: meta.label, value: Number(points[0].value).toFixed(1), unit: meta.unit })
    }
  }
  return list
})
</script>

<style scoped>
.snapshot-card {
  min-width: 260px;
  background: #f7f9fc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 10px 12px;
}
.snap-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
}
.snap-gh {
  font-weight: 400;
  color: #909399;
}
.snap-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}
.snap-item {
  text-align: center;
  background: #fff;
  border-radius: 6px;
  padding: 6px 4px;
}
.snap-label {
  font-size: 11px;
  color: #909399;
}
.snap-value {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-top: 2px;
}
.snap-unit {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
  margin-left: 2px;
}
.snap-empty {
  margin-top: 8px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>