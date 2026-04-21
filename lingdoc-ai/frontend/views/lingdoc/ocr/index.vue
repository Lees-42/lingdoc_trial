<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="任务状态" clearable>
          <el-option label="待处理" value="0" />
          <el-option label="处理中" value="1" />
          <el-option label="成功" value="2" />
          <el-option label="失败" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain :icon="Plus" @click="handleUpload">上传识别</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务名称" prop="taskName" :show-overflow-tooltip="true" />
      <el-table-column label="文件名" prop="fileName" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="fileType" width="80" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="ocrStatusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="页数" prop="pageCount" width="70" />
      <el-table-column label="字符数" prop="charCount" width="80" />
      <el-table-column label="耗时" prop="processTime" width="100">
        <template #default="scope">
          <span v-if="scope.row.processTime">{{ scope.row.processTime }}ms</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" :icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" :icon="Refresh" @click="handleReprocess(scope.row)" :disabled="scope.row.status === '1'">重新识别</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 上传对话框 -->
    <el-dialog :title="uploadTitle" v-model="uploadOpen" width="500px" append-to-body>
      <el-form ref="uploadRef" :model="uploadForm" :rules="uploadRules" label-width="80px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="uploadForm.taskName" placeholder="默认使用文件名" />
        </el-form-item>
        <el-form-item label="识别模式" prop="async">
          <el-radio-group v-model="uploadForm.async">
            <el-radio :label="false">同步识别</el-radio>
            <el-radio :label="true">异步识别</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload
            ref="uploadRefEl"
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :file-list="fileList"
            accept=".pdf,.docx,.doc,.jpg,.jpeg,.png,.bmp"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word、JPG、PNG 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitUpload" :loading="uploadLoading">开始识别</el-button>
          <el-button @click="uploadOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 结果查看对话框 -->
    <el-dialog :title="resultTitle" v-model="resultOpen" width="800px" append-to-body>
      <div v-if="currentResult" class="ocr-result">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ currentResult.taskId }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentResult.fileName }}</el-descriptions-item>
          <el-descriptions-item label="页数">{{ currentResult.pageCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="字符数">{{ currentResult.charCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ currentResult.processTime ? currentResult.processTime + 'ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="单页耗时">{{ currentResult.avgPageTime ? currentResult.avgPageTime + 'ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <dict-tag :options="ocrStatusOptions" :value="currentResult.status" />
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <!-- 识别结果文本 -->
        <div v-if="parsedResult && parsedResult.pages">
          <div v-for="(page, pIdx) in parsedResult.pages" :key="pIdx" class="ocr-page">
            <h4 v-if="parsedResult.pages.length > 1">第 {{ page.page_num }} 页</h4>
            <div v-for="(line, lIdx) in page.lines" :key="lIdx" class="ocr-line">
              <span class="ocr-confidence" :class="getConfidenceClass(line.confidence)">
                {{ (line.confidence * 100).toFixed(0) }}%
              </span>
              <span class="ocr-text">{{ line.text }}</span>
            </div>
          </div>
        </div>

        <!-- 错误信息 -->
        <el-alert v-if="currentResult.errorMsg" :title="currentResult.errorMsg" type="error" show-icon />
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="OcrTask">
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Refresh, Plus, View, Delete } from '@element-plus/icons-vue'
import { listOcrTask, getOcrTask, uploadAndOcr, reprocessOcr, delOcrTask } from '@/api/lingdoc/ocr'

const { proxy } = getCurrentInstance()

// 响应式数据
const loading = ref(false)
const showSearch = ref(true)
const taskList = ref([])
const total = ref(0)
const ids = ref([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  taskName: undefined,
  status: undefined
})

// 上传对话框
const uploadOpen = ref(false)
const uploadTitle = ref('上传文件识别')
const uploadLoading = ref(false)
const fileList = ref([])
const uploadForm = reactive({
  taskName: undefined,
  async: false
})
const uploadRules = {
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

// 结果查看
const resultOpen = ref(false)
const resultTitle = ref('识别结果')
const currentResult = ref(null)
const parsedResult = computed(() => {
  if (!currentResult.value || !currentResult.value.resultJson) return null
  try {
    return JSON.parse(currentResult.value.resultJson)
  } catch (e) {
    return null
  }
})

// 状态字典
const ocrStatusOptions = [
  { label: '待处理', value: '0' },
  { label: '处理中', value: '1' },
  { label: '成功', value: '2' },
  { label: '失败', value: '3' }
]

// 查询列表
async function getList() {
  loading.value = true
  try {
    const res = await listOcrTask(queryParams)
    taskList.value = res.rows
    total.value = res.total
  } catch (error) {
    proxy.$modal.msgError('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

// 重置
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// 选择项变化
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.taskId)
}

// 上传对话框
function handleUpload() {
  uploadOpen.value = true
  fileList.value = []
  uploadForm.taskName = undefined
  uploadForm.async = false
}

// 文件选择变化
function handleFileChange(file) {
  if (!uploadForm.taskName && file.name) {
    uploadForm.taskName = file.name
  }
}

// 提交上传
async function submitUpload() {
  if (fileList.value.length === 0) {
    proxy.$modal.msgError('请选择文件')
    return
  }

  const file = fileList.value[0].raw
  const formData = new FormData()
  formData.append('file', file)

  uploadLoading.value = true
  try {
    const res = await uploadAndOcr(formData, uploadForm.taskName, uploadForm.async)
    proxy.$modal.msgSuccess(res.msg || '上传成功')
    uploadOpen.value = false
    getList()

    // 同步识别完成后直接查看结果
    if (!uploadForm.async && res.data && res.data.status === '2') {
      setTimeout(() => {
        handleViewById(res.data.taskId)
      }, 500)
    }
  } catch (error) {
    proxy.$modal.msgError('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}

// 查看详情
async function handleView(row) {
  resultOpen.value = true
  resultTitle.value = '识别结果 - ' + row.taskName
  currentResult.value = row

  // 如果状态是待处理或处理中，尝试重新获取最新状态
  if (row.status === '0' || row.status === '1') {
    try {
      const res = await getOcrTask(row.taskId)
      if (res.data) {
        currentResult.value = res.data
        // 更新列表中的状态
        const idx = taskList.value.findIndex(t => t.taskId === row.taskId)
        if (idx !== -1) {
          taskList.value[idx] = res.data
        }
      }
    } catch (e) {
      // 忽略
    }
  }
}

async function handleViewById(taskId) {
  try {
    const res = await getOcrTask(taskId)
    if (res.data) {
      resultOpen.value = true
      resultTitle.value = '识别结果 - ' + res.data.taskName
      currentResult.value = res.data
    }
  } catch (e) {
    // 忽略
  }
}

// 重新识别
async function handleReprocess(row) {
  await proxy.$modal.confirm('确认重新识别该文件？')
  try {
    const res = await reprocessOcr(row.taskId)
    proxy.$modal.msgSuccess('重新识别完成')
    getList()
  } catch (error) {
    proxy.$modal.msgError('重新识别失败')
  }
}

// 删除
async function handleDelete(row) {
  await proxy.$modal.confirm('确认删除该识别任务？')
  try {
    await delOcrTask(row.taskId)
    proxy.$modal.msgSuccess('删除成功')
    getList()
  } catch (error) {
    proxy.$modal.msgError('删除失败')
  }
}

// 置信度样式
function getConfidenceClass(confidence) {
  if (confidence >= 0.95) return 'confidence-high'
  if (confidence >= 0.8) return 'confidence-medium'
  return 'confidence-low'
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.ocr-result {
  max-height: 600px;
  overflow-y: auto;
}

.ocr-page {
  margin-bottom: 20px;
}

.ocr-page h4 {
  margin: 10px 0;
  padding: 5px 0;
  border-bottom: 1px solid #eee;
}

.ocr-line {
  display: flex;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f5f5f5;
}

.ocr-confidence {
  display: inline-block;
  min-width: 40px;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 10px;
  text-align: center;
}

.confidence-high {
  background-color: #67c23a;
  color: white;
}

.confidence-medium {
  background-color: #e6a23c;
  color: white;
}

.confidence-low {
  background-color: #f56c6c;
  color: white;
}

.ocr-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
}
</style>
