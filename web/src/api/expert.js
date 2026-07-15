import request from '@/utils/request'

const BASE = '/admin/experts'

/** 获取专家列表 */
export function getExperts() {
  return request.get(BASE)
}

/** 切换专家在线状态 */
export function toggleExpertOnline(id, online) {
  return request.put(`${BASE}/${id}/online`, null, { params: { online } })
}

/** 获取全量授权记录 */
export function getAuthorizations(params) {
  return request.get(`${BASE}/authorizations`, { params })
}

/** 获取专家工作台统计 */
export function getExpertStats() {
  return request.get(`${BASE}/stats`)
}
