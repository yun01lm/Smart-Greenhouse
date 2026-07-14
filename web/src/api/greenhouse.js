import request from '@/utils/request'

/** 获取大棚列表 */
export function getGreenhouses() {
  return request.get('/greenhouses')
}
