<template>
  <div class="admin-device-page">
    <!-- 地区 + 搜索 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <span class="filter-label">查看范围：</span>
        <RegionCascader v-model="regionPath" width="360px" @change="onRegionChange" />
        <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 姓名 / 手机号"
          clearable
          style="width: 240px; margin-left: 12px"
          :prefix-icon="Search"
          @keyup.enter="loadData"
        />
        <el-button type="primary" style="margin-left: 12px" :loading="loading" @click="loadData">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <span v-if="regionText" class="filter-text">当前范围：{{ regionText }}</span>
      </div>
    </el-card>

    <!-- 总体统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #409EFF">{{ stats.deviceTotal }}</div>
          <div class="stat-label">设备总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #67C23A">
            {{ stats.deviceOnline }}<span class="stat-sub"> 在线</span>
          </div>
          <div class="stat-label">设备在线（离线 {{ stats.deviceOffline }} / 告警 {{ stats.deviceAlarm }}）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #E6A23C">
            {{ stats.ownerOnline }}<span class="stat-sub"> / {{ stats.ownerCount }}</span>
          </div>
          <div class="stat-label">农户在线 / 总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #94a3b8">{{ stats.greenhouseCount }}</div>
          <div class="stat-label">大棚数（当前范围）</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 农户列表 -->
    <el-card class="owner-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>农户列表（按农户查找设备）</span>
          <span class="header-sub">共 {{ owners.length }} 户</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="owners" stripe border style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="greenhouseCount" label="大棚数" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' || row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' || row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDeviceDialog(row)">设备管理</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前范围暂无农户" :image-size="80" />
        </template>
      </el-table>
    </el-card>

    <!-- 农户设备管理弹窗 -->
    <el-dialog append-to-body
      v-model="deviceDialogVisible"
      :title="`设备管理 — ${currentOwner?.realName || currentOwner?.username || ''}`"
      width="1100px"
      top="6vh"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="deviceLoading" class="owner-devices">
        <el-empty v-if="!deviceLoading && ownerGreenhouses.length === 0" description="该农户名下暂无大棚/设备" :image-size="80" />
        <div v-for="gh in ownerGreenhouses" :key="gh.greenhouseId" class="gh-block">
          <div class="gh-header">
            <div class="gh-title">
              <el-icon size="16" style="margin-right: 6px"><OfficeBuilding /></el-icon>
              <span>{{ gh.greenhouseName }}</span>
              <span class="gh-loc">{{ gh.location || '未填地址' }}</span>
            </div>
            <div class="gh-actions">
              <el-tag size="small" type="info">{{ gh.deviceCount }} 台设备</el-tag>
              <el-button type="primary" size="small" :icon="Plus" @click="openCreateForm(gh)">添加设备</el-button>
            </div>
          </div>
          <el-table :data="gh.devices" size="small" stripe border style="width: 100%">
            <el-table-column prop="deviceSn" label="设备编号" min-width="110" />
            <el-table-column prop="firmwareId" label="固件ID" width="100" align="center" />
            <el-table-column prop="name" label="设备名称" min-width="120" />
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.deviceType === 'SENSOR' ? 'success' : 'warning'" size="small">
                  {{ row.deviceType === 'SENSOR' ? '传感器' : '控制器' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="传感器" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.sensorType">{{ sensorTypeLabel(row.sensorType) }}</span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="85" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最新值" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.lastValue" class="last-value">{{ row.lastValue }}</span>
                <span v-else class="text-muted">无数据</span>
              </template>
            </el-table-column>
            <el-table-column prop="installLocation" label="安装位置" min-width="110" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEditForm(gh, row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDelete(gh, row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 添加/编辑设备 -->
    <el-dialog append-to-body
      v-model="formDialogVisible"
      :title="isEditing ? '编辑设备' : `添加设备 — ${formGreenhouse?.greenhouseName || ''}`"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入设备名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="固件ID" prop="firmwareId">
          <el-input
            v-model="formData.firmwareId"
            placeholder="请输入8位数字固件ID（印在设备标签上）"
            :disabled="isEditing"
            maxlength="8"
          />
        </el-form-item>
        <el-form-item v-if="isEditing" label="设备编号" prop="deviceSn">
          <el-input v-model="formData.deviceSn" :disabled="true" maxlength="50" />
        </el-form-item>
        <el-form-item v-else label="设备编号">
          <el-input model-value="系统自动生成" disabled placeholder="GH{大棚ID}-{序号}" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="formData.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option label="传感器" value="SENSOR" />
            <el-option label="控制器" value="CONTROLLER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.deviceType === 'SENSOR'" label="传感器类型" prop="sensorType">
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
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入设备描述（可选）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search, Plus, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RegionCascader from '@/components/RegionCascader.vue'
import {
  getAdminDeviceOverview,
  getAdminDeviceOwners,
  getAdminOwnerDevices
} from '@/api/admin-device'
import {
  getDevices,
  createDevice,
  updateDevice,
  deleteDevice
} from '@/api/device'

const loading = ref(false)
const regionPath = ref([])
const regionText = ref('全部地区')
const keyword = ref('')
const stats = reactive({ greenhouseCount: 0, ownerCount: 0, ownerOnline: 0, deviceTotal: 0, deviceOnline: 0, deviceOffline: 0, deviceAlarm: 0 })
const owners = ref([])

// 设备管理弹窗
const deviceDialogVisible = ref(false)
const deviceLoading = ref(false)
const currentOwner = ref(null)
const ownerGreenhouses = ref([])

// 添加/编辑表单
const formDialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formGreenhouse = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const formData = reactive({ name: '', firmwareId: '', deviceSn: '', deviceType: 'SENSOR', sensorType: '', installLocation: '', description: '' })

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  firmwareId: [
    { required: true, message: '请输入固件ID', trigger: 'blur' },
    { pattern: /^\d{8}$/, message: '固件ID必须为8位数字', trigger: 'blur' }
  ],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  sensorType: [{ required: true, message: '请选择传感器类型', trigger: 'change' }]
}

function regionParams() {
  const p = regionPath.value || []
  return {
    province: p[0] || undefined,
    city: p[1] || undefined,
    district: p[2] || undefined,
    town: p[3] || undefined,
    village: p[4] || undefined
  }
}

function regionLabel() {
  const p = regionPath.value || []
  return p.length === 0 ? '全部地区' : p.join(' / ')
}

function onRegionChange() {
  regionText.value = regionLabel()
}

function resetFilter() {
  regionPath.value = []
  regionText.value = '全部地区'
  keyword.value = ''
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [ovRes, ownerRes] = await Promise.all([
      getAdminDeviceOverview(regionParams()),
      getAdminDeviceOwners({ ...regionParams(), keyword: keyword.value || undefined })
    ])
    Object.assign(stats, ovRes.data || {})
    owners.value = ownerRes.data || []
    regionText.value = regionLabel()
  } catch (e) {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

async function openDeviceDialog(owner) {
  currentOwner.value = owner
  ownerGreenhouses.value = []
  deviceDialogVisible.value = true
  deviceLoading.value = true
  try {
    const res = await getAdminOwnerDevices(owner.id)
    ownerGreenhouses.value = res.data || []
  } catch (e) {
    // 拦截器已处理
  } finally {
    deviceLoading.value = false
  }
}

function openCreateForm(gh) {
  isEditing.value = false
  editingId.value = null
  formGreenhouse.value = gh
  Object.assign(formData, { name: '', firmwareId: '', deviceSn: '', deviceType: 'SENSOR', sensorType: '', installLocation: '', description: '' })
  formDialogVisible.value = true
}

function openEditForm(gh, row) {
  isEditing.value = true
  editingId.value = row.id
  formGreenhouse.value = gh
  Object.assign(formData, {
    name: row.name || '',
    firmwareId: row.firmwareId || '',
    deviceSn: row.deviceSn || '',
    deviceType: row.deviceType || 'SENSOR',
    sensorType: row.sensorType || '',
    installLocation: row.installLocation || '',
    description: row.description || ''
  })
  formDialogVisible.value = true
}

function submitForm() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const ghId = formGreenhouse.value.greenhouseId
      const payload = {
        name: formData.name,
        firmwareId: formData.firmwareId,
        deviceSn: formData.deviceSn,
        deviceType: formData.deviceType,
        sensorType: formData.deviceType === 'SENSOR' ? formData.sensorType : null,
        installLocation: formData.installLocation,
        description: formData.description
      }
      if (isEditing.value) {
        await updateDevice(ghId, editingId.value, payload)
        ElMessage.success('设备更新成功')
      } else {
        await createDevice(ghId, payload)
        ElMessage.success('设备添加成功')
      }
      formDialogVisible.value = false
      await openDeviceDialog(currentOwner.value)
      loadData()
    } catch (e) {
      // 拦截器已处理
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(gh, row) {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteDevice(gh.greenhouseId, row.id)
    ElMessage.success('设备删除成功')
    await openDeviceDialog(currentOwner.value)
    loadData()
  } catch (e) {
    // 取消或拦截器错误提示
  }
}

function statusTagType(status) {
  return { ONLINE: 'success', OFFLINE: 'info', ALARM: 'danger' }[status] || 'info'
}

function statusLabel(status) {
  return { ONLINE: '在线', OFFLINE: '离线', ALARM: '告警' }[status] || status
}

function sensorTypeLabel(type) {
  const map = {
    TEMPERATURE: '温度', HUMIDITY: '湿度', LIGHT: '光照', CO2: 'CO₂',
    SOIL_MOISTURE: '土壤湿度', SOIL_TEMP: '土壤温度', SOIL_PH: '土壤pH', WIND_SPEED: '风速'
  }
  return map[type] || type
}

loadData()
</script>

<style scoped>
.admin-device-page { padding: 0; }
.filter-card { margin-bottom: 16px; }
.filter-row { display: flex; align-items: center; }
.filter-label { font-size: 14px; color: #a0aec0; white-space: nowrap; }
.filter-text { margin-left: 12px; font-size: 13px; color: #94a3b8; }
.stat-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; }
.stat-sub { font-size: 14px; font-weight: 400; color: #94a3b8; }
.stat-label { margin-top: 4px; font-size: 13px; color: #94a3b8; }
.owner-card { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.header-sub { font-size: 13px; color: #94a3b8; }
.owner-devices { max-height: 62vh; overflow-y: auto; }
.gh-block { margin-bottom: 16px; }
.gh-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.gh-title { display: flex; align-items: center; font-size: 15px; font-weight: 600; }
.gh-loc { margin-left: 10px; font-size: 12px; font-weight: 400; color: #94a3b8; }
.gh-actions { display: flex; align-items: center; gap: 10px; }
.text-muted { color: #64748b; }
.last-value { font-weight: 600; color: #409EFF; }
</style>