<template>
  <el-card class="preview-card" shadow="never">
    <template #header>
      <span>预览 / 详情</span>
      <el-button
        v-if="currentFile"
        type="primary"
        size="small"
        icon="ZoomIn"
        style="float: right"
        @click="emit('enlarge')"
      >
        放大预览
      </el-button>
    </template>

    <div v-if="!currentFile" class="empty-preview">
      <el-empty description="请选择文件以预览" />
    </div>

    <div v-else class="preview-body">
      <!-- 文本预览 -->
      <div v-if="isTextFile" class="text-preview">
        <div class="line-numbers">
          <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
        </div>
        <pre class="code-content">{{ previewContent }}</pre>
      </div>

      <!-- 非文本文件详情 -->
      <div v-else class="file-info">
        <el-icon :size="64"><Document /></el-icon>
        <div class="info-item"><label>文件名：</label>{{ currentFile.fileName }}</div>
        <div class="info-item"><label>类型：</label>{{ currentFile.fileType }}</div>
        <div class="info-item"><label>大小：</label>{{ formatSize(currentFile.fileSize) }}</div>
      </div>

      <!-- 元数据 -->
      <el-divider />
      <div class="meta-info">
        <div class="info-item"><label>路径：</label>{{ currentFile.vaultPath }}</div>
        <div class="info-item"><label>来源：</label>{{ sourceTypeName(currentFile.sourceType) }}</div>
        <div class="info-item"><label>创建时间：</label>{{ currentFile.createTime }}</div>
        <div class="info-item"><label>更新时间：</label>{{ currentFile.updateTime }}</div>
        <div class="info-item"><label>Checksum：</label>{{ currentFile.checksum?.substring(0, 8) }}...</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentFile: { type: Object, default: null },
  previewContent: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['enlarge'])

const textTypes = ['txt', 'md', 'csv', 'json', 'xml', 'yaml', 'yml', 'html', 'htm', 'js', 'css', 'java', 'py', 'c', 'cpp', 'h', 'sql']

const isTextFile = computed(() => {
  if (!props.currentFile) return false
  return textTypes.includes(props.currentFile.fileType?.toLowerCase())
})

const lineCount = computed(() => {
  if (!props.previewContent) return 0
  return props.previewContent.split(/\r?\n/).length
})

function formatSize(size) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let value = size
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return value.toFixed(2) + ' ' + units[index]
}

function sourceTypeName(type) {
  const map = { '0': '手动上传', '1': '自动规整', '2': '表格助手生成' }
  return map[type] || type
}
</script>

<style scoped>
.preview-card {
  height: 100%;
}
.preview-card :deep(.el-card__body) {
  height: calc(100% - 40px);
  overflow: auto;
}
.empty-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.preview-body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.text-preview {
  flex: 1;
  overflow: auto;
  display: flex;
  background: #f8f9fa;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}
.line-numbers {
  padding: 8px 12px;
  background: #e9ecef;
  color: #868e96;
  text-align: right;
  user-select: none;
  border-right: 1px solid #dee2e6;
}
.line-number {
  min-width: 24px;
}
.code-content {
  flex: 1;
  padding: 8px 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
}
.file-info {
  text-align: center;
  padding: 24px;
}
.meta-info {
  padding: 8px 0;
}
.info-item {
  margin: 6px 0;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}
.info-item label {
  color: #909399;
  font-weight: 500;
}
</style>
