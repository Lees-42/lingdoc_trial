<template>
  <el-dialog
    v-model="visible"
    title="仓库管理"
    width="680px"
    :close-on-click-modal="false"
    :show-close="!props.forceCreate"
    :close-on-press-escape="!props.forceCreate"
  >
    <div class="repo-manager">
      <!-- 仓库列表 -->
      <div class="repo-section">
        <h4>我的仓库</h4>
        <el-table :data="repoList" size="small" border style="margin-bottom: 16px">
          <el-table-column prop="repoName" label="仓库名称" width="140" />
          <el-table-column prop="repoPath" label="路径" show-overflow-tooltip />
          <el-table-column label="默认" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault === '1'" type="success" size="small">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.isDefault !== '1'"
                link
                type="primary"
                size="small"
                @click="handleSetDefault(row)"
              >设为默认</el-button>
              <el-button
                link
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-divider />

      <!-- 创建新仓库 -->
      <div class="repo-section">
        <h4>创建新仓库</h4>
        <p class="repo-tip">添加一个新的仓库路径，不会迁移旧仓库文件。</p>
        <el-form :model="createForm" label-width="100px">
          <el-form-item label="仓库路径" required>
            <el-input
              v-model="createForm.repoPath"
              placeholder="例如：D:/MyVault 或 /home/user/MyVault"
            />
          </el-form-item>
          <el-form-item label="仓库名称">
            <el-input v-model="createForm.repoName" placeholder="默认仓库" />
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建仓库</el-button>
      </div>

      <el-divider />

      <!-- 迁移仓库 -->
      <div class="repo-section">
        <h4>迁移仓库</h4>
        <p class="repo-tip">将指定仓库中的全部文件复制到新路径，并更新数据库记录。原文件保留作为备份。</p>
        <el-form :model="migrateForm" label-width="100px">
          <el-form-item label="选择仓库" required>
            <el-select v-model="migrateForm.repoId" placeholder="请选择要迁移的仓库" style="width: 100%">
              <el-option
                v-for="repo in repoList"
                :key="repo.repoId"
                :label="repo.repoName"
                :value="repo.repoId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="新路径" required>
            <el-input
              v-model="migrateForm.newRepoPath"
              placeholder="例如：E:/LingDocVault"
            />
          </el-form-item>
        </el-form>
        <el-button type="warning" :loading="migrateLoading" @click="handleMigrate">开始迁移</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createVaultRepo, migrateVaultRepo, deleteVaultRepo, setDefaultVaultRepo } from '@/api/lingdoc/vault'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  repoList: { type: Array, default: () => [] },
  forceCreate: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const createForm = reactive({
  repoPath: '',
  repoName: ''
})

const migrateForm = reactive({
  repoId: '',
  newRepoPath: ''
})

const createLoading = ref(false)
const migrateLoading = ref(false)

watch(() => props.modelValue, (val) => {
  if (val) {
    createForm.repoPath = ''
    createForm.repoName = ''
    migrateForm.repoId = ''
    migrateForm.newRepoPath = ''
  }
})

async function handleCreate() {
  if (!createForm.repoPath.trim()) {
    ElMessage.warning('请输入仓库路径')
    return
  }
  createLoading.value = true
  try {
    const res = await createVaultRepo({
      repoPath: createForm.repoPath.trim(),
      repoName: createForm.repoName.trim() || undefined
    })
    if (res.code === 200) {
      ElMessage.success('新仓库创建成功')
      emit('success')
      visible.value = false
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建仓库失败: ' + (e.message || e))
  } finally {
    createLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除仓库 "${row.repoName}" 吗？\n删除后该仓库的 SQLite 数据库文件会被清理，但物理文件会保留。`,
      '删除仓库确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await deleteVaultRepo(row.repoId)
    if (res.code === 200) {
      ElMessage.success('仓库删除成功')
      emit('success')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除仓库失败: ' + (e.message || e))
  }
}

async function handleSetDefault(row) {
  try {
    const res = await setDefaultVaultRepo(row.repoId)
    if (res.code === 200) {
      ElMessage.success('默认仓库设置成功')
      emit('success')
    } else {
      ElMessage.error(res.msg || '设置失败')
    }
  } catch (e) {
    ElMessage.error('设置默认仓库失败: ' + (e.message || e))
  }
}

async function handleMigrate() {
  if (!migrateForm.repoId) {
    ElMessage.warning('请选择要迁移的仓库')
    return
  }
  if (!migrateForm.newRepoPath.trim()) {
    ElMessage.warning('请输入新路径')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要将仓库迁移到 "${migrateForm.newRepoPath}" 吗？\n迁移过程会复制所有文件到新路径，原文件保留作为备份。`,
      '仓库迁移确认',
      {
        confirmButtonText: '确认迁移',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (e) {
    return
  }

  migrateLoading.value = true
  try {
    const res = await migrateVaultRepo(migrateForm.repoId, {
      newRepoPath: migrateForm.newRepoPath.trim()
    })
    if (res.code === 200) {
      ElMessage.success('仓库迁移成功')
      emit('success')
      visible.value = false
    } else {
      ElMessage.error(res.msg || '迁移失败')
    }
  } catch (e) {
    ElMessage.error('迁移仓库失败: ' + (e.message || e))
  } finally {
    migrateLoading.value = false
  }
}
</script>

<script>
import { computed, ref } from 'vue'
export default { name: 'VaultRepoManager' }
</script>

<style scoped>
.repo-manager {
  padding: 0 8px;
}
.repo-section h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #303133;
}
.repo-tip {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
</style>
