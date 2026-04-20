import request from '@/utils/request'

// ==================== 知识库管理 ====================

// 查询知识库列表
export function listKb(query) {
  return request({
    url: '/lingdoc/ai/knowledge/list',
    method: 'get',
    params: query
  })
}

// 获取知识库详情
export function getKb(kbId) {
  return request({
    url: '/lingdoc/ai/knowledge/' + kbId,
    method: 'get'
  })
}

// 新增知识库
export function addKb(data) {
  return request({
    url: '/lingdoc/ai/knowledge',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateKb(data) {
  return request({
    url: '/lingdoc/ai/knowledge',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKb(kbId) {
  return request({
    url: '/lingdoc/ai/knowledge/' + kbId,
    method: 'delete'
  })
}

// 获取用户可访问的知识库列表（下拉选择）
export function listAccessibleKb() {
  return request({
    url: '/lingdoc/ai/knowledge/accessible',
    method: 'get'
  })
}

// ==================== 文档管理 ====================

// 查询文档列表
export function listDoc(kbId, query) {
  return request({
    url: '/lingdoc/ai/knowledge/doc/list',
    method: 'get',
    params: { kbId, ...query }
  })
}

// 上传文档
export function uploadDoc(kbId, data) {
  return request({
    url: '/lingdoc/ai/knowledge/doc/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除文档
export function delDoc(docId) {
  return request({
    url: '/lingdoc/ai/knowledge/doc/' + docId,
    method: 'delete'
  })
}

// 查询文档解析状态（用于轮询）
export function getDocStatus(docId) {
  return request({
    url: '/lingdoc/ai/knowledge/doc/status/' + docId,
    method: 'get'
  })
}

// ==================== 索引管理 ====================

// 重建知识库索引
export function reindexKb(kbId) {
  return request({
    url: '/lingdoc/ai/knowledge/reindex/' + kbId,
    method: 'post'
  })
}
