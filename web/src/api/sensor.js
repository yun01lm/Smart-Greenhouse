import request from '@/utils/request'

/** 获取大棚实时传感器数据 */
export function getRealtimeData(greenhouseId) {
  return request.get('/sensors/realtime', { params: { greenhouseId } })
}
