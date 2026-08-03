import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})


// 请求拦截器：自动附加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)


// Token 刷新状态（防止并发刷新）
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const res = await axios.post('/api/v1/auth/refresh', { refreshToken })
    const data = res.data
    if (data.token) {
      localStorage.setItem('token', data.token)
      if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken)
      return data.token
    }
  } catch (e) {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    router.push('/login')
  }
  return null
}

// 响应拦截器：统一错误处理 + token自动刷新
request.interceptors.response.use(
  response => {
    // 二进制流（文件下载）直接返回，不做业务码判断
    if (response.config?.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200 && res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async error => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(resolve => {
          addRefreshSubscriber(token => {
            originalRequest.headers.Authorization = 'Bearer ' + token
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const newToken = await refreshToken()
      isRefreshing = false

      if (newToken) {
        onRefreshed(newToken)
        originalRequest.headers.Authorization = 'Bearer ' + newToken
        return request(originalRequest)
      }
    }

    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败 (' + status + ')')
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
