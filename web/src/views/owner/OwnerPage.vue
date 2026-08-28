<template>
  <div class="owner-page">
    <div class="page-header">
      <h3>棚主管理</h3>
      <p class="page-desc">查看所有棚主账号及其名下的大棚和员工信息；可搜索、按地区筛选，或进入棚主视角查看其系统</p>
    </div>

    <!-- 搜索 + 地区筛选（R10） -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名 / 姓名 / 手机号"
        clearable
        style="width: 240px"
        @keyup.enter="loadOwners(1)"
        @clear="loadOwners(1)"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <RegionCascader v-model="regionPath" width="300px" />
      <el-button type="primary" :loading="loading" @click="loadOwners(1)">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
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
      <el-table-column label="地区" min-width="160">
        <template #default="{ row }">
          {{ row.regionText || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="大棚数" width="90" align="center">
        <template #default="{ row }">
          <el-tag type="success" size="small" effect="dark">{{ row.greenhouseCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="员工数" width="90" align="center">
        <template #default="{ row }">
          <el-tag type="warning" size="small" effect="dark">{{ row.employeeCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'" size="small" effect="dark">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="enterOwnerView(row)">
            进入管理
          </el-button>
          <el-button type="info" link size="small" @click="showGreenhouses(row)">
            查看大棚
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页（R10） -->
    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadOwners()"
        @size-change="loadOwners(1)"
      />
    </div>

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
import { useRouter } from 'vue-router'
import { getOwners, getOwnerGreenhouses } from '@/api/owner'
import { useViewModeStore } from '@/stores/viewMode'
import RegionCascader from '@/components/RegionCascader.vue'

const router = useRouter()
const viewStore = useViewModeStore()

// ===== 数据 =====
const loading = ref(false)
const owners = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

// ===== 筛选条件（R10）=====
const keyword = ref('')
const regionPath = ref([])

// ===== 大棚详情弹窗 =====
const dialogVisible = ref(false)
const ghLoading = ref(false)
const selectedOwner = ref(null)
const greenhouses = ref([])

// ===== 数据加载 =====
async function loadOwners(targetPage) {
  if (targetPage) page.value = targetPage
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      page: page.value - 1,
      size: size.value
    }
    const rp = regionToParams(regionPath.value)
    Object.assign(params, rp)
    const res = await getOwners(params)
    owners.value = res.data?.list || []
    total.value = res.data?.total || 0
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

/** 进入棚主视角（R10，Q3 方案A：页面内切换，可一键切回） */
async function enterOwnerView(owner) {
  try {
    const res = await getOwnerGreenhouses(owner.id)
    const ghs = res.data || []
    if (ghs.length === 0) {
      ElMessage.warning('该棚主暂无大棚，无法进入管理')
      return
    }
    viewStore.enterOwnerView(owner, ghs)
    router.push('/dashboard')
  } catch { /* handled */ }
}

// ===== 工具 =====
function resetFilters() {
  keyword.value = ''
  regionPath.value = []
  loadOwners(1)
}

function regionToParams(path) {
  if (!path || path.length === 0) return {}
  return {
    province: path[0] || undefined,
    city: path[1] || undefined,
    district: path[2] || undefined,
    town: path[3] || undefined,
    village: path[4] || undefined
  }
}

function formatRegion(row) {
  const parts = [row.province, row.city, row.district, row.town, row.village].filter(Boolean)
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
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
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
  color: #94a3b8;
  margin: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.owner-name {
  font-weight: 500;
}

.gh-name {
  font-weight: 500;
  color: #409EFF;
}
</style>
