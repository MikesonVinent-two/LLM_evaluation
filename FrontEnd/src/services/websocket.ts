import { Client } from '@stomp/stompjs'
import { appConfig } from '@/config'
import { ref } from 'vue'
import {
  WebSocketConnectionStatus,
  WebSocketMessageType,
  WebSocketTopic,
  WebSocketDestination,
  type WebSocketConfig,
  type WebSocketMessage,
  type ProgressUpdateMessage,
  type StatusChangeMessage,
  type ErrorMessage,
  type NotificationMessage,
  type BatchesStatusMessage,
  type BatchInfo,
  BatchStatus
} from '@/types/websocketTypes'

// WebSocket服务类
class WebSocketService {
  private client: Client | null = null
  private subscriptions: Map<string, { unsubscribe: () => void }> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 3000 // 重连间隔，单位毫秒
  private connectionListeners: Array<(status: WebSocketConnectionStatus) => void> = []
  private manualDisconnect = false // 标记是否为手动断开连接
  private connectPromise: Promise<boolean> | null = null // 防止并发连接
  private maxMessages = 1000 // 限制消息存储数量

  // 响应式状态
  public status = ref<WebSocketConnectionStatus>(WebSocketConnectionStatus.DISCONNECTED)
  public lastError = ref<string | null>(null)
  public messages = ref<WebSocketMessage[]>([])

  // 最后一次连接尝试时间
  private lastConnectAttempt = 0

  // 连接超时，单位毫秒
  private connectTimeout = 10000 // 10秒

  // 添加连接状态监听器
  public addConnectionListener(listener: (status: WebSocketConnectionStatus) => void): () => void {
    this.connectionListeners.push(listener)
    // 立即触发一次当前状态
    listener(this.status.value)

    // 返回取消监听的函数
    return () => {
      const index = this.connectionListeners.indexOf(listener)
      if (index !== -1) {
        this.connectionListeners.splice(index, 1)
      }
    }
  }

  // 设置连接状态并通知监听器
  private setStatus(status: WebSocketConnectionStatus): void {
    this.status.value = status
    // 通知所有监听器
    this.connectionListeners.forEach(listener => {
      try {
        listener(status)
      } catch (error) {
        console.error('执行连接状态监听器出错:', error)
      }
    })
  }

  // 连接WebSocket服务器
  public async connect(url: string = `${appConfig.api.baseUrl}/api/ws`): Promise<boolean> {
    // 如果正在连接中，返回现有的连接Promise
    if (this.connectPromise) {
      return this.connectPromise
    }

    // 如果已连接，直接返回成功
    if (this.client && this.client.connected) {
      return true
    }

    // 如果存在客户端实例，确保先完全断开
    if (this.client) {
      await this.disconnect()
    }

    // 创建新的连接Promise
    this.connectPromise = this.doConnect(url)

    try {
      // 等待连接完成并返回结果
      const result = await this.connectPromise
      return result
    } finally {
      // 无论成功或失败，都清除connectPromise
      this.connectPromise = null
    }
  }

  // 执行实际的连接逻辑
  private doConnect(url: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.lastConnectAttempt = Date.now()
      this.manualDisconnect = false
      this.setStatus(WebSocketConnectionStatus.CONNECTING)
      console.log(`正在连接WebSocket服务器: ${url}`)

      try {
        // 创建STOMP客户端，使用原生WebSocket
        this.client = new Client({
          webSocketFactory: () => {
            // 将 http:// 或 https:// 替换为 ws:// 或 wss://
            const wsUrl = url.replace(/^http(s)?:\/\//, 'ws$1://')
            return new WebSocket(wsUrl)
          },
          connectHeaders: {
            // 可以在这里添加认证信息
            //Authorization: `Bearer ${localStorage.getItem(appConfig.storage.tokenKey)}`
          },
          debug: function(str) {
            if (appConfig.env === 'development') {
              console.log('STOMP: ' + str)
            }
          },
          // 禁用STOMP自动重连，使用自定义重连逻辑
          reconnectDelay: 0,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000
        })

        // 连接成功回调
        this.client.onConnect = (frame) => {
          this.setStatus(WebSocketConnectionStatus.CONNECTED)
          this.reconnectAttempts = 0
          console.log('WebSocket连接成功', frame)

          // 发送连接成功消息
          this.addMessage({
            type: WebSocketMessageType.CONNECTION_ESTABLISHED,
            payload: { message: 'WebSocket连接已建立' },
            timestamp: new Date().toISOString()
          })

          // 订阅全局消息
          this.subscribeToGlobal()

          // 如果有用户信息，订阅用户特定消息
          const user = localStorage.getItem(appConfig.storage.userKey)
          if (user) {
            try {
              const userData = JSON.parse(user)
              if (userData && userData.id) {
                this.subscribeToUserMessages(userData.id)
              }
            } catch (e) {
              console.error('解析用户信息失败:', e)
            }
          }

          // 解析Promise为成功
          resolve(true)
        }

        // 连接错误回调
        this.client.onStompError = (frame) => {
          const errorMessage = frame.headers['message'] || '未知STOMP错误'
          this.handleError('STOMP协议错误', errorMessage)
          resolve(false)
        }

        // 连接断开回调
        this.client.onWebSocketClose = () => {
          // 无论之前是什么状态，都设置为断开状态
          this.setStatus(WebSocketConnectionStatus.DISCONNECTED)
          console.log('WebSocket连接已断开')

          // 发送连接断开消息
          this.addMessage({
            type: WebSocketMessageType.CONNECTION_CLOSED,
            payload: { message: 'WebSocket连接已断开' },
            timestamp: new Date().toISOString()
          })

          // 只有在非手动断开的情况下才尝试重连
          if (!this.manualDisconnect) {
            this.reconnect()
          }

          // 如果仍处于连接中状态，解析Promise为失败
          if (this.connectPromise) {
            resolve(false)
          }
        }

        // 启动连接
        this.client.activate()

        // 设置连接超时检查
        setTimeout(() => {
          // 如果连接状态还是CONNECTING，说明连接超时
          if (this.status.value === WebSocketConnectionStatus.CONNECTING) {
            this.handleError('WebSocket连接超时')
            resolve(false)
          }
        }, this.connectTimeout)

      } catch (error) {
        this.handleError('创建WebSocket连接失败', error)
        resolve(false)
      }
    })
  }

  // 断开连接
  public async disconnect(): Promise<void> {
    this.manualDisconnect = true

    // 清除所有订阅
    this.subscriptions.forEach((subscription) => {
      try {
        subscription.unsubscribe()
      } catch (e) {
        console.error('取消订阅失败:', e)
      }
    })
    this.subscriptions.clear()

    // 断开STOMP客户端连接
    if (this.client) {
      return new Promise<void>((resolve) => {
        const prevStatus = this.status.value

        // 设置超时以防止deactivate卡住
        const timeout = setTimeout(() => {
          this.client = null
          this.setStatus(WebSocketConnectionStatus.DISCONNECTED)
          resolve()
        }, 2000)

        try {
          // 使用非空断言操作符，确保TypeScript知道this.client不为null
          this.client!.deactivate().then(() => {
            clearTimeout(timeout)
            this.client = null
            this.setStatus(WebSocketConnectionStatus.DISCONNECTED)

            // 只有在之前连接成功的情况下才添加断开消息
            if (prevStatus === WebSocketConnectionStatus.CONNECTED) {
              this.addMessage({
                type: WebSocketMessageType.CONNECTION_CLOSED,
                payload: { message: 'WebSocket连接已手动断开' },
                timestamp: new Date().toISOString()
              })
            }

            console.log('WebSocket连接已断开')
            resolve()
          }).catch((error) => {
            clearTimeout(timeout)
            console.error('断开连接出错:', error)
            this.client = null
            this.setStatus(WebSocketConnectionStatus.DISCONNECTED)
            resolve()
          })
        } catch (e) {
          clearTimeout(timeout)
          console.error('调用deactivate方法出错:', e)
          this.client = null
          this.setStatus(WebSocketConnectionStatus.DISCONNECTED)
          resolve()
        }
      })
    }
  }

  // 发送消息
  public send(destination: string, body: unknown): boolean {
    if (this.client && this.client.connected) {
      console.log(`发送消息到 ${destination}:`, body)
      this.client.publish({
        destination,
        body: JSON.stringify(body),
        headers: { 'content-type': 'application/json' }
      })
      return true
    } else {
      console.error('WebSocket未连接，无法发送消息')
      return false
    }
  }

  // 订阅全局消息
  private subscribeToGlobal(): void {
    if (!this.client || !this.client.connected) return

    // 订阅全局消息主题
    const subscription = this.client.subscribe(WebSocketTopic.GLOBAL, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set('global', subscription)

    // 发送订阅确认请求
    this.send(WebSocketDestination.GLOBAL_SUBSCRIBE, {
      timestamp: Date.now()
    })
    console.log('已订阅全局消息')
  }

  // 订阅用户特定消息
  public subscribeToUserMessages(userId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅用户消息')
      return
    }

    const topicId = `user-${userId}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromUserMessages(userId)
    }

    // 订阅用户特定主题
    const subscription = this.client.subscribe(`/user/${userId}/queue/messages`, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)

    // 发送订阅确认请求
    this.send(`/app/user/${userId}/subscribe`, {})
    console.log(`已订阅用户 ${userId} 的消息`)
  }

  // 取消订阅用户特定消息
  public unsubscribeFromUserMessages(userId: number): void {
    const topicId = `user-${userId}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅用户 ${userId} 的消息`)
    }
  }

  // 订阅批次消息
  public subscribeToBatchUpdates(batchId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅批次更新')
      return
    }

    const topicId = `batch-${batchId}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromBatchUpdates(batchId)
    }

    // 订阅批次特定主题
    const subscription = this.client.subscribe(`/topic/batch/${batchId}`, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)

    // 发送订阅确认请求
    this.send(`/app/batch/${batchId}/subscribe`, {})
    console.log(`已订阅批次 ${batchId} 的状态更新`)
  }

  // 取消订阅批次消息
  public unsubscribeFromBatchUpdates(batchId: number): void {
    const topicId = `batch-${batchId}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅批次 ${batchId} 的状态更新`)
    }
  }

  // 订阅运行消息
  public subscribeToRunUpdates(runId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅运行更新')
      return
    }

    const topicId = `run-${runId}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromRunUpdates(runId)
    }

    // 订阅运行特定主题
    const subscription = this.client.subscribe(`/topic/run/${runId}`, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)

    // 发送订阅确认请求
    this.send(`/app/run/${runId}/subscribe`, {})
    console.log(`已订阅运行 ${runId} 的状态更新`)
  }

  // 取消订阅运行消息
  public unsubscribeFromRunUpdates(runId: number): void {
    const topicId = `run-${runId}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅运行 ${runId} 的状态更新`)
    }
  }

  // 订阅队列消息
  public subscribeToQueue(queueName: string, callback?: (message: WebSocketMessage) => void): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅队列消息')
      return
    }

    const topicId = `queue-${queueName}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromQueue(queueName)
    }

    // 订阅队列特定主题
    const subscription = this.client.subscribe(`/queue/${queueName}`, (message) => {
      const parsedMessage = this.handleMessage(message)
      if (callback && parsedMessage) {
        callback(parsedMessage)
      }
    })

    this.subscriptions.set(topicId, subscription)
    console.log(`已订阅队列 ${queueName} 的消息`)
  }

  // 取消订阅队列消息
  public unsubscribeFromQueue(queueName: string): void {
    const topicId = `queue-${queueName}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅队列 ${queueName} 的消息`)
    }
  }

  // 添加消息到消息列表，控制消息数量
  private addMessage(message: WebSocketMessage): void {
    // 限制消息数量，防止内存泄漏
    if (this.messages.value.length >= this.maxMessages) {
      // 移除最早的消息
      this.messages.value.shift()
    }

    // 确保timestamp是字符串类型
    const messageWithStringTimestamp = {
      ...message,
      timestamp: typeof message.timestamp === 'number'
        ? new Date(message.timestamp).toISOString()
        : message.timestamp
    }

    // 添加新消息
    this.messages.value.push(messageWithStringTimestamp)
  }

  // 处理接收到的消息
  private handleMessage(message: { body: string }): WebSocketMessage | undefined {
    try {
      if (!message || !message.body) {
        console.error('收到无效的WebSocket消息')
        return undefined
      }

      const parsedMessage = JSON.parse(message.body)
      console.log('收到WebSocket消息:', parsedMessage)

      // 确保消息格式正确
      if (!parsedMessage || !parsedMessage.type) {
        console.error('WebSocket消息格式不正确:', parsedMessage)
        return undefined
      }

      // 创建消息对象
      const webSocketMessage: WebSocketMessage = {
        type: parsedMessage.type as WebSocketMessageType,
        payload: parsedMessage.payload || {},
        timestamp: parsedMessage.timestamp
      }

      // 根据消息类型进行特定处理
      switch (webSocketMessage.type) {
        case WebSocketMessageType.STATUS_CHANGE:
          this.handleStatusChange(webSocketMessage.payload)
          break
        case WebSocketMessageType.PROGRESS_UPDATE:
          this.handleProgressUpdate(webSocketMessage.payload)
          break
        case WebSocketMessageType.TASK_COMPLETED:
        case WebSocketMessageType.TASK_FAILED:
          this.handleTaskUpdate(webSocketMessage.payload)
          break
        case WebSocketMessageType.QUESTION_COMPLETED:
        case WebSocketMessageType.QUESTION_FAILED:
          this.handleQuestionUpdate(webSocketMessage.payload)
          break
      }

      // 将消息添加到消息列表
      this.addMessage(webSocketMessage)

      return webSocketMessage
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
      return undefined
    }
  }

  // 处理状态变更消息
  private handleStatusChange(data: WebSocketMessage['payload']): void {
    // 如果是订阅确认消息，直接返回
    if (data.subscribed) {
      console.log(data.message || '订阅成功')
      return
    }

    // 检查必要字段是否存在
    const entityId = data.batchId || data.entityId
    const oldStatus = data.oldStatus
    const newStatus = data.newStatus

    if (!entityId || !oldStatus || !newStatus) {
      console.warn('状态变更消息缺少必要字段:', data)
      return
    }

    console.log(`实体 ${entityId} 的状态从 ${oldStatus} 变更为 ${newStatus}`)
  }

  // 处理进度更新消息
  private handleProgressUpdate(data: WebSocketMessage['payload']): void {
    if (!data.batchId || data.progressPercentage === undefined) {
      console.warn('进度更新消息缺少必要字段:', data)
      return
    }

    console.log(`批次 ${data.batchId} 的进度: ${data.progressPercentage}%`)
  }

  // 处理任务更新消息
  private handleTaskUpdate(data: WebSocketMessage['payload']): void {
    if (!data.batchId || !data.status) {
      console.warn('任务更新消息缺少必要字段:', data)
      return
    }

    console.log(`批次 ${data.batchId} 的任务状态更新为: ${data.status}`)
  }

  // 处理问题更新消息
  private handleQuestionUpdate(data: WebSocketMessage['payload']): void {
    console.log('收到问题更新消息:', data)

    // 可以在这里添加特定的问题更新处理逻辑
    if (data.questionId) {
      console.log(`问题 ${data.questionId} 状态更新`)
    }

    if (data.batchId) {
      console.log(`批次 ${data.batchId} 中的问题状态更新`)
    }
  }

  // 重连
  private reconnect(): void {
    if (this.manualDisconnect) {
      console.log('手动断开连接，不进行重连')
      return
    }

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`WebSocket重连失败，已达到最大重试次数: ${this.maxReconnectAttempts}`)
      this.setStatus(WebSocketConnectionStatus.ERROR)
      this.lastError.value = '重连失败，已达到最大重试次数'
      return
    }

    this.reconnectAttempts++
    console.log(`WebSocket尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

    // 使用指数退避算法计算下一次重连间隔
    const nextInterval = Math.min(
      this.reconnectInterval * Math.pow(1.5, this.reconnectAttempts - 1),
      30000 // 最长间隔30秒
    )

    setTimeout(() => {
      this.connect()
    }, nextInterval)
  }

  // 处理错误
  private handleError(message: string, error?: unknown): void {
    this.setStatus(WebSocketConnectionStatus.ERROR)
    const errorMessage = error ? `${message}: ${String(error)}` : message
    this.lastError.value = errorMessage
    console.error(`WebSocket错误: ${errorMessage}`)

    // 发送错误消息
    this.addMessage({
      type: WebSocketMessageType.ERROR,
      payload: { message: errorMessage },
      timestamp: new Date().toISOString()
    })

    // 尝试重连
    this.reconnect()
  }

  // 清除消息历史
  public clearMessages(): void {
    this.messages.value = []
  }

  // 检查连接状态
  public isConnected(): boolean {
    return this.client !== null && this.client.connected === true
  }

  // 获取连接状态
  public getStatus(): WebSocketConnectionStatus {
    return this.status.value
  }

  // 检查连接状态并尝试重连
  public async checkConnectionAndReconnect(): Promise<boolean> {
    // 如果当前已连接，直接返回true
    if (this.isConnected()) {
      return true
    }

    // 如果正在连接中，等待结果
    if (this.status.value === WebSocketConnectionStatus.CONNECTING && this.connectPromise) {
      return this.connectPromise
    }

    // 如果连接出错或断开状态，并且距离上次连接尝试超过重连间隔，则尝试重连
    const now = Date.now()
    if ((this.status.value === WebSocketConnectionStatus.ERROR ||
         this.status.value === WebSocketConnectionStatus.DISCONNECTED) &&
        (now - this.lastConnectAttempt) > this.reconnectInterval) {
      console.log('连接状态检查：未连接，尝试重新连接')
      return this.connect()
    }

    // 返回false表示未连接
    return false
  }

  // 等待连接成功
  public async waitForConnection(timeoutMs: number = 5000): Promise<boolean> {
    // 如果已经连接，直接返回true
    if (this.isConnected()) {
      return true
    }

    // 如果正在连接中，等待现有的连接Promise
    if (this.status.value === WebSocketConnectionStatus.CONNECTING && this.connectPromise) {
      // 设置超时
      const timeoutPromise = new Promise<boolean>((resolve) => {
        setTimeout(() => resolve(false), timeoutMs)
      })

      // 哪个先完成就返回哪个结果
      return Promise.race([this.connectPromise, timeoutPromise])
    }

    // 尝试连接
    return Promise.race([
      this.connect(),
      new Promise<boolean>((resolve) => setTimeout(() => resolve(false), timeoutMs))
    ])
  }

  // 获取连接统计信息
  public getConnectionStats(): {
    status: WebSocketConnectionStatus,
    reconnectAttempts: number,
    lastError: string | null,
    messageCount: number
  } {
    return {
      status: this.status.value,
      reconnectAttempts: this.reconnectAttempts,
      lastError: this.lastError.value,
      messageCount: this.messages.value.length
    }
  }

  // 订阅状态更新消息
  public subscribeToStatusUpdates(entityId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅状态更新')
      return
    }

    const topicId = `status-${entityId}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromStatusUpdates(entityId)
    }

    // 订阅状态更新主题
    const subscription = this.client.subscribe(`${WebSocketTopic.STATUS}/${entityId}`, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)
    console.log(`已订阅实体 ${entityId} 的状态更新`)
  }

  // 取消订阅状态更新消息
  public unsubscribeFromStatusUpdates(entityId: number): void {
    const topicId = `status-${entityId}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅实体 ${entityId} 的状态更新`)
    }
  }

  // 订阅进度更新消息
  public subscribeToProgressUpdates(runId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅进度更新')
      return
    }

    const topicId = `progress-${runId}`

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromProgressUpdates(runId)
    }

    // 订阅进度更新主题
    const subscription = this.client.subscribe(`${WebSocketTopic.RUN_PROGRESS}/${runId}`, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)
    console.log(`已订阅运行 ${runId} 的进度更新`)
  }

  // 取消订阅进度更新消息
  public unsubscribeFromProgressUpdates(runId: number): void {
    const topicId = `progress-${runId}`

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log(`已取消订阅运行 ${runId} 的进度更新`)
    }
  }

  // 订阅错误消息
  public subscribeToErrors(): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅错误消息')
      return
    }

    // 订阅错误消息主题
    const subscription = this.client.subscribe(WebSocketTopic.ERRORS, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set('errors', subscription)
    console.log('已订阅错误消息')
  }

  // 订阅所有批次状态
  public subscribeToAllBatches(): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket未连接，无法订阅批次状态')
      return
    }

    const topicId = 'batches-all'

    // 如果已经订阅，先取消订阅
    if (this.subscriptions.has(topicId)) {
      this.unsubscribeFromAllBatches()
    }

    // 订阅所有批次状态主题
    const subscription = this.client.subscribe(WebSocketTopic.BATCHES_ALL, (message) => {
      this.handleMessage(message)
    })

    this.subscriptions.set(topicId, subscription)

    // 发送订阅确认请求
    this.send(WebSocketDestination.BATCHES_ALL_SUBSCRIBE, {
      timestamp: Date.now()
    })
    console.log('已订阅所有批次状态')
  }

  // 取消订阅所有批次状态
  public unsubscribeFromAllBatches(): void {
    const topicId = 'batches-all'

    if (this.subscriptions.has(topicId)) {
      this.subscriptions.get(topicId)?.unsubscribe()
      this.subscriptions.delete(topicId)
      console.log('已取消订阅所有批次状态')
    }
  }
}

// 创建单例实例
export const websocketService = new WebSocketService()
