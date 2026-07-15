<template>
  <div class="owner-page">
    <div class="page-header">
      <h3>棚主管理</h3>
      <p class="page-desc">查看所有棚主账号及其名下的大棚和员工信息</p>
    </div>

    <el-table v-loading="loading" :data="owners" stripe border>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column label="用户名" min-width="120">
        <template #default="{ row }">
          <span class="owner-name">{{ row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column label="真实姓名" min-width="100">
        <template #default="{ row }">
          {{ row.realName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          {{ row.phone || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="大棚数" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="success" size="small" effect="dark">{{ row.greenhouseCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="员工数" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="warning" size="small" effect="dark">{{ row.employeeCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small" effect="dark">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showGreenhouses(row)">
            查看大棚
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 大棚详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${selectedOwner?.username} 名下的大棚`"
      width="680px"
    >
      <el-table v-loading="ghLoading" :data="greenhouses" stripe border size="small">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="大棚名称" min-width="140">
          <template #default="{ row }">
            <span class="gh-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="位置" min-width="120">
          <template #default="{ row }">{{ row.location || '-' }}</template>
        </el-table-column>
        <el-table-column label="作物" width="100">
          <template #default="{ row }">{{ row.cropType || '-' }}</template>
        </el-table-column>
        <el-table-column label="地区" min-width="150">
          <template #default="{ row }">
            {{ formatRegion(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'" size="small">
              {{ row.status ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!ghLoading && greenhouses.length === 0" description="该棚主暂无大棚" />

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOwners, getOwnerGreenhouses } from '@/api/owner'

// ===== 数据 =====
const loading = ref(false)
const owners = ref([])

// ===== 大棚详情弹窗 =====
const dialogVisible = ref(false)
const ghLoading = ref(false)
const selectedOwner = ref(null)
const greenhouses = ref([])

// ===== 数据加载 =====
async function loadOwners() {
  loading.value = true
  try {
    const res = await getOwners()
    owners.value = res.data || []
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function showGreenhouses(owner) {
  selectedOwner.value = owner
  dialogVisible.value = true
  ghLoading.value = true
  try {
    const res = await getOwnerGreenhouses(owner.id)
    greenhouses.value = res.data || []
  } catch { /* handled */ }
  finally { ghLoading.value = false }
}

// ===== 工具 =====
function formatRegion(row) {
  const parts = [row.province, row.city, row.district].filter(Boolean)
  return parts.length > 0 ? parts.join(' / ') : '-'
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  loadOwners()
})
</script>

<style scoped>
.owner-page {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
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

.owner-name {
  font-weight: 500;
}

.gh-name {
  font-weight: 500;
  color: #409EFF;
}
</style>
