<template>
  <div class="alert-rule-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: 预警规则管理 -->
      <el-tab-pane label="预警规则" name="rules">
        <div class="tab-content">
          <!-- 操作栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select
                v-model="ruleFilter.greenhouseId"
                placeholder="按大棚筛选"
                clearable
                style="width: 180px"
                @change="loadRules"
              >
                <el-option
                  v-for="gh in greenhouses"
                  :key="gh.id"
                  :label="gh.name"
                  :value="gh.id"
                />
              </el-select>
              <el-select
                v-model="ruleFilter.sensorType"
                placeholder="传感器类型"
                clearable
                style="width: 150px; margin-left: 12px"
                @change="loadRules"
              >
                <el-option
                  v-for="st in sensorTypes"
                  :key="st.value"
                  :label="st.label"
                  :value="st.value"
                />
              </el-select>
            </div>
            <el-button type="primary" :icon="Plus" @click="openRuleDialog()">新建规则</el-button>
          </div>

          <!-- 规则表格 -->
          <el-table v-loading="ruleLoading" :data="filteredRules" stripe border>
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="大棚" min-width="120">
              <template #default="{ row }">
                {{ getGreenhouseName(row.greenhouseId) }}
              </template>
            </el-table-column>
            <el-table-column label="传感器" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ sensorLabel(row.sensorType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="规则类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="ruleTypeTag(row.ruleType)" size="small">
                  {{ ruleTypeLabel(row.ruleType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="条件" min-width="180">
              <template #default="{ row }">
                <code class="condition-code">{{ formatCondition(row) }}</code>
              </template>
            </el-table-column>
            <el-table-column label="告警级别" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="levelTag(row.alertLevel)" size="small" effect="dark">
                  {{ levelLabel(row.alertLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled"
                  size="small"
                  @change="(val) => toggleRule(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170" align="center">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openRuleDialog(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper" v-if="rules.length > 0">
            <span class="total-info">共 {{ filteredRules.length }} 条</span>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="!ruleLoading && rules.length === 0" description="暂无预警规则，点击「新建规则」创建" />
        </div>
      </el-tab-pane>

      <!-- Tab 2: 自定义阈值管理 -->
      <el-tab-pane label="自定义阈值" name="thresholds">
        <div class="tab-content">
          <!-- 操作栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select
                v-model="thresholdFilter.greenhouseId"
                placeholder="按大棚筛选"
                clearable
                style="width: 180px"
                @change="loadThresholds"
              >
                <el-option
                  v-for="gh in greenhouses"
                  :key="gh.id"
                  :label="gh.name"
                  :value="gh.id"
                />
              </el-select>
            </div>
          </div>

          <!-- 阈值表格 -->
          <el-table v-loading="thresholdLoading" :data="filteredThresholds" stripe border>
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="大棚" min-width="120">
              <template #default="{ row }">
                {{ getGreenhouseName(row.greenhouseId) }}
              </template>
            </el-table-column>
            <el-table-column label="用户ID" width="80" align="center">
              <template #default="{ row }">
                {{ row.userId }}
              </template>
            </el-table-column>
            <el-table-column label="传感器类型" width="130" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ sensorLabel(row.sensorType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最低阈值" width="110" align="center">
              <template #default="{ row }">
                <span :class="{ 'text-muted': row.minThreshold == null }">
                  {{ row.minThreshold != null ? row.minThreshold : '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="最高阈值" width="110" align="center">
              <template #default="{ row }">
                <span :class="{ 'text-muted': row.maxThreshold == null }">
                  {{ row.maxThreshold != null ? row.maxThreshold : '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170" align="center">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleDeleteThreshold(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper" v-if="thresholds.length > 0">
            <span class="total-info">共 {{ filteredThresholds.length }} 条</span>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="!thresholdLoading && thresholds.length === 0" description="暂无自定义阈值记录" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 规则编辑对话框 -->
    <el-dialog
      v-model="ruleDialogVisible"
      :title="editingRule ? '编辑预警规则' : '新建预警规则'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="ruleFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="所属大棚" prop="greenhouseId">
          <el-select v-model="ruleForm.greenhouseId" placeholder="请选择大棚" style="width: 100%" :disabled="!!editingRule">
            <el-option v-for="gh in greenhouses" :key="gh.id" :label="gh.name" :value="gh.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="传感器类型" prop="sensorType">
          <el-select v-model="ruleForm.sensorType" placeholder="请选择传感器类型" style="width: 100%">
            <el-option v-for="st in sensorTypes" :key="st.value" :label="st.label" :value="st.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="ruleForm.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option label="阈值规则 (THRESHOLD)" value="THRESHOLD" />
            <el-option label="趋势规则 (TREND)" value="TREND" />
            <el-option label="复合规则 (COMPOSITE)" value="COMPOSITE" />
            <el-option label="天气关联 (WEATHER)" value="WEATHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则条件" prop="conditionJson">
          <el-input
            v-model="ruleForm.conditionJson"
            type="textarea"
            :rows="4"
            placeholder='JSON 格式，如 {"min":15,"max":35}'
          />
          <div class="form-tip">
            <span v-if="ruleForm.ruleType === 'THRESHOLD'">阈值格式: {"min":最小值, "max":最大值}</span>
            <span v-else-if="ruleForm.ruleType === 'TREND'">趋势格式: {"direction":"rising","rate":5,"duration":300}</span>
            <span v-else-if="ruleForm.ruleType === 'COMPOSITE'">复合格式: {"conditions":[...],"logic":"AND"}</span>
            <span v-else>自定义 JSON</span>
          </div>
        </el-form-item>
        <el-form-item label="告警级别" prop="alertLevel">
          <el-radio-group v-model="ruleForm.alertLevel">
            <el-radio-button value="INFO">提示</el-radio-button>
            <el-radio-button value="WARNING">警告</el-radio-button>
            <el-radio-button value="CRITICAL">严重</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="ruleForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSubmitting" @click="submitRuleForm">
          {{ editingRule ? '保存修改' : '创建规则' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGreenhouses } from '@/api/greenhouse'
import { useViewModeStore } from '@/stores/viewMode'
import {
  getAlertRules, createAlertRule, updateAlertRule, deleteAlertRule,
  getThresholds, deleteThreshold
} from '@/api/alert-rule'

// ===== 基础数据 =====
const activeTab = ref('rules')
const greenhouses = ref([])

// ===== 预警规则 =====
const ruleLoading = ref(false)
const rules = ref([])

const ruleFilter = reactive({
  greenhouseId: null,
  sensorType: ''
})

const filteredRules = computed(() => {
  let list = rules.value
  if (ruleFilter.sensorType) {
    list = list.filter(r => r.sensorType === ruleFilter.sensorType)
  }
  return list
})

// 规则对话框
const ruleDialogVisible = ref(false)
const editingRule = ref(null)
const ruleSubmitting = ref(false)
const ruleFormRef = ref(null)

const defaultRuleForm = () => ({
  greenhouseId: null,
  sensorType: '',
  ruleType: 'THRESHOLD',
  conditionJson: '',
  alertLevel: 'WARNING',
  enabled: true
})

const ruleForm = reactive(defaultRuleForm())

const ruleFormRules = {
  greenhouseId: [{ required: true, message: '请选择大棚', trigger: 'change' }],
  sensorType: [{ required: true, message: '请选择传感器类型', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  conditionJson: [
    { required: true, message: '请输入规则条件', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        try { JSON.parse(value); callback() }
        catch { callback(new Error('条件格式无效，请输入合法 JSON')) }
      },
      trigger: 'blur'
    }
  ],
  alertLevel: [{ required: true, message: '请选择告警级别', trigger: 'change' }]
}

// ===== 自定义阈值 =====
const thresholdLoading = ref(false)
const thresholds = ref([])

const thresholdFilter = reactive({
  greenhouseId: null
})

const filteredThresholds = computed(() => {
  return thresholds.value
})

// ===== 传感器类型映射 =====
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

const viewStore = useViewModeStore()

// ===== 数据加载 =====
async function loadGreenhouses() {
  // R10：棚主视角下使用该棚主的大棚列表
  if (viewStore.active) {
    greenhouses.value = viewStore.greenhouses || []
    return
  }
  try {
    const res = await getGreenhouses()
    greenhouses.value = res.data || []
  } catch { /* handled by interceptor */ }
}

async function loadRules() {
  ruleLoading.value = true
  try {
    const res = await getAlertRules(ruleFilter.greenhouseId)
    rules.value = res.data || []
  } catch { /* handled by interceptor */ }
  finally { ruleLoading.value = false }
}

async function loadThresholds() {
  thresholdLoading.value = true
  try {
    const res = await getThresholds(thresholdFilter.greenhouseId)
    thresholds.value = res.data || []
  } catch { /* handled by interceptor */ }
  finally { thresholdLoading.value = false }
}

// ===== 规则 CRUD =====
function openRuleDialog(row) {
  if (row) {
    editingRule.value = row
    Object.assign(ruleForm, {
      greenhouseId: row.greenhouseId,
      sensorType: row.sensorType,
      ruleType: row.ruleType,
      conditionJson: row.conditionJson,
      alertLevel: row.alertLevel,
      enabled: row.enabled
    })
  } else {
    editingRule.value = null
    Object.assign(ruleForm, defaultRuleForm())
  }
  ruleDialogVisible.value = true
}

async function submitRuleForm() {
  try {
    await ruleFormRef.value.validate()
  } catch { return }

  ruleSubmitting.value = true
  try {
    const data = {
      greenhouseId: ruleForm.greenhouseId,
      sensorType: ruleForm.sensorType,
      ruleType: ruleForm.ruleType,
      conditionJson: ruleForm.conditionJson,
      alertLevel: ruleForm.alertLevel,
      enabled: ruleForm.enabled
    }

    if (editingRule.value) {
      await updateAlertRule(editingRule.value.id, data)
      ElMessage.success('规则更新成功')
    } else {
      await createAlertRule(data)
      ElMessage.success('规则创建成功')
    }
    ruleDialogVisible.value = false
    await loadRules()
  } catch { /* handled by interceptor */ }
  finally { ruleSubmitting.value = false }
}

async function toggleRule(row, enabled) {
  try {
    await updateAlertRule(row.id, {
      greenhouseId: row.greenhouseId,
      sensorType: row.sensorType,
      ruleType: row.ruleType,
      conditionJson: row.conditionJson,
      alertLevel: row.alertLevel,
      enabled
    })
    row.enabled = enabled
    ElMessage.success(enabled ? '规则已启用' : '规则已禁用')
  } catch { /* handled by interceptor */ }
}

async function handleDeleteRule(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除该预警规则吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteAlertRule(row.id)
    ElMessage.success('规则已删除')
    await loadRules()
  } catch { /* cancelled or error */ }
}

// ===== 阈值操作 =====
async function handleDeleteThreshold(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除该自定义阈值吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteThreshold(row.id)
    ElMessage.success('阈值已删除')
    await loadThresholds()
  } catch { /* cancelled or error */ }
}

// ===== 工具函数 =====
function getGreenhouseName(id) {
  const gh = greenhouses.value.find(g => g.id === id)
  return gh ? gh.name : `大棚 #${id}`
}

function sensorLabel(type) {
  const st = sensorTypes.find(s => s.value === type)
  return st ? st.label : type
}

function ruleTypeLabel(type) {
  const map = { THRESHOLD: '阈值', TREND: '趋势', COMPOSITE: '复合', WEATHER: '天气' }
  return map[type] || type
}

function ruleTypeTag(type) {
  const map = { THRESHOLD: '', TREND: 'warning', COMPOSITE: 'danger', WEATHER: 'info' }
  return map[type] || ''
}

function levelLabel(level) {
  const map = { INFO: '提示', WARNING: '警告', CRITICAL: '严重' }
  return map[level] || level
}

function levelTag(level) {
  const map = { INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' }
  return map[level] || 'info'
}

function formatCondition(row) {
  if (row.ruleType === 'THRESHOLD') {
    try {
      const c = JSON.parse(row.conditionJson)
      const parts = []
      if (c.min != null) parts.push(`min=${c.min}`)
      if (c.max != null) parts.push(`max=${c.max}`)
      return parts.join(', ')
    } catch { return row.conditionJson }
  }
  if (row.conditionJson && row.conditionJson.length > 60) {
    return row.conditionJson.substring(0, 60) + '...'
  }
  return row.conditionJson
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ===== 初始化 =====
onMounted(async () => {
  await loadGreenhouses()
  loadRules()
  loadThresholds()
})
</script>

<style scoped>
.alert-rule-page {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.tab-content {
  padding: 16px;
  min-height: 400px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.condition-code {
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #606266;
  word-break: break-all;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.text-muted {
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.total-info {
  font-size: 13px;
  color: #909399;
}
</style>
