import request from '@/utils/request'

/** 获取大棚实时传感器数据 */
export function getRealtimeData(greenhouseId) {
  return request.get('/sensors/realtime', { params: { greenhouseId } })
}

/** 获取大棚历史传感器数据（时间范围 + 聚合） */
export function getSensorHistory(greenhouseId, payload) {
  return request.post('/sensors/history', payload, { params: { greenhouseId } })
}

/** 获取传感器短期预测（统计外推，预留 LSTM 替换） */
export function getSensorForecast(greenhouseId, sensorType, steps = 4, intervalMinutes = 30) {
  return request.get('/sensors/forecast', { params: { greenhouseId, sensorType, steps, intervalMinutes } })
}
