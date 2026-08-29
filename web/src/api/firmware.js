import request from '@/utils/request'

/**
 * 固件管理 API（仅管理员）
 * 后端路径: /api/v1/admin/firmwares
 */

/** 批量预注册固件 */
export function batchRegisterFirmwares(data) {
  return request.post('/admin/firmwares/batch', data)
}

/** 固件列表（可按状态筛选） */
export function getFirmwares(params = {}) {
  return request.get('/admin/firmwares', { params })
}

/** 固件详情 */
export function getFirmware(firmwareId) {
  return request.get(`/admin/firmwares/${firmwareId}`)
}

/** 未绑定固件数量统计 */
export function getUnboundFirmwareCount() {
  return request.get('/admin/firmwares/stats/unbound-count')
}
