import request from '@/utils/request'

/**
 * 员工管理 API（棚主端，R23/R24）
 * 后端路径: /api/v1/owner/employees
 */

/** 员工列表（WORKER + TECHNICIAN，返回 role 字段） */
export function getEmployees() {
  return request.get('/owner/employees')
}

/**
 * 添加员工（两种模式）
 * - 创建模式：username/realName/phone/password + roleType(WORKER|TECHNICIAN) + greenhouseId
 * - 邀请模式：identifier（已存在账号的用户名/手机号）+ greenhouseId
 */
export function addEmployee(data) {
  return request.post('/owner/employees', data)
}

/** 重置员工密码 */
export function resetEmployeePassword(id, newPassword) {
  return request.put(`/owner/employees/${id}/password`, { newPassword })
}

/** 更新员工权限 */
export function updateEmployeePermission(id, data) {
  return request.put(`/owner/employees/${id}/permissions`, data)
}

/** 更新员工基本信息（姓名/手机号） */
export function updateEmployeeInfo(id, data) {
  return request.put(`/owner/employees/${id}`, data)
}

/** 查看员工权限列表 */
export function getEmployeePermissions(id) {
  return request.get(`/owner/employees/${id}/permissions`)
}

/** 移除员工（解除归属+删除权限记录） */
export function removeEmployee(id) {
  return request.delete(`/owner/employees/${id}`)
}