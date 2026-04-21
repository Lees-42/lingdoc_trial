<template>
  <el-dialog
    v-model="visible"
    title="仓库管理"
    width="560px"
    :close-on-click-modal="false"
  >
    <div class="repo-manager">
      <!-- 当前仓库信息 -->
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="仓库名称">{{ repoInfo.repoName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库路径">
          <el-text type="info" size="small">{{ repoInfo.repoPath || '-' }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <!-- 创建新仓库 -->
      <div class="repo-section">
        <h4>创建新仓库</h4>
        <p class="repo-tip">切换到新的仓库路径，旧仓库文件不会自动迁移。</p>
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
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建并切换</el-button>
      </div>

      <el-divider />

      <!-- 迁移仓库 -->
      <div class="repo-section">
        <h4>迁移仓库</h4>
        <p class="repo-tip">将当前仓库中的全部文件复制到新路径，并更新数据库记录。原文件保留作为备份。</p>
        <el-form :model="migrateForm" label-width="100px">
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
import { createVaultRepo, migrateVaultRepo } from '@/api/lingdoc/vault'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  repoInfo: { type: Object, default: () => ({}) }
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
  newRepoPath: ''
})

const createLoading = ref(false)
const migrateLoading = ref(false)

watch(() => props.modelValue, (val) => {
  if (val) {
    createForm.repoPath = ''
    createForm.repoName = ''
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

async function handleMigrate() {
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
    const res = await migrateVaultRepo({
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
