import request from '@/utils/request'

/** 获取预警列表 */
export function getAlerts(greenhouseId, page = 1, size = 5) {
  return request.get('/alerts', { params: { greenhouseId, page, size } })
}

/** 获取未读预警数量 */
export function getUnreadAlertCount(greenhouseId) {
  return request.get('/alerts/unread-count', { params: { greenhouseId } })
}
