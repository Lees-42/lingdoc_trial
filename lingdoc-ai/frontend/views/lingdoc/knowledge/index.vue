<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="知识库名" prop="kbName">
        <el-input
          v-model="queryParams.kbName"
          placeholder="请输入知识库名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['lingdoc:knowledge:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="knowledgeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="知识库ID" align="center" prop="kbId" :show-overflow-tooltip="true" />
      <el-table-column label="知识库名称" align="center" prop="kbName" :show-overflow-tooltip="true" />
      <el-table-column label="描述" align="center" prop="kbDescription" :show-overflow-tooltip="true" />
      <el-table-column label="分块大小" align="center" prop="chunkSize" />
      <el-table-column label="重叠长度" align="center" prop="chunkOverlap" />
      <el-table-column label="文件数" align="center" prop="totalFiles" />
      <el-table-column label="分块数" align="center" prop="totalChunks" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dicts.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
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

    <!-- 添加或修改知识库对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="knowledgeRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="知识库名称" prop="kbName">
          <el-input v-model="form.kbName" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="kbDescription">
          <el-input v-model="form.kbDescription" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="分块大小" prop="chunkSize">
          <el-input-number v-model="form.chunkSize" :min="128" :max="2048" :step="64" />
        </el-form-item>
        <el-form-item label="重叠长度" prop="chunkOverlap">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="256" :step="16" />
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
import { listKnowledge, getKnowledge, addKnowledge, updateKnowledge, delKnowledge } from '@/api/lingdoc/knowledge'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict('sys_normal_disable')

const dicts = reactive({
  sys_normal_disable: sys_normal_disable
})

const knowledgeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    kbName: undefined
  },
  rules: {
    kbName: [{ required: true, message: "知识库名称不能为空", trigger: "blur" }],
    chunkSize: [{ required: true, message: "分块大小不能为空", trigger: "blur" }],
    chunkOverlap: [{ required: true, message: "重叠长度不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询知识库列表 */
function getList() {
  loading.value = true
  listKnowledge(queryParams.value).then(response => {
    knowledgeList.value = response.rows
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
    kbDescription: undefined,
    chunkSize: 512,
    chunkOverlap: 50,
    embeddingModel: 'text-embedding-3-small',
    status: "0"
  }
  proxy.resetForm("knowledgeRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.kbId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加知识库"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const kbId = row.kbId || ids.value[0]
  getKnowledge(kbId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改知识库"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["knowledgeRef"].validate(valid => {
    if (valid) {
      if (form.value.kbId != undefined) {
        updateKnowledge(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addKnowledge(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const kbIds = row.kbId || ids.value
  proxy.$modal.confirm('是否确认删除知识库编号为"' + kbIds + '"的数据项？').then(function() {
    return delKnowledge(kbIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>
