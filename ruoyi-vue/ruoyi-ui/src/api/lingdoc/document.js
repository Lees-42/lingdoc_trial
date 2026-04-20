import request from '@/utils/request'

// 查询知识库文档列表
export function listDocuments(kbId, params) {
  return request({
    url: '/ai/doc/list/' + kbId,
    method: 'get',
    params: params
  })
}

// 获取文档详情
export function getDocument(docId) {
  return request({
    url: '/ai/doc/' + docId,
    method: 'get'
  })
}

// 上传文档
export function uploadDocument(data) {
  return request({
    url: '/ai/doc/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除文档
export function delDocument(docId) {
  return request({
    url: '/ai/doc/' + docId,
    method: 'delete'
  })
}

// 重新处理文档
export function reprocessDocument(docId) {
  return request({
    url: '/ai/doc/reprocess/' + docId,
    method: 'post'
  })
}
