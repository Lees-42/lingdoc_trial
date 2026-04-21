<template>
  <el-dialog
    title="选择保存目录"
    v-model="dialogVisible"
    width="480px"
    append-to-body
    destroy-on-close
  >
    <div class="picker-body">
      <el-tree
        ref="treeRef"
        :data="vaultTree"
        node-key="path"
        highlight-current
        :expand-on-click-node="false"
        :default-expanded-keys="expandedKeys"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="tree-node" :class="{ 'is-selected': selectedPath === data.path }">
            <el-icon class="node-icon"><Folder /></el-icon>
            <span class="node-name">{{ data.name }}</span>
            <div v-if="data.tags?.length" class="node-tags">
              <el-tag
                v-for="tag in data.tags"
                :key="tag.tagId"
                size="small"
                :color="tag.tagColor"
                effect="dark"
                class="node-tag"
              >{{ tag.tagName }}</el-tag>
            </div>
          </div>
        </template>
      </el-tree>
    </div>

    <div class="picker-footer-bar">
      <div class="selected-path">
        <span class="path-label">选中路径：</span>
        <span class="path-value">{{ selectedPath || '未选择' }}</span>
      </div>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取 消</el-button>
        <el-button type="primary" @click="handleConfirm">确 认</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Folder } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  defaultPath: {
    type: String,
    default: ''
  },
  folderTagMap: {
    type: Object,
    default: () => ({})
  },
  treeData: {
    type: Array,
    default: () => null
  }
})

const emit = defineEmits(['update:visible', 'confirm'])

const dialogVisible = ref(props.visible)
const treeRef = ref(null)
const selectedPath = ref(props.defaultPath || '')
const expandedKeys = ref([])

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    selectedPath.value = props.defaultPath || ''
    nextTick(() => {
      if (treeRef.value && selectedPath.value) {
        treeRef.value.setCurrentKey(selectedPath.value)
      }
    })
  }
})

watch(dialogVisible, (val) => {
  if (!val) {
    emit('update:visible', false)
  }
})

// Mock 目录树数据
const mockTreeData = [
  {
    name: '学习资料',
    path: '/学习资料',
    children: [
      {
        name: '大三上',
        path: '/学习资料/大三上',
        children: [
          { name: '操作系统', path: '/学习资料/大三上/操作系统', children: [] },
          { name: '计算机网络', path: '/学习资料/大三上/计算机网络', children: [] }
        ]
      },
      {
        name: '大三下',
        path: '/学习资料/大三下',
        children: []
      }
    ]
  },
  {
    name: '申请材料',
    path: '/申请材料',
    children: [
      { name: '奖学金', path: '/申请材料/奖学金', children: [] },
      { name: '实习', path: '/申请材料/实习', children: [] }
    ]
  },
  {
    name: '实验报告',
    path: '/实验报告',
    children: [
      { name: '计算机组成原理', path: '/实验报告/计算机组成原理', children: [] }
    ]
  },
  {
    name: '文献资料',
    path: '/文献资料',
    children: []
  },
  {
    name: '工作文档',
    path: '/工作文档',
    children: [
      { name: '实习材料', path: '/工作文档/实习材料', children: [] }
    ]
  },
  {
    name: '图片素材',
    path: '/图片素材',
    children: [
      { name: '截图', path: '/图片素材/截图', children: [] },
      { name: '扫描件', path: '/图片素材/扫描件', children: [] },
      { name: '证书', path: '/图片素材/证书', children: [] }
    ]
  }
]

// 目录树数据：优先使用外部传入的真实数据
const vaultTree = ref([...mockTreeData])

watch(() => props.treeData, (val) => {
  vaultTree.value = val === null || val === undefined ? [...mockTreeData] : val
}, { immediate: true })

function injectTags(nodes) {
  return nodes.map(node => ({
    ...node,
    tags: props.folderTagMap[node.path] || [],
    children: injectTags(node.children || [])
  }))
}

const treeData = ref(injectTags(vaultTree.value))

watch([() => props.folderTagMap, vaultTree], () => {
  treeData.value = injectTags(vaultTree.value)
}, { deep: true })

function initExpandedKeys() {
  if (!props.defaultPath) {
    expandedKeys.value = []
    return
  }
  const keys = []
  let path = props.defaultPath
  while (path && path !== '/') {
    keys.push(path)
    const lastSlash = path.lastIndexOf('/')
    path = lastSlash > 0 ? path.slice(0, lastSlash) : '/'
  }
  expandedKeys.value = keys
}

watch(() => props.visible, (val) => {
  if (val) {
    initExpandedKeys()
  }
})

function handleNodeClick(data) {
  selectedPath.value = data.path
}

function handleConfirm() {
  if (!selectedPath.value) {
    ElMessage.warning('请选择保存目录')
    return
  }
  emit('confirm', selectedPath.value)
  dialogVisible.value = false
}

function handleCancel() {
  dialogVisible.value = false
}
</script>

<style scoped>
.picker-body {
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  padding: 2px 0;
}

.node-icon {
  font-size: 16px;
  color: var(--el-color-primary);
  flex-shrink: 0;
}

.node-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.node-tags {
  display: flex;
  gap: 4px;
  margin-left: auto;
  flex-wrap: wrap;
}

.node-tag {
  border: none;
  font-size: 11px;
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
}

.picker-footer-bar {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.selected-path {
  margin-bottom: 12px;
  font-size: 13px;
}

.path-label {
  color: var(--el-text-color-secondary);
}

.path-value {
  color: var(--el-color-primary);
  font-weight: 500;
  word-break: break-all;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-tree-node__content) {
  height: 36px;
  padding-right: 8px;
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
}
</style>
