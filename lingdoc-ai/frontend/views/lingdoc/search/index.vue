<template>
  <div class="app-container search-page">
    <!-- 历史侧边栏 -->
    <aside class="history-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-button
          type="primary"
          :icon="Plus"
          class="new-chat-btn"
          @click="createNewSession"
        >
          <span v-show="!sidebarCollapsed">新会话</span>
        </el-button>
        <el-button
          class="toggle-btn"
          :icon="sidebarCollapsed ? Expand : Fold"
          circle
          size="small"
          @click="sidebarCollapsed = !sidebarCollapsed"
        />
      </div>

      <div v-show="!sidebarCollapsed" class="sidebar-body">
        <div v-if="historyList.length === 0" class="history-empty">
          <el-icon :size="32" color="#c0c4cc"><ChatDotSquare /></el-icon>
          <p>暂无会话历史</p>
        </div>
        <div v-else class="history-list">
          <div
            v-for="item in historyList"
            :key="item.sessionId"
            class="history-item"
            :class="{ active: currentSessionId === item.sessionId }"
            @click="switchSession(item.sessionId)"
          >
            <div class="history-item__icon">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="history-item__info">
              <p class="history-item__title" :title="item.title">{{ item.title }}</p>
              <p class="history-item__time">{{ formatTime(item.updateTime) }}</p>
            </div>
            <el-button
              link
              :icon="Delete"
              class="history-item__delete"
              @click.stop="handleDeleteHistory(item.sessionId)"
            />
          </div>
        </div>
      </div>

      <div v-show="!sidebarCollapsed && historyList.length > 0" class="sidebar-footer">
        <el-button link :icon="DeleteFilled" @click="handleCleanHistory">
          清空历史
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="search-main">
      <!-- 页面标题 -->
      <div class="search-header">
        <h2 class="page-title">灵犀问答</h2>
        <p class="page-desc">与 AI 对话，从本地 Vault 中提取答案、定位源文件，支持附件上传与多轮追问。</p>
      </div>

      <!-- 消息列表区 -->
      <div ref="messageScrollRef" class="message-area">
        <!-- 空状态 -->
        <el-empty v-if="currentMessages.length === 0" class="search-empty">
          <template #image>
            <el-icon :size="64" color="#c0c4cc"><Search /></el-icon>
          </template>
          <template #description>
            <div class="empty-content">
              <p class="empty-title">开始你的第一次对话</p>
              <p class="empty-tip">无需记忆文件名，直接用自然语言描述你想找的内容</p>
              <div class="quick-tags">
                <el-tag
                  v-for="tag in quickTags"
                  :key="tag"
                  class="quick-tag"
                  effect="plain"
                  size="large"
                  @click="handleQuickTag(tag)"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-empty>

        <!-- 消息列表 -->
        <div v-else class="message-list">
          <div
            v-for="msg in currentMessages"
            :key="msg.id"
            class="message-row"
            :class="msg.role"
          >
            <!-- 头像 -->
            <div class="message-avatar">
              <el-avatar
                :size="36"
                :icon="msg.role === 'user' ? UserFilled : Cpu"
                :class="msg.role"
              />
            </div>

            <!-- 内容 -->
            <div class="message-bubble">
              <!-- 用户消息 -->
              <template v-if="msg.role === 'user'">
                <div class="message-content">{{ msg.content }}</div>
                <div v-if="msg.attachments && msg.attachments.length" class="message-attachments">
                  <el-tag
                    v-for="(att, idx) in msg.attachments"
                    :key="idx"
                    size="small"
                    type="info"
                    effect="plain"
                    class="msg-attachment-tag"
                  >
                    <svg-icon :icon-class="getFileIcon(att.name.split('.').pop())" style="margin-right:4px" />
                    {{ att.name }}
                  </el-tag>
                </div>
              </template>

              <!-- AI 消息 -->
              <template v-else>
                <!-- 加载中 -->
                <div v-if="msg.status === 'loading'" class="message-loading">
                  <el-icon class="loading-icon"><Loading /></el-icon>
                  <span>{{ msg.content }}</span>
                </div>

                <!-- 正常回答 -->
                <template v-else>
                  <div class="message-content ai-content">{{ msg.content }}</div>

                  <!-- 引用源文件 -->
                  <div v-if="msg.sources && msg.sources.length" class="source-section">
                    <div class="source-header">
                      <el-icon><Document /></el-icon>
                      <span>引用来源（{{ msg.sources.length }}）</span>
                    </div>
                    <div class="source-list">
                      <div
                        v-for="source in msg.sources"
                        :key="source.fileId"
                        class="source-card"
                      >
                        <div class="source-card__info">
                          <div class="source-card__name">
                            <svg-icon :icon-class="getFileIcon(source.fileName)" class="source-icon" />
                            <span>{{ source.fileName }}</span>
                          </div>
                          <div class="source-card__path">{{ source.filePath }}</div>
                          <div v-if="source.snippet" class="source-card__snippet">
                            "{{ source.snippet }}"
                          </div>
                        </div>
                        <div class="source-card__meta">
                          <el-tag size="small" type="info">相关度 {{ source.relevance }}%</el-tag>
                          <el-button link type="primary" size="small" :icon="View">
                            查看
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Token 消耗 -->
                  <div v-if="msg.tokens" class="token-hint">
                    <el-icon><Coin /></el-icon>
                    <span>本次消耗 {{ msg.tokens }} Tokens</span>
                  </div>
                </template>
              </template>

              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="search-input-area">
        <el-card shadow="never" class="input-card">
          <div class="input-wrapper">
            <!-- 功能工具栏 -->
            <div class="toolbar-row">
              <div class="toolbar-switches">
                <el-check-tag
                  :checked="localSearchEnabled"
                  @change="localSearchEnabled = !localSearchEnabled"
                  size="small"
                  class="tool-tag"
                >
                  <el-icon><Collection /></el-icon>
                  本地检索
                </el-check-tag>
                <el-check-tag
                  :checked="webSearchEnabled"
                  @change="webSearchEnabled = !webSearchEnabled"
                  size="small"
                  class="tool-tag"
                >
                  <el-icon><Connection /></el-icon>
                  联网搜索
                </el-check-tag>
                <el-check-tag
                  :checked="deepThinkEnabled"
                  @change="deepThinkEnabled = !deepThinkEnabled"
                  size="small"
                  class="tool-tag"
                >
                  <el-icon><Lightning /></el-icon>
                  深度思考
                </el-check-tag>
              </div>
            </div>

            <!-- 附件标签 -->
            <div v-if="attachedFiles.length" class="attachment-row">
              <el-tag
                v-for="(file, idx) in attachedFiles"
                :key="idx"
                closable
                size="small"
                type="info"
                class="attachment-tag"
                @close="removeAttachment(idx)"
              >
                <svg-icon :icon-class="getFileIcon(file.name.split('.').pop())" style="margin-right:4px" />
                {{ file.name }}
              </el-tag>
            </div>

            <el-input
              v-model="queryText"
              type="textarea"
              :rows="2"
              placeholder="请输入你的问题，支持上传附件…"
              resize="none"
              :disabled="isSearching"
              @keydown.enter.prevent="handleSend"
            />
            <div class="input-actions">
              <div class="input-left">
                <input
                  ref="fileInputRef"
                  type="file"
                  multiple
                  style="display: none"
                  @change="handleFileSelect"
                />
                <el-button
                  link
                  :icon="Paperclip"
                  :disabled="isSearching"
                  @click="fileInputRef?.click()"
                >
                  添加附件
                </el-button>
              </div>
              <div class="input-right">
                <div class="func-bar">
                  <el-check-tag
                    v-for="func in mainFuncs"
                    :key="func.key"
                    :checked="selectedFunc === func.key"
                    @change="handleFuncSelect(func.key)"
                    size="small"
                    class="func-tag"
                    :title="func.label"
                  >
                    <el-icon>
                      <component :is="func.icon" />
                    </el-icon>
                    <span>{{ func.label }}</span>
                  </el-check-tag>
                  <el-dropdown trigger="click" @command="handleFuncSelect">
                    <el-check-tag
                      :checked="isMoreFuncSelected"
                      size="small"
                      class="func-tag more-tag"
                    >
                      <span>{{ moreSelectedLabel || '更多' }}</span>
                      <el-icon><ArrowDown /></el-icon>
                    </el-check-tag>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="func in moreFuncs"
                          :key="func.key"
                          :command="func.key"
                          :class="{ active: selectedFunc === func.key }"
                        >
                          <el-icon style="margin-right:6px">
                            <component :is="func.icon" />
                          </el-icon>
                          {{ func.label }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <span class="input-hint">Enter 发送</span>
                <el-button
                  type="primary"
                  :icon="Promotion"
                  :loading="isSearching"
                  :disabled="!queryText.trim() && !attachedFiles.length"
                  @click="handleSend"
                >
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<script setup name="Search">
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import {
  Plus, Expand, Fold, Delete, DeleteFilled,
  ChatDotSquare, ChatLineRound, Search,
  UserFilled, Cpu, Loading, Document, View, Coin, Promotion,
  Collection, Connection, Lightning, Paperclip,
  Grid, DocumentCopy, Picture, DocumentChecked, EditPen, ArrowDown
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const STORAGE_KEY = 'lingdoc_search_sessions'

// ========== 侧边栏状态 ==========
const sidebarCollapsed = ref(false)

// ========== 会话与消息 ==========
const historyList = ref([])
const currentSessionId = ref('')
const queryText = ref('')
const isSearching = ref(false)
const messageScrollRef = ref(null)
const fileInputRef = ref(null)

// ========== 功能开关 ==========
const localSearchEnabled = ref(true)
const webSearchEnabled = ref(false)
const deepThinkEnabled = ref(false)

// ========== 附件 ==========
const attachedFiles = ref([])

// ========== 功能选项 ==========
const selectedFunc = ref('')
const mainFuncs = [
  { key: 'table', label: '表格填写', icon: Grid },
  { key: 'ppt', label: 'PPT制作', icon: DocumentCopy }
]
const moreFuncs = [
  { key: 'image', label: '生图', icon: Picture },
  { key: 'summary', label: '文档总结', icon: DocumentChecked },
  { key: 'code', label: '代码解释', icon: EditPen }
]

const isMoreFuncSelected = computed(() => {
  return moreFuncs.some(f => f.key === selectedFunc.value)
})

const moreSelectedLabel = computed(() => {
  const found = moreFuncs.find(f => f.key === selectedFunc.value)
  return found ? found.label : ''
})

function handleFuncSelect(key) {
  selectedFunc.value = selectedFunc.value === key ? '' : key
}

// 当前会话的消息列表
const currentMessages = computed(() => {
  const session = historyList.value.find(s => s.sessionId === currentSessionId.value)
  return session ? session.messages : []
})

// 快捷问题标签
const quickTags = [
  '实验报告的截止时间是什么时候？',
  '国家奖学金申请需要哪些材料？',
  '我的实习简历放在哪里了？',
  '上学期操作系统课程的大作业要求'
]

// ========== 模拟答案模板 ==========
const mockTemplates = [
  {
    keywords: ['奖学金', '国家奖学金', '助学金'],
    answer: '根据本地 Vault 中的文档，国家奖学金申请需要以下材料：\n\n1. 《国家奖学金申请审批表》（需双面打印）\n2. 成绩单原件（加盖教务处公章）\n3. 获奖证书复印件\n4. 社会实践证明\n5. 推荐信（导师签字）\n\n申请截止时间为每年的 **10月15日**，需要提交纸质版和电子版各一份。',
    sources: [
      { fileName: '国家奖学金申请表_20260901.docx', filePath: '/申请材料/奖学金', relevance: 98, snippet: '申请材料清单：申请表、成绩单、获奖证书、社会实践证明、推荐信' },
      { fileName: '奖学金申请通知_20260901.pdf', filePath: '/申请材料/奖学金', relevance: 95, snippet: '截止时间：2026年10月15日17:00前，逾期不予受理' }
    ],
    tokens: 342
  },
  {
    keywords: ['实验报告', '实验', '报告要求', '截止时间', 'DDL'],
    answer: '《实验报告要求》中明确说明：\n\n- **提交截止时间**：2026年5月20日 23:59\n- **提交格式**：PDF 格式，命名规则为"学号_姓名_实验X.pdf"\n- **内容要求**：包含实验目的、实验原理、实验步骤、结果分析和思考题五个部分\n- **字数要求**：正文不少于 2000 字\n- **提交平台**：通过教学管理系统上传',
    sources: [
      { fileName: '实验报告要求_20260501.pdf', filePath: '/学习资料/大三下/计算机网络', relevance: 99, snippet: '提交截止时间：2026年5月20日23:59，逾期成绩记零分' },
      { fileName: '实验报告模板.docx', filePath: '/学习资料/大三下/计算机网络', relevance: 88, snippet: '实验报告须包含：目的、原理、步骤、结果分析、思考题' }
    ],
    tokens: 287
  },
  {
    keywords: ['简历', '实习', '工作', '求职'],
    answer: '在本地 Vault 中找到以下与实习相关的文件：\n\n您有一份名为《个人简历_2026版》的文档，存放在 `/工作文档/实习材料` 路径下，最后更新于 2026年3月。\n\n此外，还找到以下相关文件：\n- 实习证明模板\n- 推荐信草稿\n- 面试准备笔记',
    sources: [
      { fileName: '个人简历_2026版.docx', filePath: '/工作文档/实习材料', relevance: 96, snippet: '个人简历 - 张三 - 计算机科学与技术专业' },
      { fileName: '实习证明模板.pdf', filePath: '/工作文档/实习材料', relevance: 82, snippet: '兹证明张三同学于2026年1月至4月在本公司实习' }
    ],
    tokens: 198
  },
  {
    keywords: ['操作系统', '大作业', '课程设计', '期末'],
    answer: '《操作系统课程大作业》要求如下：\n\n**题目**：实现一个简单的进程调度模拟器\n\n**要求**：\n1. 支持 FCFS、SJF、RR 三种调度算法\n2. 提供可视化界面展示调度过程\n3. 撰写设计文档（不少于 3000 字）\n4. 代码需提交至 GitHub 并附链接\n\n**截止时间**：2026年6月30日\n**提交方式**：教学系统 + 课堂演示',
    sources: [
      { fileName: 'OS大作业要求_20260315.pdf', filePath: '/学习资料/大三下/操作系统', relevance: 99, snippet: '实现进程调度模拟器，支持FCFS、SJF、RR算法' },
      { fileName: 'OS课程大纲.pdf', filePath: '/学习资料/大三下/操作系统', relevance: 76, snippet: '大作业占总评成绩的30%，需课堂演示' }
    ],
    tokens: 412
  }
]

const defaultMock = {
  answer: '已在本地 Vault 中检索到相关内容。根据您的提问，我找到了一些可能相关的文档。\n\n建议您提供更具体的关键词（如文件类型、时间范围、课程名称等），以便我给出更精确的答案。\n\n您也可以尝试使用右侧的快捷问题，或继续追问以缩小检索范围。',
  sources: [
    { fileName: '文档索引_2026.xlsx', filePath: '/元数据', relevance: 45, snippet: 'Vault 文档总索引，包含文件名、路径、标签信息' }
  ],
  tokens: 156
}

// ========== 初始化 ==========
onMounted(() => {
  loadSessions()
  if (historyList.value.length === 0) {
    createNewSession()
  } else {
    currentSessionId.value = historyList.value[0].sessionId
  }
})

// ========== 本地存储 ==========
function loadSessions() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      historyList.value = JSON.parse(raw)
    }
  } catch (e) {
    console.error('加载检索历史失败', e)
  }
}

function saveSessions() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(historyList.value))
}

// ========== 会话操作 ==========
function createNewSession() {
  const session = {
    sessionId: 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    title: '新对话',
    messages: [],
    createTime: Date.now(),
    updateTime: Date.now()
  }
  historyList.value.unshift(session)
  currentSessionId.value = session.sessionId
  saveSessions()
}

function switchSession(sessionId) {
  currentSessionId.value = sessionId
}

function updateSessionTitle(sessionId, firstQuestion) {
  const session = historyList.value.find(s => s.sessionId === sessionId)
  if (session && session.title === '新对话') {
    session.title = firstQuestion.slice(0, 20) + (firstQuestion.length > 20 ? '…' : '')
  }
}

function handleDeleteHistory(sessionId) {
  ElMessageBox.confirm('确认删除该检索记录？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    historyList.value = historyList.value.filter(s => s.sessionId !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = historyList.value.length ? historyList.value[0].sessionId : ''
      if (!currentSessionId.value) {
        createNewSession()
      }
    }
    saveSessions()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function handleCleanHistory() {
  ElMessageBox.confirm('确认清空所有检索历史？', '提示', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    historyList.value = []
    saveSessions()
    createNewSession()
    ElMessage.success('清空成功')
  }).catch(() => {})
}

// ========== 发送消息 ==========
function handleFileSelect(e) {
  const files = Array.from(e.target.files || [])
  files.forEach(file => {
    attachedFiles.value.push({
      name: file.name,
      size: file.size,
      type: file.type
    })
  })
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function removeAttachment(index) {
  attachedFiles.value.splice(index, 1)
}

function handleSend() {
  const text = queryText.value.trim()
  if ((!text && !attachedFiles.value.length) || isSearching.value) return

  const session = historyList.value.find(s => s.sessionId === currentSessionId.value)
  if (!session) return

  // 添加用户消息
  const userMsg = {
    id: 'msg_' + Date.now(),
    role: 'user',
    content: text,
    func: selectedFunc.value,
    attachments: attachedFiles.value.map(f => ({ ...f })),
    timestamp: Date.now()
  }
  session.messages.push(userMsg)
  updateSessionTitle(currentSessionId.value, text || '附件消息')
  session.updateTime = Date.now()
  saveSessions()

  const currentFunc = selectedFunc.value
  queryText.value = ''
  attachedFiles.value = []
  selectedFunc.value = ''
  isSearching.value = true
  scrollToBottom()

  // 模拟 AI 思考过程
  const loadingMsg = {
    id: 'msg_' + Date.now() + '_loading',
    role: 'assistant',
    content: '正在理解问题…',
    status: 'loading',
    timestamp: Date.now()
  }
  session.messages.push(loadingMsg)
  saveSessions()
  scrollToBottom()

  // 阶段 1：理解问题
  setTimeout(() => {
    loadingMsg.content = '正在检索本地文档…'
    saveSessions()
    scrollToBottom()

    // 阶段 2：生成答案
    setTimeout(() => {
      const result = generateMockAnswer(text, currentFunc)

      // 移除 loading 消息
      session.messages = session.messages.filter(m => m.id !== loadingMsg.id)

      // 添加 AI 回答
      const aiMsg = {
        id: 'msg_' + Date.now(),
        role: 'assistant',
        content: result.answer,
        status: 'done',
        sources: result.sources,
        tokens: result.tokens,
        timestamp: Date.now()
      }
      session.messages.push(aiMsg)
      session.updateTime = Date.now()
      saveSessions()

      isSearching.value = false
      scrollToBottom()
    }, 1200 + Math.random() * 800)
  }, 800)
}

function handleQuickTag(tag) {
  queryText.value = tag
  handleSend()
}

// ========== 模拟答案生成 ==========
function generateMockAnswer(question, func) {
  const lower = question.toLowerCase()
  let result = null
  for (const tmpl of mockTemplates) {
    if (tmpl.keywords.some(k => lower.includes(k))) {
      result = {
        answer: tmpl.answer,
        sources: tmpl.sources.map((s, i) => ({
          fileId: 'file_' + i,
          ...s
        })),
        tokens: tmpl.tokens
      }
      break
    }
  }
  if (!result) {
    result = {
      answer: defaultMock.answer,
      sources: defaultMock.sources.map((s, i) => ({
        fileId: 'file_' + i,
        ...s
      })),
      tokens: defaultMock.tokens
    }
  }

  // 根据功能调整回答风格
  if (func) {
    const funcLabel = [...mainFuncs, ...moreFuncs].find(f => f.key === func)?.label || func
    switch (func) {
      case 'table':
        result.answer = `【${funcLabel}模式】\n\n已为您整理为表格形式：\n\n| 项目 | 内容 |\n|------|------|\n| 问题 | ${question} |\n\n${result.answer.split('\n').map(line => `| ${line.replace(/^\d+\.\s*/, '').replace(/\*\*(.*?)\*\*/, '$1')} |`).join('\n')}`
        break
      case 'ppt':
        result.answer = `【${funcLabel}模式】\n\n---\n\n# 第1页：封面\n${question}\n\n# 第2页：核心内容\n${result.answer}\n\n# 第3页：总结\n- 关键结论提炼\n- 后续行动建议\n\n---\n\n（以上为 PPT 大纲，可导入 PowerPoint 生成幻灯片）`
        break
      case 'image':
        result.answer = `【${funcLabel}模式】\n\n已根据您的描述生成相关图片：\n\n🖼️ [图片占位]\n\n图片描述：基于"${question}"生成的概念图\n\n（实际环境中将调用图像生成模型）\n\n${result.answer}`
        break
      case 'summary':
        result.answer = `【${funcLabel}模式】\n\n📋 内容摘要：\n\n**核心要点：**\n${result.answer.split('\n').filter(l => l.trim()).slice(0, 5).map((l, i) => `${i + 1}. ${l.replace(/^\d+\.\s*/, '').replace(/\*\*(.*?)\*\*/, '「$1」')}`).join('\n')}\n\n**总结：** 以上是与您提问相关的关键信息提炼。`
        break
      case 'code':
        result.answer = `【${funcLabel}模式】\n\n\`\`\`python\n# 基于问题 "${question}" 的示例代码\ndef search_documents(query):\n    \"\"\"\n    在本地 Vault 中检索文档\n    \"\"\"\n    results = vault.query(query)\n    return results\n\n# 调用示例\nresult = search_documents("${question}")\nprint(result)\n\`\`\`\n\n**代码说明：**\n${result.answer}`
        break
    }
  }

  return result
}

// ========== 文件图标 ==========
function getFileIcon(fileName) {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const iconMap = {
    pdf: 'pdf',
    doc: 'word',
    docx: 'word',
    xls: 'excel',
    xlsx: 'excel',
    png: 'image',
    jpg: 'image',
    jpeg: 'image',
    txt: 'txt',
    md: 'txt'
  }
  return iconMap[ext] || 'file'
}

// ========== 工具函数 ==========
function scrollToBottom() {
  nextTick(() => {
    const el = messageScrollRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const hm = d.toTimeString().slice(0, 5)
  if (isToday) return hm
  const md = `${d.getMonth() + 1}月${d.getDate()}日`
  const y = d.getFullYear()
  if (y === now.getFullYear()) return `${md} ${hm}`
  return `${y}年${md} ${hm}`
}

// 监听消息变化自动滚动
watch(() => currentMessages.value.length, () => {
  scrollToBottom()
})
</script>

<style scoped>
.search-page {
  display: flex;
  height: calc(100vh - 84px);
  padding: 0;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.history-sidebar {
  display: flex;
  flex-direction: column;
  width: 260px;
  min-width: 260px;
  border-right: 1px solid var(--el-border-color-light);
  background: #fafbfc;
  transition: width 0.3s ease, min-width 0.3s ease;
}

.history-sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.new-chat-btn {
  flex: 1;
  margin-right: 8px;
}

.collapsed .new-chat-btn {
  margin-right: 0;
  padding: 8px;
}

.collapsed .new-chat-btn :deep(span) {
  display: none;
}

.toggle-btn {
  flex-shrink: 0;
}

.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
}

.history-empty p {
  margin-top: 8px;
  font-size: 13px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.history-item:hover {
  background: #eceff5;
}

.history-item:hover .history-item__delete {
  opacity: 1;
}

.history-item.active {
  background: #e6f0ff;
}

.history-item__icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #e8ecf1;
  color: #5a6b7c;
}

.history-item__info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.history-item__title {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item__time {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.history-item__delete {
  opacity: 0;
  padding: 4px !important;
  transition: opacity 0.2s;
  color: var(--el-text-color-secondary);
}

.sidebar-footer {
  padding: 8px 12px;
  border-top: 1px solid var(--el-border-color-light);
  text-align: center;
}

/* ========== 主内容区 ========== */
.search-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: #fff;
}

.search-header {
  flex-shrink: 0;
  padding: 20px 24px 0;
}

.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

/* ========== 消息区 ========== */
.message-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
}

.search-empty :deep(.el-empty__description) {
  margin-top: 16px;
}

.empty-content {
  text-align: center;
}

.empty-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.empty-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  max-width: 600px;
  margin: 0 auto;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: #f0f7ff;
}

/* ========== 消息列表 ========== */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar .el-avatar {
  background: #e8ecf1;
  color: #5a6b7c;
}

.message-avatar .el-avatar.user {
  background: var(--el-color-primary);
  color: #fff;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f7fa;
  font-size: 14px;
  line-height: 1.8;
  color: var(--el-text-color-primary);
}

.message-row.user .message-bubble {
  background: var(--el-color-primary-light-9);
  color: var(--el-text-color-primary);
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-content {
  line-height: 1.9;
}

.message-time {
  margin-top: 6px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  text-align: right;
}

/* 加载中 */
.message-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.loading-icon {
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 引用源 */
.source-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--el-border-color-light);
}

.source-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
}

.source-card__info {
  flex: 1;
  min-width: 0;
}

.source-card__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.source-icon {
  font-size: 16px;
  color: var(--el-color-primary);
}

.source-card__path {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.source-card__snippet {
  margin-top: 6px;
  padding: 6px 10px;
  border-radius: 4px;
  background: #f8f9fb;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  font-style: italic;
}

.source-card__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

/* Token 消耗 */
.token-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* ========== 消息附件 ========== */
.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.msg-attachment-tag {
  display: inline-flex;
  align-items: center;
}

/* ========== 输入区 ========== */
.search-input-area {
  flex-shrink: 0;
  padding: 12px 24px 20px;
}

.input-card {
  max-width: 900px;
  margin: 0 auto;
  border-radius: 16px;
}

.input-card :deep(.el-card__body) {
  padding: 12px 16px;
}

/* 功能工具栏 */
.toolbar-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.toolbar-switches {
  display: flex;
  gap: 8px;
}

.tool-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-tag.is-checked {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

/* 附件标签 */
.attachment-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.attachment-tag {
  display: inline-flex;
  align-items: center;
}

.input-wrapper :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 4px 0;
  font-size: 14px;
  resize: none;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 功能选项栏 */
.func-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 4px;
}

.func-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.func-tag.is-checked {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.more-tag .el-icon {
  margin-left: 2px;
  font-size: 10px;
}

:deep(.el-dropdown-menu__item.active) {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .history-sidebar {
    position: absolute;
    z-index: 100;
    height: 100%;
  }

  .history-sidebar.collapsed {
    width: 0;
    min-width: 0;
    overflow: hidden;
    border: none;
  }

  .message-bubble {
    max-width: 92%;
  }
}
</style>
