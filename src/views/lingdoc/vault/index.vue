<template>
  <div class="app-container vault-browser">
    <!-- 仓库初始化引导 -->
    <el-alert
      v-if="state.repoInitializing"
      title="正在为您初始化默认仓库..."
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 8px"
    />

    <!-- 顶部工具栏 -->
    <VaultToolbar
      v-model:view-mode="state.viewMode"
      v-model:keyword="state.queryParams.keyword"
      v-model:current-vault-path="vaultStore.currentVaultPath"
      :repo-list="vaultStore.repoList"
      @search="handleSearch"
      @refresh="handleRefresh"
      @sync="handleSync"
      @create-folder="handleCreateFolder"
      @repo-manage="state.repoDialogVisible = true"
      @upload="openUploadDialog"
      @vault-change="handleVaultChange"
    />

    <!-- 两栏可调布局 -->
    <div class="vault-main" @mouseup="stopResize" @mousemove="onResize">
      <!-- 左侧：目录树 + 文件列表 -->
      <div class="vault-sidebar" :style="{ width: sidebarWidth + 'px' }">
        <VaultFileTree
          :tree-data="state.treeData"
          :loading="state.treeLoading"
          @node-click="handleTreeNodeClick"
          @delete-folder="handleDeleteFolder"
        />
        <VaultFileList
          :file-list="state.fileList"
          :loading="state.fileLoading"
          :total="state.total"
          :view-mode="state.viewMode"
          :selected-files="state.selectedFiles"
          @page-change="handlePageChange"
          @row-click="handleFileClick"
          @selection-change="handleSelectionChange"
          @rename="handleRename"
          @move="handleMove"
          @delete="handleDelete"
          @download="handleDownload"
        />
      </div>

      <!-- 拖拽分隔条 -->
      <div class="vault-resizer" @mousedown="startResize">
        <div class="resizer-line" />
      </div>

      <!-- 右侧预览/详情 -->
      <div class="vault-preview">
        <VaultFilePreview
          :current-file="state.currentFile"
          :preview-content="state.previewContent"
          :loading="state.previewLoading"
          @enlarge="handleEnlargePreview"
        />
      </div>
    </div>

    <!-- 放大预览弹窗 -->
    <VaultPreviewDialog
      v-model:visible="state.previewDialogVisible"
      :file-name="state.currentFile?.fileName"
      :content="state.previewContent"
    />

    <!-- 仓库管理弹窗 -->
    <VaultRepoManager
      v-model="state.repoDialogVisible"
      :repo-list="vaultStore.repoList"
      @success="handleRepoChanged"
    />

    <!-- 重复文件处理弹窗 -->
    <DuplicateFileDialog
      v-model="state.duplicateDialogVisible"
      :duplicate-data="state.duplicateFiles"
      @deleted="handleRefresh"
    />

    <!-- 文件上传弹窗 -->
    <el-dialog
      title="上传文件到 Vault"
      v-model="state.uploadDialogVisible"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="目标目录">
          <el-input
            v-model="state.uploadTargetPath"
            placeholder="留空则上传到根目录"
            clearable
          />
          <p class="form-tip">默认使用当前选中的目录：{{ state.selectedNode?.value || '根目录' }}</p>
        </el-form-item>
      </el-form>
      <el-upload
        ref="uploadRef"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleUploadChange"
        :show-file-list="true"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.bmp,.webp,.txt,.md,.csv,.json,.xml,.yaml,.yml"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <span class="upload-primary">拖拽文件到此处，或 <em>点击上传</em></span>
          <p class="upload-tip">支持 PDF、Word、Excel、图片、文本等格式</p>
        </div>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="state.uploadDialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  getVaultTree,
  listVaultFiles,
  getVaultFileContent,
  downloadVaultFile,
  renameVaultFile,
  moveVaultFile,
  delVaultFile,
  batchDelVaultFile,
  syncVault,
  listDuplicateFiles,
  createVaultFolder,
  getVaultRepo,
  uploadVaultFile,
  listVaultRepos,
  deleteFolder
} from '@/api/lingdoc/vault'
import { getFileTags, getFolderTags } from '@/api/lingdoc/tag'
import useVaultStore from '@/store/modules/vault'
import VaultToolbar from './components/VaultToolbar.vue'
import VaultFileTree from './components/VaultFileTree.vue'
import VaultFileList from './components/VaultFileList.vue'
import VaultFilePreview from './components/VaultFilePreview.vue'
import VaultPreviewDialog from './components/VaultPreviewDialog.vue'
import VaultRepoManager from './components/VaultRepoManager.vue'
import DuplicateFileDialog from './components/DuplicateFileDialog.vue'

const vaultStore = useVaultStore()

const state = reactive({
  treeData: [],
  treeLoading: false,
  selectedNode: null,
  fileList: [],
  fileLoading: false,
  total: 0,
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    subPath: '',
    keyword: '',
    fileType: '',
    sourceType: ''
  },
  viewMode: 'list',
  selectedFiles: [],
  currentFile: null,
  previewContent: '',
  previewLoading: false,
  previewDialogVisible: false,
  repoInfo: {},
  repoDialogVisible: false,
  repoInitializing: false,
  duplicateDialogVisible: false,
  duplicateFiles: [],
  uploadDialogVisible: false,
  uploadTargetPath: ''
})

const uploadRef = ref(null)

// 左侧边栏宽度（可拖拽调整，持久化到 localStorage）
const sidebarWidth = ref(parseInt(localStorage.getItem('vaultSidebarWidth')) || 420)
const isResizing = ref(false)

function startResize(e) {
  isResizing.value = true
  e.preventDefault()
}
function onResize(e) {
  if (!isResizing.value) return
  const newWidth = e.clientX
  const minW = 300
  const maxW = window.innerWidth * 0.65
  if (newWidth >= minW && newWidth <= maxW) {
    sidebarWidth.value = newWidth
  }
}
function stopResize() {
  if (isResizing.value) {
    isResizing.value = false
    localStorage.setItem('vaultSidebarWidth', String(sidebarWidth.value))
  }
}

/** 为目录树节点递归注入文件夹标签 */
async function injectFolderTags(nodes) {
  if (!nodes || !nodes.length) return
  await Promise.all(
    nodes.map(async (node) => {
      try {
        const res = await getFolderTags(node.value)
        if (res.code === 200 && res.data) {
          node.tags = res.data.map(t => ({
            tagId: t.tagId,
            tagName: t.tagName,
            tagColor: t.tagColor || '#409EFF'
          }))
        }
      } catch (e) {
        console.error('加载文件夹标签失败', node.value, e)
      }
      if (node.children && node.children.length) {
        try {
          await injectFolderTags(node.children)
        } catch (e) {
          console.error('递归加载子目录标签失败', e)
        }
      }
    })
  )
}

/** 加载目录树 */
async function loadTree() {
  state.treeLoading = true
  try {
    const res = await getVaultTree()
    const children = res.data || []
    // 包装显式根目录节点
    const rootNode = {
      label: '根目录',
      value: '/',
      children: children
    }
    await injectFolderTags([rootNode])
    state.treeData = [rootNode]
  } catch (e) {
    ElMessage.error('加载目录树失败')
  } finally {
    state.treeLoading = false
  }
}

/** 加载文件列表（并并行查询每个文件的标签） */
async function loadFiles() {
  state.fileLoading = true
  try {
    const res = await listVaultFiles(state.queryParams)
    const fileList = res.rows || []
    state.total = res.total || 0
    // 并行查询文件标签
    await Promise.all(
      fileList.map(async (file) => {
        try {
          const tagRes = await getFileTags(file.fileId)
          if (tagRes.code === 200 && tagRes.data) {
            file.tags = tagRes.data.filter(t => t.bindType === '0')
            file.inheritedTags = tagRes.data.filter(t => t.bindType === '1')
          }
        } catch (e) {
          console.error('加载文件标签失败', file.fileId, e)
        }
      })
    )
    state.fileList = fileList
  } catch (e) {
    ElMessage.error('加载文件列表失败')
  } finally {
    state.fileLoading = false
  }
}

/** 点击目录树节点 */
function handleTreeNodeClick(node) {
  state.selectedNode = node
  state.queryParams.subPath = node.value
  state.queryParams.pageNum = 1
  loadFiles()
}

/** 搜索 */
function handleSearch() {
  state.queryParams.pageNum = 1
  loadFiles()
}

/** 分页变化 */
function handlePageChange(page, pageSize) {
  state.queryParams.pageNum = page
  state.queryParams.pageSize = pageSize
  loadFiles()
}

/** 点击文件行 */
async function handleFileClick(file) {
  state.currentFile = file
  state.previewContent = ''
  state.previewLoading = true
  try {
    const res = await getVaultFileContent(file.fileId)
    if (res.code === 200 && res.data) {
      state.previewContent = res.data.content || ''
    } else {
      state.previewContent = res.msg || '无法预览此文件'
    }
    // 若文件尚无标签数据，补查询一次
    if (!file.tags && !file.inheritedTags) {
      const tagRes = await getFileTags(file.fileId)
      if (tagRes.code === 200 && tagRes.data) {
        file.tags = tagRes.data.filter(t => t.bindType === '0')
        file.inheritedTags = tagRes.data.filter(t => t.bindType === '1')
      }
    }
  } catch (e) {
    state.previewContent = '预览加载失败'
  } finally {
    state.previewLoading = false
  }
}

/** 选择变化 */
function handleSelectionChange(selection) {
  state.selectedFiles = selection
}

/** 放大预览 */
function handleEnlargePreview() {
  if (!state.currentFile) {
    ElMessage.warning('请先选择一个文件')
    return
  }
  state.previewDialogVisible = true
}

/** 刷新 */
async function handleRefresh() {
  await loadTree()
  await loadFiles()
  ElMessage.success('刷新成功')
}

/** 同步Vault */
async function handleSync() {
  try {
    const res = await syncVault()
    if (res.code === 200) {
      const msg = `同步完成：新增 ${res.data.added}，更新 ${res.data.updated}，删除 ${res.data.deleted}`
      if (res.data.duplicates > 0) {
        ElMessage.warning(`${msg}，发现 ${res.data.duplicates} 个重复文件`)
        const dupRes = await listDuplicateFiles()
        state.duplicateFiles = dupRes.data || []
        state.duplicateDialogVisible = true
      } else {
        ElMessage.success(msg)
      }
      await loadTree()
      await loadFiles()
    }
  } catch (e) {
    ElMessage.error('同步失败')
  }
}

/** 新建文件夹 */
async function handleCreateFolder(folderName) {
  try {
    const parentPath = state.selectedNode ? state.selectedNode.value : '/'
    const subPath = parentPath === '/' ? folderName : parentPath + '/' + folderName
    await createVaultFolder({ subPath })
    ElMessage.success('文件夹创建成功')
    await loadTree()
  } catch (e) {
    ElMessage.error('创建文件夹失败')
  }
}

/** 重命名 */
async function handleRename(file) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新文件名', '重命名', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: file.fileName,
      inputPattern: /^[a-zA-Z0-9_\-|.\u4e00-\u9fa5]+$/,
      inputErrorMessage: '文件名包含非法字符'
    })
    await renameVaultFile(file.fileId, { newName: value })
    ElMessage.success('重命名成功')
    await loadFiles()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('重命名失败: ' + (e.message || e))
    }
  }
}

/** 移动 */
async function handleMove(file) {
  try {
    const { value } = await ElMessageBox.prompt('请输入目标路径', '移动文件', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: file.subPath || '',
      inputErrorMessage: '目标路径不能为空'
    })
    await moveVaultFile(file.fileId, { targetSubPath: value })
    ElMessage.success('移动成功')
    await loadTree()
    await loadFiles()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('移动失败: ' + (e.message || e))
    }
  }
}

/** 删除 */
async function handleDelete(file) {
  try {
    await ElMessageBox.confirm(`确定要删除文件 "${file.fileName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delVaultFile(file.fileId)
    ElMessage.success('删除成功')
    if (state.currentFile && state.currentFile.fileId === file.fileId) {
      state.currentFile = null
      state.previewContent = ''
    }
    await loadFiles()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || e))
    }
  }
}

/** 删除文件夹 */
async function handleDeleteFolder(folder) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件夹 "${folder.label}" 及其所有内容吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteFolder(folder.value)
    ElMessage.success('文件夹删除成功')
    if (state.currentFolder && state.currentFolder.value === folder.value) {
      state.currentFolder = state.treeData[0]
      state.currentSubPath = ''
    }
    await loadTree()
    await loadFiles()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除文件夹失败: ' + (e.message || e))
    }
  }
}

/** 下载 */
async function handleDownload(file) {
  try {
    const res = await downloadVaultFile(file.fileId)
    const blob = new Blob([res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = file.fileName
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const router = useRouter()

/** 跳转到文档上传页面 */
function openUploadDialog() {
  router.push('/lingdoc/file-upload')
}

/** 上传文件变更回调 */
async function handleUploadChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return

  const formData = new FormData()
  formData.append('file', raw)
  const targetPath = state.uploadTargetPath || state.selectedNode?.value || ''
  if (targetPath) {
    formData.append('subPath', targetPath)
  }

  try {
    const res = await uploadVaultFile(formData)
    if (res.code === 200) {
      ElMessage.success(`上传成功：${res.data.fileName}`)
      await loadTree()
      // 如果上传目录与当前查询目录一致，刷新文件列表
      if (targetPath === state.queryParams.subPath) {
        await loadFiles()
      }
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败：' + (e.message || e))
  }
}

/** 加载仓库列表 */
async function loadRepos() {
  state.repoInitializing = true
  try {
    await vaultStore.loadRepos()
    state.repoInfo = vaultStore.currentRepo || {}
  } catch (e) {
    ElMessage.error('获取仓库配置失败')
  } finally {
    state.repoInitializing = false
  }
}

/** 切换 Vault */
async function handleVaultChange(repo) {
  vaultStore.setCurrentVault(repo)
  state.repoInfo = repo
  state.selectedNode = null
  state.queryParams.subPath = ''
  state.queryParams.pageNum = 1
  state.currentFile = null
  state.previewContent = ''
  await loadTree()
  await loadFiles()
  ElMessage.success(`已切换到仓库：${repo.repoName}`)
}

/** 仓库变更后刷新 */
async function handleRepoChanged() {
  await loadRepos()
  await loadTree()
  await loadFiles()
  ElMessage.success('仓库配置已更新')
}

onMounted(() => {
  loadRepos().then(() => {
    loadTree()
    loadFiles()
  })
})
</script>

<style scoped>
.vault-browser {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}
.vault-main {
  flex: 1;
  overflow: hidden;
  margin-top: 8px;
  display: flex;
}
.vault-sidebar {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.vault-preview {
  flex: 1;
  height: 100%;
  overflow: hidden;
  min-width: 260px;
}
.vault-resizer {
  width: 6px;
  height: 100%;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: transparent;
  transition: background 0.2s;
}
.vault-resizer:hover {
  background: #e4e7ed;
}
.resizer-line {
  width: 2px;
  height: 40px;
  border-radius: 1px;
  background: #c0c4cc;
}

.upload-icon {
  font-size: 48px;
  color: var(--el-color-primary);
  margin-bottom: 12px;
}

.upload-primary {
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.upload-primary em {
  color: var(--el-color-primary);
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
