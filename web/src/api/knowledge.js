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

/** 更新文档标记信息（编号/标题/分类/简介） */
export function updateDocument(id, data) {
  return request.put(`/knowledge/documents/${id}`, data)
}

/** 问答测试 */
export function testQa(data) {
  return request.post('/knowledge/test', data)
}

/** 分类管理：列表（含文档数） */
export function getManagedCategories() {
  return request.get('/knowledge/categories/managed')
}

/** 分类管理：新增 */
export function createCategory(data) {
  return request.post('/knowledge/categories/managed', data)
}

/** 分类管理：重命名/编辑 */
export function updateCategory(id, data) {
  return request.put(`/knowledge/categories/managed/${id}`, data)
}

/** 分类管理：删除 */
export function deleteCategory(id) {
  return request.delete(`/knowledge/categories/managed/${id}`)
}

/** 文档内容预览（只读角色；返回 blob） */
export function getDocumentContent(id) {
  return request.get(`/knowledge/documents/${id}/content`, {
    responseType: 'blob',
    timeout: 60000
  })
}
