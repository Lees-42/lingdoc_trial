import request from '@/utils/request'

// ==================== 任务管理 ====================

// 查询表格填写任务列表
export function listForm(query) {
  return request({
    url: '/lingdoc/form/list',
    method: 'get',
    params: query
  })
}

// 获取任务详情
export function getFormTask(taskId) {
  return request({
    url: '/lingdoc/form/' + taskId,
    method: 'get'
  })
}

// 上传表格文件
export function uploadForm(data) {
  return request({
    url: '/lingdoc/form/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

// 修改任务
export function updateForm(data) {
  return request({
    url: '/lingdoc/form',
    method: 'put',
    data: data
  })
}

// 删除任务
export function delForm(taskId) {
  return request({
    url: '/lingdoc/form/' + taskId,
    method: 'delete'
  })
}

// 批量删除
export function batchDelForm(taskIds) {
  return request({
    url: '/lingdoc/form/batch',
    method: 'delete',
    data: taskIds
  })
}

// ==================== 字段管理 ====================

// 批量更新字段值
export function updateFormFields(data) {
  return request({
    url: '/lingdoc/form/fields',
    method: 'put',
    data: data
  })
}

// ==================== 生成与下载 ====================

// 触发AI生成
export function generateForm(data) {
  return request({
    url: '/lingdoc/form/generate',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 下载填写后文档
export function downloadForm(taskId) {
  return request({
    url: '/lingdoc/form/download/' + taskId,
    method: 'get',
    responseType: 'blob'
  })
}

// ==================== 参考文档 ====================

// 获取参考文档列表
export function getFormReferences(taskId) {
  return request({
    url: '/lingdoc/form/references/' + taskId,
    method: 'get'
  })
}
