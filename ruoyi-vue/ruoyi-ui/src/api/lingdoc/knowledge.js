import request from '@/utils/request'

// 查询知识库列表
export function listKnowledgeBase(query) {
  return request({
    url: '/ai/kb/list',
    method: 'get',
    params: query
  })
}

// 别名导出（兼容不同命名风格）
export const listKnowledgeBases = listKnowledgeBase

// 获取知识库详情
export function getKnowledgeBase(kbId) {
  return request({
    url: '/ai/kb/' + kbId,
    method: 'get'
  })
}

// 新增知识库
export function addKnowledgeBase(data) {
  return request({
    url: '/ai/kb/create',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateKnowledgeBase(kbId, data) {
  return request({
    url: '/ai/kb/' + kbId,
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKnowledgeBase(kbId) {
  return request({
    url: '/ai/kb/' + kbId,
    method: 'delete'
  })
}
