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
          <div class="stat-value" style="color: #94a3b8">{{ stats.convTotal }}</div>
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

      <!-- Tab 3: 咨询记录（R9） -->
      <el-tab-pane label="咨询记录" name="conversations">
        <div class="tab-content">
          <!-- 筛选工具栏 -->
          <div class="toolbar">
            <el-select v-model="convFilter.expertId" placeholder="按专家筛选" clearable filterable style="width: 170px">
              <el-option v-for="e in experts" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input
              v-model="convFilter.userKeyword"
              placeholder="按用户账号搜索"
              clearable
              style="width: 180px; margin-left: 12px"
              @keyup.enter="searchConversations"
            />
            <el-date-picker
              v-model="convFilter.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="x"
              style="width: 280px; margin-left: 12px"
            />
            <el-button type="primary" style="margin-left: 12px" @click="searchConversations">查询</el-button>
            <el-button type="success" :loading="convExporting" @click="doExportConversations">
              <el-icon style="margin-right: 4px"><Download /></el-icon>导出 Excel
            </el-button>
          </div>

          <el-table v-loading="convLoading" :data="conversations" stripe border>
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="专家" min-width="100">
              <template #default="{ row }">{{ row.expertName }}</template>
            </el-table-column>
            <el-table-column label="用户" min-width="100">
              <template #default="{ row }">{{ row.userName }}</template>
            </el-table-column>
            <el-table-column label="大棚" min-width="120">
              <template #default="{ row }">{{ row.greenhouseName || '-' }}</template>
            </el-table-column>
            <el-table-column label="咨询主题" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.subject }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="convStatusTag(row.status)" size="small">{{ convStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="messageCount" label="消息数" width="80" align="center" />
            <el-table-column label="创建时间" width="170" align="center">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openConversationDetail(row)">查看明细</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="convPage"
              v-model:page-size="convSize"
              :page-sizes="[10, 20, 50]"
              :total="convTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="loadConversations"
              @current-change="loadConversations"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 对话明细抽屉（R9） -->
    <el-drawer v-model="detailVisible" title="对话记录明细" size="560px">
      <div v-if="detailMessages.length === 0" class="drawer-empty">暂无消息记录</div>
      <div v-else class="msg-list">
        <div v-for="m in detailMessages" :key="m.id" class="msg-item" :class="m.senderType === 'EXPERT' ? 'from-expert' : 'from-user'">
          <div class="msg-meta">
            <span class="msg-sender">{{ m.senderName }}</span>
            <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
          </div>
          <div class="msg-bubble">{{ m.content || (m.filePath ? '[' + m.messageType + ' 附件]' : '[' + m.messageType + ']') }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import {
  getExperts, toggleExpertOnline, getAuthorizations, getExpertStats,
  getConversations, getConversationMessages, exportConversations
} from '@/api/expert'

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

// ===== 咨询记录（R9） =====
const convLoading = ref(false)
const conversations = ref([])
const convPage = ref(1)
const convSize = ref(20)
const convTotal = ref(0)
const convExporting = ref(false)
const convFilter = reactive({ expertId: null, userKeyword: '', dateRange: null })

// 对话明细
const detailVisible = ref(false)
const detailMessages = ref([])

async function loadConversations() {
  convLoading.value = true
  try {
    const params = { page: convPage.value - 1, size: convSize.value }
    if (convFilter.expertId) params.expertId = convFilter.expertId
    if (convFilter.userKeyword) params.userKeyword = convFilter.userKeyword.trim()
    if (convFilter.dateRange && convFilter.dateRange.length === 2) {
      params.startTime = convFilter.dateRange[0]
      params.endTime = convFilter.dateRange[1]
    }
    const res = await getConversations(params)
    conversations.value = res.data?.list || []
    convTotal.value = res.data?.total || 0
  } catch { /* handled by interceptor */ }
  finally { convLoading.value = false }
}

function searchConversations() {
  convPage.value = 1
  loadConversations()
}

async function openConversationDetail(row) {
  try {
    const res = await getConversationMessages(row.id)
    detailMessages.value = res.data || []
    detailVisible.value = true
  } catch { /* handled by interceptor */ }
}

async function doExportConversations() {
  const params = {}
  if (convFilter.expertId) params.expertId = convFilter.expertId
  if (convFilter.userKeyword) params.userKeyword = convFilter.userKeyword.trim()
  if (convFilter.dateRange && convFilter.dateRange.length === 2) {
    params.startTime = convFilter.dateRange[0]
    params.endTime = convFilter.dateRange[1]
  }
  convExporting.value = true
  try {
    const blob = await exportConversations(params)
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    downloadBlob(blob, `咨询记录_${today}.xlsx`)
    ElMessage.success('咨询记录导出成功')
  } catch { ElMessage.error('导出失败，请稍后重试') }
  finally { convExporting.value = false }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function convStatusLabel(s) {
  const m = { WAITING: '等待中', ACTIVE: '进行中', CLOSED: '已关闭' }
  return m[s] || s
}

function convStatusTag(s) {
  const m = { WAITING: 'warning', ACTIVE: 'success', CLOSED: 'info' }
  return m[s] || 'info'
}

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
  loadConversations()
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
  color: #94a3b8;
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

/* ===== R9 对话明细 ===== */
.drawer-empty {
  text-align: center;
  color: #94a3b8;
  padding: 40px 0;
}
.msg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.msg-item {
  display: flex;
  flex-direction: column;
}
.msg-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}
.msg-sender {
  font-weight: 600;
  color: #a0aec0;
}
.msg-bubble {
  background: rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #e0e6ed;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-item.from-expert .msg-bubble {
  background: #ecf5ff;
}
</style>
