<template>
  <div class="firmware-page">
    <div class="page-header">
      <div>
        <h3>固件管理</h3>
        <p class="page-desc">
          出厂前批量预注册固件ID（8位数字，全局唯一），烧录进 ESP32 并印在标签上；
          用户绑定时按固件ID添加设备，系统自动生成设备编号。
        </p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openBatchDialog">批量预注册</el-button>
    </div>

    <!-- 状态筛选 -->
    <div class="toolbar">
      <el-select v-model="filterStatus" placeholder="固件状态" clearable style="width: 180px" @change="onStatusChange">
        <el-option label="未绑定" value="UNBOUND" />
        <el-option label="已绑定" value="BOUND" />
      </el-select>
      <el-tag type="success" effect="plain">未绑定 {{ unboundCount }} 个</el-tag>
    </div>

    <!-- 固件表格 -->
    <el-table v-loading="loading" :data="firmwares" stripe border style="width: 100%">
      <el-table-column prop="firmwareId" label="固件ID" width="120" align="center" />
      <el-table-column label="设备类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.deviceType === 'SENSOR' ? 'success' : 'warning'" size="small">
            {{ row.deviceType === 'SENSOR' ? '传感器' : '控制器' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="传感器类型" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.sensorType">{{ sensorTypeLabel(row.sensorType) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="firmwareVersion" label="固件版本" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.firmwareVersion">{{ row.firmwareVersion }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="batchNo" label="批次号" width="130" align="center">
        <template #default="{ row }">
          <span v-if="row.batchNo">{{ row.batchNo }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'BOUND' ? 'info' : 'success'" size="small" effect="dark">
            {{ row.status === 'BOUND' ? '已绑定' : '未绑定' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定设备ID" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.boundDeviceId">{{ row.boundDeviceId }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="预注册时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页：每页 15 条 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[15, 30, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadFirmwares"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 批量预注册对话框 -->
    <el-dialog append-to-body v-model="batchVisible" title="批量预注册固件" width="480px" :close-on-click-modal="false">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="110px" label-position="right">
        <el-form-item label="预注册数量" prop="count">
          <el-input-number v-model="batchForm.count" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="batchForm.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option label="传感器" value="SENSOR" />
            <el-option label="控制器" value="CONTROLLER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="batchForm.deviceType === 'SENSOR'" label="传感器类型" prop="sensorType">
          <el-select v-model="batchForm.sensorType" placeholder="请选择传感器类型" style="width: 100%">
            <el-option label="温度" value="TEMPERATURE" />
            <el-option label="湿度" value="HUMIDITY" />
            <el-option label="光照" value="LIGHT" />
            <el-option label="CO₂浓度" value="CO2" />
            <el-option label="土壤湿度" value="SOIL_MOISTURE" />
            <el-option label="土壤温度" value="SOIL_TEMP" />
            <el-option label="土壤pH" value="SOIL_PH" />
            <el-option label="风速" value="WIND_SPEED" />
          </el-select>
        </el-form-item>
        <el-form-item label="固件版本" prop="firmwareVersion">
          <el-input v-model="batchForm.firmwareVersion" placeholder="如 1.0.0（可选）" maxlength="20" />
        </el-form-item>
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="batchForm.batchNo" placeholder="如 B20260801（可选）" maxlength="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBatch">确定预注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { batchRegisterFirmwares, getFirmwares, getUnboundFirmwareCount } from '@/api/firmware'

// ===== 数据 =====
const loading = ref(false)
const firmwares = ref([])
const filterStatus = ref('')
const unboundCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

// ===== 批量预注册对话框 =====
const batchVisible = ref(false)
const submitting = ref(false)
const batchFormRef = ref(null)
const batchForm = ref({ count: 10, deviceType: 'SENSOR', sensorType: null, firmwareVersion: '', batchNo: '' })

const batchRules = {
  count: [{ required: true, message: '请输入预注册数量', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  sensorType: [{
    validator: (_rule, value, callback) => {
      if (batchForm.value.deviceType === 'SENSOR' && !value) {
        callback(new Error('请选择传感器类型'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }]
}

// ===== 方法 =====
async function loadFirmwares() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await getFirmwares(params)
    const data = res.data || {}
    firmwares.value = data.records || []
    total.value = data.total || 0
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    loading.value = false
  }
}

function onStatusChange() {
  currentPage.value = 1
  loadFirmwares()
}

function onSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadFirmwares()
}

async function loadUnboundCount() {
  try {
    const res = await getUnboundFirmwareCount()
    unboundCount.value = res.data?.count || 0
  } catch {
    // 忽略统计失败
  }
}

function openBatchDialog() {
  batchForm.value = { count: 10, deviceType: 'SENSOR', sensorType: null, firmwareVersion: '', batchNo: '' }
  batchVisible.value = true
}

async function submitBatch() {
  try {
    await batchFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await batchRegisterFirmwares(batchForm.value)
    const list = res.data || []
    ElMessage.success(`预注册成功，生成固件ID ${list.length} 个（${list[0]?.firmwareId} ~ ${list[list.length - 1]?.firmwareId}）`)
    batchVisible.value = false
    currentPage.value = 1
    await Promise.all([loadFirmwares(), loadUnboundCount()])
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    submitting.value = false
  }
}

// ===== 工具函数 =====
function sensorTypeLabel(type) {
  const map = {
    TEMPERATURE: '温度', HUMIDITY: '湿度', LIGHT: '光照', CO2: 'CO₂',
    SOIL_MOISTURE: '土壤湿度', SOIL_TEMP: '土壤温度', SOIL_PH: '土壤pH', WIND_SPEED: '风速'
  }
  return map[type] || type
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ===== 初始化 =====
onMounted(() => {
  loadFirmwares()
  loadUnboundCount()
})
</script>

<style scoped>
.firmware-page {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.page-header h3 {
  margin: 0 0 4px;
  color: #e0e6ed;
}

.page-desc {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.text-muted {
  color: #64748b;
}
</style>
