<template>
  <div class="expert-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #409EFF">{{ stats.expertTotal }}</div>
          <div class="stat-label">专家总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #67C23A">{{ stats.onlineCount }}</div>
          <div class="stat-label">在线专家</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #E6A23C">{{ stats.authTotal }}</div>
          <div class="stat-label">授权记录</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #909399">{{ stats.convTotal }}</div>
          <div class="stat-label">咨询会话</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: 专家列表 -->
      <el-tab-pane label="专家列表" name="experts">
        <div class="tab-content">
          <el-table v-loading="expertLoading" :data="experts" stripe border>
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="专家姓名" min-width="120">
              <template #default="{ row }">
                <span class="expert-name">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column label="手机号" width="130">
              <template #default="{ row }">
                {{ row.phone || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="在线状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.isOnline === 1"
                  size="small"
                  active-text="在线"
                  inactive-text="离线"
                  @change="(val) => toggleOnline(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="最大并发" width="90" align="center">
              <template #default="{ row }">
                {{ row.maxConcurrent }}
              </template>
            </el-table-column>
            <el-table-column label="咨询数" width="90" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.consultCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近活跃" width="170" align="center">
              <template #default="{ row }">
                {{ formatTime(row.lastActiveAt) }}
              </template>
            </el-table-column>
            <el-table-column label="账号状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status ? 'success' : 'danger'" size="small" effect="dark">
                  {{ row.status ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab 2: 授权管理 -->
      <el-tab-pane label="授权管理" name="authorizations">
        <div class="tab-content">
          <!-- 筛选 -->
          <div class="toolbar">
            <el-select
              v-model="authFilter.status"
              placeholder="授权状态筛选"
              clearable
              style="width: 160px"
              @change="loadAuthorizations"
            >
              <el-option label="待处理" value="PENDING" />
              <el-option label="已同意" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已撤销" value="REVOKED" />
              <el-option label="已过期" value="EXPIRED" />
            </el-select>
          </div>

          <el-table v-loading="authLoading" :data="authorizations" stripe border>
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="专家" min-width="100">
              <template #default="{ row }">{{ row.expertName }}</template>
            </el-table-column>
            <el-table-column label="用户" min-width="100">
              <template #default="{ row }">{{ row.userName }}</template>
            </el-table-column>
            <el-table-column label="大棚" min-width="120">
              <template #default="{ row }">{{ row.greenhouseName }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="authStatusTag(row.status)" size="small">
                  {{ authStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="理由" min-width="150">
              <template #default="{ row }">
                <span class="text-ellipsis" :title="row.reason">{{ row.reason || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="剩余天数" width="90" align="center">
              <template #default="{ row }">
                <span v-if="row.status === 'APPROVED'" :class="{ 'text-danger': row.remainingDays <= 1 }">
                  {{ row.remainingDays }}天
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="请求时间" width="170" align="center">
              <template #default="{ row }">{{ formatTime(row.requestedAt) }}</template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="authPage"
              v-model:page-size="authSize"
              :page-sizes="[10, 20, 50]"
              :total="authTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="loadAuthorizations"
              @current-change="loadAuthorizations"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getExperts, toggleExpertOnline, getAuthorizations, getExpertStats } from '@/api/expert'

// ===== 统计 =====
const stats = reactive({ expertTotal: 0, onlineCount: 0, authTotal: 0, convTotal: 0 })

// ===== 专家列表 =====
const expertLoading = ref(false)
const experts = ref([])

// ===== 授权管理 =====
const activeTab = ref('experts')
const authLoading = ref(false)
const authorizations = ref([])
const authPage = ref(1)
const authSize = ref(20)
const authTotal = ref(0)
const authFilter = reactive({ status: '' })

// ===== 数据加载 =====
async function loadStats() {
  try {
    const res = await getExpertStats()
    if (res.data) Object.assign(stats, res.data)
  } catch { /* handled */ }
}

async function loadExperts() {
  expertLoading.value = true
  try {
    const res = await getExperts()
    experts.value = res.data || []
  } catch { /* handled */ }
  finally { expertLoading.value = false }
}

async function loadAuthorizations() {
  authLoading.value = true
  try {
    const params = { page: authPage.value - 1, size: authSize.value }
    if (authFilter.status) params.status = authFilter.status
    const res = await getAuthorizations(params)
    authorizations.value = res.data?.list || []
    authTotal.value = res.data?.total || 0
  } catch { /* handled */ }
  finally { authLoading.value = false }
}

// ===== 操作 =====
async function toggleOnline(row, online) {
  try {
    await toggleExpertOnline(row.id, online)
    row.isOnline = online ? 1 : 0
    ElMessage.success(online ? '已设为在线' : '已设为离线')
    loadStats()
  } catch { /* handled */ }
}

// ===== 工具 =====
function authStatusLabel(s) {
  const m = { PENDING: '待处理', APPROVED: '已同意', REJECTED: '已拒绝', REVOKED: '已撤销', EXPIRED: '已过期' }
  return m[s] || s
}

function authStatusTag(s) {
  const m = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info', REVOKED: 'danger', EXPIRED: 'info' }
  return m[s] || 'info'
}

function formatTime(d) {
  if (!d) return '-'
  const dt = new Date(d)
  const pad = n => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

onMounted(() => {
  loadStats()
  loadExperts()
  loadAuthorizations()
})
</script>

<style scoped>
.expert-page {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-card :deep(.el-card__body) {
  padding: 20px 16px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.tab-content {
  padding: 16px;
  min-height: 300px;
}

.toolbar {
  margin-bottom: 16px;
}

.expert-name {
  font-weight: 500;
}

.text-ellipsis {
  display: block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.text-danger {
  color: #F56C6C;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
