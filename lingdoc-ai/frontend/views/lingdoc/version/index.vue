<template>
  <div class="app-container version-page">
    <!-- 页面标题 -->
    <div class="version-header">
      <h2 class="page-title">版本溯源</h2>
      <p class="page-desc">查看已归档文件的变更历史，随时回滚到任意历史版本。</p>
    </div>

    <!-- 左右分栏主体 -->
    <div class="version-body">
      <!-- 左侧：文件列表面板 -->
      <aside class="file-panel">
        <div class="file-panel__search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名或路径…"
            clearable
            :prefix-icon="Search"
          />
        </div>

        <div class="file-panel__stats">
          已归档文件（{{ filteredFiles.length }}）
        </div>

        <div class="file-panel__list">
          <div
            v-for="file in filteredFiles"
            :key="file.fileId"
            class="file-card"
            :class="{ active: selectedFileId === file.fileId }"
            @click="selectFile(file.fileId)"
          >
            <div class="file-card__icon">
              <svg-icon :icon-class="getFileIcon(file.fileType)" class="file-type-icon" />
            </div>
            <div class="file-card__info">
              <p class="file-card__name" :title="file.fileName">{{ file.fileName }}</p>
              <p class="file-card__meta">
                <el-tag type="success" size="small" effect="dark">v{{ file.currentVersion }}</el-tag>
                <span class="file-card__path">{{ file.archivePath }}</span>
              </p>
              <p class="file-card__time">{{ formatDateTime(file.archiveTime) }}</p>
            </div>
          </div>

          <el-empty v-if="filteredFiles.length === 0" description="未找到匹配文件">
            <template #description>
              <p>未找到匹配文件</p>
              <p class="empty-tip">尝试更换搜索关键词</p>
            </template>
          </el-empty>
        </div>
      </aside>

      <!-- 右侧：版本历史面板 -->
      <main class="history-panel">
        <template v-if="selectedFile">
          <!-- 文件信息摘要 -->
          <div class="history-panel__header">
            <div class="history-panel__file-info">
              <svg-icon :icon-class="getFileIcon(selectedFile.fileType)" class="history-file-icon" />
              <div>
                <h3 class="history-file-name">{{ selectedFile.fileName }}</h3>
                <p class="history-file-path">{{ selectedFile.archivePath }}</p>
              </div>
            </div>
            <div class="history-panel__badges">
              <el-tag type="success" effect="dark" size="large">
                当前版本 v{{ selectedFile.currentVersion }}
              </el-tag>
              <el-tag type="info" size="large">
                共 {{ selectedFile.versions.length }} 个版本
              </el-tag>
            </div>
          </div>

          <!-- 版本时间线 -->
          <div class="history-panel__timeline">
            <el-timeline>
              <el-timeline-item
                v-for="(ver, idx) in selectedFile.versions"
                :key="ver.versionId"
                :type="ver.isCurrent ? 'primary' : ''"
                :icon="ver.isCurrent ? Check : ''"
                :timestamp="formatDateTime(ver.operationTime)"
                placement="top"
              >
                <el-card
                  shadow="hover"
                  class="version-node"
                  :class="{ current: ver.isCurrent }"
                >
                  <div class="version-node__header">
                    <div class="version-node__title">
                      <el-tag
                        :type="ver.isCurrent ? 'success' : 'info'"
                        size="small"
                        effect="dark"
                      >
                        v{{ ver.versionNumber }}
                      </el-tag>
                      <el-tag
                        :type="getOpTypeTag(ver.operationType)"
                        size="small"
                        effect="plain"
                        class="op-tag"
                      >
                        {{ getOpTypeLabel(ver.operationType) }}
                      </el-tag>
                      <span v-if="ver.isCurrent" class="current-badge">当前版本</span>
                    </div>
                    <div class="version-node__actions">
                      <el-button
                        v-if="!ver.isCurrent"
                        type="warning"
                        size="small"
                        :icon="RefreshLeft"
                        @click="handleSetAsCurrent(ver)"
                      >设为此版本</el-button>
                      <el-button
                        link
                        type="primary"
                        size="small"
                        :icon="View"
                        @click="handlePreview(ver)"
                      >预览</el-button>
                    </div>
                  </div>

                  <div class="version-node__body">
                    <div class="version-detail-row">
                      <span class="version-detail__label">操作人</span>
                      <span class="version-detail__value">{{ ver.operator }}</span>
                    </div>
                    <div class="version-detail-row">
                      <span class="version-detail__label">文件大小</span>
                      <span class="version-detail__value">{{ formatFileSize(ver.fileSize) }}</span>
                    </div>
                    <div v-if="ver.remark" class="version-detail-row">
                      <span class="version-detail__label">备注</span>
                      <span class="version-detail__value remark">{{ ver.remark }}</span>
                    </div>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </template>

        <!-- 空状态：未选中文件 -->
        <el-empty v-else class="history-empty">
          <template #image>
            <el-icon :size="64" color="#c0c4cc"><Clock /></el-icon>
          </template>
          <template #description>
            <div class="empty-content">
              <p class="empty-title">选择文件查看版本历史</p>
              <p class="empty-tip">点击左侧已归档文件，即可查看其完整的版本变更时间线</p>
            </div>
          </template>
        </el-empty>
      </main>
    </div>

    <!-- 设为此版本确认弹窗 -->
    <el-dialog
      title="确认回滚"
      v-model="rollbackOpen"
      width="480px"
      append-to-body
    >
      <el-alert
        title="回滚后该版本将成为最新版本"
        description="系统将保留所有历史版本记录，并基于选定的版本内容生成一个新的版本记录（版本号+1），该新版本即为当前最新版本。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-descriptions v-if="rollbackTarget" :column="1" border>
        <el-descriptions-item label="文件">{{ selectedFile?.fileName }}</el-descriptions-item>
        <el-descriptions-item label="回滚目标">v{{ rollbackTarget.versionNumber }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ getOpTypeLabel(rollbackTarget.operationType) }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(rollbackTarget.operationTime) }}</el-descriptions-item>
        <el-descriptions-item label="新版本号">v{{ getNextVersionNumber(selectedFile?.currentVersion) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="warning" @click="confirmSetAsCurrent">确认回滚</el-button>
          <el-button @click="rollbackOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog
      :title="'预览 — v' + previewTarget?.versionNumber"
      v-model="previewOpen"
      width="560px"
      append-to-body
    >
      <div class="preview-box">
        <el-icon :size="48" color="#c0c4cc"><Document /></el-icon>
        <p class="preview-title">版本快照预览</p>
        <p class="preview-detail">文件：{{ selectedFile?.fileName }}</p>
        <p class="preview-detail">版本：v{{ previewTarget?.versionNumber }}</p>
        <p class="preview-detail">操作：{{ getOpTypeLabel(previewTarget?.operationType) }}</p>
        <p class="preview-detail">时间：{{ formatDateTime(previewTarget?.operationTime) }}</p>
        <p class="preview-detail">大小：{{ formatFileSize(previewTarget?.fileSize) }}</p>
        <el-tag v-if="previewTarget?.isCurrent" type="success" effect="dark" style="margin-top: 12px;">当前版本</el-tag>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="previewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Version">
import { ref, computed } from 'vue'
import { Search, Check, RefreshLeft, View, Clock, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// ========== 搜索与选中 ==========
const searchKeyword = ref('')
const selectedFileId = ref('')
const rollbackOpen = ref(false)
const previewOpen = ref(false)
const rollbackTarget = ref(null)
const previewTarget = ref(null)

// ========== 操作类型枚举 ==========
const opTypeMap = {
  create: { label: '创建', tag: 'success' },
  edit: { label: '编辑', tag: 'primary' },
  rename: { label: '重命名', tag: 'info' },
  move: { label: '移动', tag: 'info' },
  rollback: { label: '回滚恢复', tag: 'warning' }
}

function getOpTypeLabel(type) {
  return opTypeMap[type]?.label || type
}

function getOpTypeTag(type) {
  return opTypeMap[type]?.tag || 'info'
}

// ========== 工具函数 ==========
function getFileIcon(fileType) {
  const iconMap = {
    pdf: 'pdf', doc: 'word', docx: 'word',
    xls: 'excel', xlsx: 'excel',
    png: 'image', jpg: 'image', jpeg: 'image',
    txt: 'txt', md: 'txt'
  }
  return iconMap[fileType?.toLowerCase()] || 'file'
}

function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

function formatDateTime(ts) {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getNextVersionNumber(current) {
  const num = parseInt(current, 10)
  return String(num + 1).padStart(3, '0')
}

// ========== 生成 Mock 版本历史 ==========
function generateVersions(baseSize, count, basePath, fileName) {
  const versions = []
  const now = Date.now()
  const opTypes = ['create', 'edit', 'edit', 'rename', 'edit', 'move']

  for (let i = 1; i <= count; i++) {
    const timeOffset = (count - i) * 86400000 * (1 + Math.floor(Math.random() * 2))
    const sizeVariation = Math.floor((Math.random() - 0.3) * baseSize * 0.15)
    const opType = i === 1 ? 'create' : opTypes[Math.floor(Math.random() * opTypes.length)]

    versions.push({
      versionId: 'ver_' + Math.random().toString(36).substr(2, 10),
      versionNumber: String(i).padStart(3, '0'),
      operationType: opType,
      operationTime: now - timeOffset,
      operator: 'admin',
      fileSize: Math.max(1024, baseSize + sizeVariation),
      remark: i === 1 ? '初始创建' : (opType === 'rename' ? `重命名为 ${fileName}` : opType === 'move' ? `移动到 ${basePath}` : ''),
      isCurrent: i === count
    })
  }

  // 时间倒序，最新的在前
  return versions.sort((a, b) => b.operationTime - a.operationTime)
}

// ========== 预置 Mock 文件数据 ==========
const rawFiles = [
  {
    fileId: 'f001',
    fileName: '实验报告要求_20260501.pdf',
    fileType: 'pdf',
    fileSize: 1024 * 256,
    archivePath: '/学习资料/大三下/计算机网络',
    archiveTime: Date.now() - 86400000 * 15,
    versionCount: 5
  },
  {
    fileId: 'f002',
    fileName: '国家奖学金申请表_20260901.docx',
    fileType: 'docx',
    fileSize: 1024 * 128,
    archivePath: '/申请材料/奖学金',
    archiveTime: Date.now() - 86400000 * 30,
    versionCount: 6
  },
  {
    fileId: 'f003',
    fileName: '个人简历_2026版.docx',
    fileType: 'docx',
    fileSize: 1024 * 64,
    archivePath: '/工作文档/实习材料',
    archiveTime: Date.now() - 86400000 * 45,
    versionCount: 4
  },
  {
    fileId: 'f004',
    fileName: 'OS大作业要求_20260315.pdf',
    fileType: 'pdf',
    fileSize: 1024 * 512,
    archivePath: '/学习资料/大三下/操作系统',
    archiveTime: Date.now() - 86400000 * 60,
    versionCount: 5
  },
  {
    fileId: 'f005',
    fileName: '课程论文初稿_20260410.docx',
    fileType: 'docx',
    fileSize: 1024 * 320,
    archivePath: '/学习资料/大三下/文献综述',
    archiveTime: Date.now() - 86400000 * 8,
    versionCount: 4
  },
  {
    fileId: 'f006',
    fileName: '社团活动策划书_20260520.docx',
    fileType: 'docx',
    fileSize: 1024 * 180,
    archivePath: '/申请材料/社团',
    archiveTime: Date.now() - 86400000 * 3,
    versionCount: 5
  },
  {
    fileId: 'f007',
    fileName: '成绩单_大三下.pdf',
    fileType: 'pdf',
    fileSize: 1024 * 1024,
    archivePath: '/申请材料/成绩单',
    archiveTime: Date.now() - 86400000 * 20,
    versionCount: 4
  },
  {
    fileId: 'f008',
    fileName: '实习证明_2026.pdf',
    fileType: 'pdf',
    fileSize: 1024 * 200,
    archivePath: '/工作文档/实习材料',
    archiveTime: Date.now() - 86400000 * 50,
    versionCount: 3
  }
]

const fileList = ref(rawFiles.map(f => ({
  ...f,
  currentVersion: String(f.versionCount).padStart(3, '0'),
  versions: generateVersions(f.fileSize, f.versionCount, f.archivePath, f.fileName)
})))

// ========== 计算属性 ==========
const filteredFiles = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return fileList.value
  return fileList.value.filter(f =>
    f.fileName.toLowerCase().includes(kw) ||
    f.archivePath.toLowerCase().includes(kw)
  )
})

const selectedFile = computed(() => {
  return fileList.value.find(f => f.fileId === selectedFileId.value) || null
})

// ========== 交互方法 ==========
function selectFile(fileId) {
  selectedFileId.value = fileId
}

function handleSetAsCurrent(ver) {
  rollbackTarget.value = ver
  rollbackOpen.value = true
}

function confirmSetAsCurrent() {
  if (!selectedFile.value || !rollbackTarget.value) return

  const file = selectedFile.value
  const targetVer = rollbackTarget.value
  const newVersionNumber = getNextVersionNumber(file.currentVersion)

  // 生成新版本（基于回滚目标的内容）
  const newVersion = {
    versionId: 'ver_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    versionNumber: newVersionNumber,
    operationType: 'rollback',
    operationTime: Date.now(),
    operator: 'admin',
    fileSize: targetVer.fileSize,
    remark: `从 v${targetVer.versionNumber} 恢复`,
    isCurrent: true
  }

  // 更新文件数据：旧版本全部取消 current，新版本插入到最前面
  const fileIdx = fileList.value.findIndex(f => f.fileId === file.fileId)
  if (fileIdx !== -1) {
    const updatedVersions = fileList.value[fileIdx].versions.map(v => ({
      ...v,
      isCurrent: false
    }))
    updatedVersions.unshift(newVersion)

    fileList.value[fileIdx] = {
      ...fileList.value[fileIdx],
      currentVersion: newVersionNumber,
      versionCount: fileList.value[fileIdx].versionCount + 1,
      versions: updatedVersions
    }
  }

  rollbackOpen.value = false
  ElMessage.success(`已回滚到 v${targetVer.versionNumber}，新版本号为 v${newVersionNumber}`)
}

function handlePreview(ver) {
  previewTarget.value = ver
  previewOpen.value = true
}
</script>

<style scoped>
.version-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 20px 24px;
  overflow: hidden;
}

.version-header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

/* ========== 左右分栏主体 ========== */
.version-body {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 16px;
}

/* ========== 左侧文件面板 ========== */
.file-panel {
  display: flex;
  flex-direction: column;
  width: 340px;
  min-width: 340px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: #fafbfc;
  overflow: hidden;
}

.file-panel__search {
  padding: 12px 12px 8px;
}

.file-panel__stats {
  padding: 0 12px 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.file-panel__list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

/* 文件卡片 */
.file-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  margin-bottom: 6px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.file-card:hover {
  border-color: var(--el-color-primary-light-7);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.file-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.file-card__icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.file-type-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.file-card__info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.file-card__name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.file-card__path {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-card__time {
  margin: 0;
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.empty-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

/* ========== 右侧历史面板 ========== */
.history-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  background: #fff;
  overflow: hidden;
}

.history-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fafbfc;
}

.history-panel__file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-file-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.history-file-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.history-file-path {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.history-panel__badges {
  display: flex;
  gap: 8px;
}

.history-panel__timeline {
  flex: 1;
  overflow-y: auto;
  padding: 20px 20px 20px 16px;
}

/* 版本节点卡片 */
.version-node {
  transition: all 0.2s;
}

.version-node.current {
  border-color: var(--el-color-success-light-5);
  background: linear-gradient(135deg, #f0f9eb 0%, #f6ffed 100%);
}

.version-node :deep(.el-card__body) {
  padding: 14px 16px;
}

.version-node__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.version-node__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.op-tag {
  margin-left: 2px;
}

.current-badge {
  font-size: 12px;
  color: var(--el-color-success);
  font-weight: 600;
}

.version-node__actions {
  display: flex;
  gap: 8px;
}

.version-node__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-detail-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
}

.version-detail__label {
  color: var(--el-text-color-secondary);
  min-width: 56px;
}

.version-detail__value {
  color: var(--el-text-color-primary);
}

.version-detail__value.remark {
  font-style: italic;
  color: var(--el-text-color-secondary);
}

/* 空状态 */
.history-empty :deep(.el-empty__description) {
  margin-top: 16px;
}

.empty-content {
  text-align: center;
}

.empty-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* 弹窗 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.preview-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  border-radius: 8px;
  background: #f5f7fa;
  text-align: center;
}

.preview-title {
  margin: 12px 0 8px;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.preview-detail {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .version-body {
    flex-direction: column;
  }

  .file-panel {
    width: 100%;
    min-width: auto;
    max-height: 40%;
  }
}
</style>
