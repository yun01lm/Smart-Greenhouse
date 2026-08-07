import request from '@/utils/request'

/**
 * 专家咨询对话 API（专家端 R27）
 * 后端路径: /api/v1/chat
 */

/** 会话列表（专家查自己收到的咨询） */
export function getConversations(params = {}) {
  return request.get('/chat/conversations', { params })
}

/** 会话消息历史 */
export function getConversationMessages(id, params = {}) {
  return request.get(`/chat/conversations/${id}/messages`, { params })
}

/** 发送消息 */
export function sendMessage(data) {
  return request.post('/chat/messages', data)
}

/** 关闭会话 */
export function closeConversation(id) {
  return request.put(`/chat/conversations/${id}/close`)
}

/** 未读消息数 */
export function getChatUnread() {
  return request.get('/chat/unread')
}