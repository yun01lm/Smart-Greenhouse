<template>
  <div class="role-overview">
    <div v-loading="loading" class="role-cards">
      <div
        v-for="item in roleStats"
        :key="item.role"
        class="role-card"
        :class="'role-' + item.role.toLowerCase()"
      >
        <div class="role-icon">
          <el-icon :size="28">
            <UserFilled v-if="item.role === 'ADMIN'" />
            <HomeFilled v-else-if="item.role === 'OWNER'" />
            <Avatar v-else-if="item.role === 'WORKER'" />
            <Star v-else />
          </el-icon>
        </div>
        <div class="role-info">
          <span class="role-label">{{ item.roleLabel }}</span>
          <span class="role-code">{{ item.role }}</span>
        </div>
        <div class="role-count">{{ item.count }}</div>
      </div>
    </div>

    <!-- 权限说明 -->
    <el-divider />
    <div class="permission-desc">
      <h4>角色权限说明</h4>
      <el-table :data="rolePermissionTable" border size="small" style="width: 100%">
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="desc" label="说明" min-width="200" />
        <el-table-column prop="scope" label="访问范围" min-width="200" />
        <el-table-column label="端" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.platform === 'Web'" type="primary" size="small">Web 端</el-tag>
            <el-tag v-else-if="row.platform === 'APP'" type="success" size="small">APP 端</el-tag>
            <el-tag v-else type="warning" size="small">Web + APP</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserFilled, HomeFilled, Avatar, Star } from '@element-plus/icons-vue'
import { getRoleStats } from '@/api/admin'

const loading = ref(false)
const roleStats = ref([])

const rolePermissionTable = [
  { role: '管理员', desc: '管理平台全部数据，用户管理，系统配置', scope: '全部大棚、全部功能', platform: 'Web' },
  { role: '棚主', desc: '管理自己的大棚、设备、员工，查看数据和控制', scope: '自己的大棚', platform: 'Web + APP' },
  { role: '员工', desc: '由棚主分配权限，按授权范围访问功能', scope: '棚主授权的功能和大棚', platform: 'APP' },
  { role: '专家', desc: '接收诊断求助，7天授权查看环境数据', scope: '被授权的用户数据', platform: 'Web' }
]

async function loadRoleStats() {
  loading.value = true
  try {
    const res = await getRoleStats()
    roleStats.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRoleStats()
})
</script>

<style scoped>
.role-overview {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(10px);
}

.role-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.role-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  border-radius: 12px;
  transition: transform 0.2s;
  color: #fff;
}

.role-card:hover {
  transform: translateY(-2px);
}

.role-admin {
  background: linear-gradient(135deg, #f56c6c, #e64242);
}

.role-owner {
  background: linear-gradient(135deg, #e6a23c, #d48806);
}

.role-worker {
  background: linear-gradient(135deg, #67c23a, #3e8e41);
}

.role-expert {
  background: linear-gradient(135deg, #909399, #606266);
}

.role-icon {
  margin-bottom: 8px;
}

.role-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 12px;
}

.role-label {
  font-size: 16px;
  font-weight: 600;
}

.role-code {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 2px;
}

.role-count {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.permission-desc h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .role-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
