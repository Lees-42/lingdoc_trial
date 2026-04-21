import request from '@/utils/request'

// ==================== 文件上传管理 ====================

// 查询已上传文件列表
// 权限: lingdoc:upload:list
export function listUpload(query) {
  return request({
    url: '/lingdoc/upload/list',
    method: 'get',
    params: query
  })
}

// 上传文件
// 权限: lingdoc:upload:upload
export function uploadFile(data) {
  return request({
    url: '/lingdoc/upload/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

// 获取 AI 规整建议
// 权限: lingdoc:upload:list
export function getUploadSuggestion(fileId) {
  return request({
    url: '/lingdoc/upload/suggestion/' + fileId,
    method: 'get'
  })
}

// 触发单文件自动规整
// 权限: lingdoc:upload:organize
export function organizeUpload(fileId) {
  return request({
    url: '/lingdoc/upload/organize',
    method: 'post',
    data: { fileId }
  })
}

// 批量触发自动规整
// 权限: lingdoc:upload:organize
export function batchOrganizeUpload(fileIds) {
  return request({
    url: '/lingdoc/upload/batchOrganize',
    method: 'post',
    data: fileIds
  })
}

// 确认归档
// 权限: lingdoc:upload:confirm
export function confirmUpload(data) {
  return request({
    url: '/lingdoc/upload/confirm',
    method: 'post',
    data: data
  })
}

// 批量确认归档
// 权限: lingdoc:upload:confirm
export function batchConfirmUpload(data) {
  return request({
    url: '/lingdoc/upload/batchConfirm',
    method: 'post',
    data: data
  })
}

// 删除已上传文件
// 权限: lingdoc:upload:delete
export function delUpload(fileId) {
  return request({
    url: '/lingdoc/upload/' + fileId,
    method: 'delete'
  })
}

// 批量删除已上传文件
// 权限: lingdoc:upload:delete
export function batchDelUpload(fileIds) {
  return request({
    url: '/lingdoc/upload/batch',
    method: 'delete',
    data: fileIds
  })
}

// 清空已上传文件列表
// 权限: lingdoc:upload:delete
export function cleanUpload() {
  return request({
    url: '/lingdoc/upload/clean',
    method: 'delete'
  })
}
