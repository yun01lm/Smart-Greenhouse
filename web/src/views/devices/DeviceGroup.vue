<template>
  <div class="device-group">
    <div class="group-layout">
      <!-- 左侧：分组列表 -->
      <div class="group-panel">
        <div class="panel-header">
          <h3>设备分组</h3>
          <el-button type="primary" size="small" :icon="Plus" @click="openCreateGroupDialog">新建分组</el-button>
        </div>
        <el-scrollbar height="calc(100vh - 260px)">
          <div v-loading="groupLoading" class="group-list">
            <div v-if="groups.length === 0" class="empty-hint">暂无分组，点击上方按钮创建</div>
            <div
              v-for="group in groups"
              :key="group.id"
              class="group-item"
              :class="{ active: selectedGroup?.id === group.id }"
              @click="selectGroup(group)"
            >
              <div class="group-info">
                <el-icon><Folder /></el-icon>
                <span class="group-name">{{ group.name }}</span>
                <el-tag size="small" type="info" effect="plain">{{ group.deviceCount }}</el-tag>
              </div>
              <div class="group-actions" @click.stop>
                <el-button type="primary" link size="small" @click="openEditGroupDialog(group)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteGroup(group)">删除</el-button>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 右侧：分组详情 + 设备分配 -->
      <div class="detail-panel">
        <div v-if="!selectedGroup" class="empty-state">
          <el-icon :size="48"><FolderOpened /></el-icon>
          <p>请选择一个分组查看详情</p>
        </div>
        <div v-else class="group-detail">
          <div class="detail-header">
            <h3>{{ selectedGroup.name }}</h3>
            <p v-if="selectedGroup.description" class="group-desc">{{ selectedGroup.description }}</p>
            <p class="group-meta">共 {{ selectedGroup.deviceCount }} 个设备 · 创建于 {{ formatTime(selectedGroup.createdAt) }}</p>
          </div>
          <el-divider />

          <!-- 组内设备列表 -->
          <div class="detail-toolbar">
            <span class="sub-title">组内设备</span>
            <el-button type="primary" size="small" :icon="Plus" @click="openAssignDialog">分配设备</el-button>
          </div>
          <el-table
            v-loading="deviceLoading"
            :data="groupDevices"
            stripe
            border
            size="small"
            style="width: 100%"
            empty-text="该分组暂无设备"
          >
            <el-table-column prop="deviceSn" label="编号" min-width="110" />
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.deviceType === 'SENSOR' ? 'success' : 'warning'" size="small">
                  {{ row.deviceType === 'SENSOR' ? '传感器' : '控制器' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleRemoveDevice(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 创建/编辑分组对话框 -->
    <el-dialog append-to-body
      v-model="groupDialogVisible"
      :title="isEditingGroup ? '编辑分组' : '新建分组'"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="groupFormRef"
        :model="groupForm"
        :rules="groupFormRules"
        label-width="80px"
      >
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入分组名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入分组描述（可选）"
            maxlength="100"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitGroupForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配设备对话框 -->
    <el-dialog append-to-body
      v-model="assignDialogVisible"
      title="分配设备到分组"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-transfer
        v-model="assignDeviceIds"
        :data="transferData"
        :titles="['可选设备', '已选设备']"
        filterable
        filter-placeholder="搜索设备"
      />
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Folder, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeviceGroups, createDeviceGroup, updateDeviceGroup, deleteDeviceGroup,
  addDeviceToGroup, removeDeviceFromGroup
} from '@/api/device'
import { getDevices } from '@/api/device'

const props = defineProps({
  greenhouseId: { type: [Number, String], required: true }
})

// ===== 分组数据 =====
const groupLoading = ref(false)
const groups = ref([])
const selectedGroup = ref(null)

// ===== 组内设备 =====
const deviceLoading = ref(false)
const allDevices = ref([])
const groupDevices = ref([])

// ===== 分组对话框 =====
const groupDialogVisible = ref(false)
const isEditingGroup = ref(false)
const editingGroupId = ref(null)
const groupSubmitting = ref(false)
const groupFormRef = ref(null)

const defaultGroupForm = () => ({ name: '', description: '' })
const groupForm = ref(defaultGroupForm())
const groupFormRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

// ===== 分配对话框 =====
const assignDialogVisible = ref(false)
const assignDeviceIds = ref([])
const assignSubmitting = ref(false)

const transferData = computed(() => {
  return allDevices.value
    .filter(d => d.deviceType === 'SENSOR')
    .map(d => ({
      key: d.id,
      label: `${d.name} (${d.deviceSn})`
    }))
})

// ===== 加载分组 =====
async function loadGroups() {
  groupLoading.value = true
  try {
    const res = await getDeviceGroups(props.greenhouseId)
    groups.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    groupLoading.value = false
  }
}

// ===== 加载全部设备 =====
async function loadAllDevices() {
  try {
    const res = await getDevices(props.greenhouseId)
    allDevices.value = res.data || []
  } catch {
    // handled by interceptor
  }
}

// ===== 选择分组 =====
async function selectGroup(group) {
  selectedGroup.value = group
  await refreshGroupDevices()
}

async function refreshGroupDevices() {
  if (!selectedGroup.value) return
  deviceLoading.value = true
  try {
    // 根据分组中的 deviceIds 过滤出设备
    const ids = selectedGroup.value.deviceIds || []
    groupDevices.value = allDevices.value.filter(d => ids.includes(d.id))
  } finally {
    deviceLoading.value = false
  }
}

// ===== 分组 CRUD =====
function openCreateGroupDialog() {
  isEditingGroup.value = false
  editingGroupId.value = null
  groupForm.value = defaultGroupForm()
  groupDialogVisible.value = true
}

function openEditGroupDialog(group) {
  isEditingGroup.value = true
  editingGroupId.value = group.id
  groupForm.value = {
    name: group.name,
    description: group.description || ''
  }
  groupDialogVisible.value = true
}

async function submitGroupForm() {
  try {
    await groupFormRef.value.validate()
  } catch {
    return
  }

  groupSubmitting.value = true
  try {
    if (isEditingGroup.value) {
      await updateDeviceGroup(props.greenhouseId, editingGroupId.value, groupForm.value)
      ElMessage.success('分组更新成功')
    } else {
      await createDeviceGroup(props.greenhouseId, groupForm.value)
      ElMessage.success('分组创建成功')
    }
    groupDialogVisible.value = false
    await loadGroups()
  } catch {
    // handled by interceptor
  } finally {
    groupSubmitting.value = false
  }
}

async function handleDeleteGroup(group) {
  try {
    await ElMessageBox.confirm(
      `确认删除分组「${group.name}」吗？组内设备不会被删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteDeviceGroup(props.greenhouseId, group.id)
    ElMessage.success('分组已删除')
    if (selectedGroup.value?.id === group.id) {
      selectedGroup.value = null
    }
    await loadGroups()
  } catch {
    // cancelled or error
  }
}

// ===== 设备分配 =====
function openAssignDialog() {
  assignDeviceIds.value = selectedGroup.value?.deviceIds || []
  assignDialogVisible.value = true
}

async function submitAssign() {
  if (!selectedGroup.value) return
  assignSubmitting.value = true
  try {
    const oldIds = new Set(selectedGroup.value.deviceIds || [])
    const newIds = new Set(assignDeviceIds.value)

    // 需要添加的设备
    const toAdd = [...newIds].filter(id => !oldIds.has(id))
    // 需要移除的设备
    const toRemove = [...oldIds].filter(id => !newIds.has(id))

    // 并行操作
    const tasks = []
    toAdd.forEach(id => {
      tasks.push(addDeviceToGroup(props.greenhouseId, selectedGroup.value.id, id))
    })
    toRemove.forEach(id => {
      tasks.push(removeDeviceFromGroup(props.greenhouseId, selectedGroup.value.id, id))
    })

    await Promise.allSettled(tasks)

    ElMessage.success('设备分配已保存')
    assignDialogVisible.value = false
    await loadGroups()
    // 重新选中分组刷新
    const updated = groups.value.find(g => g.id === selectedGroup.value.id)
    if (updated) {
      selectedGroup.value = updated
      await refreshGroupDevices()
    }
  } catch {
    // handled by interceptor
  } finally {
    assignSubmitting.value = false
  }
}

async function handleRemoveDevice(row) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.name}」从当前分组移除？`,
      '移除设备',
      { type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消' }
    )
    await removeDeviceFromGroup(props.greenhouseId, selectedGroup.value.id, row.id)
    ElMessage.success('设备已移除')
    await loadGroups()
    const updated = groups.value.find(g => g.id === selectedGroup.value.id)
    if (updated) {
      selectedGroup.value = updated
      await refreshGroupDevices()
    }
  } catch {
    // cancelled or error
  }
}

// ===== 工具函数 =====
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

// ===== 初始化 =====
watch(() => props.greenhouseId, () => {
  loadGroups()
  loadAllDevices()
  selectedGroup.value = null
}, { immediate: true })
</script>

<style scoped>
.device-group {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  min-height: calc(100vh - 200px);
  backdrop-filter: blur(10px);
}

.group-layout {
  display: flex;
  gap: 16px;
  height: 100%;
}

.group-panel {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  padding-right: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.group-list {
  padding-right: 4px;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
}

.group-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.group-item.active {
  background: rgba(64, 158, 255, 0.18);
  border-left: 3px solid #409eff;
}

.group-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-name {
  font-size: 14px;
  font-weight: 500;
}

.group-actions {
  display: none;
}

.group-item:hover .group-actions {
  display: flex;
  gap: 4px;
}

.detail-panel {
  flex: 1;
  min-width: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #64748b;
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.detail-header h3 {
  margin: 0 0 8px 0;
  font-size: 17px;
}

.group-desc {
  color: #94a3b8;
  font-size: 13px;
  margin: 0 0 4px 0;
}

.group-meta {
  color: #64748b;
  font-size: 12px;
  margin: 0;
}

.detail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sub-title {
  font-size: 14px;
  font-weight: 600;
}

.empty-hint {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  padding: 32px 0;
}
</style>
