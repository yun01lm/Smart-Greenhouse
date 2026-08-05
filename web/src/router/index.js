import { createRouter, createWebHashHistory } from 'vue-router'
import { useViewModeStore } from '@/stores/viewMode'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardPage.vue'),
        meta: { title: '数据总览', roles: ['ADMIN', 'OWNER', 'WORKER', 'EXPERT'] }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/devices/DevicePage.vue'),
        meta: { title: '设备管理', roles: ['ADMIN', 'OWNER', 'WORKER'] }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/UserPage.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgePage.vue'),
        meta: { title: '知识库管理', roles: ['ADMIN'] }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alerts/AlertRulePage.vue'),
        meta: { title: '预警配置', roles: ['OWNER', 'WORKER'] }
      },
      {
        path: 'export',
        name: 'Export',
        component: () => import('@/views/export/ReportPage.vue'),
        meta: { title: '数据导出', roles: ['OWNER', 'WORKER'] }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/MonitorPage.vue'),
        meta: { title: '系统监控', roles: ['ADMIN'] }
      },
      {
        path: 'corpus',
        name: 'Corpus',
        component: () => import('@/views/corpus/CorpusPage.vue'),
        meta: { title: '语料管理', roles: ['ADMIN'] }
      },
      {
        path: 'expert',
        name: 'Expert',
        component: () => import('@/views/expert/ExpertPage.vue'),
        meta: { title: '专家工作台', roles: ['ADMIN'] }
      },
      {
        path: 'owner',
        name: 'Owner',
        component: () => import('@/views/owner/OwnerPage.vue'),
        meta: { title: '棚主管理', roles: ['ADMIN'] }
      },
      {
        path: 'qa',
        name: 'Qa',
        component: () => import('@/views/qa/QaPage.vue'),
        meta: { title: 'AI 问答', roles: ['ADMIN', 'OWNER', 'WORKER', 'EXPERT'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：未登录跳转登录页；角色不符跳回数据总览
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  // 角色权限校验（R10：棚主视角下有效角色为 OWNER，放行棚主页面）
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const viewStore = useViewModeStore()
  const role = viewStore.active ? 'OWNER' : (user?.role || '')
  const allowed = to.meta?.roles
  if (allowed && allowed.length > 0 && !allowed.includes(role)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
