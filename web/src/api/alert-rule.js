import request from '@/utils/request'

const BASE = '/alerts'

// ===== 预警规则 =====

/** 获取预警规则列表（可选 greenhouseId 筛选） */
export function getAlertRules(greenhouseId) {
  return request.get(`${BASE}/rules`, { params: greenhouseId ? { greenhouseId } : {} })
}

/** 创建预警规则 */
export function createAlertRule(data) {
  return request.post(`${BASE}/rules`, data)
}

/** 更新预警规则 */
export function updateAlertRule(id, data) {
  return request.put(`${BASE}/rules/${id}`, data)
}

/** 删除预警规则 */
export function deleteAlertRule(id) {
  return request.delete(`${BASE}/rules/${id}`)
}

// ===== 自定义阈值 =====

/** 获取自定义阈值列表（可选 greenhouseId 筛选） */
export function getThresholds(greenhouseId) {
  return request.get(`${BASE}/thresholds`, { params: greenhouseId ? { greenhouseId } : {} })
}

/** 删除自定义阈值 */
export function deleteThreshold(id) {
  return request.delete(`${BASE}/thresholds/${id}`)
}
