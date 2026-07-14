<template>
  <div class="health-score dashboard-card">
    <h3>大棚健康评分</h3>
    <div class="score-ring">
      <div class="score-number" :style="{ color: scoreColor }">{{ scoreValue }}</div>
      <div class="score-label">{{ scoreLevel }}</div>
    </div>
    <div class="score-details">
      <div class="score-item">
        <span class="score-item-label">环境评分</span>
        <el-progress
          :percentage="envScore"
          :color="envScoreColor"
          :stroke-width="8"
        />
      </div>
      <div class="score-item">
        <span class="score-item-label">视觉评分</span>
        <el-progress
          :percentage="visualScore"
          :color="visualScoreColor"
          :stroke-width="8"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, default: null }
})

const scoreValue = computed(() => {
  if (!props.data?.overallScore) return '--'
  return Number(props.data.overallScore).toFixed(0)
})

const scoreLevel = computed(() => props.data?.level || '暂无数据')

const scoreColor = computed(() => {
  const s = parseInt(scoreValue.value)
  if (isNaN(s)) return '#909399'
  if (s >= 80) return '#4CAF50'
  if (s >= 60) return '#2196F3'
  if (s >= 40) return '#FF9800'
  return '#F44336'
})

const envScore = computed(() => {
  if (!props.data?.envScore) return 0
  return Math.round(Number(props.data.envScore))
})

const visualScore = computed(() => {
  if (!props.data?.visualScore) return 0
  return Math.round(Number(props.data.visualScore))
})

const envScoreColor = computed(() => {
  if (envScore.value >= 80) return '#4CAF50'
  if (envScore.value >= 60) return '#2196F3'
  if (envScore.value >= 40) return '#FF9800'
  return '#F44336'
})

const visualScoreColor = computed(() => {
  if (visualScore.value >= 80) return '#4CAF50'
  if (visualScore.value >= 60) return '#2196F3'
  if (visualScore.value >= 40) return '#FF9800'
  return '#F44336'
})
</script>

<style scoped>
.health-score {
  padding: 20px;
}

.health-score h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
  margin-bottom: 16px;
}

.score-ring {
  text-align: center;
  margin-bottom: 20px;
}

.score-number {
  font-size: 48px;
  font-weight: 700;
  font-family: 'DIN', monospace;
}

.score-label {
  font-size: 14px;
  color: #a0aec0;
  margin-top: 4px;
}

.score-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-item-label {
  font-size: 13px;
  color: #a0aec0;
  white-space: nowrap;
  min-width: 60px;
}

.score-item :deep(.el-progress) {
  flex: 1;
}
</style>
