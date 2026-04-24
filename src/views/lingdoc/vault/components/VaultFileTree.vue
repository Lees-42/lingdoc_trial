<template>
  <el-card class="tree-card" shadow="never">
    <template #header>
      <span>目录树</span>
    </template>
    <el-tree
      :data="treeData"
      :props="{ label: 'label', children: 'children' }"
      :highlight-current="true"
      :expand-on-click-node="false"
      node-key="value"
      :default-expanded-keys="['/']"
      @node-click="handleNodeClick"
      v-loading="loading"
    >
      <template #default="{ node, data }">
        <el-dropdown
          trigger="contextmenu"
          @command="(cmd) => handleCommand(cmd, data)"
        >
          <div class="tree-node" @contextmenu.prevent>
            <span class="node-label">
              <el-icon><component :is="data.value === '/' ? House : Folder" /></el-icon>
              <span class="node-text">{{ node.label }}</span>
            </span>
            <span v-if="data.tags && data.tags.length" class="node-tags">
              <el-tag
                v-for="tag in data.tags.slice(0, 3)"
                :key="tag.tagId"
                size="small"
                :color="tag.tagColor"
                effect="dark"
                class="folder-tag"
              >{{ tag.tagName }}</el-tag>
              <el-tag
                v-if="data.tags.length > 3"
                size="small"
                type="info"
                class="folder-tag"
              >+{{ data.tags.length - 3 }}</el-tag>
            </span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" :disabled="data.value === '/'">
                <el-icon><Delete /></el-icon>
                <span style="margin-left: 4px">删除文件夹</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </el-tree>
  </el-card>
</template>

<script setup>
import { Folder, House, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  treeData: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['node-click', 'delete-folder'])

function handleNodeClick(data) {
  emit('node-click', data)
}

function handleCommand(command, data) {
  if (command === 'delete') {
    emit('delete-folder', data)
  }
}
</script>

<style scoped>
.tree-card {
  height: 45%;
  min-height: 160px;
  flex-shrink: 0;
  margin-bottom: 8px;
}
.tree-card :deep(.el-card__body) {
  height: calc(100% - 40px);
  overflow: auto;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}
.node-label {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  overflow: hidden;
}
.node-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 8px;
}
.folder-tag {
  border: none;
  color: #fff;
}
</style>
