<template>
  <div class="app-container form-assistant-page">
    <!-- 顶部工具栏 -->
    <div class="form-toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">表格填写助手</h2>
        <el-tag v-if="currentTask" :type="statusTagType" effect="dark" size="small">
          {{ currentTask.statusName || statusText }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
          上传表格
        </el-button>
        <el-button :icon="Document" :disabled="!currentTask" @click="handleSelectReferences">
          参考文档
        </el-button>
        <el-button type="success" :icon="MagicStick" :disabled="!canGenerate" @click="handleGenerate">
          AI 填写
        </el-button>
        <el-button :icon="Download" :disabled="!canDownload" @click="handleDownload">
          下载结果
        </el-button>
        <el-button :icon="Refresh" @click="resetPage">重置</el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div v-if="currentTask" class="form-main">
      <!-- 文档预览区（左右分栏） -->
      <div class="preview-area">
        <!-- 左侧：原始空白文档 -->
        <div class="preview-panel">
          <div class="panel-header">
            <span class="panel-title">📄 原始空白文档</span>
            <span class="file-name">{{ currentTask.originalFileName }}</span>
          </div>
          <div class="panel-body">
            <iframe v-if="originalFileUrl" :src="originalFileUrl" class="doc-preview"></iframe>
            <div v-else class="empty-preview">
              <el-icon :size="48" color="#ccc"><Document /></el-icon>
              <p>暂无文档预览</p>
            </div>
          </div>
        </div>

        <!-- 右侧：AI填写后文档 -->
        <div class="preview-panel">
          <div class="panel-header">
            <span class="panel-title">✨ AI 填写后文档</span>
            <span v-if="currentTask.filledFileName" class="file-name">{{ currentTask.filledFileName }}</span>
            <span v-else class="file-name status-waiting">等待生成...</span>
          </div>
          <div class="panel-body">
            <iframe v-if="filledFileUrl" :src="filledFileUrl" class="doc-preview"></iframe>
            <div v-else class="empty-preview">
              <el-icon :size="48" color="#ccc"><MagicStick /></el-icon>
              <p>请先确认字段并点击"AI 填写"生成文档</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 字段编辑面板 -->
      <div class="field-editor-panel">
        <div class="panel-header">
          <span class="panel-title">📝 字段确认</span>
          <div class="panel-actions">
            <el-text type="info">共 {{ fields.length }} 个字段，已确认 {{ confirmedCount }} 个</el-text>
            <el-button type="primary" size="small" :disabled="!hasUnconfirmedFields" @click="confirmAllHighConfidence">
              一键确认高置信度字段（≥80%）
            </el-button>
          </div>
        </div>
        <div class="panel-body">
          <el-table :data="fields" border size="small" style="width: 100%">
            <el-table-column type="index" width="50" align="center" />
            <el-table-column prop="fieldName" label="字段名称" width="120" />
            <el-table-column prop="fieldType" label="类型" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="fieldTypeTag(row.fieldType)">
                  {{ fieldTypeText(row.fieldType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="aiValue" label="AI 建议值" width="180">
              <template #default="{ row }">
                <div class="ai-value-cell">
                  <span>{{ row.aiValue || '-' }}</span>
                  <el-tag v-if="row.confidence" size="small" :type="confidenceTag(row.confidence)">
                    {{ (row.confidence * 100).toFixed(0) }}%
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="userValue" label="用户确认值" min-width="200">
              <template #default="{ row }">
                <el-input
                  v-model="row.userValue"
                  size="small"
                  :placeholder="row.aiValue || '请输入...'"
                  clearable
                  @change="handleFieldChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="isConfirmed" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.isConfirmed"
                  active-value="1"
                  inactive-value="0"
                  size="small"
                  @change="handleFieldChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="sourceDocName" label="来源文档" width="150" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <!-- 参考文档面板 -->
      <div class="reference-panel">
        <div class="panel-header">
          <span class="panel-title">📚 参考文档（Vault）</span>
          <el-text type="info">以下文档被用于 AI 智能填写</el-text>
        </div>
        <div class="panel-body">
          <div v-if="references.length === 0" class="empty-references">
            <el-empty description="暂无参考文档" />
          </div>
          <div v-else class="reference-list">
            <div
              v-for="ref in references"
              :key="ref.refId"
              class="reference-card"
              :class="{ 'is-selected': ref.isSelected === '1' }"
              @click="toggleReference(ref)"
            >
              <div class="ref-icon">
                <svg-icon :icon-class="getFileIcon(ref.docType)" class-name="doc-icon" />
              </div>
              <div class="ref-info">
                <div class="ref-name">{{ ref.docName }}</div>
                <div class="ref-meta">
                  <el-tag size="small" :type="ref.relevance >= 0.8 ? 'success' : 'info'">
                    相关性 {{ (ref.relevance * 100).toFixed(0) }}%
                  </el-tag>
                </div>
              </div>
              <el-checkbox v-model="ref.isSelected" true-value="1" false-value="0" @click.stop />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty description="欢迎使用表格填写助手">
        <template #image>
          <el-icon :size="80" color="#409EFF"><Document /></el-icon>
        </template>
        <template #description>
          <p>上传空白表格，AI 自动识别字段并从 Vault 中提取信息填写</p>
        </template>
        <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
          上传表格文件
        </el-button>
      </el-empty>
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传空白表格" width="500px" destroy-on-close>
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="uploadForm.taskName" placeholder="留空将使用文件名作为任务名称" />
        </el-form-item>
        <el-form-item label="表格文件">
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".pdf,.docx,.xlsx,.doc,.xls"
          >
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、Excel 格式，单个文件不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FormAssistant">
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Document, MagicStick, Download, Refresh } from '@element-plus/icons-vue'
import { uploadForm, getFormTask, updateFormFields, generateForm, downloadForm } from '@/api/lingdoc/form'

// ========== 状态 ==========
const currentTask = ref(null)
const fields = ref([])
const references = ref([])
const showUploadDialog = ref(false)
const uploading = ref(false)
const generating = ref(false)
const uploadRef = ref(null)
const uploadForm = reactive({ taskName: '', file: null })
const originalFileUrl = ref('')
const filledFileUrl = ref('')

// ========== 计算属性 ==========
const statusText = computed(() => {
  const map = { '0': '待上传', '1': '识别中', '2': '待确认', '3': '已生成', '4': '失败' }
  return map[currentTask.value?.status] || '未知'
})

const statusTagType = computed(() => {
  const map = { '0': 'info', '1': 'warning', '2': 'primary', '3': 'success', '4': 'danger' }
  return map[currentTask.value?.status] || 'info'
})

const confirmedCount = computed(() => fields.value.filter(f => f.isConfirmed === '1').length)

const hasUnconfirmedFields = computed(() => {
  return fields.value.some(f => f.isConfirmed !== '1' && f.confidence >= 0.8)
})

const canGenerate = computed(() => {
  return currentTask.value && currentTask.value.status === '2' && confirmedCount.value > 0
})

const canDownload = computed(() => {
  return currentTask.value && currentTask.value.status === '3' && currentTask.value.filledFileUrl
})

// ========== 方法 ==========

function handleFileChange(file) {
  uploadForm.file = file.raw
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请删除后再选择')
}

async function submitUpload() {
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    if (uploadForm.taskName) {
      formData.append('taskName', uploadForm.taskName)
    }
    const res = await uploadForm(formData)
    if (res.code === 200) {
      ElMessage.success(res.msg || '上传成功')
      showUploadDialog.value = false
      // 加载任务详情
      await loadTaskDetail(res.data.taskId)
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败：' + e.message)
  } finally {
    uploading.value = false
    uploadForm.taskName = ''
    uploadForm.file = null
    uploadRef.value?.clearFiles()
  }
}

async function loadTaskDetail(taskId) {
  try {
    const res = await getFormTask(taskId)
    if (res.code === 200) {
      currentTask.value = res.data
      fields.value = res.fields || []
      references.value = res.references || []
      // 构建预览URL
      if (currentTask.value.originalFileUrl) {
        originalFileUrl.value = import.meta.env.VITE_APP_BASE_API + currentTask.value.originalFileUrl
      }
      if (currentTask.value.filledFileUrl) {
        filledFileUrl.value = import.meta.env.VITE_APP_BASE_API + currentTask.value.filledFileUrl
      }
    }
  } catch (e) {
    ElMessage.error('加载任务详情失败')
  }
}

function handleFieldChange(row) {
  // 用户修改字段时，如果输入了值则自动标记为已确认
  if (row.userValue && row.isConfirmed !== '1') {
    row.isConfirmed = '1'
  }
}

function confirmAllHighConfidence() {
  let count = 0
  fields.value.forEach(f => {
    if (f.isConfirmed !== '1' && f.confidence >= 0.8) {
      f.userValue = f.aiValue
      f.isConfirmed = '1'
      count++
    }
  })
  ElMessage.success(`已自动确认 ${count} 个高置信度字段`)
}

async function handleGenerate() {
  if (!currentTask.value) return
  // 先保存字段
  const changedFields = fields.value.filter(f => f.userValue !== undefined)
  if (changedFields.length > 0) {
    try {
      await updateFormFields(changedFields)
    } catch (e) {
      ElMessage.error('保存字段失败')
      return
    }
  }

  generating.value = true
  try {
    const res = await generateForm({ taskId: currentTask.value.taskId })
    if (res.code === 200) {
      ElMessage.success('文档生成成功')
      await loadTaskDetail(currentTask.value.taskId)
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成失败：' + e.message)
  } finally {
    generating.value = false
  }
}

async function handleDownload() {
  if (!currentTask.value) return
  try {
    const res = await downloadForm(currentTask.value.taskId)
    const blob = new Blob([res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = currentTask.value.filledFileName || '已填写表格.docx'
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

function handleSelectReferences() {
  ElMessage.info('参考文档选择功能开发中...')
}

function toggleReference(ref) {
  ref.isSelected = ref.isSelected === '1' ? '0' : '1'
}

function resetPage() {
  currentTask.value = null
  fields.value = []
  references.value = []
  originalFileUrl.value = ''
  filledFileUrl.value = ''
}

// ========== 工具函数 ==========
function fieldTypeTag(type) {
  const map = { text: '', date: 'warning', number: 'success', select: 'info', checkbox: 'danger' }
  return map[type] || ''
}

function fieldTypeText(type) {
  const map = { text: '文本', date: '日期', number: '数字', select: '选择', checkbox: '复选' }
  return map[type] || type
}

function confidenceTag(confidence) {
  if (confidence >= 0.9) return 'success'
  if (confidence >= 0.8) return 'warning'
  return 'danger'
}

function getFileIcon(type) {
  const map = { pdf: 'pdf', docx: 'word', doc: 'word', xlsx: 'excel', xls: 'excel', png: 'image', jpg: 'image' }
  return map[type?.toLowerCase()] || 'file'
}
</script>

<style scoped>
.form-assistant-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 20px 24px;
  gap: 16px;
}

/* 工具栏 */
.form-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 主内容区 */
.form-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* 预览区 */
.preview-area {
  display: flex;
  gap: 16px;
  height: 400px;
  flex-shrink: 0;
}

.preview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #f8f9fa;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.file-name {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-waiting {
  color: var(--el-color-warning);
}

.panel-body {
  flex: 1;
  min-height: 0;
  position: relative;
}

.doc-preview {
  width: 100%;
  height: 100%;
  border: none;
}

.empty-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

/* 字段编辑面板 */
.field-editor-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  flex-shrink: 0;
}

.field-editor-panel .panel-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ai-value-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 参考文档面板 */
.reference-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  flex-shrink: 0;
  max-height: 200px;
}

.reference-panel .panel-body {
  padding: 16px;
}

.reference-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.reference-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 200px;
}

.reference-card:hover {
  border-color: var(--el-color-primary);
  background: #f0f7ff;
}

.reference-card.is-selected {
  border-color: var(--el-color-success);
  background: #f0f9f0;
}

.ref-icon {
  font-size: 32px;
  color: var(--el-color-primary);
}

.ref-info {
  flex: 1;
  min-width: 0;
}

.ref-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ref-meta {
  display: flex;
  gap: 8px;
}

.empty-references {
  padding: 20px;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}
</style>
