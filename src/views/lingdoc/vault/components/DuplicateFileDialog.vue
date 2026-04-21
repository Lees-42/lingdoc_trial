<template>
  <el-dialog
    v-model="visible"
    title="重复文件处理"
    width="680px"
    :close-on-click-modal="false"
  >
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #title>
        发现 {{ duplicateData.length }} 组重复文件（内容完全相同）。<br>
        请勾选需要删除的文件，保留的文件将留在仓库中。
      </template>
    </el-alert>

    <div v-for="(group, idx) in duplicateData" :key="group.checksum" class="dup-group">
      <div class="dup-group-title">
        <el-tag size="small" type="info">组 {{ idx + 1 }}</el-tag>
        <span class="dup-count">{{ group.count }} 个文件</span>
      </div>
      <el-checkbox-group v-model="selectedMap[group.checksum]">
        <div v-for="file in group.files" :key="file.fileId" class="dup-file-item">
          <el-checkbox :label="file.fileId">
            <span class="dup-file-name">{{ file.fileName }}</span>
            <span class="dup-file-path">{{ file.vaultPath }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleKeepAll">全部保留</el-button>
        <el-button type="danger" :disabled="selectedFileIds.length === 0" @click="handleDeleteSelected">
          删除选中（{{ selectedFileIds.length }}）
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchDelVaultFile } from '@/api/lingdoc/vault'

const props = defineProps({
  modelValue: Boolean,
  duplicateData: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'deleted'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectedMap = ref({})

const selectedFileIds = computed(() => {
  const ids = []
  Object.values(selectedMap.value).forEach((arr) => {
    ids.push(...arr)
  })
  return ids
})

watch(
  () => props.duplicateData,
  (data) => {
    selectedMap.value = {}
    data.forEach((group) => {
      selectedMap.value[group.checksum] = []
    })
  },
  { immediate: true }
)

async function handleDeleteSelected() {
  const ids = selectedFileIds.value
  if (ids.length === 0) {
    ElMessage.warning('请先勾选要删除的文件')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个重复文件吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const fileIdsStr = ids.join(',')
    await batchDelVaultFile(fileIdsStr)
    ElMessage.success('删除成功')
    visible.value = false
    emit('deleted')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || e))
    }
  }
}

function handleKeepAll() {
  visible.value = false
}
</script>

<style scoped>
.dup-group {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
}

.dup-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
}

.dup-count {
  color: #909399;
  font-size: 12px;
}

.dup-file-item {
  padding: 4px 0;
}

.dup-file-name {
  font-weight: 500;
  margin-right: 8px;
}

.dup-file-path {
  color: #909399;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
