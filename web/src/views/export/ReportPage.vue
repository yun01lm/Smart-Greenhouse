<template>
  <div class="report-page">
    <div class="page-header">
      <h3>数据导出报表</h3>
      <p class="page-desc">选择数据类型、设置筛选条件，导出为 Excel（.xlsx）文件</p>
    </div>

    <!-- 通用筛选区 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <span class="filter-label">所属大棚：</span>
        <el-select
          v-model="filter.greenhouseId"
          placeholder="请选择大棚"
          style="width: 200px"
        >
          <el-option v-for="gh in greenhouses" :key="gh.id" :label="gh.name" :value="gh.id" />
        </el-select>
        <span class="filter-label" style="margin-left: 24px">时间范围：</span>
        <el-date-picker
          v-model="filter.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="x"
          style="width: 280px"
        />
      </div>
    </el-card>

    <!-- 4 个导出卡片 -->
    <el-row :gutter="16" class="card-row">
      <!-- 1. 传感器历史数据 -->
      <el-col :span="12">
        <el-card class="export-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon size="20" color="#409EFF"><Histogram /></el-icon>
              <span>传感器历史数据</span>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">导出指定传感器的历史读数数据，含时间、设备名称、数值</p>
            <el-select
              v-model="exportForm.sensorType"
              placeholder="选择传感器类型（必选）"
              style="width: 100%"
            >
              <el-option v-for="st in sensorTypes" :key="st.value" :label="st.label" :value="st.value" />
            </el-select>
          </div>
          <div class="card-footer">
            <el-button
              type="primary"
              :icon="Download"
              :loading="loading.sensors"
              :disabled="!canExport('sensors')"
              @click="doExport('sensors')"
            >
              导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 2. 预警记录 -->
      <el-col :span="12">
        <el-card class="export-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon size="20" color="#E6A23C"><WarningFilled /></el-icon>
              <span>预警记录</span>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">导出预警触发记录，含级别、标题、内容、传感器信息、时间</p>
            <el-select
              v-model="exportForm.alertLevel"
              placeholder="预警级别（可选，不选导出全部）"
              clearable
              style="width: 100%"
            >
              <el-option label="提示 (INFO)" value="INFO" />
              <el-option label="警告 (WARNING)" value="WARNING" />
              <el-option label="严重 (CRITICAL)" value="CRITICAL" />
            </el-select>
          </div>
          <div class="card-footer">
            <el-button
              type="primary"
              :icon="Download"
              :loading="loading.alerts"
              :disabled="!canExport('alerts')"
              @click="doExport('alerts')"
            >
              导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 3. 设备控制日志 -->
      <el-col :span="12">
        <el-card class="export-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon size="20" color="#67C23A"><SwitchIcon /></el-icon>
              <span>设备控制日志</span>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">导出设备操作记录，含设备名称、动作（ON/OFF）、来源、结果、时间</p>
            <el-tag type="info" size="small">按大棚 + 时间范围筛选，无需额外条件</el-tag>
          </div>
          <div class="card-footer">
            <el-button
              type="primary"
              :icon="Download"
              :loading="loading.controls"
              :disabled="!canExport('controls')"
              @click="doExport('controls')"
            >
              导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 4. 健康评分记录 -->
      <el-col :span="12">
        <el-card class="export-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon size="20" color="#F56C6C"><TrendCharts /></el-icon>
              <span>健康评分记录</span>
            </div>
          </template>
          <div class="card-body">
            <p class="card-desc">导出多模态健康综合评分，含综合分、等级、环境分、视觉分、天气修正</p>
            <el-tag type="info" size="small">默认最近 30 天，按大棚 + 时间范围筛选</el-tag>
          </div>
          <div class="card-footer">
            <el-button
              type="primary"
              :icon="Download"
              :loading="loading.health"
              :disabled="!canExport('health')"
              @click="doExport('health')"
            >
              导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导出说明 -->
    <el-card class="info-card">
      <template #header>
        <span>导出说明</span>
      </template>
      <ul class="info-list">
        <li>所有导出文件格式为 <b>Excel (.xlsx)</b>，支持 Microsoft Excel、WPS、LibreOffice 等软件打开。</li>
        <li>传感器历史数据需指定<b>传感器类型</b>，默认导出最近 7 天数据。</li>
        <li>预警记录、控制日志、健康评分默认导出最近 30 天数据。</li>
        <li>时间范围可自定义，起始日期不填则使用默认范围。</li>
        <li>文件命名格式：<code>类型_日期.xlsx</code>（如 传感器数据_20260715.xlsx）。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Download, Histogram, WarningFilled, Switch as SwitchIcon, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getGreenhouses } from '@/api/greenhouse'
import { useViewModeStore } from '@/stores/viewMode'
import { exportSensors, exportAlerts, exportControls, exportHealth, downloadBlob } from '@/api/report'

// ===== 基础数据 =====
const greenhouses = ref([])

// ===== 通用筛选 =====
const filter = reactive({
  greenhouseId: null,
  dateRange: null
})

// ===== 各类型专属筛选 =====
const exportForm = reactive({
  sensorType: '',
  alertLevel: ''
})

// ===== 加载状态 =====
const loading = reactive({
  sensors: false,
  alerts: false,
  controls: false,
  health: false
})

// ===== 传感器类型 =====
const sensorTypes = [
  { value: 'TEMP', label: '温度' },
  { value: 'HUMIDITY', label: '空气湿度' },
  { value: 'LIGHT', label: '光照强度' },
  { value: 'CO2', label: 'CO₂浓度' },
  { value: 'O2', label: 'O₂浓度' },
  { value: 'SOIL_TEMP', label: '土壤温度' },
  { value: 'SOIL_HUMIDITY', label: '土壤湿度' },
  { value: 'EC', label: '土壤EC值' },
  { value: 'N', label: '氮含量' },
  { value: 'P', label: '磷含量' },
  { value: 'K', label: '钾含量' },
  { value: 'WIND_SPEED', label: '风速' }
]

// ===== 导出逻辑 =====
function canExport(type) {
  if (!filter.greenhouseId) return false
  if (type === 'sensors' && !exportForm.sensorType) return false
  return true
}

async function doExport(type) {
  const params = {
    greenhouseId: filter.greenhouseId
  }

  if (filter.dateRange && filter.dateRange.length === 2) {
    params.startTime = filter.dateRange[0]
    params.endTime = filter.dateRange[1]
  }

  const config = {
    sensors: { fn: exportSensors, extra: { sensorType: exportForm.sensorType }, prefix: '传感器数据' },
    alerts: { fn: exportAlerts, extra: { level: exportForm.alertLevel || undefined }, prefix: '预警记录' },
    controls: { fn: exportControls, extra: {}, prefix: '控制日志' },
    health: { fn: exportHealth, extra: {}, prefix: '健康评分' }
  }

  const { fn, extra, prefix } = config[type]
  loading[type] = true

  try {
    const res = await fn({ ...params, ...extra })
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const filename = `${prefix}_${today}.xlsx`
    downloadBlob(res, filename)
    ElMessage.success(`${prefix}导出成功`)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    loading[type] = false
  }
}

const viewStore = useViewModeStore()

// ===== 初始化 =====
onMounted(async () => {
  try {
    // R10：棚主视角下使用该棚主的大棚列表
    if (viewStore.active) {
      greenhouses.value = viewStore.greenhouses || []
    } else {
      const res = await getGreenhouses()
      greenhouses.value = res.data || []
    }
    if (greenhouses.value.length > 0) {
      filter.greenhouseId = greenhouses.value[0].id
    }
  } catch { /* handled by interceptor */ }
})
</script>

<style scoped>
.report-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.card-row {
  margin-bottom: 16px;
}

.export-card {
  margin-bottom: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.export-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.card-body {
  flex: 1;
}

.card-desc {
  font-size: 13px;
  color: #606266;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.info-card {
  background: #fafbfc;
}

.info-list {
  margin: 0;
  padding-left: 20px;
}

.info-list li {
  font-size: 13px;
  color: #606266;
  line-height: 2;
}

.info-list code {
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
</style>
