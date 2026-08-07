/**
 * 聊天消息 WebSocket 客户端（R27）
 * 订阅 /user/queue/chat 接收专家/用户实时消息，自动重连、心跳保活。
 */
class ChatSocketClient {
  constructor() {
    this.ws = null
    this.callback = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.connected = false
    this.manualClosed = false
  }

  connect(onMessage) {
    const token = localStorage.getItem('token')
    if (!token) return
    this.callback = onMessage
    this.manualClosed = false

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${location.host}/ws/connect`
    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      this.sendFrame(`CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${token}\n\n\0`)
    }

    this.ws.onmessage = (event) => this.handleFrame(event.data)

    this.ws.onclose = () => {
      this.connected = false
      this.stopHeartbeat()
      if (!this.manualClosed) {
        this.reconnectTimer = setTimeout(() => this.connect(this.callback), 5000)
      }
    }

    this.ws.onerror = () => { /* onclose 处理重连 */ }
  }

  handleFrame(raw) {
    if (raw.startsWith('CONNECTED')) {
      this.connected = true
      this.startHeartbeat()
      this.sendFrame('SUBSCRIBE\nid:sub-chat\ndestination:/user/queue/chat\n\n\0')
      return
    }

    if (raw.startsWith('MESSAGE')) {
      const lines = raw.split('\n')
      let bodyStart = -1
      for (let i = 0; i < lines.length; i++) {
        if (lines[i] === '' && i + 1 < lines.length) {
          bodyStart = i + 1
          break
        }
      }
      if (bodyStart > 0) {
        let body = ''
        for (let i = bodyStart; i < lines.length; i++) {
          body += lines[i]
          if (lines[i].endsWith('\0')) {
            body = body.substring(0, body.length - 1)
            break
          }
        }
        try {
          const data = JSON.parse(body)
          if (this.callback) this.callback(data)
        } catch (e) { /* ignore */ }
      }
      return
    }

    if (raw.startsWith('ERROR')) {
      console.error('[CHAT STOMP ERROR]', raw)
    }
  }

  sendFrame(frame) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(frame)
    }
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) this.ws.send('\n')
    }, 10000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  disconnect() {
    this.manualClosed = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      try { this.sendFrame('DISCONNECT\nreceipt:disconnect\n\n\0') } catch (e) { /* ignore */ }
      this.ws.close()
      this.ws = null
    }
    this.connected = false
  }
}

export default new ChatSocketClient()