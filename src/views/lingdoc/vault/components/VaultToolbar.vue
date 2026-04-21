<template>
  <div class="vault-toolbar">
    <el-button type="primary" icon="FolderAdd" @click="handleCreateFolder">新建文件夹</el-button>
    <el-button type="success" icon="Upload" @click="emit('upload')">上传文件</el-button>
    <el-button icon="Refresh" @click="emit('refresh')">刷新</el-button>
    <el-button icon="RefreshRight" @click="emit('sync')">同步Vault</el-button>
    <el-divider direction="vertical" />
    <el-input
      v-model="keyword"
      placeholder="搜索文件名或内容"
      style="width: 220px"
      clearable
      @keyup.enter="emit('search')"
    >
      <template #append>
        <el-button icon="Search" @click="emit('search')" />
      </template>
    </el-input>
    <el-divider direction="vertical" />
    <el-radio-group v-model="viewMode" size="small">
      <el-radio-button label="list">列表</el-radio-button>
      <el-radio-button label="icon">图标</el-radio-button>
    </el-radio-group>
    <el-divider direction="vertical" />
    <el-button icon="Setting" @click="emit('repo-manage')">仓库设置</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  viewMode: { type: String, default: 'list' },
  keyword: { type: String, default: '' }
})

const emit = defineEmits(['update:viewMode', 'update:keyword', 'search', 'refresh', 'sync', 'create-folder', 'repo-manage', 'upload'])

const viewMode = computed({
  get: () => props.viewMode,
  set: (val) => emit('update:viewMode', val)
})

const keyword = computed({
  get: () => props.keyword,
  set: (val) => emit('update:keyword', val)
})

async function handleCreateFolder() {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '文件夹名称不能为空'
    })
    emit('create-folder', value)
  } catch (e) {
    // cancel
  }
}
</script>

<style scoped>
.vault-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}
</style>
