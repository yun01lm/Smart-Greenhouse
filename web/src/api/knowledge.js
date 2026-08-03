import request from '@/utils/request'

/**
 * 知识库管理 API
 * 后端路径: /api/v1/knowledge
 * 权限: ADMIN
 */

/** 文档列表（分页 + 分类筛选 + 关键词搜索） */
export function getDocuments(params = {}) {
  return request.get('/knowledge/documents', { params })
}

/** 获取分类列表 */
export function getCategories() {
  return request.get('/knowledge/categories')
}

/** 上传文档（含切片+向量化，放宽超时到 180s，避免大文档处理超时） */
export function uploadDocument(formData) {
  return request.post('/knowledge/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000
  })
}

/** 触发向量化索引 */
export function indexDocument(documentId) {
  return request.post('/knowledge/index', null, { params: { documentId } })
}

/** 删除文档 */
export function deleteDocument(id) {
  return request.delete(`/knowledge/documents/${id}`)
}

/** 问答测试 */
export function testQa(data) {
  return request.post('/knowledge/test', data)
}
