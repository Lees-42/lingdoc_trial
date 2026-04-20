import request from '@/utils/request'
import { streamRequest } from '@/utils/request'

// ==================== 会话管理 ====================

// 查询会话列表
export function listSession(query) {
  return request({
    url: '/lingdoc/ai/chat/session/list',
    method: 'get',
    params: query
  })
}

// 获取会话详情
export function getSession(sessionId) {
  return request({
    url: '/lingdoc/ai/chat/session/' + sessionId,
    method: 'get'
  })
}

// 新增会话
export function addSession(data) {
  return request({
    url: '/lingdoc/ai/chat/session',
    method: 'post',
    data: data
  })
}

// 修改会话（重命名/置顶）
export function updateSession(data) {
  return request({
    url: '/lingdoc/ai/chat/session',
    method: 'put',
    data: data
  })
}

// 删除会话
export function delSession(sessionId) {
  return request({
    url: '/lingdoc/ai/chat/session/' + sessionId,
    method: 'delete'
  })
}

// ==================== 消息管理 ====================

// 获取消息历史列表
export function listMessage(sessionId, query) {
  return request({
    url: '/lingdoc/ai/chat/message/list/' + sessionId,
    method: 'get',
    params: query
  })
}

// ==================== 对话 ====================

// 非流式对话（等待完整响应）
export function chat(data) {
  return request({
    url: '/lingdoc/ai/chat/send',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 流式对话（SSE，逐字输出）
export function streamChat(data, onMessage, onError, onDone) {
  return streamRequest('/lingdoc/ai/chat/stream', data, onMessage, onError, onDone)
}

// 终止生成
export function stopChat(sessionId) {
  return request({
    url: '/lingdoc/ai/chat/stop/' + sessionId,
    method: 'post'
  })
}
