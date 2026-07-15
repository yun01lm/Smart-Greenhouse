import request from '@/utils/request'

const BASE = '/admin/owners'

/** 获取棚主列表 */
export function getOwners() {
  return request.get(BASE)
}

/** 查看棚主名下大棚 */
export function getOwnerGreenhouses(ownerId) {
  return request.get(`${BASE}/${ownerId}/greenhouses`)
}
