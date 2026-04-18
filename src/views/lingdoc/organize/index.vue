<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <h2 class="page-title">自动规整</h2>
    <p class="page-desc">拖拽文件到下方区域，AI 将自动解析内容并推荐规范命名与分类路径。</p>

    <!-- 文件上传区 -->
    <el-card class="upload-card" shadow="never">
      <el-upload
        ref="uploadRef"
        action="#"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.bmp,.webp,.txt,.md"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <span class="upload-primary">拖拽文件到此处，或 <em>点击上传</em></span>
          <p class="upload-tip">
            支持 PDF、Word、Excel、图片、文本等格式，单次最多上传 20 个文件
          </p>
        </div>
      </el-upload>
    </el-card>

    <!-- 文件列表 -->
    <el-card v-if="fileList.length > 0" class="list-card" shadow="never">
      <template #header>
        <div class="list-header">
          <span class="list-title">待规整文件（{{ fileList.length }}）</span>
          <div class="list-actions">
            <el-button
              type="primary"
              plain
              icon="Check"
              :disabled="!hasPendingConfirm"
              @click="handleBatchConfirm"
              v-hasPermi="['lingdoc:organize:confirm']"
            >批量归档</el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="!selectedIds.length"
              @click="handleBatchDelete"
              v-hasPermi="['lingdoc:organize:remove']"
            >批量删除</el-button>
            <el-button
              plain
              icon="Refresh"
              @click="handleClean"
              v-hasPermi="['lingdoc:organize:remove']"
            >清空列表</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="fileList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="文件名" prop="originalName" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <div class="file-name-cell">
              <svg-icon :icon-class="getFileIcon(scope.row.fileType)" class="file-icon" />
              <span>{{ scope.row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="fileType" width="80" align="center">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.fileType?.toUpperCase() || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" prop="fileSize" width="100" align="center">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 推荐文件名" prop="suggestedName" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.suggestedName" class="suggested-text">{{ scope.row.suggestedName }}</span>
            <span v-else class="placeholder-text">--</span>
          </template>
        </el-table-column>
        <el-table-column label="AI 推荐路径" prop="suggestedPath" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.suggestedPath" class="suggested-text">{{ scope.row.suggestedPath }}</span>
            <span v-else class="placeholder-text">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'pending'"
              link
              type="primary"
              icon="Edit"
              @click="handleEdit(scope.row)"
              v-hasPermi="['lingdoc:organize:edit']"
            >编辑</el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              link
              type="success"
              icon="Check"
              @click="handleConfirm(scope.row)"
              v-hasPermi="['lingdoc:organize:confirm']"
            >归档</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['lingdoc:organize:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-else description="暂无待规整文件">
      <template #description>
        <p>暂无待规整文件</p>
        <p class="empty-tip">上传文件后，AI 将自动解析并推荐归档方案</p>
      </template>
    </el-empty>

    <!-- 编辑弹窗 -->
    <el-dialog
      :title="'编辑归档方案 — ' + form.originalName"
      v-model="open"
      width="560px"
      append-to-body
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="原始文件名">
          <el-input v-model="form.originalName" disabled />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-input v-model="form.fileType" disabled />
        </el-form-item>
        <el-form-item label="推荐名称" prop="suggestedName">
          <el-input v-model="form.suggestedName" placeholder="请输入规范文件名" clearable />
        </el-form-item>
        <el-form-item label="推荐路径" prop="suggestedPath">
          <el-input
            v-model="form.suggestedPath"
            placeholder="请输入分类路径，如 /学习资料/大三上/操作系统"
            clearable
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确认归档</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Organize">
import { ref, reactive, computed } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const open = ref(false)
const uploadRef = ref(null)
const formRef = ref(null)

// 文件列表
const fileList = ref([])

// 选中项
const selectedIds = ref([])

// 是否存在待确认的文件
const hasPendingConfirm = computed(() => {
  return fileList.value.some(item => item.status === 'pending')
})

// 表单数据
const form = reactive({
  fileId: undefined,
  originalName: '',
  fileType: '',
  suggestedName: '',
  suggestedPath: '',
  remark: ''
})

// 状态枚举
const statusMap = {
  uploading: { label: '上传中', type: 'info' },
  parsing: { label: '解析中', type: 'warning' },
  pending: { label: '待确认', type: 'primary' },
  archived: { label: '已归档', type: 'success' },
  failed: { label: '失败', type: 'danger' }
}

/** 获取状态标签 */
function getStatusLabel(status) {
  return statusMap[status]?.label || status
}

/** 获取状态类型 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/** 根据文件类型获取图标 */
function getFileIcon(fileType) {
  const iconMap = {
    pdf: 'pdf',
    doc: 'word',
    docx: 'word',
    xls: 'excel',
    xlsx: 'excel',
    png: 'image',
    jpg: 'image',
    jpeg: 'image',
    gif: 'image',
    bmp: 'image',
    webp: 'image',
    txt: 'txt',
    md: 'txt'
  }
  return iconMap[fileType?.toLowerCase()] || 'file'
}

/** 格式化文件大小 */
function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

/** 文件变更回调（模拟上传 + AI 解析） */
function handleFileChange(uploadFile) {
  const raw = uploadFile.raw
  const ext = raw.name.split('.').pop()
  const fileId = Date.now() + '_' + Math.random().toString(36).substr(2, 9)

  const item = {
    fileId: fileId,
    originalName: raw.name,
    fileType: ext,
    fileSize: raw.size,
    status: 'uploading',
    suggestedName: '',
    suggestedPath: '',
    remark: ''
  }

  fileList.value = [...fileList.value, item]
  ElMessage.success(`已添加文件：${raw.name}`)

  // 模拟上传完成 → 进入解析
  setTimeout(() => {
    const idx = fileList.value.findIndex(f => f.fileId === fileId)
    if (idx !== -1) {
      fileList.value[idx] = { ...fileList.value[idx], status: 'parsing' }

      // 模拟 AI 解析完成
      setTimeout(() => {
        const i = fileList.value.findIndex(f => f.fileId === fileId)
        if (i !== -1) {
          const mockName = generateMockName(raw.name, ext)
          const mockPath = generateMockPath(ext)
          fileList.value[i] = {
            ...fileList.value[i],
            status: 'pending',
            suggestedName: mockName,
            suggestedPath: mockPath
          }
          ElMessage.info(`AI 已生成归档建议：${mockName}`)
        }
      }, 1500 + Math.random() * 1500)
    }
  }, 800)
}

/** 生成模拟文件名 */
function generateMockName(originalName, ext) {
  const nameWithoutExt = originalName.replace(/\.[^/.]+$/, '')
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  return `${nameWithoutExt}_${dateStr}.${ext}`
}

/** 生成模拟路径 */
function generateMockPath(ext) {
  const paths = [
    '/学习资料/大三上/操作系统',
    '/学习资料/大三上/计算机网络',
    '/申请材料/奖学金',
    '/实验报告/计算机组成原理',
    '/文献资料/论文素材',
    '/工作文档/实习材料'
  ]
  const imagePaths = [
    '/图片素材/截图',
    '/图片素材/扫描件',
    '/图片素材/证书'
  ]
  const isImage = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'].includes(ext?.toLowerCase())
  return isImage
    ? imagePaths[Math.floor(Math.random() * imagePaths.length)]
    : paths[Math.floor(Math.random() * paths.length)]
}

/** 多选框变化 */
function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.fileId)
}

/** 编辑按钮 */
function handleEdit(row) {
  Object.assign(form, {
    fileId: row.fileId,
    originalName: row.originalName,
    fileType: row.fileType,
    suggestedName: row.suggestedName,
    suggestedPath: row.suggestedPath,
    remark: row.remark || ''
  })
  open.value = true
}

/** 取消编辑 */
function cancel() {
  open.value = false
  resetForm()
}

/** 重置表单 */
function resetForm() {
  Object.assign(form, {
    fileId: undefined,
    originalName: '',
    fileType: '',
    suggestedName: '',
    suggestedPath: '',
    remark: ''
  })
}

/** 提交表单（确认归档） */
function submitForm() {
  if (!form.suggestedName.trim()) {
    ElMessage.warning('推荐文件名不能为空')
    return
  }
  if (!form.suggestedPath.trim()) {
    ElMessage.warning('推荐路径不能为空')
    return
  }

  const idx = fileList.value.findIndex(f => f.fileId === form.fileId)
  if (idx !== -1) {
    fileList.value[idx] = {
      ...fileList.value[idx],
      suggestedName: form.suggestedName,
      suggestedPath: form.suggestedPath,
      remark: form.remark,
      status: 'archived'
    }
  }

  open.value = false
  resetForm()
  ElMessage.success('归档成功')
}

/** 单个确认归档 */
function handleConfirm(row) {
  ElMessageBox.confirm(
    `确认将 "${row.originalName}" 归档到 "${row.suggestedPath}"？`,
    '确认归档',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    const idx = fileList.value.findIndex(f => f.fileId === row.fileId)
    if (idx !== -1) {
      fileList.value[idx] = { ...fileList.value[idx], status: 'archived' }
    }
    ElMessage.success('归档成功')
  }).catch(() => {})
}

/** 删除单个 */
function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除文件 "${row.originalName}"？`).then(() => {
    fileList.value = fileList.value.filter(f => f.fileId !== row.fileId)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 批量归档 */
function handleBatchConfirm() {
  const pendingList = fileList.value.filter(f => f.status === 'pending')
  if (!pendingList.length) {
    ElMessage.warning('没有待确认的文件')
    return
  }

  ElMessageBox.confirm(
    `确认批量归档 ${pendingList.length} 个文件？`,
    '批量归档',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    fileList.value = fileList.value.map(f =>
      f.status === 'pending' ? { ...f, status: 'archived' } : f
    )
    ElMessage.success(`成功归档 ${pendingList.length} 个文件`)
  }).catch(() => {})
}

/** 批量删除 */
function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择一项')
    return
  }

  proxy.$modal.confirm(`是否确认删除选中的 ${selectedIds.value.length} 个文件？`).then(() => {
    fileList.value = fileList.value.filter(f => !selectedIds.value.includes(f.fileId))
    selectedIds.value = []
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 清空列表 */
function handleClean() {
  if (!fileList.value.length) {
    ElMessage.warning('列表已为空')
    return
  }
  proxy.$modal.confirm('是否确认清空所有待规整文件？').then(() => {
    fileList.value = []
    selectedIds.value = []
    ElMessage.success('清空成功')
  }).catch(() => {})
}
</script>

<style scoped>
.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.upload-card {
  margin-bottom: 20px;
}

.upload-card :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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

.list-card {
  margin-top: 20px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.list-actions {
  display: flex;
  gap: 8px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.suggested-text {
  color: var(--el-color-primary);
  font-weight: 500;
}

.placeholder-text {
  color: var(--el-text-color-placeholder);
}

.empty-tip {
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
