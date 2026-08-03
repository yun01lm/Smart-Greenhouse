import request from '@/utils/request'

/**
 * 管理员设备管理 API（R4）
 * 后端路径: /api/v1/admin/devices
 * 权限: ADMIN
 */

const BASE = '/admin/devices'

/** 设备总体统计（按地区范围） */
export function getAdminDeviceOverview(params = {}) {
  return request.get(`${BASE}/overview`, { params })
}

/** 地区范围内棚主列表（支持关键词搜索） */
export function getAdminDeviceOwners(params = {}) {
  return request.get(`${BASE}/owners`, { params })
}

/** 某棚主名下全部设备（按大棚分组） */
export function getAdminOwnerDevices(ownerId) {
  return request.get(`${BASE}/owners/${ownerId}/devices`)
}