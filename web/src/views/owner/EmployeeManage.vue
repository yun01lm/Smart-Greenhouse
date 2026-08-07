<template>
  <div class="employee-manage">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/姓名/手机号"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @clear="loadEmployees"
          @keyup.enter="loadEmployees"
        />
        <el-button type="primary" style="margin-left: 12px" :loading="loading" @click="loadEmployees">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增员工</el-button>
      </div>
    </div>

    <!-- 员工表格 -->
    <el-table v-loading="loading" :data="filteredEmployees" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100">
        <template #default="{ row }">
          <span>{{ row.realName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">
          <span>{{ row.phone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 'TECHNICIAN' ? 'warning' : 'info'" size="small">
            {{ row.role === 'TECHNICIAN' ? '技术员' : '普通员工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="{ row }">
          <span>{{ formatTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openPermDialog(row)">权限</el-button>
          <el-button type="warning" link size="small" @click="openResetPwdDialog(row)">重置密码</el-button>
          <el-button type="danger" link size="small" @click="handleRemove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增员工对话框 -->
    <el-dialog v-model="addVisible" title="新增员工" width="560px" :close-on-click-modal="false">
      <el-tabs v-model="addMode">
        <el-tab-pane label="创建账号" name="create">
          <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px" label-position="right">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="createForm.username" placeholder="登录用户名（3-50位）" maxlength="50" />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="createForm.realName" placeholder="真实姓名" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="createForm.phone" placeholder="11位手机号" maxlength="20" />
            </el-form-item>
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="createForm.password" type="password" show-password placeholder="至少8位，含字母和数字" maxlength="100" />
            </el-form-item>
            <el-form-item label="员工类型" prop="roleType">
              <el-radio-group v-model="createForm.roleType">
                <el-radio value="WORKER">普通员工</el-radio>
                <el-radio value="TECHNICIAN">技术员</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="授权大棚" prop="greenhouseId">
              <el-select v-model="createForm.greenhouseId" placeholder="选择授权大棚" style="width: 100%">
                <el-option v-for="gh in greenhouses" :key="gh.id" :label="gh.name" :value="gh.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="邀请已有账号" name="invite">
          <el-form ref="inviteFormRef" :model="inviteForm" :rules="inviteRules" label-width="90px" label-position="right">
            <el-form-item label="用户名/手机号" prop="identifier">
              <el-input v-model="inviteForm.identifier" placeholder="输入已存在员工账号的用户名或手机号" maxlength="50" />
            </el-form-item>
            <el-form-item label="授权大棚" prop="greenhouseId">
              <el-select v-model="inviteForm.greenhouseId" placeholder="选择授权大棚" style="width: 100%">
                <el-option v-for="gh in greenhouses" :key="gh.id" :label="gh.name" :value="gh.id" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addSubmitting" @click="submitAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 权限编辑对话框 -->
    <el-dialog v-model="permVisible" title="权限设置" width="620px" :close-on-click-modal="false">
      <el-alert
        v-if="permEmployee"
        :title="`${permEmployee.realName || permEmployee.username}（${permEmployee.role === 'TECHNICIAN' ? '技术员' : '普通员工'}）`"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-form v-if="permList.length" label-width="90px" label-position="left">
        <template v-for="(perm, idx) in permList" :key="perm.greenhouseId">
          <el-divider v-if="idx > 0" />
          <el-form-item :label="`大棚：${perm.greenhouseName}`">
            <div class="perm-grid">
              <el-checkbox v-model="perm.canViewData">查看数据</el-checkbox>
              <el-checkbox v-model="perm.canControlDevice">控制设备</el-checkbox>
              <el-checkbox v-model="perm.canDiagnose">病虫害诊断</el-checkbox>
              <el-checkbox v-model="perm.canAskExpert">专家咨询</el-checkbox>
              <el-checkbox v-model="perm.canViewAlerts">查看预警</el-checkbox>
              <el-checkbox v-model="perm.canViewHistory">查看历史</el-checkbox>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <el-empty v-else description="该员工暂无权限记录，请先在大棚管理中添加" />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" :disabled="!permList.length" @click="submitPerm">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="80px" label-position="right">
        <el-form-item v-if="resetEmployee" label="员工">
          <el-input :model-value="`${resetEmployee.realName || resetEmployee.username}`" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少8位，含字母和数字" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getGreenhouses } from '@/api/greenhouse'
import {
  getEmployees,
  addEmployee,
  getEmployeePermissions,
  updateEmployeePermission,
  resetEmployeePassword,
  removeEmployee
} from '@/api/employee'

const loading = ref(false)
const employees = ref([])
const searchKeyword = ref('')
const greenhouses = ref([])

const filteredEmployees = computed(() => {
  const kw = (searchKeyword.value || '').trim().toLowerCase()
  if (!kw) return employees.value
  return employees.value.filter(
    e =>
      (e.username || '').toLowerCase().includes(kw) ||
      (e.realName || '').toLowerCase().includes(kw) ||
      (e.phone || '').toLowerCase().includes(kw)
  )
})

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function loadEmployees() {
  loading.value = true
  try {
    const res = await getEmployees()
    employees.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

// ===== 新增员工 =====
const addVisible = ref(false)
const addMode = ref('create')
const addSubmitting = ref(false)
const createFormRef = ref(null)
const inviteFormRef = ref(null)
const createForm = ref({ username: '', realName: '', phone: '', password: '', roleType: 'WORKER', greenhouseId: null })
const inviteForm = ref({ identifier: '', greenhouseId: null })

const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码至少8位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && !/[a-zA-Z]/.test(value)) return callback(new Error('密码必须包含字母'))
        if (value && !/[0-9]/.test(value)) return callback(new Error('密码必须包含数字'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  greenhouseId: [{ required: true, message: '请选择授权大棚', trigger: 'change' }]
}
const inviteRules = {
  identifier: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  greenhouseId: [{ required: true, message: '请选择授权大棚', trigger: 'change' }]
}

function openAddDialog() {
  addMode.value = 'create'
  createForm.value = { username: '', realName: '', phone: '', password: '', roleType: 'WORKER', greenhouseId: null }
  inviteForm.value = { identifier: '', greenhouseId: null }
  addVisible.value = true
}

async function submitAdd() {
  if (addMode.value === 'create') {
    try {
      await createFormRef.value.validate()
    } catch {
      return
    }
  } else {
    try {
      await inviteFormRef.value.validate()
    } catch {
      return
    }
  }
  addSubmitting.value = true
  try {
    const payload =
      addMode.value === 'create'
        ? { ...createForm.value }
        : { identifier: inviteForm.value.identifier, greenhouseId: inviteForm.value.greenhouseId }
    await addEmployee(payload)
    ElMessage.success('员工添加成功')
    addVisible.value = false
    loadEmployees()
  } catch {
    // handled by interceptor
  } finally {
    addSubmitting.value = false
  }
}

// ===== 权限编辑 =====
const permVisible = ref(false)
const permSubmitting = ref(false)
const permEmployee = ref(null)
const permList = ref([])

async function openPermDialog(row) {
  permEmployee.value = row
  permList.value = []
  permVisible.value = true
  try {
    const res = await getEmployeePermissions(row.id)
    permList.value = res.data || []
  } catch {
    // handled by interceptor
  }
}

async function submitPerm() {
  permSubmitting.value = true
  try {
    for (const perm of permList.value) {
      await updateEmployeePermission(permEmployee.value.id, {
        greenhouseId: perm.greenhouseId,
        canViewData: perm.canViewData,
        canControlDevice: perm.canControlDevice,
        canDiagnose: perm.canDiagnose,
        canAskExpert: perm.canAskExpert,
        canViewAlerts: perm.canViewAlerts,
        canViewHistory: perm.canViewHistory
      })
    }
    ElMessage.success('权限保存成功')
    permVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    permSubmitting.value = false
  }
}

// ===== 重置密码 =====
const resetVisible = ref(false)
const resetSubmitting = ref(false)
const resetEmployee = ref(null)
const resetFormRef = ref(null)
const resetForm = ref({ newPassword: '' })
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码至少8位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && !/[a-zA-Z]/.test(value)) return callback(new Error('密码必须包含字母'))
        if (value && !/[0-9]/.test(value)) return callback(new Error('密码必须包含数字'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function openResetPwdDialog(row) {
  resetEmployee.value = row
  resetForm.value = { newPassword: '' }
  resetVisible.value = true
}

async function submitReset() {
  try {
    await resetFormRef.value.validate()
  } catch {
    return
  }
  resetSubmitting.value = true
  try {
    await resetEmployeePassword(resetEmployee.value.id, resetForm.value.newPassword)
    ElMessage.success('密码重置成功')
    resetVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    resetSubmitting.value = false
  }
}

// ===== 移除员工 =====
async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定移除员工「${row.realName || row.username}」吗？移除后该员工将失去本棚所有大棚的访问权限。`,
      '移除员工',
      { type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await removeEmployee(row.id)
    ElMessage.success('员工已移除')
    loadEmployees()
  } catch {
    // handled by interceptor
  }
}

onMounted(async () => {
  loadEmployees()
  try {
    const res = await getGreenhouses()
    greenhouses.value = res.data || []
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.employee-manage {
  min-height: calc(100vh - 200px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

.perm-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 20px;
  width: 100%;
}
</style>