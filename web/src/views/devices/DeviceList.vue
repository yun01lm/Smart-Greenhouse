<template>
  <div class="device-list">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="filterType"
          placeholder="设备类型"
          clearable
          style="width: 140px"
          @change="onFilterChange"
        >
          <el-option label="传感器" value="SENSOR" />
          <el-option label="控制器" value="CONTROLLER" />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="设备状态"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="onFilterChange"
        >
          <el-option label="在线" value="ONLINE" />
          <el-option label="离线" value="OFFLINE" />
          <el-option label="告警" value="ALARM" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索设备名称/编号"
          clearable
          style="width: 220px; margin-left: 12px"
          :prefix-icon="Search"
          @clear="onFilterChange"
          @keyup.enter="onFilterChange"
        />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">添加设备</el-button>
      </div>
    </div>

    <!-- 设备表格 -->
    <el-table
      v-loading="loading"
      :data="pagedDevices"
      stripe
      border
      style="width: 100%"
      :default-sort="{ prop: 'createdAt', order: 'descending' }"
    >
      <el-table-column prop="deviceSn" label="设备编号" min-width="130" />
      <el-table-column prop="name" label="设备名称" min-width="140" />
      <el-table-column label="设备类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.deviceType === 'SENSOR' ? 'success' : 'warning'" size="small">
            {{ row.deviceType === 'SENSOR' ? '传感器' : '控制器' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="传感器类型" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.sensorType">{{ sensorTypeLabel(row.sensorType) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            :type="statusTagType(row.status)"
            size="small"
            effect="dark"
          >
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最新值" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.lastValue" class="last-value">{{ row.lastValue }}</span>
          <span v-else class="text-muted">无数据</span>
        </template>
      </el-table-column>
      <el-table-column prop="installLocation" label="安装位置" min-width="120" />
      <el-table-column label="最近数据" width="160" align="center">
        <template #default="{ row }">
          <span v-if="row.lastDataTime">{{ formatTime(row.lastDataTime) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="filteredDevices.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredDevices.length"
        layout="total, sizes, prev, pager, next"
        @size-change="onPageSizeChange"
      />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑设备' : '添加设备'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入设备名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="设备编号" prop="deviceSn">
          <el-input v-model="formData.deviceSn" placeholder="请输入设备SN编号" :disabled="isEditing" maxlength="50" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="formData.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option label="传感器" value="SENSOR" />
            <el-option label="控制器" value="CONTROLLER" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="formData.deviceType === 'SENSOR'"
          label="传感器类型"
          prop="sensorType"
        >
          <el-select v-model="formData.sensorType" placeholder="请选择传感器类型" style="width: 100%">
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
        <el-form-item label="安装位置" prop="installLocation">
          <el-input v-model="formData.installLocation" placeholder="请输入安装位置" maxlength="100" />
        </el-form-item>
        <el-form-item label="设备描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入设备描述（可选）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDevices, createDevice, updateDevice, deleteDevice
} from '@/api/device'

const props = defineProps({
  greenhouseId: { type: [Number, String], required: true }
})

// ===== 数据 =====
const loading = ref(false)
const devices = ref([])
const filterType = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// ===== 对话框 =====
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  name: '',
  deviceSn: '',
  deviceType: 'SENSOR',
  sensorType: null,
  installLocation: '',
  description: ''
})

const formData = ref(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceSn: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  sensorType: [{
    validator: (_rule, value, callback) => {
      if (formData.value.deviceType === 'SENSOR' && !value) {
        callback(new Error('请选择传感器类型'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }]
}

// ===== 计算属性 =====
const filteredDevices = computed(() => {
  let list = devices.value
  if (filterType.value) {
    list = list.filter(d => d.deviceType === filterType.value)
  }
  if (filterStatus.value) {
    list = list.filter(d => d.status === filterStatus.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(d =>
      d.name.toLowerCase().includes(kw) ||
      d.deviceSn.toLowerCase().includes(kw)
    )
  }
  return list
})

const pagedDevices = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredDevices.value.slice(start, start + pageSize.value)
})

// ===== 方法 =====
async function loadDevices() {
  loading.value = true
  try {
    const params = {}
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value) params.status = filterStatus.value

    const res = await getDevices(props.greenhouseId, params)
    devices.value = res.data || []
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  currentPage.value = 1
  loadDevices()
}

function onPageSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
}

function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  formData.value = defaultForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEditing.value = true
  editingId.value = row.id
  formData.value = {
    name: row.name,
    deviceSn: row.deviceSn,
    deviceType: row.deviceType,
    sensorType: row.sensorType || null,
    installLocation: row.installLocation || '',
    description: row.description || ''
  }
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEditing.value) {
      await updateDevice(props.greenhouseId, editingId.value, formData.value)
      ElMessage.success('设备更新成功')
    } else {
      await createDevice(props.greenhouseId, formData.value)
      ElMessage.success('设备添加成功')
    }
    dialogVisible.value = false
    await loadDevices()
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除设备「${row.name}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteDevice(props.greenhouseId, row.id)
    ElMessage.success('设备已删除')
    await loadDevices()
  } catch {
    // 取消删除或请求失败
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

function statusLabel(status) {
  const map = { ONLINE: '在线', OFFLINE: '离线', ALARM: '告警' }
  return map[status] || status
}

function statusTagType(status) {
  const map = { ONLINE: 'success', OFFLINE: 'info', ALARM: 'danger' }
  return map[status] || 'info'
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ===== 监听 greenhouseId 变化重新加载 =====
watch(() => props.greenhouseId, () => {
  loadDevices()
}, { immediate: true })
</script>

<style scoped>
.device-list {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.last-value {
  font-weight: 600;
  color: #e0e6ed;
}

.text-muted {
  color: #64748b;
}
</style>
