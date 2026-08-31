<template>
  <div class="gh-page">
    <!-- 用途说明 -->
    <div class="page-desc">
      管理您名下的大棚：新增大棚后即可添加设备、配置预警规则与场景联动。删除大棚会<em>级联清理</em>其下全部设备、规则、场景、授权与历史数据，请谨慎操作。
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="total-tip">共 {{ list.length }} 个大棚（上限 {{ MAX_LIMIT }} 个）</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增大棚</el-button>
    </div>

    <!-- 大棚列表 -->
    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column label="大棚名称" min-width="150">
        <template #default="{ row }">
          <span class="gh-name">{{ row.name }}</span>
          <el-tag v-if="row.cropType" size="small" type="success" style="margin-left: 8px">{{ row.cropType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地区" min-width="200">
        <template #default="{ row }">
          {{ [row.province, row.city, row.district, row.town, row.village].filter(Boolean).join(' ') || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="位置描述" min-width="120">
        <template #default="{ row }">
          <span class="text-ellipsis" :title="row.location">{{ row.location || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="165" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑大棚' : '新增大棚'"
      width="min(560px, 92vw)"
      append-to-body
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="大棚名称" prop="name">
          <el-input v-model="form.name" placeholder="如：一号番茄大棚" maxlength="100" />
        </el-form-item>
        <el-form-item label="所在地区">
          <RegionCascader v-model="form.region" />
        </el-form-item>
        <el-form-item label="作物类型">
          <el-input v-model="form.cropType" placeholder="如：番茄 / 黄瓜 / 草莓（可选）" maxlength="50" />
        </el-form-item>
        <el-form-item label="位置描述">
          <el-input v-model="form.location" placeholder="如：村东头第三排（可选）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGreenhouses, createGreenhouse, updateGreenhouse, deleteGreenhouse } from '@/api/greenhouse'
import RegionCascader from '@/components/RegionCascader.vue'
import { useViewModeStore } from '@/stores/viewMode'

const viewStore = useViewModeStore()
const MAX_LIMIT = 10

const loading = ref(false)
const list = ref([])

// ===== 新增/编辑 =====
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({
  name: '',
  region: [],
  cropType: '',
  location: ''
})

const rules = {
  name: [{ required: true, message: '请输入大棚名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getGreenhouses()
    const all = res.data || []
    // 棚主视角（管理员代看）：按该棚主过滤；否则按当前用户角色返回
    list.value = viewStore.active
      ? all.filter(g => g.ownerId === viewStore.ownerId)
      : all
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function openCreate() {
  if (list.value.length >= MAX_LIMIT) {
    ElMessage.warning(`大棚数量已达上限（${MAX_LIMIT} 个）`)
    return
  }
  editingId.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.region = [row.province, row.city, row.district, row.town, row.village].filter(Boolean)
  form.cropType = row.cropType || ''
  form.location = row.location || ''
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.region = []
  form.cropType = ''
  form.location = ''
  formRef.value?.clearValidate()
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  const payload = {
    name: form.name,
    cropType: form.cropType || null,
    location: form.location || null,
    province: form.region[0] || null,
    city: form.region[1] || null,
    district: form.region[2] || null,
    town: form.region[3] || null,
    village: form.region[4] || null,
    // 棚主视角（管理员代看）：代建给当前查看的棚主
    ownerId: viewStore.active ? viewStore.ownerId : undefined
  }
  try {
    if (editingId.value) {
      await updateGreenhouse(editingId.value, payload)
      ElMessage.success('大棚已更新')
    } else {
      await createGreenhouse(payload)
      ElMessage.success('大棚创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除大棚「${row.name}」吗？其下所有设备（固件将解绑）、预警规则、场景、授权与历史数据将被一并清理，不可恢复。`,
      '删除大棚',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteGreenhouse(row.id)
    ElMessage.success('大棚已删除')
    await loadData()
  } catch { /* cancelled or error */ }
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(loadData)
</script>

<style scoped>
.gh-page {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
}

.page-desc {
  background: rgba(64, 158, 255, 0.12);
  border: 1px solid rgba(64, 158, 255, 0.3);
  border-radius: 6px;
  color: #9cc3f0;
  font-size: 13px;
  line-height: 1.7;
  padding: 10px 14px;
  margin-bottom: 16px;
}

.page-desc em {
  color: #F56C6C;
  font-style: normal;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.total-tip {
  font-size: 13px;
  color: #94a3b8;
}

.gh-name {
  font-size: 14px;
  color: #e0e6ed;
  font-weight: 600;
}

.text-ellipsis {
  display: block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
</style>
