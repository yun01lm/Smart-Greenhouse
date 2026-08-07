import request from '@/utils/request'

/** 获取预警列表 */
export function getAlerts(greenhouseId, page = 1, size = 5) {
  // 后端分页为 0 起始，前端统一按 1 起始传入
  return request.get('/alerts', { params: { greenhouseId, page: Math.max(0, page - 1), size } })
}

/** 获取未读预警数量 */
export function getUnreadAlertCount(greenhouseId) {
  return request.get('/alerts/unread-count', { params: { greenhouseId } })
}

/** 标记预警已处理 */
export function markAlertHandled(id) {
  return request.put(`/alerts/${id}/handle`)
}
