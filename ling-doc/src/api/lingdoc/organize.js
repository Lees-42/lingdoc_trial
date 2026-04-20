import request from '@/utils/request'

// 查询待规整文件列表
export function listOrganize(query) {
  return request({
    url: '/lingdoc/organize/list',
    method: 'get',
    params: query
  })
}

// 上传文件
export function uploadOrganize(data) {
  return request({
    url: '/lingdoc/organize/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取 AI 规整建议
export function getOrganizeSuggestion(fileId) {
  return request({
    url: '/lingdoc/organize/suggestion/' + fileId,
    method: 'get'
  })
}

// 确认归档
export function confirmOrganize(data) {
  return request({
    url: '/lingdoc/organize/confirm',
    method: 'post',
    data: data
  })
}

// 批量确认归档
export function batchConfirmOrganize(data) {
  return request({
    url: '/lingdoc/organize/batchConfirm',
    method: 'post',
    data: data
  })
}

// 删除待处理文件
export function delOrganize(fileId) {
  return request({
    url: '/lingdoc/organize/' + fileId,
    method: 'delete'
  })
}

// 批量删除待处理文件
export function batchDelOrganize(fileIds) {
  return request({
    url: '/lingdoc/organize/batch',
    method: 'delete',
    data: fileIds
  })
}

// 清空待处理列表
export function cleanOrganize() {
  return request({
    url: '/lingdoc/organize/clean',
    method: 'delete'
  })
}
