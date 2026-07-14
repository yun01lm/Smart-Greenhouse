<template>
  <div class="user-page">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="用户列表" name="list">
        <UserList />
      </el-tab-pane>
      <el-tab-pane label="角色概览" name="role">
        <RoleOverview />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import UserList from './UserList.vue'
import RoleOverview from './RoleOverview.vue'

const activeTab = ref('list')
</script>

<style scoped>
.user-page {
  min-height: calc(100vh - 200px);
}

:deep(.el-tabs--border-card) {
  border-radius: 8px;
  overflow: hidden;
}
</style>
