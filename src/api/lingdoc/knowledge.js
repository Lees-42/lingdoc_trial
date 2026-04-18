import request from '@/utils/request'

// 查询知识库列表
export function listKnowledge(query) {
  return request({
    url: '/lingdoc/knowledge/list',
    method: 'get',
    params: query
  })
}

// 查询知识库详细
export function getKnowledge(kbId) {
  return request({
    url: '/lingdoc/knowledge/' + kbId,
    method: 'get'
  })
}

// 新增知识库
export function addKnowledge(data) {
  return request({
    url: '/lingdoc/knowledge',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateKnowledge(data) {
  return request({
    url: '/lingdoc/knowledge',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKnowledge(kbId) {
  return request({
    url: '/lingdoc/knowledge/' + kbId,
    method: 'delete'
  })
}

// 获取可访问的知识库列表
export function listAccessibleKnowledge() {
  return request({
    url: '/lingdoc/knowledge/accessible',
    method: 'get'
  })
}
