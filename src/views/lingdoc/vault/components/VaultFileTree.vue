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
      @node-click="handleNodeClick"
      v-loading="loading"
    />
  </el-card>
</template>

<script setup>
const props = defineProps({
  treeData: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['node-click'])

function handleNodeClick(data) {
  emit('node-click', data)
}
</script>

<style scoped>
.tree-card {
  height: 100%;
}
.tree-card :deep(.el-card__body) {
  height: calc(100% - 40px);
  overflow: auto;
}
</style>
