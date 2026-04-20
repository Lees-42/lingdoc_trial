<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['lingdoc:knowledge:add']"
        >新增知识库</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="kbList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="知识库ID" align="center" prop="kbId" :show-overflow-tooltip="true" />
      <el-table-column label="名称" align="center" prop="kbName" :show-overflow-tooltip="true" />
      <el-table-column label="描述" align="center" prop="kbDesc" :show-overflow-tooltip="true" />
      <el-table-column label="文档数" align="center" prop="docCount" width="80" />
      <el-table-column label="分块数" align="center" prop="chunkCount" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
          <el-tag v-else-if="scope.row.status === 0" type="danger">禁用</el-tag>
          <el-tag v-else type="warning">构建中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="ChatDotRound" @click="handleChat(scope.row)" v-hasPermi="['lingdoc:chat:query']">对话</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lingdoc:knowledge:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lingdoc:knowledge:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="kbRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="kbName">
          <el-input v-model="form.kbName" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="kbDesc">
          <el-input v-model="form.kbDesc" type="textarea" placeholder="请输入知识库描述" />
        </el-form-item>
        <el-form-item label="分块大小" prop="chunkSize">
          <el-input-number v-model="form.chunkSize" :min="100" :max="2048" :step="64" />
          <span class="text-gray-400 text-xs ml-2">字符数</span>
        </el-form-item>
        <el-form-item label="重叠大小" prop="chunkOverlap">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="200" :step="10" />
          <span class="text-gray-400 text-xs ml-2">字符数</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Knowledge">
import { listKnowledgeBase, getKnowledgeBase, addKnowledgeBase, updateKnowledgeBase, delKnowledgeBase } from '@/api/lingdoc/knowledge'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()

const kbList = ref([])
const loading = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10
  },
  rules: {
    kbName: [{ required: true, message: '知识库名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询知识库列表 */
function getList() {
  loading.value = true
  listKnowledgeBase(queryParams.value).then(response => {
    kbList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    kbId: undefined,
    kbName: undefined,
    kbDesc: undefined,
    chunkSize: 512,
    chunkOverlap: 50
  }
  proxy.resetForm('kbRef')
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.kbId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '新增知识库'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const kbId = row.kbId || ids.value[0]
  getKnowledgeBase(kbId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改知识库'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['kbRef'].validate(valid => {
    if (valid) {
      if (form.value.kbId) {
        updateKnowledgeBase(form.value.kbId, form.value).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addKnowledgeBase(form.value).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const kbIds = row.kbId ? [row.kbId] : ids.value
  proxy.$modal.confirm('是否确认删除选中的知识库？').then(function() {
    return delKnowledgeBase(kbIds[0])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 对话按钮操作 */
function handleChat(row) {
  proxy.$router.push('/lingdoc/chat?kbId=' + row.kbId)
}

getList()
</script>
