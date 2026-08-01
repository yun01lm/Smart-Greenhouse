<template>
  <div class="qa-page">
    <div class="qa-header">
      <h3>🤖 AI 智能问答</h3>
      <span class="qa-hint">基于知识库的 RAG 智能问答，支持引用来源追溯</span>
    </div>

    <div class="qa-chat" ref="chatContainer">
      <div v-if="messages.length === 0" class="qa-empty">
        <p>👋 你好！我是智慧大棚AI助手</p>
        <p>可以问我关于大棚管理、病虫害防治、作物种植等问题</p>
      </div>

      <div v-for="(msg, idx) in messages" :key="idx" :class="['qa-message', msg.role]">
        <template v-if="msg.role === 'user'">
          <div class="msg-bubble user-bubble">
            <div class="msg-text">{{ msg.content }}</div>
            <div v-if="msg.isVoice" class="msg-voice-tag">🎤 语音输入</div>
          </div>
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
        </template>

        <template v-else>
          <div class="msg-bubble error-bubble">
            <div class="msg-text">❌ {{ msg.content }}</div>
          </div>
        </template>
      </div>

      <div v-if="loading" class="qa-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在思考...</span>
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
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { askText } from '@/api/qa'
import { ElMessage } from 'element-plus'

const props = defineProps({
  greenhouseId: { type: Number, default: null }
})

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const chatContainer = ref(null)
const speakingIdx = ref(-1)

let speechSynth = null

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
  messages.value.push({ role: 'user', content: text, isVoice: false })
  loading.value = true

  try {
    const res = await askText(text, props.greenhouseId)
    const data = res.data || res
    messages.value.push({
      role: 'ai',
      content: data.answer || '',
      sources: data.sources || []
    })
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '请求失败'
    messages.value.push({ role: 'error', content: errMsg })
  } finally {
    loading.value = false
    await nextTick()
    if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

onBeforeUnmount(() => {
  if (speechSynth) speechSynth.cancel()
})
</script>

<style scoped>
.qa-page { display: flex; flex-direction: column; height: calc(100vh - 120px); background: #fff; border-radius: 8px; overflow: hidden; }
.qa-header { padding: 16px 20px; border-bottom: 1px solid #ebeef5; background: #fafafa; }
.qa-header h3 { margin: 0 0 4px 0; font-size: 16px; }
.qa-hint { font-size: 12px; color: #909399; }
.qa-chat { flex: 1; overflow-y: auto; padding: 16px 20px; }
.qa-empty { text-align: center; color: #909399; padding-top: 80px; }
.qa-empty p { margin: 8px 0; font-size: 14px; }
.qa-message { margin-bottom: 16px; display: flex; }
.qa-message.user { justify-content: flex-end; }
.qa-message.ai, .qa-message.error { justify-content: flex-start; }
.msg-bubble { max-width: 75%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.user-bubble { background: #409eff; color: #fff; border-bottom-right-radius: 4px; }
.ai-bubble { background: #f0f2f5; color: #303133; border-bottom-left-radius: 4px; }
.error-bubble { background: #fef0f0; color: #f56c6c; border-bottom-left-radius: 4px; }
.msg-voice-tag { font-size: 11px; margin-top: 4px; opacity: 0.7; }
.msg-sources { margin-top: 10px; padding: 8px 10px; background: #fff; border-radius: 6px; font-size: 12px; color: #606266; }
.sources-title { font-weight: 600; margin-bottom: 4px; }
.source-item { padding: 2px 0; display: flex; align-items: center; gap: 6px; }
.btn-tts { margin-top: 6px; font-size: 12px; }
.qa-loading { display: flex; align-items: center; gap: 8px; padding: 8px 0; color: #909399; font-size: 13px; }
.qa-input { padding: 12px 20px; border-top: 1px solid #ebeef5; background: #fafafa; }
</style>