<template>
  <div class="alert-list dashboard-card">
    <div class="alert-header">
      <h3>最新预警</h3>
      <el-tag v-if="unreadCount > 0" type="danger" size="small">{{ unreadCount }} 条未读</el-tag>
    </div>
    <div v-if="alerts.length === 0" class="alert-empty">
      <el-icon :size="40" color="#4CAF50"><SuccessFilled /></el-icon>
      <p>暂无预警，大棚运行正常</p>
    </div>
    <div v-else class="alert-items">
      <div
        v-for="alert in alerts"
        :key="alert.id"
        class="alert-item"
        :class="['alert-' + alert.level, { 'is-handled': alert.handled }]"
      >
        <div class="alert-level-tag">
          <el-tag
            :type="levelTagType(alert.level)"
            size="small"
            effect="dark"
          >
            {{ levelLabel(alert.level) }}
          </el-tag>
        </div>
        <div class="alert-content">
          <div class="alert-message">{{ alert.message || alert.title }}</div>
          <div class="alert-time">{{ formatTime(alert.createdAt || alert.createTime) }}</div>
        </div>
        <div class="alert-action">
          <template v-if="alert.handled">
            <el-tag type="success" size="small" effect="plain">已处理</el-tag>
          </template>
          <template v-else>
            <el-tag type="warning" size="small" effect="plain">未处理</el-tag>
            <el-button
              size="small"
              type="primary"
              plain
              :loading="handlingId === alert.id"
              @click="handleAlert(alert)"
            >处理</el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { SuccessFilled } from '@element-plus/icons-vue'
import { markAlertHandled } from '@/api/alert'

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  unreadCount: { type: Number, default: 0 }
})

// 正在处理的预警ID（按钮 loading）
const handlingId = ref(null)

/** 标记预警为已处理 */
async function handleAlert(alert) {
  if (handlingId.value) return
  handlingId.value = alert.id
  try {
    await markAlertHandled(alert.id)
    alert.handled = true
    ElMessage.success('已标记为已处理')
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally {
    handlingId.value = null
  }
}

function levelTagType(level) {
  const map = { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}

function levelLabel(level) {
  const map = { CRITICAL: '严重', WARNING: '警告', INFO: '提示' }
  return map[level] || level
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.alert-list {
  padding: 20px;
}

.alert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.alert-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #e0e6ed;
}

.alert-empty {
  text-align: center;
  padding: 24px;
  color: #a0aec0;
}

.alert-empty p {
  margin-top: 8px;
  font-size: 14px;
}

.alert-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.03);
  border-left: 3px solid transparent;
}

.alert-item.alert-CRITICAL { border-left-color: #F44336; }
.alert-item.alert-WARNING { border-left-color: #FF9800; }
.alert-item.alert-INFO { border-left-color: #2196F3; }

.alert-message {
  font-size: 14px;
  color: #e0e6ed;
  margin-bottom: 2px;
}

.alert-action {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.alert-item.is-handled {
  opacity: 0.6;
}

.alert-time {
  font-size: 12px;
  color: #64748b;
}
</style>
