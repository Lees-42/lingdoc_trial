import request from '@/utils/request'

// 获取会话列表
export function listSessions(params) {
  return request({
    url: '/ai/chat/sessions',
    method: 'get',
    params: params
  })
}

// 创建会话
export function createSession(kbId, title) {
  return request({
    url: '/ai/chat/sessions',
    method: 'post',
    params: { kbId, title }
  })
}

// 删除会话
export function deleteSession(sessionId) {
  return request({
    url: '/ai/chat/sessions/' + sessionId,
    method: 'delete'
  })
}

// 获取会话消息
export function getMessages(sessionId) {
  return request({
    url: '/ai/chat/sessions/' + sessionId + '/messages',
    method: 'get'
  })
}

// 发送消息
export function sendMessage(data) {
  return request({
    url: '/ai/chat/send',
    method: 'post',
    data: data
  })
}