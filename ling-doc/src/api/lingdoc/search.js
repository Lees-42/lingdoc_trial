import request from '@/utils/request'

// 自然语言检索
export function searchQuery(data) {
  return request({
    url: '/lingdoc/search/query',
    method: 'post',
    data: data
  })
}

// 获取检索历史列表
export function listSearchHistory(query) {
  return request({
    url: '/lingdoc/search/history/list',
    method: 'get',
    params: query
  })
}

// 删除单条检索历史
export function delSearchHistory(sessionId) {
  return request({
    url: '/lingdoc/search/history/' + sessionId,
    method: 'delete'
  })
}

// 清空检索历史
export function cleanSearchHistory() {
  return request({
    url: '/lingdoc/search/history/clean',
    method: 'delete'
  })
}

// 获取会话详情
export function getSearchSession(sessionId) {
  return request({
    url: '/lingdoc/search/session/' + sessionId,
    method: 'get'
  })
}
