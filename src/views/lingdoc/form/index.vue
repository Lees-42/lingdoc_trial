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
        <el-button type="warning" :icon="View" @click="loadDemoData">
          加载演示
        </el-button>
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

      <!-- 字段编辑面板（可折叠） -->
      <div class="field-editor-panel">
        <div class="panel-header" style="cursor: pointer;" @click="isFieldPanelCollapsed = !isFieldPanelCollapsed">
          <span class="panel-title">📝 字段确认</span>
          <div class="panel-actions">
            <el-text type="info">共 {{ fields.length }} 个字段，已确认 {{ confirmedCount }} 个</el-text>
            <el-button type="primary" size="small" :disabled="!hasUnconfirmedFields" @click.stop="confirmAllHighConfidence">
              一键确认高置信度字段（≥80%）
            </el-button>
            <el-icon :size="16" class="collapse-icon" :class="{ 'is-collapsed': isFieldPanelCollapsed }">
              <ArrowDown v-if="!isFieldPanelCollapsed" />
              <ArrowUp v-else />
            </el-icon>
          </div>
        </div>
        <div v-show="!isFieldPanelCollapsed" class="panel-body">
          <el-table
            :data="fields"
            border
            size="small"
            style="width: 100%"
            :row-class-name="rowClassName"
          >
            <el-table-column type="index" width="50" align="center" />
            <el-table-column prop="fieldName" label="字段名称" width="120" />
            <el-table-column prop="fieldType" label="字段类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="fieldTypeTag(row.fieldType)">
                  {{ fieldTypeText(row.fieldType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="aiValue" label="AI 建议值" min-width="140">
              <template #default="{ row }">
                <span>{{ row.aiValue || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.confidence !== undefined && row.confidence !== null" size="small" :type="confidenceTag(row.confidence)">
                  {{ (row.confidence * 100).toFixed(0) }}%
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="userValue" label="用户确认" min-width="220">
              <template #default="{ row }">
                <div class="user-value-cell">
                  <el-input
                    v-model="row.userValue"
                    size="small"
                    :placeholder="row.aiValue || '请输入...'"
                    clearable
                    @change="handleFieldChange(row)"
                  />
                  <el-tooltip :content="row.isConfirmed === '1' ? '已确认' : '点击确认'" placement="top">
                    <el-button
                      text
                      circle
                      size="small"
                      :type="row.isConfirmed === '1' ? 'success' : 'info'"
                      @click="toggleFieldConfirm(row)"
                    >
                      <el-icon><Check /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sourceDocName" label="来源文档" width="140" show-overflow-tooltip />
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
        <el-divider />
        <el-button type="warning" :icon="View" @click="loadDemoData">
          加载演示数据
        </el-button>
      </el-empty>
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传空白表格" width="500px" destroy-on-close>
      <el-form :model="uploadData" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="uploadData.taskName" placeholder="留空将使用文件名作为任务名称" />
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
import { Upload, Document, MagicStick, Download, Refresh, Check, ArrowDown, ArrowUp, View } from '@element-plus/icons-vue'
import { uploadForm, getFormTask, updateFormFields, generateForm, downloadForm } from '@/api/lingdoc/form'

// ========== 状态 ==========
const currentTask = ref(null)
const fields = ref([])
const references = ref([])
const showUploadDialog = ref(false)
const uploading = ref(false)
const generating = ref(false)
const uploadRef = ref(null)
const uploadData = reactive({ taskName: '', file: null })
const originalFileUrl = ref('')
const filledFileUrl = ref('')
const isFieldPanelCollapsed = ref(false)

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
  uploadData.file = file.raw
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请删除后再选择')
}

async function submitUpload() {
  if (!uploadData.file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadData.file)
    if (uploadData.taskName) {
      formData.append('taskName', uploadData.taskName)
    }
    const res = await uploadForm(formData)
    if (res.code === 200) {
      ElMessage.success(res.msg || '上传成功')
      showUploadDialog.value = false
      // 加载任务详情
      await loadTaskDetail(res.taskId)
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败：' + e.message)
  } finally {
    uploading.value = false
    uploadData.taskName = ''
    uploadData.file = null
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
  const changedFields = fields.value
    .filter(f => f.userValue !== undefined && f.userValue !== null && f.userValue !== '')
    .map(f => ({
      fieldId: f.fieldId,
      userValue: f.userValue,
      isConfirmed: f.isConfirmed
    }))
  if (changedFields.length > 0) {
    try {
      await updateFormFields({
        taskId: currentTask.value.taskId,
        fields: changedFields
      })
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
    const blob = new Blob([res], { type: res.type || 'application/octet-stream' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = currentTask.value.filledFileName || '已填写表格.docx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

function loadDemoData() {
  // 模拟任务数据
  const taskId = 'demo_task_' + Date.now()
  currentTask.value = {
    taskId: taskId,
    taskName: '国家奖学金申请表（演示）',
    originalFileName: '国家奖学金申请表.html',
    originalFileUrl: '/国家奖学金申请表.html',
    filledFileName: '国家奖学金申请表_已填写_20260419.html',
    filledFileUrl: '/国家奖学金申请表_已填写.html',
    status: '2',
    statusName: '待确认',
    fieldCount: 14,
    confirmedCount: 0,
    tokenCost: 850,
    errorMsg: null,
    createTime: '2026-04-19 10:30:00'
  }

  // 模拟字段识别结果（严格对照产品需求文档示例）
  fields.value = [
    { fieldId: 'field_001', fieldName: '姓名', fieldType: 'text', aiValue: '张三', userValue: '', isConfirmed: '0', confidence: 0.92, sourceDocName: '个人简历.docx', sortOrder: 1 },
    { fieldId: 'field_002', fieldName: '性别', fieldType: 'text', aiValue: '男', userValue: '', isConfirmed: '0', confidence: 0.88, sourceDocName: '学生证.pdf', sortOrder: 2 },
    { fieldId: 'field_003', fieldName: '出生年月', fieldType: 'date', aiValue: '2002-03-15', userValue: '', isConfirmed: '0', confidence: 0.90, sourceDocName: '身份证.pdf', sortOrder: 3 },
    { fieldId: 'field_004', fieldName: '民族', fieldType: 'text', aiValue: '汉族', userValue: '', isConfirmed: '0', confidence: 0.85, sourceDocName: '身份证.pdf', sortOrder: 4 },
    { fieldId: 'field_005', fieldName: '政治面貌', fieldType: 'text', aiValue: '共青团员', userValue: '', isConfirmed: '0', confidence: 0.72, sourceDocName: null, sortOrder: 5 },
    { fieldId: 'field_006', fieldName: '学号', fieldType: 'text', aiValue: '2023001001', userValue: '', isConfirmed: '0', confidence: 0.95, sourceDocName: '学生证.pdf', sortOrder: 6 },
    { fieldId: 'field_007', fieldName: '院系', fieldType: 'text', aiValue: '计算机科学与技术学院', userValue: '', isConfirmed: '0', confidence: 0.93, sourceDocName: '学生证.pdf', sortOrder: 7 },
    { fieldId: 'field_008', fieldName: '专业', fieldType: 'text', aiValue: '软件工程', userValue: '', isConfirmed: '0', confidence: 0.91, sourceDocName: '个人简历.docx', sortOrder: 8 },
    { fieldId: 'field_009', fieldName: '年级', fieldType: 'text', aiValue: '2023级', userValue: '', isConfirmed: '0', confidence: 0.89, sourceDocName: '学生证.pdf', sortOrder: 9 },
    { fieldId: 'field_010', fieldName: '入学时间', fieldType: 'date', aiValue: '2023-09-01', userValue: '', isConfirmed: '0', confidence: 0.87, sourceDocName: '学生证.pdf', sortOrder: 10 },
    { fieldId: 'field_011', fieldName: '必修课成绩', fieldType: 'number', aiValue: '92.5', userValue: '', isConfirmed: '0', confidence: 0.94, sourceDocName: '成绩单.pdf', sortOrder: 11 },
    { fieldId: 'field_012', fieldName: '综合排名', fieldType: 'text', aiValue: '3/120', userValue: '', isConfirmed: '0', confidence: 0.91, sourceDocName: '成绩单.pdf', sortOrder: 12 },
    { fieldId: 'field_013', fieldName: '英语水平', fieldType: 'text', aiValue: 'CET-6（568分）', userValue: '', isConfirmed: '0', confidence: 0.86, sourceDocName: '个人简历.docx', sortOrder: 13 },
    { fieldId: 'field_014', fieldName: '申请日期', fieldType: 'date', aiValue: '2026-04-19', userValue: '', isConfirmed: '0', confidence: 0.65, sourceDocName: null, sortOrder: 14 }
  ]

  // 模拟参考文档（Vault）
  references.value = [
    { refId: 'ref_001', docName: '个人简历.docx', docType: 'docx', relevance: 0.95, isSelected: '1' },
    { refId: 'ref_002', docName: '成绩单.pdf', docType: 'pdf', relevance: 0.92, isSelected: '1' },
    { refId: 'ref_003', docName: '获奖证书.pdf', docType: 'pdf', relevance: 0.88, isSelected: '1' },
    { refId: 'ref_004', docName: '学生证.pdf', docType: 'pdf', relevance: 0.85, isSelected: '1' },
    { refId: 'ref_005', docName: '身份证.pdf', docType: 'pdf', relevance: 0.90, isSelected: '0' },
    { refId: 'ref_006', docName: '劳动合同.pdf', docType: 'pdf', relevance: 0.78, isSelected: '0' }
  ]

  // 设置预览 URL（直接指向 public 目录下的演示文件）
  const baseUrl = window.location.origin
  originalFileUrl.value = baseUrl + '/国家奖学金申请表.html'
  filledFileUrl.value = baseUrl + '/国家奖学金申请表_已填写.html'

  isFieldPanelCollapsed.value = false
  ElMessage.success('演示数据已加载，共识别 14 个字段，其中 2 个低置信度字段已标红提醒')
}

function handleSelectReferences() {
  ElMessage.info('参考文档选择功能开发中...')
}

function toggleFieldConfirm(row) {
  row.isConfirmed = row.isConfirmed === '1' ? '0' : '1'
  if (row.isConfirmed === '1' && !row.userValue && row.aiValue) {
    row.userValue = row.aiValue
  }
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

function rowClassName({ row }) {
  if (row.confidence !== undefined && row.confidence !== null && row.confidence < 0.8) {
    return 'low-confidence-row'
  }
  return ''
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
  height: 80vh;
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

.user-value-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-value-cell .el-input {
  flex: 1;
}

.collapse-icon {
  transition: transform 0.2s;
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

/* 低置信度字段行标红提醒 */
:deep(.low-confidence-row) {
  background-color: var(--el-color-danger-light-9) !important;
}

:deep(.low-confidence-row:hover > td) {
  background-color: var(--el-color-danger-light-8) !important;
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
