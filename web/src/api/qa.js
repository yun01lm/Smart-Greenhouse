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

/** 问答历史 */
export function getRecords(page = 0, size = 10) {
  return request.get(`${BASE}/records`, { params: { page, size } })
}