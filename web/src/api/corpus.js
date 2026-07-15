import request from '@/utils/request'

const BASE = '/admin/corpus'

/** 获取语料列表（分页 + 筛选） */
export function getCorpusList(params) {
  return request.get(BASE, { params })
}

/** 获取所有方言类型 */
export function getDialects() {
  return request.get(`${BASE}/dialects`)
}

/** 上传语料 */
export function uploadCorpus(formData) {
  return request.post(BASE, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除语料 */
export function deleteCorpus(id) {
  return request.delete(`${BASE}/${id}`)
}
