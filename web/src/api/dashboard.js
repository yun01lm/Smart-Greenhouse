import request from '@/utils/request'

/**
 * 管理员数据总览 API
 * 后端路径: /api/v1/admin/dashboard
 * 权限: ADMIN
 */

/** 地区数据总览（统计/环境聚合/预警总览/健康评分/最新预警/天气/系统监控） */
export function getAdminOverview(params = {}) {
  return request.get('/admin/dashboard/overview', { params })
}
