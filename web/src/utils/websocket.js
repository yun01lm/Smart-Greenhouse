/**
 * WebSocket 实时数据推送客户端
 * 
 * 连接后端 STOMP WebSocket 端点，接收传感器实时数据推送。
 * 自动重连，心跳保活。
 */
class RealtimeClient {
  constructor() {
    this.ws = null
    this.subscriptions = new Map()
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.heartbeatTimer = null
    this.connected = false
  }

  /**
   * 连接到 WebSocket 服务器
   * @param {string} greenhouseId 大棚 ID
   */
  connect(greenhouseId) {
    const token = localStorage.getItem('token')
    if (!token) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${location.host}/ws/connect`

    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      console.log('[WebSocket] 已连接')
      // 发送 STOMP CONNECT 帧
      this.sendFrame(`CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${token}\n\n\0`)
    }

    this.ws.onmessage = (event) => {
      this.handleFrame(event.data, greenhouseId)
    }

    this.ws.onclose = (e) => {
      console.log('[WebSocket] 已断开:', e.code, e.reason)
      this.connected = false
      this.stopHeartbeat()
      // 5 秒后自动重连
      this.reconnectTimer = setTimeout(() => this.connect(greenhouseId), 5000)
    }

    this.ws.onerror = (e) => {
      console.error('[WebSocket] 错误:', e)
    }
  }

  /**
   * 处理 STOMP 帧
   */
  handleFrame(raw, greenhouseId) {
    if (raw.startsWith('CONNECTED')) {
      this.connected = true
      this.startHeartbeat()
      // 订阅大棚实时数据
      this.subscribe(`/topic/greenhouse/${greenhouseId}/realtime`)
      return
    }

    if (raw.startsWith('MESSAGE')) {
      const lines = raw.split('\n')
      let destination = ''
      let bodyStart = -1

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]
        if (line.startsWith('destination:')) {
          destination = line.substring('destination:'.length).trim()
        }
        if (line === '' && i + 1 < lines.length) {
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
          const callback = this.subscriptions.get(destination)
          if (callback) callback(data)
        } catch (e) {
          // 忽略 JSON 解析错误
        }
      }
      return
    }

    if (raw.startsWith('ERROR')) {
      console.error('[STOMP ERROR]', raw)
    }
  }

  /**
   * 订阅目标
   */
  subscribe(destination) {
    const subId = 'sub-' + this.subscriptions.size
    this.sendFrame(`SUBSCRIBE\nid:${subId}\ndestination:${destination}\n\n\0`)
  }

  /**
   * 设置消息回调
   */
  onMessage(destination, callback) {
    this.subscriptions.set(destination, callback)
  }

  /**
   * 发送 STOMP 帧
   */
  sendFrame(frame) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(frame)
    }
  }

  /**
   * 心跳
   */
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('\n')
      }
    }, 10000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
      this.reconnectAttempts = 0
    this.reconnectAttempts = 0
    }
    if (this.ws) {
      this.sendFrame('DISCONNECT\nreceipt:disconnect\n\n\0')
      this.ws.close()
      this.ws = null
    }
    this.subscriptions.clear()
    this.connected = false
  }
}

export default new RealtimeClient()
