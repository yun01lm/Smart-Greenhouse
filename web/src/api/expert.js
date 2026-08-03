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

// ===== 咨询记录（R9） =====

/** 咨询记录分页查询（支持 expertId/userKeyword/startTime/endTime 筛选） */
export function getConversations(params) {
  return request.get(`${BASE}/conversations`, { params })
}

/** 对话消息明细 */
export function getConversationMessages(id) {
  return request.get(`${BASE}/conversations/${id}/messages`)
}

/** 导出咨询记录 Excel */
export function exportConversations(params) {
  return request.get(`${BASE}/conversations/export`, {
    params,
    responseType: 'blob',
    timeout: 60000
  })
}
