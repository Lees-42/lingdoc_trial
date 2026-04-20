<template>
  <div class="app-container">
    <!-- 知识库选择 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>文档管理</span>
              <el-button type="primary" @click="handleUpload" :disabled="!currentKbId">
                <el-icon><Plus /></el-icon>上传文档
              </el-button>
            </div>
          </template>

          <!-- 知识库选择器 -->
          <el-form :inline="true">
            <el-form-item label="选择知识库">
              <el-select v-model="currentKbId" placeholder="请选择知识库" @change="handleKbChange" style="width: 300px">
                <el-option
                  v-for="kb in kbList"
                  :key="kb.kbId"
                  :label="kb.kbName"
                  :value="kb.kbId"
                >
                  <span>{{ kb.kbName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ kb.docCount || 0 }} 文档</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>

          <!-- 文档列表 -->
          <el-table v-loading="loading" :data="docList" v-if="currentKbId">
            <el-table-column label="文件名" prop="originalName" :show-overflow-tooltip="true" />
            <el-table-column label="类型" prop="fileType" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ row.fileType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" prop="fileSizeFormatted" width="100" />
            <el-table-column label="页数/字数" width="120">
              <template #default="{ row }">
                <span v-if="row.pageCount">{{ row.pageCount }}页</span>
                <span v-else-if="row.charCount">{{ row.charCount }}字</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="processStatusName" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.processStatus)" size="small">
                  {{ row.processStatusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分块数" prop="chunkCount" width="80">
              <template #default="{ row }">
                <span v-if="row.chunkCount">{{ row.chunkCount }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" prop="createdAt" width="160" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleReprocess(row)"
                  :disabled="row.processStatus === 1"
                >
                  重处理
                </el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 未选择知识库提示 -->
          <el-empty v-if="!currentKbId" description="请先选择知识库" />

          <!-- 分页 -->
          <pagination
            v-if="currentKbId && total > 0"
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传对话框 -->
    <el-dialog title="上传文档" v-model="uploadOpen" width="500px" append-to-body>
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="知识库">
          <el-input v-model="currentKbName" disabled />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :file-list="fileList"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF/Word/Excel/TXT/图片，单个文件不超过50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="uploadOpen = false">取消</el-button>
          <el-button type="primary" @click="submitUpload" :loading="uploadLoading">上传</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LingDocDocument">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listKnowledgeBases } from '@/api/lingdoc/knowledge'
import { listDocuments, uploadDocument, delDocument, reprocessDocument } from '@/api/lingdoc/document'

const { proxy } = getCurrentInstance()

// 数据状态
const loading = ref(false)
const uploadLoading = ref(false)
const uploadOpen = ref(false)
const kbList = ref([])
const currentKbId = ref('')
const docList = ref([])
const total = ref(0)
const fileList = ref([])
const currentFile = ref(null)

const uploadForm = reactive({
  kbId: ''
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

// 计算属性
const currentKbName = computed(() => {
  const kb = kbList.value.find(k => k.kbId === currentKbId.value)
  return kb ? kb.kbName : ''
})

// 获取知识库列表
function getKbList() {
  listKnowledgeBases().then(response => {
    kbList.value = response.data || []
  })
}

// 获取文档列表
function getList() {
  if (!currentKbId.value) return
  
  loading.value = true
  listDocuments(currentKbId.value, queryParams).then(response => {
    docList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// 知识库切换
function handleKbChange() {
  queryParams.pageNum = 1
  getList()
}

// 打开上传对话框
function handleUpload() {
  uploadForm.kbId = currentKbId.value
  fileList.value = []
  currentFile.value = null
  uploadOpen.value = true
}

// 文件选择变化
function handleFileChange(file) {
  currentFile.value = file.raw
}

// 提交上传
function submitUpload() {
  if (!currentFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploadLoading.value = true
  const formData = new FormData()
  formData.append('kbId', currentKbId.value)
  formData.append('file', currentFile.value)
  formData.append('overwrite', 'false')

  uploadDocument(formData).then(response => {
    ElMessage.success('上传成功')
    uploadOpen.value = false
    uploadLoading.value = false
    getList()
  }).catch(error => {
    ElMessage.error(error.message || '上传失败')
    uploadLoading.value = false
  })
}

// 删除文档
function handleDelete(row) {
  ElMessageBox.confirm('确认删除文档 "' + row.originalName + '" 吗？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    delDocument(row.docId).then(() => {
      ElMessage.success('删除成功')
      getList()
    })
  })
}

// 重新处理
function handleReprocess(row) {
  ElMessageBox.confirm('确认重新处理文档 "' + row.originalName + '" 吗？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(() => {
    reprocessDocument(row.docId).then(() => {
      ElMessage.success('已开始重新处理')
      getList()
    })
  })
}

// 状态标签类型
function getStatusType(status) {
  switch (status) {
    case 0: return 'info'      // 待处理
    case 1: return 'warning'   // 处理中
    case 2: return 'success'   // 已完成
    case 3: return 'danger'    // 失败
    default: return ''
  }
}

// 初始化
onMounted(() => {
  getKbList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
