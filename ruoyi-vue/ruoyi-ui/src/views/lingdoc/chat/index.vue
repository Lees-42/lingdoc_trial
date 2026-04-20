<template>
  <div class="app-container chat-container">
    <el-row :gutter="20" style="height: calc(100vh - 120px);">
      <!-- 左侧会话列表 -->
      <el-col :span="6" style="height: 100%;">
        <el-card style="height: 100%; display: flex; flex-direction: column;">
          <template #header>
            <div class="card-header">
              <span>对话列表</span>
              <el-button type="primary" size="small" @click="handleNewChat">
                <el-icon><Plus /></el-icon>新建
              </el-button>
            </div>
          </template>

          <!-- 知识库选择（新建会话时�?-->
          <el-select v-model="selectedKbId" placeholder="选择知识库（可选）" clearable style="margin-bottom: 15px;">
            <el-option
              v-for="kb in kbList"
              :key="kb.kbId"
              :label="kb.kbName"
              :value="kb.kbId"
            />
          </el-select>

          <!-- 会话列表 -->
          <div class="session-list" v-loading="loading">
            <div
              v-for="session in sessionList"
              :key="session.sessionId"
              :class="['session-item', { active: currentSessionId === session.sessionId }]"
              @click="switchSession(session)"
            >
              <div class="session-title">
                <el-icon v-if="session.isPinned"><Top /></el-icon>
                {{ session.sessionTitle }}
              </div>
              <div class="session-meta">
                <el-tag size="small" :type="session.sessionType === 1 ? 'success' : 'info'">
                  {{ session.sessionTypeName }}
                </el-tag>
                <span v-if="session.kbName" class="kb-name">{{ session.kbName }}</span>
              </div>
              <div class="session-stats">
                {{ session.messageCount || 0 }} 消息 · 
                {{ formatTime(session.lastMessageAt) }}
              </div>
              <el-button
                class="delete-btn"
                link
                type="danger"
                size="small"
                @click.stop="handleDeleteSession(session)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <el-empty v-if="!loading && sessionList.length === 0" description="暂无对话" />
        </el-card>
      </el-col>

      <!-- 右侧聊天区域 -->
      <el-col :span="18" style="height: 100%;">
        <el-card style="height: 100%; display: flex; flex-direction: column;">
          <!-- 聊天标题 -->
          <template #header v-if="currentSession">
            <div class="chat-header">
              <span>{{ currentSession.sessionTitle }}</span>
              <el-tag v-if="currentSession.kbName" size="small" type="success">
                {{ currentSession.kbName }}
              </el-tag>
            </div>
          </template>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div v-if="!currentSessionId" class="empty-chat">
              <el-empty description="选择一个对话或新建对话开始聊�? />
            </div>

            <template v-else>
              <div
                v-for="msg in messageList"
                :key="msg.messageId"
                :class="['message-item', msg.role]"
              >
                <div class="message-avatar">
                  <el-avatar :size="40" :icon="msg.role === 'user' ? User : ChatDotRound" />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="role-name">{{ msg.role === 'user' ? '�? : 'AI助手' }}</span>
                    <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                  </div>
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                </div>
              </div>

              <!-- AI正在输入提示 -->
              <div v-if="isLoading" class="message-item assistant">
                <div class="message-avatar">
                  <el-avatar :size="40" :icon="el-icon-chat-dot-round" />
                </div>
                <div class="message-content">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- 参考来源（仅知识库问答显示�?-->
          <div v-if="lastResponse?.usedRag && lastResponse?.sources" class="sources-panel">
            <div class="sources-title">参考来源：</div>
            <div class="sources-list">
              <el-tag
                v-for="(source, index) in lastResponse.sources"
                :key="index"
                size="small"
                effect="plain"
                class="source-tag"
                :title="source.chunkText"
              >
                {{ source.docName }} ({{ (source.relevanceScore * 100).toFixed(0) }}%)
              </el-tag>
            </div>
          </div>

          <!-- 输入�?-->
          <div class="input-area">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息，按Enter发送，Shift+Enter换行..."
              :disabled="!currentSessionId || isLoading"
              @keydown.enter.prevent="handleSend"
            />
            <div class="input-actions">
              <el-checkbox v-model="useRag" :disabled="!currentSession?.kbId">
                使用知识�?
              </el-checkbox>
              <el-button
                type="primary"
                :disabled="!inputMessage.trim() || !currentSessionId || isLoading"
                @click="handleSend"
              >
                发�?<el-icon class="el-icon--right"><Promotion /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="LingDocChat">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, User, ChatDotRound, Top, Promotion } from '@element-plus/icons-vue'
import { listSessions, createSession, deleteSession, getMessages, sendMessage } from '@/api/lingdoc/chat'
import { listKnowledgeBases } from '@/api/lingdoc/knowledge'

const { proxy } = getCurrentInstance()

// 数据状�?
const loading = ref(false)
const isLoading = ref(false)
const sessionList = ref([])
const kbList = ref([])
const currentSessionId = ref('')
const currentSession = computed(() => {
  return sessionList.value.find(s => s.sessionId === currentSessionId.value)
})
const messageList = ref([])
const inputMessage = ref('')
const selectedKbId = ref('')
const useRag = ref(true)
const lastResponse = ref(null)
const messageListRef = ref(null)

// 获取会话列表
function getSessionList() {
  loading.value = true
  listSessions().then(response => {
    sessionList.value = response.rows || []
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// 获取知识库列�?
function getKbList() {
  listKnowledgeBases().then(response => {
    kbList.value = response.data || []
  })
}

// 新建对话
function handleNewChat() {
  const title = inputMessage.value.trim() || '新对�?
  createSession(selectedKbId.value || null, title).then(response => {
    ElMessage.success('创建成功')
    getSessionList()
    switchSession(response.data)
  })
}

// 切换会话
function switchSession(session) {
  currentSessionId.value = session.sessionId
  lastResponse.value = null
  getMessageList()
}

// 获取消息列表
function getMessageList() {
  if (!currentSessionId.value) return
  
  getMessages(currentSessionId.value).then(response => {
    messageList.value = response.data || []
    scrollToBottom()
  })
}

// 发送消�?
function handleSend() {
  const content = inputMessage.value.trim()
  if (!content || !currentSessionId.value || isLoading.value) return

  isLoading.value = true
  const request = {
    content: content,
    sessionId: currentSessionId.value,
    useRag: useRag.value && currentSession.value?.kbId,
    retrievalTopK: 5
  }

  // 先显示用户消�?
  messageList.value.push({
    role: 'user',
    content: content,
    createdAt: new Date().toISOString()
  })
  inputMessage.value = ''
  scrollToBottom()

  sendMessage(request).then(response => {
    lastResponse.value = response.data
    messageList.value.push({
      messageId: response.data.messageId,
      role: 'assistant',
      content: response.data.content,
      createdAt: response.data.createdAt
    })
    isLoading.value = false
    scrollToBottom()
    
    // 更新会话统计
    getSessionList()
  }).catch(error => {
    ElMessage.error(error.message || '发送失�?)
    isLoading.value = false
  })
}

// 删除会话
function handleDeleteSession(session) {
  ElMessageBox.confirm('确认删除该对话？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteSession(session.sessionId).then(() => {
      ElMessage.success('删除成功')
      if (currentSessionId.value === session.sessionId) {
        currentSessionId.value = ''
        messageList.value = []
      }
      getSessionList()
    })
  })
}

// 滚动到底�?
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 格式化消息（简单处理markdown�?
function formatMessage(content) {
  if (!content) return ''
  // 简单的换行处理
  return content.replace(/\n/g, '<br>')
}

// 格式化时�?
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟�?
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时�?
  
  return date.toLocaleDateString()
}

// 初始�?
onMounted(() => {
  getSessionList()
  getKbList()
})
</script>

<style scoped>
.chat-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  margin: 0 -20px;
  padding: 0 20px;
}

.session-item {
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background-color 0.3s;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
}

.session-title {
  font-weight: 500;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.kb-name {
  font-size: 12px;
  color: #909399;
}

.session-stats {
  font-size: 12px;
  color: #909399;
}

.delete-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0;
  transition: opacity 0.3s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding: 0 10px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-content {
  background-color: #409eff;
  color: white;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
  background-color: #f4f4f5;
}

.message-header {
  margin-bottom: 8px;
  font-size: 12px;
}

.message-item.user .message-header {
  color: rgba(255, 255, 255, 0.8);
}

.role-name {
  font-weight: 500;
  margin-right: 10px;
}

.message-time {
  opacity: 0.7;
}

.message-text {
  line-height: 1.6;
  white-space: pre-wrap;
}

.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 10px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background-color: #909399;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

.sources-panel {
  padding: 10px 0;
  border-top: 1px solid #e4e7ed;
}

.sources-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.sources-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-tag {
  cursor: help;
}

.input-area {
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
</style>
