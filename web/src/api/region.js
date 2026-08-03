import request from '@/utils/request'

/**
 * 地区 API（管理员功能）
 * 后端路径: /api/v1/admin/regions
 * 权限: ADMIN
 * 地区层级从大棚登记的省/市/县(区)/乡镇/村字段聚合
 */

/** 省份列表 */
export function getProvinces() {
  return request.get('/admin/regions/provinces')
}

/** 某省下的城市 */
export function getCities(province) {
  return request.get('/admin/regions/cities', { params: { province } })
}

/** 某省市下的区县 */
export function getDistricts(province, city) {
  return request.get('/admin/regions/districts', { params: { province, city } })
}

/** 某省市县下的乡镇 */
export function getTowns(province, city, district) {
  return request.get('/admin/regions/towns', { params: { province, city, district } })
}

/** 某省市县镇下的村 */
export function getVillages(province, city, district, town) {
  return request.get('/admin/regions/villages', { params: { province, city, district, town } })
}

/** 地区范围内的棚主用户（支持关键词搜索） */
export function getRegionUsers(params) {
  return request.get('/admin/regions/users', { params })
}
