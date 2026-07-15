import request from '@/utils/request'

const BASE = '/admin/monitor'

/**
 * 获取系统监控综合概览
 * @returns {Promise} MonitorOverviewResponse
 */
export function getMonitorOverview() {
  return request.get(`${BASE}/overview`)
}
