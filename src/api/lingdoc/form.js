import request from '@/utils/request'

// 查询关系图谱列表（原表格助手，已迁移为可视化图谱）
export function listForm(query) {
  return request({
    url: '/lingdoc/form/list',
    method: 'get',
    params: query
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
    }
  })
}

// 获取 AI 字段识别结果
export function getFormFields(fileId) {
  return request({
    url: '/lingdoc/form/fields/' + fileId,
    method: 'get'
  })
}

// 获取信息源匹配结果
export function getFormSuggestions(fileId) {
  return request({
    url: '/lingdoc/form/suggestions/' + fileId,
    method: 'get'
  })
}

// 确认生成填写好的表格
export function confirmForm(data) {
  return request({
    url: '/lingdoc/form/confirm',
    method: 'post',
    data: data
  })
}

// 批量确认生成
export function batchConfirmForm(data) {
  return request({
    url: '/lingdoc/form/batchConfirm',
    method: 'post',
    data: data
  })
}

// 删除表格记录
export function delForm(fileId) {
  return request({
    url: '/lingdoc/form/' + fileId,
    method: 'delete'
  })
}

// 批量删除
export function batchDelForm(fileIds) {
  return request({
    url: '/lingdoc/form/batch',
    method: 'delete',
    data: fileIds
  })
}

// 清空列表
export function cleanForm() {
  return request({
    url: '/lingdoc/form/clean',
    method: 'delete'
  })
}
