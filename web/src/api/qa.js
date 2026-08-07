import request from '@/utils/request'

const BASE = '/qa'

/**
 * AI 问答请求专用超时：DeepSeek 生成可能 20-60 秒，
 * 不使用全局 15 秒超时，避免 LLM 慢时前端误报网络异常。
 */
const QA_TIMEOUT = 120000

/** 文字问答 */
export function askText(question, greenhouseId) {
  return request.post(`${BASE}/ask`, { question, greenhouseId }, { timeout: QA_TIMEOUT })
}

/** 语音问答 */
export function askVoice(audioFile, greenhouseId) {
  const formData = new FormData()
  formData.append('audio', audioFile)
  if (greenhouseId) formData.append('greenhouseId', greenhouseId)
  return request.post(`${BASE}/ask/voice`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: QA_TIMEOUT
  })
}

/**
 * 问答历史
 * @param {number} page 页码（从 1 开始）
 * @param {number} size 每页条数
 * @param {string} date 可选，按天查询（yyyy-MM-dd），空则查全部
 */
export function getRecords(page = 1, size = 30, date = '') {
  return request.get(`${BASE}/records`, { params: { page, size, date } })
}