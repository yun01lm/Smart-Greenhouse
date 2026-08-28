<template>
  <div class="user-list">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="filterRole"
          placeholder="角色筛选"
          clearable
          style="width: 140px"
          @change="onFilterChange"
        >
          <el-option label="管理员" value="ADMIN" />
          <el-option label="棚主" value="OWNER" />
          <el-option label="员工" value="WORKER" />
          <el-option label="技术员" value="TECHNICIAN" />
          <el-option label="专家" value="EXPERT" />
        </el-select>
        <RegionCascader v-model="regionPath" width="300px" style="margin-left: 12px" />
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/姓名/手机号"
          clearable
          style="width: 220px; margin-left: 12px"
          :prefix-icon="Search"
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        />
        <el-button type="primary" style="margin-left: 12px" :loading="loading" @click="loadUsers">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      </div>
    </div>

    <!-- 用户表格 -->
    <el-table
      v-loading="loading"
      :data="pagedUsers"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="真实姓名" min-width="100">
        <template #default="{ row }">
          <span>{{ row.realName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">
          <span>{{ row.phone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所属地区" min-width="170">
        <template #default="{ row }">
          <span v-if="row.regionText">{{ row.regionText }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">
            {{ roleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small" effect="dark">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170" align="center">
        <template #default="{ row }">
          <span>{{ formatTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="filteredUsers.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredUsers.length"
        layout="total, sizes, prev, pager, next"
        @size-change="onPageSizeChange"
      />
    </div>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑用户"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="用户名">
          <el-input :model-value="formData.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="棚主" value="OWNER" />
            <el-option label="员工" value="WORKER" />
            <el-option label="专家" value="EXPERT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="formData.status"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">修改密码（需验证绑定手机号）</el-divider>
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="验证手机号" prop="phone">
          <el-input v-model="pwdForm.phone" placeholder="请输入该用户当前绑定的手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，且包含字母和数字" maxlength="100" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" maxlength="100" />
        </el-form-item>
      </el-form>
      <div class="pwd-actions">
        <el-button type="primary" plain :loading="pwdSubmitting" @click="submitResetPassword">修改密码</el-button>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户对话框 -->
    <el-dialog
      v-model="createVisible"
      title="新增用户"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        初始密码统一为 123456，建议用户首次登录后自助修改密码。
      </el-alert>
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="3-50位，登录用" maxlength="50" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="请输入真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="棚主" value="OWNER" />
            <el-option label="员工" value="WORKER" />
            <el-option label="专家" value="EXPERT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.role === 'WORKER'" label="归属棚主" prop="ownerId">
          <el-select
            v-model="createForm.ownerId"
            placeholder="请选择归属棚主"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="o in ownerOptions"
              :key="o.id"
              :label="o.realName ? `${o.realName}（${o.username}）` : o.username"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, deleteUser, adminResetPassword } from '@/api/admin'
import RegionCascader from '@/components/RegionCascader.vue'

// ===== 数据 =====
const loading = ref(false)
const users = ref([])
const filterRole = ref('')
const searchKeyword = ref('')
const regionPath = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

// ===== 对话框 =====
const dialogVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const formData = ref({
  username: '',
  realName: '',
  phone: '',
  role: 'OWNER',
  status: true
})

const formRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// ===== 新增用户 =====
const createVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref(null)
const ownerOptions = ref([])
const createForm = ref({
  username: '',
  realName: '',
  phone: '',
  role: 'OWNER',
  ownerId: null
})

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50位', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  ownerId: [{ required: true, message: '请选择归属棚主', trigger: 'change' }]
}

// ===== 修改密码（管理员重置，需验证绑定手机号） =====
const pwdFormRef = ref(null)
const pwdSubmitting = ref(false)
const pwdForm = ref({
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  phone: [{ required: true, message: '请输入绑定手机号', trigger: 'blur' }],
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
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// ===== 计算属性（筛选由后端完成，前端仅分页） =====
const filteredUsers = computed(() => users.value)

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
})

// ===== 方法 =====
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

async function loadUsers() {
  loading.value = true
  try {
    const params = { ...regionParams() }
    if (filterRole.value) params.role = filterRole.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await getUsers(params)
    users.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  currentPage.value = 1
  loadUsers()
}

function resetFilter() {
  filterRole.value = ''
  searchKeyword.value = ''
  regionPath.value = []
  currentPage.value = 1
  loadUsers()
}

function onPageSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
}

function openEditDialog(row) {
  editingId.value = row.id
  formData.value = {
    username: row.username,
    realName: row.realName || '',
    phone: row.phone || '',
    role: row.role,
    status: row.status
  }
  resetPwdForm()
  dialogVisible.value = true
}

// ===== 新增用户方法 =====
async function openCreateDialog() {
  createForm.value = { username: '', realName: '', phone: '', role: 'OWNER', ownerId: null }
  createVisible.value = true
  await loadOwnerOptions()
}

async function loadOwnerOptions() {
  try {
    const res = await getUsers({ role: 'OWNER' })
    ownerOptions.value = res.data || []
  } catch {
    ownerOptions.value = []
  }
}

async function submitCreate() {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }

  createSubmitting.value = true
  try {
    const payload = {
      username: createForm.value.username,
      realName: createForm.value.realName || null,
      phone: createForm.value.phone || null,
      role: createForm.value.role,
      ownerId: createForm.value.role === 'WORKER' ? createForm.value.ownerId : null
    }
    await createUser(payload)
    ElMessage.success('用户创建成功，初始密码为 123456')
    createVisible.value = false
    await loadUsers()
  } catch {
    // handled by interceptor
  } finally {
    createSubmitting.value = false
  }
}

// ===== 管理员重置密码方法 =====
function resetPwdForm() {
  pwdForm.value = { phone: '', newPassword: '', confirmPassword: '' }
  pwdFormRef.value?.clearValidate()
}

async function submitResetPassword() {
  if (!editingId.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }

  pwdSubmitting.value = true
  try {
    await adminResetPassword(editingId.value, {
      phone: pwdForm.value.phone,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    resetPwdForm()
  } catch {
    // handled by interceptor
  } finally {
    pwdSubmitting.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await updateUser(editingId.value, {
      realName: formData.value.realName || null,
      phone: formData.value.phone || null,
      role: formData.value.role,
      status: formData.value.status
    })
    ElMessage.success('用户更新成功')
    dialogVisible.value = false
    await loadUsers()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除用户「${row.username}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch {
    // cancelled or error
  }
}

// ===== 工具函数 =====
function roleLabel(role) {
  const map = { ADMIN: '管理员', OWNER: '棚主', WORKER: '员工', TECHNICIAN: '技术员', EXPERT: '专家' }
  return map[role] || role
}

function roleTagType(role) {
  const map = { ADMIN: 'danger', OWNER: 'warning', WORKER: 'success', TECHNICIAN: 'primary', EXPERT: 'info' }
  return map[role] || 'info'
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-list {
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
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.pwd-actions {
  margin-top: 8px;
  text-align: right;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: #64748b;
}
</style>
