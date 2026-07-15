import { createRouter, createWebHashHistory } from 'vue-router'

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
        meta: { title: '数据总览' }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/devices/DevicePage.vue'),
        meta: { title: '设备管理' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/UserPage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgePage.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alerts/AlertRulePage.vue'),
        meta: { title: '预警配置' }
      },
      {
        path: 'export',
        name: 'Export',
        component: () => import('@/views/export/ReportPage.vue'),
        meta: { title: '数据导出' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/MonitorPage.vue'),
        meta: { title: '系统监控' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
