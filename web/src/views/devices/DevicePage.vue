<template>
  <div class="device-page">
    <!-- 管理员：管理员设备管理视图（总体统计 + 按农户查找设备） -->
    <AdminDevicePage v-if="isAdmin" />
    <!-- 农户/技术员：按大棚的设备管理 -->
    <el-tabs v-else v-model="activeTab" type="border-card">
      <el-tab-pane label="设备列表" name="list">
        <DeviceList :greenhouse-id="greenhouseId" />
      </el-tab-pane>
      <el-tab-pane label="设备分组" name="group">
        <DeviceGroup :greenhouse-id="greenhouseId" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useViewModeStore } from '@/stores/viewMode'
import DeviceList from './DeviceList.vue'
import DeviceGroup from './DeviceGroup.vue'
import AdminDevicePage from './AdminDevicePage.vue'

defineProps({
  greenhouseId: { type: [Number, String], required: true }
})

const authStore = useAuthStore()
const viewStore = useViewModeStore()
// R10：管理员进入棚主视角后按棚主版设备管理展示
const isAdmin = computed(() => authStore.isAdmin() && !viewStore.active)
const activeTab = ref('list')
</script>

<style scoped>
.device-page {
  min-height: calc(100vh - 200px);
}

:deep(.el-tabs--border-card) {
  border-radius: 8px;
  overflow: hidden;
}
</style>