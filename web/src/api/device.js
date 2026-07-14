import request from '@/utils/request'

/**
 * 设备管理 API
 * 后端路径: /api/v1/greenhouses/{greenhouseId}/devices
 */

/** 获取大棚下设备列表 */
export function getDevices(greenhouseId, params = {}) {
  return request.get(`/greenhouses/${greenhouseId}/devices`, { params })
}

/** 获取设备详情 */
export function getDevice(greenhouseId, deviceId) {
  return request.get(`/greenhouses/${greenhouseId}/devices/${deviceId}`)
}

/** 添加设备 */
export function createDevice(greenhouseId, data) {
  return request.post(`/greenhouses/${greenhouseId}/devices`, data)
}

/** 更新设备 */
export function updateDevice(greenhouseId, deviceId, data) {
  return request.put(`/greenhouses/${greenhouseId}/devices/${deviceId}`, data)
}

/** 删除设备 */
export function deleteDevice(greenhouseId, deviceId) {
  return request.delete(`/greenhouses/${greenhouseId}/devices/${deviceId}`)
}

// ===== 设备分组 =====

/** 获取分组列表 */
export function getDeviceGroups(greenhouseId) {
  return request.get(`/greenhouses/${greenhouseId}/device-groups`)
}

/** 获取分组详情 */
export function getDeviceGroup(greenhouseId, groupId) {
  return request.get(`/greenhouses/${greenhouseId}/device-groups/${groupId}`)
}

/** 创建分组 */
export function createDeviceGroup(greenhouseId, data) {
  return request.post(`/greenhouses/${greenhouseId}/device-groups`, data)
}

/** 更新分组 */
export function updateDeviceGroup(greenhouseId, groupId, data) {
  return request.put(`/greenhouses/${greenhouseId}/device-groups/${groupId}`, data)
}

/** 向分组添加设备 */
export function addDeviceToGroup(greenhouseId, groupId, deviceId) {
  return request.post(`/greenhouses/${greenhouseId}/device-groups/${groupId}/devices/${deviceId}`)
}

/** 从分组移除设备 */
export function removeDeviceFromGroup(greenhouseId, groupId, deviceId) {
  return request.delete(`/greenhouses/${greenhouseId}/device-groups/${groupId}/devices/${deviceId}`)
}

/** 删除分组 */
export function deleteDeviceGroup(greenhouseId, groupId) {
  return request.delete(`/greenhouses/${greenhouseId}/device-groups/${groupId}`)
}
