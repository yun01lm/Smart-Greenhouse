<template>
  <div class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="layout-header">
      <div class="header-left">
        <h2>🌱 智慧大棚AIoT系统</h2>
      </div>
      <div class="header-right">
        <!-- 大棚选择器 -->
        <el-select
          v-model="currentGreenhouseId"
          placeholder="选择大棚"
          style="width: 200px; margin-right: 16px"
          @change="onGreenhouseChange"
        >
          <el-option
            v-for="gh in greenhouses"
            :key="gh.id"
            :label="gh.name"
            :value="gh.id"
          />
        </el-select>
        <span class="user-info">{{ authStore.user?.realName || authStore.user?.username }}</span>
        <el-button type="danger" text @click="authStore.logout">退出</el-button>
      </div>
    </el-header>

    <!-- 侧边栏 + 内容 -->
    <el-container class="layout-body">
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#001529"
          text-color="#ffffffa6"
          active-text-color="#fff"
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据总览</span>
          </el-menu-item>
          <el-menu-item index="/devices">
            <el-icon><Cpu /></el-icon>
            <span>设备管理</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Document /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/alerts">
            <el-icon><WarningFilled /></el-icon>
            <span>预警配置</span>
          </el-menu-item>
          <el-menu-item index="/export">
            <el-icon><Download /></el-icon>
            <span>数据导出</span>
          </el-menu-item>
          <el-menu-item index="/monitor">
            <el-icon><Monitor /></el-icon>
            <span>系统监控</span>
          </el-menu-item>
          <el-menu-item index="/corpus">
            <el-icon><Microphone /></el-icon>
            <span>语料管理</span>
          </el-menu-item>
          <el-menu-item index="/expert" disabled>
            <el-icon><Avatar /></el-icon>
            <span>专家工作台</span>
          </el-menu-item>
          <el-menu-item index="/owner" disabled>
            <el-icon><HomeFilled /></el-icon>
            <span>棚主管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="layout-main">
        <router-view :greenhouse-id="currentGreenhouseId" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getGreenhouses } from '@/api/greenhouse'

const route = useRoute()
const authStore = useAuthStore()

const greenhouses = ref([])
const currentGreenhouseId = ref(1)

const activeMenu = ref(route.path)

function onGreenhouseChange(id) {
  currentGreenhouseId.value = id
}

onMounted(async () => {
  try {
    const res = await getGreenhouses()
    greenhouses.value = res.data || []
    if (greenhouses.value.length > 0) {
      currentGreenhouseId.value = greenhouses.value[0].id
    }
  } catch (e) {
    // 使用默认大棚 ID
  }
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #001529;
  color: #fff;
  padding: 0 24px;
  height: 56px;
}

.header-left h2 {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  margin-right: 16px;
  color: #ffffffa6;
}

.layout-body {
  flex: 1;
  overflow: hidden;
}

.layout-aside {
  background: #001529;
  overflow-y: auto;
}

.layout-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>
