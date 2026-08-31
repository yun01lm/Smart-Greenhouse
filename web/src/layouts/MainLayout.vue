<template>
  <div class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="layout-header">
      <div class="header-left">
        <h2>
          <el-icon class="brand-icon" :size="22"><Grape /></el-icon>
          智慧大棚AIoT系统
        </h2>
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
          v-if="(!authStore.isAdmin() && authStore.role() !== 'EXPERT') || viewStore.active"
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
        <el-dropdown trigger="click" @command="onUserCommand">
          <span class="user-info user-dropdown">
            {{ authStore.user?.realName || authStore.user?.username }}
            <el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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

    <!-- 修改密码对话框 -->
    <el-dialog append-to-body v-model="pwdVisible" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" label-position="right">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" maxlength="100" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，且包含字母和数字" maxlength="100" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { changeMyPassword } from '@/api/auth'
import { useViewModeStore } from '@/stores/viewMode'
import { getGreenhouses } from '@/api/greenhouse'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, DataAnalysis, Cpu, UserFilled, Document, WarningFilled,
  Download, Microphone, Avatar, HomeFilled, ArrowDown, Lock, Grape, Box, OfficeBuilding
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
    { path: '/firmware', title: '固件管理', icon: Box },
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
    { path: '/greenhouses', title: '大棚管理', icon: OfficeBuilding },
    { path: '/employees', title: '员工管理', icon: UserFilled },
    { path: '/authorizations', title: '授权审批', icon: Lock },
    { path: '/alerts', title: '预警配置', icon: WarningFilled },
    { path: '/export', title: '数据导出', icon: Download },
    { path: '/knowledge', title: '知识库', icon: Document },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ],
  TECHNICIAN: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/devices', title: '设备管理', icon: Cpu },
    { path: '/alerts', title: '预警配置', icon: WarningFilled },
    { path: '/export', title: '数据导出', icon: Download },
    { path: '/knowledge', title: '知识库', icon: Document },
    { path: '/qa', title: 'AI 问答', icon: ChatDotRound }
  ],
  EXPERT: [
    { path: '/dashboard', title: '数据总览', icon: DataAnalysis },
    { path: '/expert/chat', title: '咨询会话', icon: ChatDotRound },
    { path: '/knowledge', title: '知识库', icon: Document },
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

// ===== 修改密码 =====
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码至少8位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && !/[a-zA-Z]/.test(value)) return callback(new Error('密码必须包含字母'))
        if (value && !/[0-9]/.test(value)) return callback(new Error('密码必须包含数字'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

/** 顶栏用户下拉命令 */
function onUserCommand(command) {
  if (command === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    pwdFormRef.value?.clearValidate()
    pwdVisible.value = true
  } else if (command === 'logout') {
    authStore.logout()
  }
}

async function submitPwd() {
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }

  pwdSubmitting.value = true
  try {
    await changeMyPassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    pwdSubmitting.value = false
  }
}

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
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  color: #67C23A;
  filter: drop-shadow(0 0 6px rgba(103, 194, 58, 0.45));
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  margin-right: 16px;
  color: #ffffffa6;
}

.user-dropdown {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  outline: none;
}

.user-dropdown:hover {
  color: #fff;
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
  background: transparent;
  padding: 16px;
  overflow-y: auto;
}
</style>
