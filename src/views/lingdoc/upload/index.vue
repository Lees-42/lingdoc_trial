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
                ref="fileTagSelectRef"
                v-model="pendingFileTag"
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入新标签"
                style="flex: 1;"
                :filter-method="(val) => { backupFileTag = val }"
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
                  ref="folderTagSelectRef"
                  v-model="pendingFolderTag"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入新标签"
                  style="flex: 1;"
                  :filter-method="(val) => { backupFolderTag = val }"
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
      :tree-data="realVaultTree"
      @confirm="handlePickerConfirm"
    />
  </div>
</template>

<script setup name="Upload">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { UploadFilled, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import VaultFolderPicker from '@/components/VaultFolderPicker/index.vue'
import { getVaultTree, uploadVaultFile, delVaultFile } from '@/api/lingdoc/vault'
import { listTag, addTag, bindTag, getFolderTags, unbindTagByTarget } from '@/api/lingdoc/tag'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const open = ref(false)
const uploadRef = ref(null)
const formRef = ref(null)
const fileTagSelectRef = ref(null)
const folderTagSelectRef = ref(null)

// 真实 Vault 目录树（传给 VaultFolderPicker）
const realVaultTree = ref([])

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

// 所有可用标签（从后端加载 + 用户创建的）
const allTags = ref([])

/** 页面加载时获取真实目录树和标签列表 */
async function initData() {
  try {
    // 加载目录树
    const treeRes = await getVaultTree()
    if (treeRes.code === 200 && treeRes.data) {
      realVaultTree.value = convertTreeData(treeRes.data)
    }
    // 加载标签列表
    const tagRes = await listTag()
    if (tagRes.code === 200 && tagRes.data) {
      allTags.value = tagRes.data.map(t => ({
        tagId: t.tagId,
        tagName: t.tagName,
        tagColor: t.tagColor || '#409EFF'
      }))
    }
  } catch (e) {
    console.error('初始化数据失败', e)
  }
}

/** 将后端目录树格式转换为 VaultFolderPicker 格式 */
function convertTreeData(nodes) {
  if (!nodes || !nodes.length) return []
  return nodes.map(node => ({
    name: node.label,
    path: '/' + node.value,
    children: convertTreeData(node.children)
  }))
}

onMounted(initData)

// 目录标签绑定映射（key: 目录路径，value: 标签对象数组）
const folderTagMap = ref({})

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
const backupFileTag = ref('')
const selectedParentFolder = ref('')
const pendingFolderTag = ref('')
const backupFolderTag = ref('')
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

// 标签颜色池
const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9254DE', '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4']

/** 创建新标签（仅前端，后端创建请直接调用 addTag） */
function createNewTag(name) {
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
async function addFileTag() {
  // allow-create 模式下点击按钮时 v-model 可能未更新，用 filter-method 保存的 backup 作为 fallback
  let name = pendingFileTag.value || backupFileTag.value
  if (!name || !name.trim()) return
  const trimmed = name.trim()
  if (form.tagNames.includes(trimmed)) {
    ElMessage.warning('该标签已存在')
    pendingFileTag.value = ''
    backupFileTag.value = ''
    return
  }

  let tagObj = getTagByName(trimmed)
  if (!tagObj) {
    // 预生成 tagId，确保后续 bindTag 有正确 ID
    const newTagId = 'tag_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    try {
      const res = await addTag({
        tagId: newTagId,
        tagName: trimmed,
        tagColor: colors[allTags.value.length % colors.length]
      })
      if (res.code === 200) {
        tagObj = { tagId: newTagId, tagName: trimmed, tagColor: colors[allTags.value.length % colors.length] }
        allTags.value.push(tagObj)
      }
    } catch (e) {
      tagObj = createNewTag(trimmed)
    }
  }

  form.tagNames.push(trimmed)
  pendingFileTag.value = ''
  backupFileTag.value = ''
}

/** 移除文件标签 */
function removeFileTag(name) {
  form.tagNames = form.tagNames.filter(n => n !== name)
}

/** 添加父文件夹标签 */
async function addFolderTag() {
  if (!selectedParentFolder.value) {
    ElMessage.warning('请先选择父文件夹')
    return
  }

  // allow-create 模式下点击按钮时 v-model 可能未更新，用 filter-method 保存的 backup 作为 fallback
  let name = pendingFolderTag.value || backupFolderTag.value
  if (!name || !name.trim()) return
  const trimmed = name.trim()
  if (currentFolderTagNames.value.includes(trimmed)) {
    ElMessage.warning('该标签已存在')
    pendingFolderTag.value = ''
    backupFolderTag.value = ''
    return
  }

  let tagObj = getTagByName(trimmed)
  if (!tagObj) {
    // 预生成 tagId，确保后续 bindTag 有正确 ID
    const newTagId = 'tag_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    try {
      const res = await addTag({
        tagId: newTagId,
        tagName: trimmed,
        tagColor: colors[allTags.value.length % colors.length]
      })
      if (res.code === 200) {
        tagObj = { tagId: newTagId, tagName: trimmed, tagColor: colors[allTags.value.length % colors.length] }
        allTags.value.push(tagObj)
      }
    } catch (e) {
      ElMessage.error('创建标签失败')
      return
    }
  }

  // 绑定到后端
  try {
    await bindTag({
      targetType: 'D',
      targetId: selectedParentFolder.value,
      tagId: tagObj.tagId
    })
  } catch (e) {
    ElMessage.error('绑定标签失败')
    return
  }

  const currentTags = folderTagMap.value[selectedParentFolder.value] || []
  folderTagMap.value[selectedParentFolder.value] = [...currentTags, tagObj]
  pendingFolderTag.value = ''
  backupFolderTag.value = ''
  syncInheritedTags()
}

/** 移除父文件夹标签 */
async function removeFolderTag(name) {
  const currentTags = folderTagMap.value[selectedParentFolder.value] || []
  folderTagMap.value[selectedParentFolder.value] = currentTags.filter(t => t.tagName !== name)

  // 同步到后端：先解绑该目录所有标签，再重新绑定剩余标签
  try {
    await unbindTagByTarget('D', selectedParentFolder.value)
    const remaining = folderTagMap.value[selectedParentFolder.value] || []
    for (const tag of remaining) {
      await bindTag({ targetType: 'D', targetId: selectedParentFolder.value, tagId: tag.tagId })
    }
  } catch (e) {
    ElMessage.error('更新文件夹标签失败')
  }

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

/** 文件变更回调（仅添加到前端列表，不上传） */
function handleFileChange(uploadFile) {
  const raw = uploadFile.raw
  const ext = raw.name.split('.').pop()
  const fileId = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)

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
    inheritedTags: [],
    _raw: raw
  }

  fileList.value = [...fileList.value, item]
  ElMessage.success(`已添加文件：${raw.name}，点击"确认"后保存到 Vault`)
}

/** 多选框变化 */
function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.fileId)
}

/** 自动规整按钮（单个文件） */
function handleOrganize(row) {
  // 预留：后端就绪后对接 organizeUpload API
  ElMessage.info('自动规整功能开发中...')
}

/** 批量自动规整 */
function handleBatchOrganize() {
  // 预留：后端就绪后对接 batchOrganizeUpload API
  ElMessage.info('批量自动规整功能开发中...')
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

/** 提交表单（确认）：实际上传到 Vault */
async function submitForm() {
  if (!form.suggestedName.trim()) {
    ElMessage.warning('文件名不能为空')
    return
  }

  const idx = fileList.value.findIndex(f => f.fileId === form.fileId)
  if (idx === -1) return

  const row = fileList.value[idx]
  const tagObjects = form.tagNames.map(name => getTagByName(name)).filter(Boolean)
  const inherited = computeInheritedTags(form.suggestedPath)

  // 如果文件尚未上传（有 _raw），先上传
  let fileId = row.fileId
  if (row._raw) {
    const fd = new FormData()
    fd.append('file', row._raw)
    const subPath = form.suggestedPath ? form.suggestedPath.replace(/^\//, '') : ''
    if (subPath) fd.append('subPath', subPath)

    try {
      const res = await uploadVaultFile(fd)
      if (res.code === 200 && res.data) {
        fileId = res.data.fileId
        fileList.value[idx] = {
          ...fileList.value[idx],
          fileId: res.data.fileId,
          originalName: res.data.fileName,
          vaultPath: res.data.vaultPath,
          _raw: null
        }
      } else {
        ElMessage.error(res.msg || '上传失败')
        return
      }
    } catch (e) {
      ElMessage.error('上传失败：' + (e.message || e))
      return
    }
  }

  // 先解绑旧标签，避免重复绑定导致唯一键冲突
  try {
    await unbindTagByTarget('F', fileId)
  } catch (e) {
    console.error('解绑旧标签失败', e)
  }

  // 保存文件标签
  if (tagObjects.length > 0) {
    try {
      for (const tag of tagObjects) {
        await bindTag({ targetType: 'F', targetId: fileId, tagId: tag.tagId })
      }
    } catch (e) {
      console.error('保存文件标签失败', e)
    }
  }

  fileList.value[idx] = {
    ...fileList.value[idx],
    suggestedName: form.suggestedName,
    suggestedPath: form.suggestedPath,
    remark: form.remark,
    tags: tagObjects,
    inheritedTags: inherited,
    status: 'confirmed'
  }

  open.value = false
  resetForm()
  ElMessage.success('确认成功')
}

/** 单个确认：实际上传到 Vault */
async function handleConfirm(row) {
  if (!row._raw) {
    ElMessage.warning('找不到文件数据')
    return
  }

  const idx = fileList.value.findIndex(f => f.fileId === row.fileId)
  if (idx === -1) return

  // 构建 FormData
  const formData = new FormData()
  formData.append('file', row._raw)
  const subPath = row.suggestedPath ? row.suggestedPath.replace(/^\//, '') : ''
  if (subPath) {
    formData.append('subPath', subPath)
  }

  try {
    const res = await uploadVaultFile(formData)
    if (res.code === 200 && res.data) {
      const result = res.data
      // 更新文件信息
      fileList.value[idx] = {
        ...fileList.value[idx],
        fileId: result.fileId,
        originalName: result.fileName,
        suggestedName: result.fileName,
        vaultPath: result.vaultPath,
        status: 'confirmed',
        _raw: null
      }

      // 保存文件标签
      if (row.tags && row.tags.length > 0) {
        for (const tag of row.tags) {
          await bindTag({ targetType: 'F', targetId: result.fileId, tagId: tag.tagId })
        }
      }

      ElMessage.success('文件已保存到 Vault')
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败：' + (e.message || e))
  }
}

/** 删除单个 */
async function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除文件 "${row.originalName}"？`).then(async () => {
    // 若已上传到后端（fileId 非 temp_ 开头），先调用后端删除
    if (row.fileId && !row.fileId.startsWith('temp_')) {
      try {
        await delVaultFile(row.fileId)
      } catch (e) {
        console.error('后端删除失败', e)
      }
    }
    fileList.value = fileList.value.filter(f => f.fileId !== row.fileId)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 批量确认 */
async function handleBatchConfirm() {
  const pendingList = fileList.value.filter(f => ['uploaded', 'pending'].includes(f.status))
  if (!pendingList.length) {
    ElMessage.warning('没有待确认的文件')
    return
  }
  let successCount = 0
  let failCount = 0
  for (const row of pendingList) {
    const idx = fileList.value.findIndex(f => f.fileId === row.fileId)
    if (idx === -1) continue
    if (!row._raw) {
      // 没有原始文件数据，仅改状态
      fileList.value[idx] = { ...fileList.value[idx], status: 'confirmed' }
      successCount++
      continue
    }
    const formData = new FormData()
    formData.append('file', row._raw)
    const subPath = row.suggestedPath ? row.suggestedPath.replace(/^\//, '') : ''
    if (subPath) formData.append('subPath', subPath)
    try {
      const res = await uploadVaultFile(formData)
      if (res.code === 200 && res.data) {
        const result = res.data
        fileList.value[idx] = {
          ...fileList.value[idx],
          fileId: result.fileId,
          originalName: result.fileName,
          suggestedName: result.fileName,
          vaultPath: result.vaultPath,
          status: 'confirmed',
          _raw: null
        }
        // 保存文件标签
        if (row.tags && row.tags.length > 0) {
          for (const tag of row.tags) {
            await bindTag({ targetType: 'F', targetId: result.fileId, tagId: tag.tagId })
          }
        }
        successCount++
      } else {
        failCount++
        fileList.value[idx] = { ...fileList.value[idx], status: 'failed' }
      }
    } catch (e) {
      failCount++
      fileList.value[idx] = { ...fileList.value[idx], status: 'failed' }
    }
  }
  if (failCount > 0) {
    ElMessage.warning(`成功 ${successCount} 个，失败 ${failCount} 个`)
  } else {
    ElMessage.success(`成功归档 ${successCount} 个文件`)
  }
}

/** 批量删除 */
async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择一项')
    return
  }
  proxy.$modal.confirm(`是否确认删除选中的 ${selectedIds.value.length} 个文件？`).then(async () => {
    for (const id of selectedIds.value) {
      const row = fileList.value.find(f => f.fileId === id)
      if (row && row.fileId && !row.fileId.startsWith('temp_')) {
        try {
          await delVaultFile(row.fileId)
        } catch (e) {
          console.error('后端删除失败', e)
        }
      }
    }
    fileList.value = fileList.value.filter(f => !selectedIds.value.includes(f.fileId))
    selectedIds.value = []
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 清空列表 */
async function handleClean() {
  if (!fileList.value.length) {
    ElMessage.warning('列表已为空')
    return
  }
  proxy.$modal.confirm('是否确认清空所有已上传文件？').then(async () => {
    for (const row of fileList.value) {
      if (row.fileId && !row.fileId.startsWith('temp_')) {
        try {
          await delVaultFile(row.fileId)
        } catch (e) {
          console.error('后端删除失败', e)
        }
      }
    }
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
