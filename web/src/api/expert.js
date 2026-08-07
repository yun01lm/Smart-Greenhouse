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


// ===== 专家授权申请 / 棚主审批（R28） =====

/** 专家：可申请授权的大棚列表（含授权状态） */
export function getApplyAvailable() {
  return request.get('/expert/authorize/available')
}

/** 专家：发起授权申请 */
export function requestAuthorization(data) {
  return request.post('/expert/authorize/request', data)
}

/** 专家：我的授权申请记录 */
export function getMyAuthorizations() {
  return request.get('/expert/authorize/my')
}

/** 棚主：待处理的授权申请 */
export function getPendingAuthorizations() {
  return request.get('/expert/authorize/pending')
}

/** 棚主：同意授权 */
export function approveAuthorization(id) {
  return request.put(`/expert/authorize/${id}/approve`)
}

/** 棚主：拒绝授权 */
export function rejectAuthorization(id) {
  return request.put(`/expert/authorize/${id}/reject`)
}

/** 棚主：有效授权列表 */
export function getActiveAuthorizations() {
  return request.get('/expert/authorize/active')
}

/** 棚主：撤销授权 */
export function revokeAuthorization(id) {
  return request.put(`/expert/authorize/${id}/revoke`)
}
