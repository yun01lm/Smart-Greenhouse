import request from '@/utils/request'

/**
 * 认证 API
 * 后端路径: /api/v1/auth
 */

/** 修改当前用户密码（R16，全端通用） */
export function changeMyPassword(data) {
  return request.put('/auth/password', data)
}