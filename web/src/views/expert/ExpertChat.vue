<template>
  <div class="expert-chat-page">
    <el-card shadow="never" class="chat-card">
      <div class="chat-layout">
        <!-- 左侧：会话列表 -->
        <div class="conv-panel">
          <div class="conv-header">
            <span class="conv-title">咨询会话</span>
            <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" class="unread-badge">
              <el-button size="small" :loading="loading" @click="loadAll">刷新</el-button>
            </el-badge>
          </div>
          <el-tabs v-model="statusFilter" class="conv-tabs" @tab-change="switchStatus">
            <el-tab-pane label="等待中" name="WAITING" />
            <el-tab-pane label="进行中" name="ACTIVE" />
            <el-tab-pane label="已关闭" name="CLOSED" />
            <el-tab-pane label="全部" name="ALL" />
          </el-tabs>
          <div v-loading="loading" class="conv-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conv-item"
              :class="{ active: currentConversation?.id === conv.id }"
              @click="openConversation(conv)"
            >
              <div class="conv-item-top">
                <span class="conv-subject">{{ conv.subject }}</span>
                <el-tag v-if="conv.unreadCount > 0" type="danger" size="small">
                  {{ conv.unreadCount }}
                </el-tag>
              </div>
              <div class="conv-item-meta">
                <span class="conv-user">{{ conv.userName || '用户' }}</span>
                <span class="conv-time">{{ formatTime(conv.createdAt) }}</span>
              </div>
              <div class="conv-last">
                <span v-if="conv.lastMessage" class="conv-preview">{{ conv.lastMessage }}</span>
                <span v-else class="conv-preview muted">暂无消息</span>
                <el-tag :type="convStatusTag(conv.status)" size="small" class="conv-status">
                  {{ convStatusLabel(conv.status) }}
                </el-tag>
              </div>
            </div>
            <el-empty v-if="!loading && conversations.length === 0" description="暂无会话" :image-size="60" />
          </div>
          <div class="conv-pager">
            <el-pagination
              v-if="convTotal > 0"
              layout="prev, pager, next"
              :total="convTotal"
              :page-size="convSize"
              :current-page="convPage"
              small
              @current-change="onPageChange"
            />
          </div>
        </div>

        <!-- 右侧：聊天窗口 -->
        <div class="msg-panel">
          <template v-if="currentConversation">
            <div class="msg-header">
              <div>
                <span class="msg-title">{{ currentConversation.subject }}</span>
                <span class="msg-sub">与 {{ currentConversation.userName || '用户' }} 的会话</span>
              </div>
              <div class="msg-actions">
                <el-tag :type="convStatusTag(currentConversation.status)" size="small">
                  {{ convStatusLabel(currentConversation.status) }}
                </el-tag>
                <el-button
                  v-if="currentConversation.status === 'CLOSED'"
                  size="small"
                  type="primary"
                  plain
                  :loading="reopening"
                  @click="doReopenConversation"
                >
                  重新开启
                </el-button>
                <el-button
                  v-if="currentConversation.status !== 'CLOSED'"
                  size="small"
                  type="warning"
                  plain
                  :loading="closing"
                  @click="doCloseConversation"
                >
                  关闭会话
                </el-button>
              </div>
            </div>
            <div ref="msgListRef" v-loading="msgLoading" class="msg-list">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="msg-item"
                :class="msg.senderType === 'EXPERT' ? 'from-expert' : 'from-user'"
              >
                <div class="msg-meta">
                  <span class="msg-sender">
                    {{ msg.senderType === 'EXPERT' ? '我（专家）' : '用户' }}
                  </span>
                  <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
                </div>
                <div class="msg-bubble">
                  <template v-if="msg.messageType === 'TEXT'">{{ msg.content }}</template>
                  <template v-else-if="msg.messageType === 'IMAGE' || msg.messageType === 'VIDEO'">
                    <el-tag size="small" type="info">{{ msg.messageType === 'IMAGE' ? '图片消息' : '视频消息' }}</el-tag>
                    <span v-if="msg.content" class="msg-attach-text">{{ msg.content }}</span>
                  </template>
                  <template v-else-if="msg.messageType === 'ENV_SNAPSHOT'">
                    <SnapshotCard :snapshot="parseSnapshot(msg.snapshotData)" />
                  </template>
                  <template v-else>[{{ msg.messageType }}]</template>
                </div>
              </div>
              <el-empty v-if="!msgLoading && messages.length === 0" description="暂无消息，等待用户发起咨询" :image-size="60" />
            </div>
            <div v-if="currentConversation.status !== 'CLOSED'" class="msg-input">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="输入回复内容，回车发送（Shift+Enter 换行）"
                @keydown.enter.exact.prevent="doSend"
              />
              <div class="input-actions">
                <span class="input-tip">首次回复后会话将变为「进行中」</span>
                <el-button type="primary" :loading="sending" @click="doSend">发送</el-button>
              </div>
            </div>
            <div v-else class="msg-closed">
              <el-icon><CircleClose /></el-icon>
              <span>会话已关闭</span>
            </div>
          </template>
          <div v-else class="msg-empty">
            <el-icon :size="40"><ChatDotRound /></el-icon>
            <p>选择左侧会话开始回复</p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, CircleClose } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getConversations,
  getConversationMessages,
  sendMessage,
  closeConversation,
  reopenConversation,
  getChatUnread
} from '@/api/chat'
import chatSocket from '@/utils/chatSocket'
import SnapshotCard from './SnapshotCard.vue'

const authStore = useAuthStore()

const loading = ref(false)
const conversations = ref([])
const convTotal = ref(0)
const convPage = ref(1)
const convSize = ref(20)
const statusFilter = ref('WAITING')
const unreadTotal = ref(0)

const currentConversation = ref(null)
const messages = ref([])
const msgLoading = ref(false)
const msgListRef = ref(null)
const draft = ref('')
const sending = ref(false)
const closing = ref(false)
const reopening = ref(false)

let refreshTimer = null

// ===== 会话列表 =====
async function loadConversations() {
  loading.value = true
  try {
    const params = { page: convPage.value - 1, size: convSize.value }
    if (statusFilter.value !== 'ALL') params.status = statusFilter.value
    const res = await getConversations(params)
    conversations.value = res.data || []
    convTotal.value = res.data?.length || 0
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function loadUnread() {
  try {
    const res = await getChatUnread()
    unreadTotal.value = res.data?.count || 0
  } catch (e) { /* handled */ }
}

function switchStatus() {
  convPage.value = 1
  loadConversations()
}

function onPageChange(page) {
  convPage.value = page
  loadConversations()
}

// ===== 消息 =====
async function openConversation(conv) {
  currentConversation.value = conv
  await loadMessages(conv.id)
  // R28：查看消息后后端已标记已读，刷新未读数与列表角标
  loadUnread()
  loadConversations()
}

async function loadMessages(id) {
  msgLoading.value = true
  try {
    const res = await getConversationMessages(id, { page: 0, size: 100 })
    messages.value = res.data || []
    scrollToBottom()
  } catch (e) { /* handled */ } finally { msgLoading.value = false }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

async function doSend() {
  const content = draft.value.trim()
  if (!content) return
  if (!currentConversation.value) return
  sending.value = true
  try {
    const msg = await sendMessage({
      conversationId: currentConversation.value.id,
      messageType: 'TEXT',
      content
    })
    if (msg.data && !messages.value.some(m => m.id === msg.data.id)) {
      messages.value.push(msg.data)
      scrollToBottom()
      draft.value = ''
      if (currentConversation.value.status === 'WAITING') {
        currentConversation.value.status = 'ACTIVE'
      }
    }
  } catch (e) { /* handled */ } finally { sending.value = false }
}

async function doCloseConversation() {
  if (!currentConversation.value) return
  closing.value = true
  try {
    await closeConversation(currentConversation.value.id)
    currentConversation.value.status = 'CLOSED'
    ElMessage.success('会话已关闭')
    loadConversations()
  } catch (e) { /* handled */ } finally { closing.value = false }
}

async function doReopenConversation() {
  if (!currentConversation.value) return
  reopening.value = true
  try {
    await reopenConversation(currentConversation.value.id)
    currentConversation.value.status = 'ACTIVE'
    ElMessage.success('会话已重新开启，可继续沟通')
    loadConversations()
    loadMessages(currentConversation.value.id)
  } catch (e) { /* handled */ } finally { reopening.value = false }
}

// ===== WebSocket 实时消息 =====
function onWsMessage(payload) {
  if (!payload) return
  // 刷新未读数与列表
  loadUnread()
  loadConversations()
  // 若消息属于当前会话，追加消息
  const convId = payload.conversationId
  if (currentConversation.value && currentConversation.value.id === convId) {
    // 避免与 REST 轮询重复追加（按 id 去重）
    if (!messages.value.some(m => m.id === payload.id)) {
      messages.value.push(payload)
      scrollToBottom()
    }
  }
}

function startPolling() {
  refreshTimer = setInterval(() => {
    loadUnread()
    loadConversations()
    if (currentConversation.value) {
      loadMessages(currentConversation.value.id)
    }
  }, 30000)
}

function loadAll() {
  loadUnread()
  loadConversations()
}

function parseSnapshot(json) {
  try {
    return typeof json === 'string' ? JSON.parse(json) : json
  } catch (e) {
    return null
  }
}

function convStatusLabel(s) {
  const m = { WAITING: '等待中', ACTIVE: '进行中', CLOSED: '已关闭' }
  return m[s] || s
}

function convStatusTag(s) {
  const m = { WAITING: 'warning', ACTIVE: 'success', CLOSED: 'info' }
  return m[s] || 'info'
}

function formatTime(d) {
  if (!d) return ''
  const dt = new Date(d)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

onMounted(() => {
  loadAll()
  chatSocket.connect(onWsMessage)
  startPolling()
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  chatSocket.disconnect()
})
</script>

<style scoped>
.expert-chat-page {
  height: calc(100vh - 120px);
}
.chat-card {
  height: 100%;
}
.chat-card :deep(.el-card__body) {
  height: 100%;
  padding: 0;
}
.chat-layout {
  display: flex;
  height: 100%;
}
.conv-panel {
  width: 340px;
  flex-shrink: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
}
.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 0;
}
.conv-title {
  font-size: 15px;
  font-weight: 600;
  color: #e0e6ed;
}
.conv-tabs {
  padding: 0 12px;
}
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 12px;
}
.conv-item {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.05);
  transition: all .15s;
}
.conv-item:hover {
  border-color: #409EFF;
}
.conv-item.active {
  border-color: #409EFF;
  background: #ecf5ff;
}
.conv-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.conv-subject {
  font-size: 14px;
  font-weight: 600;
  color: #e0e6ed;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.conv-user {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.conv-time {
  flex-shrink: 0;
}
.conv-last {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
}
.conv-preview {
  font-size: 12px;
  color: #a0aec0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-preview.muted { color: #64748b; }
.conv-status { flex-shrink: 0; }
.conv-pager {
  padding: 8px 12px;
  display: flex;
  justify-content: flex-end;
}
.msg-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.msg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.msg-title {
  font-size: 15px;
  font-weight: 600;
  color: #e0e6ed;
}
.msg-sub {
  margin-left: 10px;
  font-size: 12px;
  color: #94a3b8;
}
.msg-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.msg-item {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}
.msg-item.from-expert { align-self: flex-end; align-items: flex-end; }
.msg-item.from-user { align-self: flex-start; align-items: flex-start; }
.msg-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}
.msg-sender { font-weight: 600; }
.msg-bubble {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #e0e6ed;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-item.from-expert .msg-bubble {
  background: rgba(64, 158, 255, 0.14);
  border-color: rgba(64, 158, 255, 0.4);
}
.msg-attach-text { margin-left: 6px; }
.msg-input {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 12px 16px;
}
.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.input-tip { font-size: 12px; color: #64748b; }
.msg-closed {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px;
  color: #94a3b8;
}
.msg-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
}
.msg-empty p { margin-top: 8px; font-size: 14px; }
</style>
