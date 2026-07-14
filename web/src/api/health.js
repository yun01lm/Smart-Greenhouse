import request from '@/utils/request'

/** 获取健康评分 */
export function getHealthScore(greenhouseId) {
  return request.get('/health/score', { params: { greenhouseId } })
}
