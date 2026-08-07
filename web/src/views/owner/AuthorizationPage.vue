<template>
  <div class="auth-page">
    <el-card shadow="never">
      <el-tabs v-model="tab">
        <!-- Tab 1：待处理申请 -->
        <el-tab-pane label="待处理申请" name="pending">
          <el-table :data="pendingList" v-loading="loading" size="default">
            <el-table-column prop="expertName" label="申请专家" min-width="120" />
            <el-table-column prop="greenhouseName" label="大棚" min-width="140" />
            <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.requestedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" :loading="actingId === row.id" @click="approve(row)">同意</el-button>
                <el-button size="small" type="danger" :loading="actingId === row.id" @click="reject(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && pendingList.length === 0" description="暂无待处理申请" />
        </el-tab-pane>

        <!-- Tab 2：已授权大棚 -->
        <el-tab-pane label="已授权大棚" name="active">
          <el-table :data="activeList" v-loading="loadingActive" size="default">
            <el-table-column prop="expertName" label="专家" min-width="120" />
            <el-table-column prop="greenhouseName" label="大棚" min-width="140" />
            <el-table-column label="授权时间" width="160">
              <template #default="{ row }">{{ formatTime(row.approvedAt) }}</template>
            </el-table-column>
            <el-table-column label="剩余天数" width="100">
              <template #default="{ row }">{{ row.remainingDays }} 天</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="warning" :loading="actingId === row.id" @click="revoke(row)">撤销</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingActive && activeList.length === 0" description="暂无已授权大棚" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPendingAuthorizations,
  approveAuthorization,
  rejectAuthorization,
  getActiveAuthorizations,
  revokeAuthorization
} from '@/api/expert'

const tab = ref('pending')
const pendingList = ref([])
const activeList = ref([])
const loading = ref(false)
const loadingActive = ref(false)
const actingId = ref(null)

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingAuthorizations()
    pendingList.value = res.data || []
  } catch (e) { /* 拦截器统一处理 */ } finally { loading.value = false }
}

async function loadActive() {
  loadingActive.value = true
  try {
    const res = await getActiveAuthorizations()
    activeList.value = res.data || []
  } catch (e) { /* 拦截器统一处理 */ } finally { loadingActive.value = false }
}

async function approve(row) {
  try {
    await ElMessageBox.confirm(`同意专家「${row.expertName}」查看「${row.greenhouseName}」？`, '授权确认', { type: 'warning' })
  } catch (e) { return }
  actingId.value = row.id
  try {
    await approveAuthorization(row.id)
    ElMessage.success('已同意授权（有效期 7 天）')
    loadPending()
    loadActive()
  } catch (e) { /* 拦截器统一处理 */ } finally { actingId.value = null }
}

async function reject(row) {
  try {
    await ElMessageBox.confirm(`确定拒绝专家「${row.expertName}」的申请？`, '拒绝确认', { type: 'warning' })
  } catch (e) { return }
  actingId.value = row.id
  try {
    await rejectAuthorization(row.id)
    ElMessage.success('已拒绝该申请')
    loadPending()
  } catch (e) { /* 拦截器统一处理 */ } finally { actingId.value = null }
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(`确定撤销专家「${row.expertName}」对「${row.greenhouseName}」的授权？`, '撤销确认', { type: 'warning' })
  } catch (e) { return }
  actingId.value = row.id
  try {
    await revokeAuthorization(row.id)
    ElMessage.success('已撤销授权')
    loadActive()
  } catch (e) { /* 拦截器统一处理 */ } finally { actingId.value = null }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  loadPending()
  loadActive()
})
</script>

<style scoped>
.auth-page {
  padding: 8px;
}
</style>