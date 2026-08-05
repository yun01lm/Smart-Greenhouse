import request from '@/utils/request'

const BASE = '/admin/owners'

/**
 * 获取棚主列表（R10 支持关键词/五级地区筛选/分页）
 * @param {Object} params - { keyword, province, city, district, town, village, page, size }
 */
export function getOwners(params = {}) {
  return request.get(BASE, { params })
}

/** 查看棚主名下大棚 */
export function getOwnerGreenhouses(ownerId) {
  return request.get(`${BASE}/${ownerId}/greenhouses`)
}
