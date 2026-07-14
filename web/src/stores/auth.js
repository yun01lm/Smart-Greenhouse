import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  /** 登录 */
  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    token.value = res.data.token
    user.value = res.data
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  /** 退出登录 */
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }

  /** 是否已登录 */
  const isLoggedIn = () => !!token.value

  /** 当前用户角色 */
  const role = () => user.value?.role || ''

  /** 是否为管理员 */
  const isAdmin = () => role() === 'ADMIN'

  return { token, user, login, logout, isLoggedIn, role, isAdmin }
})
