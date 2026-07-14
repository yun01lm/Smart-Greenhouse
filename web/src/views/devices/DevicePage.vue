<template>
  <div class="device-page">
    <el-tabs v-model="activeTab" type="border-card">
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
import { ref } from 'vue'
import DeviceList from './DeviceList.vue'
import DeviceGroup from './DeviceGroup.vue'

defineProps({
  greenhouseId: { type: [Number, String], required: true }
})

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
