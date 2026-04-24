import request from '@/utils/request'

// ==================== 目录与文件列表 ====================

// 获取目录树
export function getVaultTree() {
  return request({
    url: '/lingdoc/vault/tree',
    method: 'get'
  })
}

// 分页查询文件列表
export function listVaultFiles(query) {
  return request({
    url: '/lingdoc/vault/files',
    method: 'get',
    params: query
  })
}

// 获取单个文件详情
export function getVaultFile(fileId) {
  return request({
    url: '/lingdoc/vault/file/' + fileId,
    method: 'get'
  })
}

// 获取文本文件内容
export function getVaultFileContent(fileId) {
  return request({
    url: '/lingdoc/vault/file/' + fileId + '/content',
    method: 'get'
  })
}

// 下载文件
export function downloadVaultFile(fileId) {
  return request({
    url: '/lingdoc/vault/file/' + fileId + '/download',
    method: 'get',
    responseType: 'blob'
  })
}

// ==================== 文件操作 ====================

// 重命名文件
export function renameVaultFile(fileId, data) {
  return request({
    url: '/lingdoc/vault/file/' + fileId + '/rename',
    method: 'put',
    data: data
  })
}

// 移动文件
export function moveVaultFile(fileId, data) {
  return request({
    url: '/lingdoc/vault/file/' + fileId + '/move',
    method: 'put',
    data: data
  })
}

// 删除文件
export function delVaultFile(fileId) {
  return request({
    url: '/lingdoc/vault/file/' + fileId,
    method: 'delete'
  })
}

// 批量删除文件
export function batchDelVaultFile(fileIds) {
  return request({
    url: '/lingdoc/vault/file/' + fileIds,
    method: 'delete'
  })
}

// 新建文件夹
export function createVaultFolder(data) {
  return request({
    url: '/lingdoc/vault/folder',
    method: 'post',
    data: data
  })
}

// 删除文件夹
export function deleteFolder(path) {
  return request({
    url: '/lingdoc/vault/folder',
    method: 'delete',
    params: { path }
  })
}

// 手动触发Vault扫描同步
export function syncVault() {
  return request({
    url: '/lingdoc/vault/sync',
    method: 'post'
  })
}

// 查询重复文件列表
export function listDuplicateFiles() {
  return request({
    url: '/lingdoc/vault/duplicates',
    method: 'get'
  })
}

// 获取文件版本列表
export function getVaultFileVersions(fileId) {
  return request({
    url: '/lingdoc/vault/file/' + fileId + '/versions',
    method: 'get'
  })
}

// ==================== 文件上传 ====================

// 上传文件到Vault
export function uploadVaultFile(data) {
  return request({
    url: '/lingdoc/vault/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

// ==================== 仓库管理 ====================

// 获取当前用户所有仓库列表
export function listVaultRepos() {
  return request({
    url: '/lingdoc/vault/repos',
    method: 'get'
  })
}

// 获取当前用户默认仓库配置（含自动初始化）
export function getVaultRepo() {
  return request({
    url: '/lingdoc/vault/repo',
    method: 'get'
  })
}

// 创建/切换到新仓库
export function createVaultRepo(data) {
  return request({
    url: '/lingdoc/vault/repo',
    method: 'post',
    data: data
  })
}

// 删除仓库
export function deleteVaultRepo(repoId) {
  return request({
    url: '/lingdoc/vault/repo/' + repoId,
    method: 'delete'
  })
}

// 设置默认仓库
export function setDefaultVaultRepo(repoId) {
  return request({
    url: '/lingdoc/vault/repo/' + repoId + '/default',
    method: 'put'
  })
}

// 迁移仓库
export function migrateVaultRepo(repoId, data) {
  return request({
    url: '/lingdoc/vault/repo/' + repoId + '/migrate',
    method: 'put',
    data: data
  })
}
