import request from '@/utils/request'

const BASE = '/admin/report'

/**
 * 导出传感器历史数据
 * @param {Object} params - { greenhouseId, sensorType, startTime?, endTime? }
 * @returns {Promise} Blob response
 */
export function exportSensors(params) {
  return request.get(`${BASE}/sensors`, {
    params,
    responseType: 'blob'
  })
}

/**
 * 导出预警记录
 * @param {Object} params - { greenhouseId, level?, startTime?, endTime? }
 */
export function exportAlerts(params) {
  return request.get(`${BASE}/alerts`, {
    params,
    responseType: 'blob'
  })
}

/**
 * 导出设备控制日志
 * @param {Object} params - { greenhouseId, startTime?, endTime? }
 */
export function exportControls(params) {
  return request.get(`${BASE}/controls`, {
    params,
    responseType: 'blob'
  })
}

/**
 * 导出健康评分记录
 * @param {Object} params - { greenhouseId, startTime?, endTime? }
 */
export function exportHealth(params) {
  return request.get(`${BASE}/health`, {
    params,
    responseType: 'blob'
  })
}

/**
 * 触发浏览器下载 Blob
 */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
