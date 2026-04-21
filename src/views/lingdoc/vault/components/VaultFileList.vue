<template>
  <el-card class="list-card" shadow="never">
    <template #header>
      <span>文件列表</span>
      <span v-if="total > 0" style="float: right; color: #909399; font-size: 13px">
        共 {{ total }} 个文件
      </span>
    </template>

    <!-- 列表视图 -->
    <el-table
      v-if="viewMode === 'list'"
      :data="fileList"
      v-loading="loading"
      highlight-current-row
      @row-click="(row) => emit('row-click', row)"
      @selection-change="(sel) => emit('selection-change', sel)"
      style="width: 100%"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column prop="fileName" label="名称" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <el-icon><Document /></el-icon>
          <span style="margin-left: 4px">{{ row.fileName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fileType" label="类型" width="70" />
      <el-table-column prop="fileSize" label="大小" width="80">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="修改日期" width="150" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Download" @click.stop="emit('download', row)" />
          <el-button link type="primary" icon="Edit" @click.stop="emit('rename', row)" />
          <el-button link type="danger" icon="Delete" @click.stop="emit('delete', row)" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 图标视图 -->
    <div v-else class="icon-view" v-loading="loading">
      <div
        v-for="file in fileList"
        :key="file.fileId"
        class="icon-item"
        :class="{ active: selectedFiles.some(f => f.fileId === file.fileId) }"
        @click="emit('row-click', file)"
      >
        <el-icon :size="40"><Document /></el-icon>
        <div class="icon-name" :title="file.fileName">{{ file.fileName }}</div>
        <div class="icon-size">{{ formatSize(file.fileSize) }}</div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      :current-page="queryParams.pageNum"
      :page-size="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @size-change="(size) => emit('page-change', queryParams.pageNum, size)"
      @current-change="(page) => emit('page-change', page, queryParams.pageSize)"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  fileList: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  viewMode: { type: String, default: 'list' },
  selectedFiles: { type: Array, default: () => [] }
})

const emit = defineEmits(['page-change', 'row-click', 'selection-change', 'rename', 'move', 'delete', 'download'])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
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
</script>

<style scoped>
.list-card {
  height: 100%;
}
.list-card :deep(.el-card__body) {
  height: calc(100% - 40px);
  overflow: auto;
  display: flex;
  flex-direction: column;
}
.icon-view {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px;
}
.icon-item {
  width: 100px;
  text-align: center;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.icon-item:hover,
.icon-item.active {
  background: #f0f9ff;
}
.icon-name {
  margin-top: 8px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.icon-size {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}
</style>
