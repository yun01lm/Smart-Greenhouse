import request from '@/utils/request'

/**
 * 设备控制/场景联动 API
 * 后端路径: /api/v1/control
 */

/** 获取大棚场景列表 */
export function getScenes(greenhouseId) {
  return request.get('/control/scenes', { params: { greenhouseId } })
}

/** 一键执行场景 */
export function executeScene(sceneId) {
  return request.post(`/control/scenes/${sceneId}/execute`)
}
