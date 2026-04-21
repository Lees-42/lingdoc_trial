<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <h2 class="page-title">文档上传</h2>
    <p class="page-desc">拖拽文件到下方区域上传，上传后可选择文件执行自动规整，AI 将推荐规范命名与分类路径。</p>

    <!-- 文件上传区 -->
    <el-card class="upload-card" shadow="never">
      <el-upload
        ref="uploadRef"
        action="#"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.bmp,.webp,.txt,.md"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <span class="upload-primary">拖拽文件到此处，或 <em>点击上传</em></span>
          <p class="upload-tip">
            支持 PDF、Word、Excel、图片、文本等格式，单次最多上传 20 个文件
          </p>
        </div>
      </el-upload>
    </el-card>

    <!-- 文件列表 -->
    <el-card v-if="fileList.length > 0" class="list-card" shadow="never">
      <template #header>
        <div class="list-header">
          <span class="list-title">已上传文件（{{ fileList.length }}）</span>
          <div class="list-actions">
            <el-button
              type="primary"
              plain
              icon="MagicStick"
              :disabled="!hasPendingOrganize"
              @click="handleBatchOrganize"
              v-hasPermi="['lingdoc:upload:organize']"
            >批量自动规整</el-button>
            <el-button
              type="success"
              plain
              icon="Check"
              :disabled="!hasPendingConfirm"
              @click="handleBatchConfirm"
              v-hasPermi="['lingdoc:upload:confirm']"
            >批量确认</el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="!selectedIds.length"
              @click="handleBatchDelete"
              v-hasPermi="['lingdoc:upload:delete']"
            >批量删除</el-button>
            <el-button
              plain
              icon="Refresh"
              @click="handleClean"
              v-hasPermi="['lingdoc:upload:delete']"
            >清空列表</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="fileList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="原始文件名" prop="originalName" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <div class="file-name-cell">
              <svg-icon :icon-class="getFileIcon(scope.row.fileType)" class="file-icon" />
              <span>{{ scope.row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="fileType" width="80" align="center">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.fileType?.toUpperCase() || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" prop="fileSize" width="100" align="center">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件名" prop="suggestedName" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.suggestedName" class="suggested-text">{{ scope.row.suggestedName }}</span>
            <span v-else class="placeholder-text">--</span>
          </template>
        </el-table-column>
        <el-table-column label="保存路径" prop="suggestedPath" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.suggestedPath" class="suggested-text">{{ scope.row.suggestedPath }}</span>
            <span v-else class="placeholder-text">--</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="160">
          <template #default="scope">
            <div class="tag-cell">
              <el-tag
                v-for="tag in scope.row.tags"
                :key="tag.tagId"
                size="small"
                :color="tag.tagColor"
                effect="dark"
                class="direct-tag"
              >{{ tag.tagName }}</el-tag>
              <el-tag
                v-for="tag in scope.row.inheritedTags"
                :key="'inherited_' + tag.tagId"
                size="small"
                :color="tag.tagColor"
                effect="plain"
                class="inherited-tag"
              >{{ tag.tagName }}</el-tag>
              <span v-if="!scope.row.tags?.length && !scope.row.inheritedTags?.length" class="placeholder-text">--</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="280" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'uploaded'"
              link
              type="primary"
              icon="MagicStick"
              @click="handleOrganize(scope.row)"
              v-hasPermi="['lingdoc:upload:organize']"
            >自动规整</el-button>
            <el-button
              v-if="['uploaded', 'pending'].includes(scope.row.status)"
              link
              type="primary"
              icon="Edit"
              @click="handleEdit(scope.row)"
              v-hasPermi="['lingdoc:upload:edit']"
            >编辑</el-button>
            <el-button
              v-if="['uploaded', 'pending'].includes(scope.row.status)"
              link
              type="success"
              icon="Check"
              @click="handleConfirm(scope.row)"
              v-hasPermi="['lingdoc:upload:confirm']"
            >确认</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['lingdoc:upload:delete']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-else description="暂无已上传文件">
      <template #description>
        <p>暂无已上传文件</p>
        <p class="empty-tip">上传文件后，可选择执行自动规整，AI 将推荐归档方案</p>
      </template>
    </el-empty>

    <!-- 编辑弹窗 -->
    <el-dialog
      :title="'编辑归档方案 — ' + form.originalName"
      v-model="open"
      width="640px"
      append-to-body
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-width="110px">
        <!-- 区块 A：文件信息（只读） -->
        <div class="form-section">
          <div class="section-title">文件信息</div>
          <el-form-item label="原始文件名">
            <el-input v-model="form.originalName" disabled />
          </el-form-item>
          <el-form-item label="文件类型">
            <el-input v-model="form.fileType" disabled />
          </el-form-item>
        </div>

        <!-- 区块 B：归档方案（可编辑） -->
        <div class="form-section">
          <div class="section-title">归档方案</div>
          <el-form-item label="文件名" prop="suggestedName">
            <el-input v-model="form.suggestedName" placeholder="请输入规范文件名" clearable />
          </el-form-item>
          <el-form-item label="保存路径">
            <div class="path-input-group">
              <el-input
                v-model="form.suggestedPath"
                placeholder="请选择或输入分类路径"
                clearable
              />
              <el-button
                type="primary"
                icon="FolderOpened"
                @click="openFolderPicker"
              >选择目录</el-button>
            </div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
        </div>

        <!-- 区块 C：标签管理 -->
        <div class="form-section">
          <div class="section-title">标签管理</div>

          <!-- 文件标签 -->
          <el-form-item label="文件标签">
            <div class="tag-preview">
              <el-tag
                v-for="name in form.tagNames"
                :key="name"
                size="small"
                :color="getTagByName(name)?.tagColor"
                effect="dark"
                class="tag-item"
                closable
                @close="removeFileTag(name)"
              >{{ name }}</el-tag>
              <span v-if="!form.tagNames.length" class="placeholder-text">暂无标签</span>
            </div>
            <div class="tag-add-row">
              <el-select
                v-model="pendingFileTag"
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入新标签"
                style="flex: 1;"
                @keyup.enter="addFileTag"
              >
                <el-option
                  v-for="tag in allTags"
                  :key="tag.tagId"
                  :label="tag.tagName"
                  :value="tag.tagName"
                />
              </el-select>
              <el-button type="primary" size="small" icon="Plus" @click="addFileTag">添加</el-button>
            </div>
          </el-form-item>

          <!-- 父文件夹标签 -->
          <el-form-item label="父文件夹标签">
            <el-select
              v-model="selectedParentFolder"
              placeholder="选择要编辑的父文件夹"
              style="width: 100%; margin-bottom: 12px;"
            >
              <el-option
                v-for="folder in parentFolderOptions"
                :key="folder.path"
                :label="folder.path || '根目录'"
                :value="folder.path"
              />
            </el-select>
            <div v-if="selectedParentFolder !== ''" class="folder-tag-editor">
              <div class="tag-preview">
                <el-tag
                  v-for="name in currentFolderTagNames"
                  :key="name"
                  size="small"
                  :color="getTagByName(name)?.tagColor"
                  effect="dark"
                  class="tag-item"
                  closable
                  @close="removeFolderTag(name)"
                >{{ name }}</el-tag>
                <span v-if="!currentFolderTagNames.length" class="placeholder-text">暂无标签</span>
              </div>
              <div class="tag-add-row">
                <el-select
                  v-model="pendingFolderTag"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入新标签"
                  style="flex: 1;"
                  @keyup.enter="addFolderTag"
                >
                  <el-option
                    v-for="tag in allTags"
                    :key="tag.tagId"
                    :label="tag.tagName"
                    :value="tag.tagName"
                  />
                </el-select>
                <el-button type="primary" size="small" icon="Plus" @click="addFolderTag">添加</el-button>
              </div>
            </div>
            <p class="form-tip">修改父文件夹标签后，该目录下所有文件将自动继承这些标签</p>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确认</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 目录选择器 -->
    <VaultFolderPicker
      v-model:visible="folderPickerVisible"
      :default-path="form.suggestedPath"
      :folder-tag-map="folderTagMap"
      @confirm="handlePickerConfirm"
    />
  </div>
</template>

<script setup name="Upload">
import { ref, reactive, computed, watch } from 'vue'
import { UploadFilled, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import VaultFolderPicker from '@/components/VaultFolderPicker/index.vue'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const open = ref(false)
const uploadRef = ref(null)
const formRef = ref(null)

// 文件列表
const fileList = ref([])

// 选中项
const selectedIds = ref([])

// 是否存在待确认的文件
const hasPendingConfirm = computed(() => {
  return fileList.value.some(item => ['uploaded', 'pending'].includes(item.status))
})

// 是否存在已上传未规整的文件
const hasPendingOrganize = computed(() => {
  return fileList.value.some(item => item.status === 'uploaded')
})

// 预置标签池
const presetTags = [
  { tagId: 't1', tagName: '课程', tagColor: '#409EFF' },
  { tagId: 't2', tagName: '奖学金', tagColor: '#67C23A' },
  { tagId: 't3', tagName: '实验报告', tagColor: '#E6A23C' },
  { tagId: 't4', tagName: '简历', tagColor: '#F56C6C' },
  { tagId: 't5', tagName: '论文', tagColor: '#909399' },
  { tagId: 't6', tagName: '证书', tagColor: '#9254DE' }
]

// 所有可用标签（预置 + 用户创建的）
const allTags = ref([...presetTags])

// 目录标签绑定映射（key: 目录路径，value: 标签对象数组）
const folderTagMap = ref({
  '/学习资料/大三上/操作系统': [
    { tagId: 't1', tagName: '课程', tagColor: '#409EFF' }
  ]
})

// 表单数据
const form = reactive({
  fileId: undefined,
  originalName: '',
  fileType: '',
  suggestedName: '',
  suggestedPath: '',
  remark: '',
  tagNames: []
})

// 弹窗内临时状态
const pendingFileTag = ref('')
const selectedParentFolder = ref('')
const pendingFolderTag = ref('')
const folderPickerVisible = ref(false)

// 父文件夹下拉选项（从 suggestedPath 解析）
const parentFolderOptions = computed(() => {
  return resolvePathChain(form.suggestedPath)
})

// 当前选中父文件夹的标签名数组
const currentFolderTagNames = computed(() => {
  const tags = folderTagMap.value[selectedParentFolder.value] || []
  return tags.map(t => t.tagName)
})

// 状态枚举
const statusMap = {
  uploaded: { label: '已上传', type: 'info' },
  organizing: { label: '规整中', type: 'warning' },
  pending: { label: '待确认', type: 'primary' },
  confirmed: { label: '已确认', type: 'success' },
  failed: { label: '失败', type: 'danger' }
}

/** 获取状态标签 */
function getStatusLabel(status) {
  return statusMap[status]?.label || status
}

/** 获取状态类型 */
function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

/** 根据文件类型获取图标 */
function getFileIcon(fileType) {
  const iconMap = {
    pdf: 'pdf',
    doc: 'word',
    docx: 'word',
    xls: 'excel',
    xlsx: 'excel',
    png: 'image',
    jpg: 'image',
    jpeg: 'image',
    gif: 'image',
    bmp: 'image',
    webp: 'image',
    txt: 'txt',
    md: 'txt'
  }
  return iconMap[fileType?.toLowerCase()] || 'file'
}

/** 格式化文件大小 */
function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

/** 解析路径为所有层级数组 */
function resolvePathChain(path) {
  if (!path || path === '/') {
    return [{ name: '根目录', path: '/' }]
  }
  const cleanPath = path.replace(/\/$/, '')
  const result = []
  let current = ''
  const parts = cleanPath.split('/').filter(Boolean)
  for (const part of parts) {
    current = current + '/' + part
    result.push({ name: part, path: current })
  }
  // 倒序排列：从最深层级到根目录
  return result.reverse()
}

/** 计算文件的继承标签 */
function computeInheritedTags(filePath) {
  if (!filePath) return []
  const inherited = new Map()
  const cleanPath = filePath.replace(/\/$/, '')
  for (const [folderPath, tags] of Object.entries(folderTagMap.value)) {
    if (cleanPath.startsWith(folderPath)) {
      for (const tag of tags) {
        inherited.set(tag.tagId, tag)
      }
    }
  }
  return Array.from(inherited.values())
}

/** 同步所有文件的继承标签 */
function syncInheritedTags() {
  fileList.value = fileList.value.map(file => ({
    ...file,
    inheritedTags: computeInheritedTags(file.suggestedPath)
  }))
}

/** 通过标签名查找标签对象 */
function getTagByName(name) {
  return allTags.value.find(t => t.tagName === name)
}

/** 创建新标签 */
function createNewTag(name) {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9254DE', '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4']
  const existing = getTagByName(name)
  if (existing) return existing
  const newTag = {
    tagId: 'tag_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5),
    tagName: name,
    tagColor: colors[allTags.value.length % colors.length]
  }
  allTags.value.push(newTag)
  return newTag
}

/** 添加文件标签 */
function addFileTag() {
  const name = pendingFileTag.value
  if (!name || !name.trim()) return
  const trimmed = name.trim()
  if (form.tagNames.includes(trimmed)) {
    ElMessage.warning('该标签已存在')
    pendingFileTag.value = ''
    return
  }
  if (!getTagByName(trimmed)) {
    createNewTag(trimmed)
  }
  form.tagNames.push(trimmed)
  pendingFileTag.value = ''
}

/** 移除文件标签 */
function removeFileTag(name) {
  form.tagNames = form.tagNames.filter(n => n !== name)
}

/** 添加父文件夹标签 */
function addFolderTag() {
  const name = pendingFolderTag.value
  if (!name || !name.trim()) return
  const trimmed = name.trim()
  if (currentFolderTagNames.value.includes(trimmed)) {
    ElMessage.warning('该标签已存在')
    pendingFolderTag.value = ''
    return
  }
  if (!getTagByName(trimmed)) {
    createNewTag(trimmed)
  }
  const tagObj = getTagByName(trimmed)
  const currentTags = folderTagMap.value[selectedParentFolder.value] || []
  folderTagMap.value[selectedParentFolder.value] = [...currentTags, tagObj]
  pendingFolderTag.value = ''
  syncInheritedTags()
}

/** 移除父文件夹标签 */
function removeFolderTag(name) {
  const currentTags = folderTagMap.value[selectedParentFolder.value] || []
  folderTagMap.value[selectedParentFolder.value] = currentTags.filter(t => t.tagName !== name)
  syncInheritedTags()
}

/** 打开目录选择器 */
function openFolderPicker() {
  folderPickerVisible.value = true
}

/** 目录选择器确认回调 */
function handlePickerConfirm(path) {
  form.suggestedPath = path
}

/** 文件变更回调（仅添加到列表，不自动触发规整） */
function handleFileChange(uploadFile) {
  const raw = uploadFile.raw
  const ext = raw.name.split('.').pop()
  const fileId = Date.now() + '_' + Math.random().toString(36).substr(2, 9)

  const item = {
    fileId: fileId,
    originalName: raw.name,
    fileType: ext,
    fileSize: raw.size,
    status: 'uploaded',
    suggestedName: raw.name,
    suggestedPath: '',
    remark: '',
    tags: [],
    inheritedTags: []
  }

  fileList.value = [...fileList.value, item]
  ElMessage.success(`已添加文件：${raw.name}`)

  // TODO: 后端就绪后，调用 uploadFile API 将文件上传到服务端临时存储
  // uploadFile(formData).then(res => { ... })
}

/** 多选框变化 */
function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.fileId)
}

/** 自动规整按钮（单个文件） */
function handleOrganize(row) {
  const idx = fileList.value.findIndex(f => f.fileId === row.fileId)
  if (idx === -1) return

  fileList.value[idx] = { ...fileList.value[idx], status: 'organizing' }
  ElMessage.info(`正在对 "${row.originalName}" 进行自动规整...`)

  // TODO: 后端就绪后，调用 organizeUpload(row.fileId) 触发 AI 规整
  // organizeUpload(row.fileId).then(res => { ... })

  // 模拟 AI 解析完成
  setTimeout(() => {
    const i = fileList.value.findIndex(f => f.fileId === row.fileId)
    if (i !== -1) {
      const mockName = generateMockName(row.originalName, row.fileType)
      const mockPath = generateMockPath(row.fileType)
      fileList.value[i] = {
        ...fileList.value[i],
        status: 'pending',
        suggestedName: mockName,
        suggestedPath: mockPath,
        inheritedTags: computeInheritedTags(mockPath)
      }
      ElMessage.success(`AI 已生成归档建议：${mockName}`)
    }
  }, 1500 + Math.random() * 1500)
}

/** 批量自动规整 */
function handleBatchOrganize() {
  const uploadedList = fileList.value.filter(f => f.status === 'uploaded')
  if (!uploadedList.length) {
    ElMessage.warning('没有可自动规整的文件')
    return
  }

  ElMessageBox.confirm(
    `确认对 ${uploadedList.length} 个文件执行自动规整？`,
    '批量自动规整',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
  ).then(() => {
    uploadedList.forEach(row => {
      handleOrganize(row)
    })
  }).catch(() => {})
}

/** 生成模拟文件名 */
function generateMockName(originalName, ext) {
  const nameWithoutExt = originalName.replace(/\.[^/.]+$/, '')
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  return `${nameWithoutExt}_${dateStr}.${ext}`
}

/** 生成模拟路径 */
function generateMockPath(ext) {
  const paths = [
    '/学习资料/大三上/操作系统',
    '/学习资料/大三上/计算机网络',
    '/申请材料/奖学金',
    '/实验报告/计算机组成原理',
    '/文献资料/论文素材',
    '/工作文档/实习材料'
  ]
  const imagePaths = [
    '/图片素材/截图',
    '/图片素材/扫描件',
    '/图片素材/证书'
  ]
  const isImage = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'].includes(ext?.toLowerCase())
  return isImage
    ? imagePaths[Math.floor(Math.random() * imagePaths.length)]
    : paths[Math.floor(Math.random() * paths.length)]
}

/** 编辑按钮 */
function handleEdit(row) {
  Object.assign(form, {
    fileId: row.fileId,
    originalName: row.originalName,
    fileType: row.fileType,
    suggestedName: row.suggestedName || row.originalName,
    suggestedPath: row.suggestedPath,
    remark: row.remark || '',
    tagNames: (row.tags || []).map(t => t.tagName)
  })
  // 设置默认选中的父文件夹为最深一级
  const chain = resolvePathChain(row.suggestedPath)
  selectedParentFolder.value = chain.length > 0 ? chain[0].path : ''
  pendingFileTag.value = ''
  pendingFolderTag.value = ''
  open.value = true
}

/** 取消编辑 */
function cancel() {
  open.value = false
  resetForm()
}

/** 重置表单 */
function resetForm() {
  Object.assign(form, {
    fileId: undefined,
    originalName: '',
    fileType: '',
    suggestedName: '',
    suggestedPath: '',
    remark: '',
    tagNames: []
  })
  pendingFileTag.value = ''
  selectedParentFolder.value = ''
  pendingFolderTag.value = ''
}

/** 提交表单（确认） */
function submitForm() {
  if (!form.suggestedName.trim()) {
    ElMessage.warning('文件名不能为空')
    return
  }

  const tagObjects = form.tagNames.map(name => getTagByName(name)).filter(Boolean)
  const inherited = computeInheritedTags(form.suggestedPath)

  const idx = fileList.value.findIndex(f => f.fileId === form.fileId)
  if (idx !== -1) {
    fileList.value[idx] = {
      ...fileList.value[idx],
      suggestedName: form.suggestedName,
      suggestedPath: form.suggestedPath,
      remark: form.remark,
      tags: tagObjects,
      inheritedTags: inherited,
      status: 'confirmed'
    }
  }

  open.value = false
  resetForm()
  ElMessage.success('确认成功，文件已归档')

  // TODO: 后端就绪后，调用 confirmUpload API 将文件实际保存到 Vault
  // confirmUpload({ fileId, suggestedName, suggestedPath, remark, tags }).then(res => { ... })
}

/** 单个确认 */
function handleConfirm(row) {
  const msg = row.suggestedPath
    ? `确认将 "${row.originalName}" 保存到 "${row.suggestedPath}"？`
    : `确认保存 "${row.originalName}"？（未指定保存路径）`

  ElMessageBox.confirm(
    msg,
    '确认归档',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    const idx = fileList.value.findIndex(f => f.fileId === row.fileId)
    if (idx !== -1) {
      fileList.value[idx] = { ...fileList.value[idx], status: 'confirmed' }
    }
    ElMessage.success('确认成功，文件已归档')
    // TODO: 后端就绪后，调用 confirmUpload API
  }).catch(() => {})
}

/** 删除单个 */
function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除文件 "${row.originalName}"？`).then(() => {
    fileList.value = fileList.value.filter(f => f.fileId !== row.fileId)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 批量确认 */
function handleBatchConfirm() {
  const pendingList = fileList.value.filter(f => ['uploaded', 'pending'].includes(f.status))
  if (!pendingList.length) {
    ElMessage.warning('没有待确认的文件')
    return
  }

  ElMessageBox.confirm(
    `确认批量归档 ${pendingList.length} 个文件？`,
    '批量确认',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    fileList.value = fileList.value.map(f =>
      ['uploaded', 'pending'].includes(f.status) ? { ...f, status: 'confirmed' } : f
    )
    ElMessage.success(`成功归档 ${pendingList.length} 个文件`)
    // TODO: 后端就绪后，调用 batchConfirmUpload API
  }).catch(() => {})
}

/** 批量删除 */
function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择一项')
    return
  }

  proxy.$modal.confirm(`是否确认删除选中的 ${selectedIds.value.length} 个文件？`).then(() => {
    fileList.value = fileList.value.filter(f => !selectedIds.value.includes(f.fileId))
    selectedIds.value = []
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 清空列表 */
function handleClean() {
  if (!fileList.value.length) {
    ElMessage.warning('列表已为空')
    return
  }
  proxy.$modal.confirm('是否确认清空所有已上传文件？').then(() => {
    fileList.value = []
    selectedIds.value = []
    ElMessage.success('清空成功')
  }).catch(() => {})
}
</script>

<style scoped>
.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.upload-card {
  margin-bottom: 20px;
}

.upload-card :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  font-size: 48px;
  color: var(--el-color-primary);
  margin-bottom: 12px;
}

.upload-primary {
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.upload-primary em {
  color: var(--el-color-primary);
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.list-card {
  margin-top: 20px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.list-actions {
  display: flex;
  gap: 8px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.suggested-text {
  color: var(--el-color-primary);
  font-weight: 500;
}

.placeholder-text {
  color: var(--el-text-color-placeholder);
}

.empty-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 标签相关样式 */
.tag-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.direct-tag {
  border: none;
}

.inherited-tag {
  opacity: 0.7;
}

.form-section {
  margin-bottom: 16px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.tag-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
  align-items: center;
  margin-bottom: 8px;
}

.tag-item {
  border: none;
}

.tag-add-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.path-input-group {
  display: flex;
  gap: 8px;
}

.path-input-group .el-input {
  flex: 1;
}

.folder-tag-editor {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 8px;
}

.form-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
</style>
