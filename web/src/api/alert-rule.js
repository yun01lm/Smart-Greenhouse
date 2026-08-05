import request from '@/utils/request'
import { useViewModeStore } from '@/stores/viewMode'

const BASE = '/alerts'

/** 棚主视角下自动附加 ownerId（ADMIN 后端代查，R10） */
function withOwner(params = {}) {
  const vm = useViewModeStore()
  return vm.active && vm.ownerId ? { ...params, ownerId: vm.ownerId } : params
}

// ===== 预警规则 =====

/** 获取预警规则列表（可选 greenhouseId / ownerId 筛选） */
export function getAlertRules(greenhouseId) {
  return request.get(`${BASE}/rules`, { params: withOwner(greenhouseId ? { greenhouseId } : {}) })
}

/** 创建预警规则 */
export function createAlertRule(data) {
  return request.post(`${BASE}/rules`, data, { params: withOwner() })
}

/** 更新预警规则 */
export function updateAlertRule(id, data) {
  return request.put(`${BASE}/rules/${id}`, data, { params: withOwner() })
}

/** 删除预警规则 */
export function deleteAlertRule(id) {
  return request.delete(`${BASE}/rules/${id}`, { params: withOwner() })
}

// ===== 自定义阈值 =====

/** 获取自定义阈值列表（可选 greenhouseId / ownerId 筛选） */
export function getThresholds(greenhouseId) {
  return request.get(`${BASE}/thresholds`, { params: withOwner(greenhouseId ? { greenhouseId } : {}) })
}

/** 删除自定义阈值 */
export function deleteThreshold(id) {
  return request.delete(`${BASE}/thresholds/${id}`, { params: withOwner() })
}
