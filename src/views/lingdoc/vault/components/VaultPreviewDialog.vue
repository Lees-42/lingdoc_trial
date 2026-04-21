<template>
  <el-dialog
    :title="fileName || '文件预览'"
    v-model="dialogVisible"
    width="90%"
    top="5vh"
    :close-on-click-modal="true"
    class="preview-dialog"
  >
    <div class="dialog-toolbar">
      <el-radio-group v-model="fontSize" size="small">
        <el-radio-button label="small">小</el-radio-button>
        <el-radio-button label="medium">中</el-radio-button>
        <el-radio-button label="large">大</el-radio-button>
      </el-radio-group>
      <el-switch
        v-model="wordWrap"
        active-text="自动换行"
        style="margin-left: 16px"
      />
      <el-input
        v-model="searchKeyword"
        placeholder="搜索关键词"
        style="width: 200px; margin-left: 16px"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="dialog-content" :style="contentStyle">
      <pre v-html="highlightedContent" />
    </div>

    <template #footer>
      <span style="color: #909399; font-size: 13px">
        {{ lineCount }} 行 | 编码: UTF-8
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  fileName: { type: String, default: '' },
  content: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const fontSize = ref('medium')
const wordWrap = ref(true)
const searchKeyword = ref('')

const fontSizeMap = { small: '12px', medium: '14px', large: '18px' }

const contentStyle = computed(() => ({
  fontSize: fontSizeMap[fontSize.value],
  whiteSpace: wordWrap.value ? 'pre-wrap' : 'pre'
}))

const lineCount = computed(() => {
  if (!props.content) return 0
  return props.content.split(/\r?\n/).length
})

const highlightedContent = computed(() => {
  if (!searchKeyword.value) {
    return escapeHtml(props.content)
  }
  const keyword = escapeHtml(searchKeyword.value)
  const regex = new RegExp(`(${keyword})`, 'gi')
  return escapeHtml(props.content).replace(regex, '<mark style="background:#ffeb3b">$1</mark>')
})

function escapeHtml(text) {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function handleSearch() {
  // 搜索高亮由 computed 自动处理
}

// ESC 关闭
watch(dialogVisible, (val) => {
  if (val) {
    const handler = (e) => {
      if (e.key === 'Escape') {
        dialogVisible.value = false
        document.removeEventListener('keydown', handler)
      }
    }
    document.addEventListener('keydown', handler)
  }
})
</script>

<style scoped>
.preview-dialog :deep(.el-dialog__body) {
  padding: 12px 20px;
  max-height: calc(80vh - 120px);
  display: flex;
  flex-direction: column;
}
.dialog-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}
.dialog-content {
  flex: 1;
  overflow: auto;
  background: #f8f9fa;
  border-radius: 4px;
  padding: 16px;
  font-family: 'Courier New', monospace;
  line-height: 1.6;
}
.dialog-content pre {
  margin: 0;
}
</style>
