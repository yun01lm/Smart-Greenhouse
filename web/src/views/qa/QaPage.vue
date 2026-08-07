<template>
  <div class="qa-page">
    <div class="qa-header">
      <div class="qa-header-top">
        <h3>🤖 AI 智能问答</h3>
        <span class="qa-hint">基于知识库的 RAG 智能问答，支持引用来源追溯</span>
      </div>
      <div class="qa-toolbar">
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="按日期查询历史"
          value-format="YYYY-MM-DD"
          :clearable="true"
          style="width: 190px"
          @change="onDateChange"
        />
        <el-button size="small" :disabled="!filterDate" @click="clearDateFilter">显示最近</el-button>
      </div>
    </div>

    <div class="qa-chat" ref="chatContainer">
      <div v-if="groups.length === 0 && !loadingHistory" class="qa-empty">
        <p>👋 你好！我是智慧大棚AI助手</p>
        <p>可以问我关于大棚管理、病虫害防治、作物种植等问题</p>
      </div>

      <template v-for="(group, gi) in groups" :key="group.key">
        <div class="qa-date-divider">{{ group.label }}</div>
        <div
          v-for="(msg, idx) in group.items"
          :key="group.key + '-' + idx"
          :class="['qa-message', msg.role]"
        >
          <template v-if="msg.role === 'user'">
            <div class="msg-bubble user-bubble">
              <div class="msg-text">{{ msg.content }}</div>
              <div v-if="msg.isVoice" class="msg-voice-tag">🎤 语音输入</div>
            </div>
            <div class="msg-time">{{ msg.timeText }}</div>
          </template>

          <template v-else-if="msg.role === 'ai'">
            <div class="msg-bubble ai-bubble">
              <div class="msg-text">{{ msg.content }}</div>
              <div v-if="msg.sources && msg.sources.length > 0" class="msg-sources">
                <div class="sources-title">📚 参考来源：</div>
                <div v-for="(src, si) in msg.sources" :key="si" class="source-item">
                  • {{ src.title }}
                  <el-tag size="small" type="info">{{ src.category }}</el-tag>
                </div>
              </div>
              <el-button
                class="btn-tts"
                :type="speakingIdx === idx ? 'warning' : 'primary'"
                size="small"
                text
                @click="toggleTts(msg.content, idx)"
              >
                {{ speakingIdx === idx ? '⏹ 停止' : '🔊 播放' }}
              </el-button>
            </div>
            <div class="msg-time">{{ msg.timeText }}</div>
          </template>

          <template v-else>
            <div class="msg-bubble error-bubble">
              <div class="msg-text">❌ {{ msg.content }}</div>
            </div>
          </template>
        </div>
      </template>

      <div v-if="loading" class="qa-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>{{ loadingHistory ? '历史记录加载中...' : 'AI 正在思考...' }}</span>
      </div>
    </div>

    <div class="qa-input">
      <el-input
        v-model="inputText"
        placeholder="输入你的问题..."
        :disabled="loading"
        @keyup.enter="sendMessage"
        clearable
      >
        <template #append>
          <el-button
            type="primary"
            :disabled="!inputText.trim() || loading"
            @click="sendMessage"
          >发送</el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { askText, getRecords } from '@/api/qa'
import { ElMessage } from 'element-plus'

const props = defineProps({
  greenhouseId: { type: Number, default: null }
})

// 历史分组：[{ key: '2026-08-07', label: '今天', items: [{role, content, sources, isVoice, timeText}] }]
const groups = ref([])
const inputText = ref('')
const loading = ref(false)
const loadingHistory = ref(false)
const filterDate = ref('')
const chatContainer = ref(null)
const speakingIdx = ref(-1)

let speechSynth = null

const pad = n => String(n).padStart(2, '0')

function dateKey(dt) {
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`
}

/** 分组标题：今天 / 昨天 / 今年M月D日 / 往年YYYY年M月D日 */
function groupLabel(key) {
  const now = new Date()
  const [y, m, d] = key.split('-').map(Number)
  if (key === dateKey(now)) return '今天'
  const yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1)
  if (key === dateKey(yesterday)) return '昨天'
  return y === now.getFullYear() ? `${m}月${d}日` : `${y}年${m}月${d}日`
}

/** 消息时间：今天只显示 HH:mm；今年显示 MM-DD HH:mm；往年显示 YYYY-MM-DD HH:mm */
function formatTime(dt) {
  const now = new Date()
  const hm = `${pad(dt.getHours())}:${pad(dt.getMinutes())}`
  if (dateKey(dt) === dateKey(now)) return hm
  const md = `${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`
  if (dt.getFullYear() === now.getFullYear()) return `${md} ${hm}`
  return `${dt.getFullYear()}-${md} ${hm}`
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  })
}

/** 历史记录（接口倒序返回）→ 按日期分组（正序展示，早在上、新在下） */
function buildGroups(list) {
  const ordered = [...list].reverse()
  const map = new Map()
  for (const item of ordered) {
    const dt = new Date(item.createdAt)
    if (Number.isNaN(dt.getTime())) continue
    const key = dateKey(dt)
    if (!map.has(key)) map.set(key, [])
    const items = map.get(key)
    items.push({
      role: 'user',
      content: item.question || '',
      isVoice: item.inputType === 'VOICE',
      timeText: formatTime(dt)
    })
    if (item.answer) {
      items.push({
        role: 'ai',
        content: item.answer,
        sources: item.sources || [],
        timeText: formatTime(dt)
      })
    }
  }
  groups.value = Array.from(map.entries()).map(([key, items]) => ({
    key,
    label: groupLabel(key),
    items
  }))
}

/** 加载历史：默认最近 30 条；选了日期则查当天 */
async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await getRecords(1, 30, filterDate.value || '')
    const data = res.data || res
    buildGroups(data.list || [])
    scrollToBottom()
  } catch (e) {
    ElMessage.error('历史记录加载失败')
  } finally {
    loadingHistory.value = false
  }
}

function onDateChange(val) {
  filterDate.value = val || ''
  loadHistory()
}

function clearDateFilter() {
  filterDate.value = ''
  loadHistory()
}

function toggleTts(text, idx) {
  if (!speechSynth) speechSynth = window.speechSynthesis
  if (!speechSynth) { ElMessage.warning('当前浏览器不支持语音播报'); return }

  if (speakingIdx.value === idx) {
    speechSynth.cancel()
    speakingIdx.value = -1
    return
  }

  speechSynth.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 1.0
  utterance.onend = () => { speakingIdx.value = -1 }
  utterance.onerror = () => { speakingIdx.value = -1 }
  speakingIdx.value = idx
  speechSynth.speak(utterance)
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  inputText.value = ''
  loading.value = true

  const now = new Date()
  const todayKey = dateKey(now)
  let todayGroup = groups.value[groups.value.length - 1]
  if (!todayGroup || todayGroup.key !== todayKey) {
    todayGroup = { key: todayKey, label: '今天', items: [] }
    groups.value.push(todayGroup)
  }
  todayGroup.items.push({ role: 'user', content: text, isVoice: false, timeText: formatTime(now) })
  scrollToBottom()

  try {
    const res = await askText(text, props.greenhouseId)
    const data = res.data || res
    todayGroup.items.push({
      role: 'ai',
      content: data.answer || '',
      sources: data.sources || [],
      timeText: formatTime(new Date())
    })
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '请求失败'
    todayGroup.items.push({ role: 'error', content: errMsg })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(loadHistory)

onBeforeUnmount(() => {
  if (speechSynth) speechSynth.cancel()
})
</script>

<style scoped>
.qa-page { display: flex; flex-direction: column; height: calc(100vh - 120px); background: #fff; border-radius: 8px; overflow: hidden; }
.qa-header { padding: 14px 20px; border-bottom: 1px solid #ebeef5; background: #fafafa; }
.qa-header-top { display: flex; align-items: center; gap: 12px; }
.qa-header h3 { margin: 0; font-size: 16px; }
.qa-hint { font-size: 12px; color: #909399; }
.qa-toolbar { margin-top: 10px; display: flex; align-items: center; gap: 8px; }
.qa-chat { flex: 1; overflow-y: auto; padding: 8px 20px 16px; }
.qa-empty { text-align: center; color: #909399; padding-top: 80px; }
.qa-empty p { margin: 8px 0; font-size: 14px; }
.qa-date-divider { text-align: center; color: #909399; font-size: 12px; margin: 12px 0 10px; }
.qa-date-divider::before, .qa-date-divider::after { content: ''; display: inline-block; width: 24px; height: 1px; background: #e4e7ed; vertical-align: middle; margin: 0 8px; }
.qa-message { margin-bottom: 14px; display: flex; flex-direction: column; }
.qa-message.user { align-items: flex-end; }
.qa-message.ai, .qa-message.error { align-items: flex-start; }
.msg-bubble { max-width: 75%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; word-break: break-word; }
.user-bubble { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.ai-bubble { background: #f0f2f5; color: #303133; border-bottom-left-radius: 4px; }
.error-bubble { background: #fef0f0; color: #f56c6c; border-bottom-left-radius: 4px; }
.msg-time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
.msg-voice-tag { font-size: 11px; margin-top: 4px; opacity: 0.7; }
.msg-sources { margin-top: 10px; padding: 8px 10px; background: #fff; border-radius: 6px; font-size: 12px; color: #606266; }
.sources-title { font-weight: 600; margin-bottom: 4px; }
.source-item { padding: 2px 0; display: flex; align-items: center; gap: 6px; }
.btn-tts { margin-top: 6px; font-size: 12px; }
.qa-loading { display: flex; align-items: center; gap: 8px; padding: 8px 0; color: #909399; font-size: 13px; }
.qa-input { padding: 12px 20px; border-top: 1px solid #ebeef5; background: #fafafa; }
</style>