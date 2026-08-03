<template>
  <div class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="layout-header">
      <div class="header-left">
        <h2>🌱 智慧大棚AIoT系统</h2>
      </div>
      <div class="header-right">
        <!-- 大棚选择器（管理员不显示，管理员按地区查看） -->
        <el-select
          v-if="!authStore.isAdmin()"
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
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getGreenhouses } from '@/api/greenhouse'
import {
  ChatDotRound, DataAnalysis, Cpu, UserFilled, Document, WarningFilled,
  Download, Microphone, Avatar, HomeFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()

const greenhouses = ref([])
const currentGreenhouseId = ref(1)

const activeMenu = ref(route.path)

// ===== 角色化菜单配置：各角色看到的页面不同 =====
const MENU_CONFIG = {
  ADMIN: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/devices', title: '设备管理', icon: Cpu },
    { path: '/users', title: '用户管理', icon: UserFilled },
    { path: '/knowledge', title: '知识库', icon: Document },
    { path: '/corpus', title: '语料管理', icon: Microphone },
    { path: '/expert', title: '专家工作台', icon: Avatar },
    { path: '/owner', title: '棚主管理', icon: HomeFilled },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ],
  OWNER: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/devices', title: '设备管理', icon: Cpu },
    { path: '/alerts', title: '预警配置', icon: WarningFilled },
    { path: '/export', title: '数据导出', icon: Download },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ],
  WORKER: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/devices', title: '设备管理', icon: Cpu },
    { path: '/alerts', title: '预警配置', icon: WarningFilled },
    { path: '/export', title: '数据导出', icon: Download },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ],
  EXPERT: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ]
}

const menus = computed(() => MENU_CONFIG[authStore.role()] || MENU_CONFIG.OWNER)

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
