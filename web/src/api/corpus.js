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

/** 上传语料（最大30MB，覆盖全局15s超时，避免大文件上传被中断卡住） */
export function uploadCorpus(formData) {
  return request.post(BASE, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 删除语料 */
export function deleteCorpus(id) {
  return request.delete(`${BASE}/${id}`)
}

/** 获取语料音频（Blob，用于播放；audio 标签不携带 JWT，需经 axios 拉取） */
export function getCorpusAudio(id) {
  return request.get(`${BASE}/${id}/audio`, { responseType: 'blob' })
}
