import request from '@/utils/request'

// ==================== 标签管理 ====================

// 获取所有标签
// 权限: lingdoc:tag:list
export function listTag() {
  return request({
    url: '/lingdoc/tag/list',
    method: 'get'
  })
}

// 新建标签
// 权限: lingdoc:tag:add
export function addTag(data) {
  return request({
    url: '/lingdoc/tag',
    method: 'post',
    data: data
  })
}

// 获取文件的标签（含继承）
// 权限: lingdoc:tag:list
export function getFileTags(fileId) {
  return request({
    url: '/lingdoc/tag/file/' + fileId,
    method: 'get'
  })
}

// 绑定标签到文件/目录
// 权限: lingdoc:tag:edit
export function bindTag(data) {
  return request({
    url: '/lingdoc/tag/bind',
    method: 'post',
    data: data
  })
}

// 解绑标签
// 权限: lingdoc:tag:edit
export function unbindTag(bindingId) {
  return request({
    url: '/lingdoc/tag/bind/' + bindingId,
    method: 'delete'
  })
}
