import request from '@/utils/request'

/** 获取大棚列表 */
export function getGreenhouses() {
  return request.get('/greenhouses')
}

/** 创建大棚（棚主创建自己的；管理员传 ownerId 代建，R45） */
export function createGreenhouse(data) {
  return request.post('/greenhouses', data)
}

/** 更新大棚（棚主自己的；管理员可代管，R45） */
export function updateGreenhouse(id, data) {
  return request.put(`/greenhouses/${id}`, data)
}

/** 删除大棚（级联清理关联数据，R45） */
export function deleteGreenhouse(id) {
  return request.delete(`/greenhouses/${id}`)
}
