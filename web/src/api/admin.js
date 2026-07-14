import request from '@/utils/request'

/**
 * 管理员 API
 * 后端路径: /api/v1/admin
 * 仅 ADMIN 角色可访问
 */

/** 获取用户列表（支持按角色筛选） */
export function getUsers(params = {}) {
  return request.get('/admin/users', { params })
}

/** 获取用户详情 */
export function getUser(userId) {
  return request.get(`/admin/users/${userId}`)
}

/** 更新用户（角色/状态/基本信息） */
export function updateUser(userId, data) {
  return request.put(`/admin/users/${userId}`, data)
}

/** 删除用户 */
export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

/** 获取角色列表及统计 */
export function getRoleStats() {
  return request.get('/admin/roles')
}
