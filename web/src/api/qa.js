import request from '@/utils/request'

const BASE = '/qa'

/** 文字问答 */
export function askText(question, greenhouseId) {
  return request.post(`${BASE}/ask`, { question, greenhouseId })
}

/** 语音问答 */
export function askVoice(audioFile, greenhouseId) {
  const formData = new FormData()
  formData.append('audio', audioFile)
  if (greenhouseId) formData.append('greenhouseId', greenhouseId)
  return request.post(`${BASE}/ask/voice`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 问答历史 */
export function getRecords(page = 0, size = 10) {
  return request.get(`${BASE}/records`, { params: { page, size } })
}
