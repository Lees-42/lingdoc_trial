import request from '@/utils/request'

// 查询文件版本列表
export function listVersion(query) {
  return request({
    url: '/lingdoc/version/list',
    method: 'get',
    params: query
  })
}

// 查询文件的历史版本
export function listFileVersions(fileId) {
  return request({
    url: '/lingdoc/version/history/' + fileId,
    method: 'get'
  })
}

// 回滚到指定版本
export function rollbackVersion(data) {
  return request({
    url: '/lingdoc/version/rollback',
    method: 'post',
    data: data
  })
}

// 预览版本文件
export function previewVersion(versionId) {
  return request({
    url: '/lingdoc/version/preview/' + versionId,
    method: 'get'
  })
}

// 删除版本记录
export function delVersion(versionId) {
  return request({
    url: '/lingdoc/version/' + versionId,
    method: 'delete'
  })
}

// 对比两个版本
export function compareVersion(data) {
  return request({
    url: '/lingdoc/version/compare',
    method: 'post',
    data: data
  })
}
