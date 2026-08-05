<template>
  <div class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="layout-header">
      <div class="header-left">
        <h2>🌱 智慧大棚AIoT系统</h2>
      </div>
      <div class="header-right">
        <!-- 棚主视角提示（R10：管理员进入棚主视角后显示，可一键切回） -->
        <div v-if="viewStore.active" class="view-banner">
          <el-tag type="warning" effect="dark" size="small">棚主视角</el-tag>
          <span class="view-text">正在以「{{ viewStore.ownerName }}」身份查看</span>
          <el-button type="primary" size="small" @click="backToAdmin">返回管理员</el-button>
        </div>
        <!-- 大棚选择器（棚主视角下显示该棚主的大棚） -->
        <el-select
          v-if="!authStore.isAdmin() || viewStore.active"
          v-model="selectGreenhouseId"
          placeholder="选择大棚"
          style="width: 200px; margin-right: 16px"
        >
          <el-option
            v-for="gh in ghOptions"
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
        <router-view :greenhouse-id="displayGreenhouseId" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useViewModeStore } from '@/stores/viewMode'
import { getGreenhouses } from '@/api/greenhouse'
import {
  ChatDotRound, DataAnalysis, Cpu, UserFilled, Document, WarningFilled,
  Download, Microphone, Avatar, HomeFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const viewStore = useViewModeStore()

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

// 棚主视角：菜单切换为棚主菜单；否则按当前角色
const menus = computed(() => {
  if (viewStore.active) return MENU_CONFIG.OWNER
  return MENU_CONFIG[authStore.role()] || MENU_CONFIG.OWNER
})

// 大棚选项：棚主视角显示该棚主的大棚，否则显示当前用户大棚
const ghOptions = computed(() => (viewStore.active ? viewStore.greenhouses : greenhouses.value))

// 生效中的大棚 ID：棚主视角取自 viewStore，否则取自本组件
const displayGreenhouseId = computed(() => (viewStore.active ? viewStore.greenhouseId : currentGreenhouseId.value))

const selectGreenhouseId = computed({
  get: () => displayGreenhouseId.value,
  set: (id) => {
    if (viewStore.active) viewStore.setGreenhouse(id)
    else currentGreenhouseId.value = id
  }
})

/** 返回管理员视角（R10） */
function backToAdmin() {
  viewStore.exitOwnerView()
  router.push('/dashboard')
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

.view-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 16px;
}

.view-text {
  color: #ffd04b;
  font-size: 13px;
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
