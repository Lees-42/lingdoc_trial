import request from '@/utils/request'

// 查询标签列表
export function listTag(query) {
  return request({
    url: '/lingdoc/tag/list',
    method: 'get',
    params: query
  })
}

// 获取标签详情
export function getTag(tagId) {
  return request({
    url: '/lingdoc/tag/' + tagId,
    method: 'get'
  })
}

// 新增标签
export function addTag(data) {
  return request({
    url: '/lingdoc/tag',
    method: 'post',
    data: data
  })
}

// 修改标签
export function updateTag(data) {
  return request({
    url: '/lingdoc/tag',
    method: 'put',
    data: data
  })
}

// 删除标签
export function delTag(tagId) {
  return request({
    url: '/lingdoc/tag/' + tagId,
    method: 'delete'
  })
}

// 获取文件的标签（含继承）
export function getFileTags(fileId) {
  return request({
    url: '/lingdoc/tag/file/' + fileId,
    method: 'get'
  })
}

// 绑定标签
export function bindTag(data) {
  return request({
    url: '/lingdoc/tag/bind',
    method: 'post',
    data: data
  })
}

// 解绑标签
export function unbindTag(bindingId) {
  return request({
    url: '/lingdoc/tag/bind/' + bindingId,
    method: 'delete'
  })
}

// 获取目录的标签（含继承）
export function getFolderTags(path) {
  return request({
    url: '/lingdoc/tag/folder',
    method: 'get',
    params: { path }
  })
}

// 按目标批量解绑标签
export function unbindTagByTarget(targetType, targetId) {
  return request({
    url: '/lingdoc/tag/bind/target',
    method: 'delete',
    params: { targetType, targetId }
  })
}

// 按目标和标签ID解绑特定标签
export function unbindTagByTargetAndTagId(targetType, targetId, tagId) {
  return request({
    url: '/lingdoc/tag/bind/target-tag',
    method: 'delete',
    params: { targetType, targetId, tagId }
  })
}
