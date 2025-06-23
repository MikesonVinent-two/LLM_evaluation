import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { websocketService } from '@/services/websocket'
import {
  WebSocketConnectionStatus,
  WebSocketMessageType
} from '@/types/websocketTypes'
import type { WebSocketMessage } from '@/types/websocketTypes'

export const useWebSocketStore = defineStore('websocket', () => {
  const messages = ref<WebSocketMessage[]>([])
  const isConnected = ref(false)
  const isConnecting = ref(false)

  // 从WebSocket服务获取状态
  const status = computed(() => websocketService.status.value)
  const lastError = computed(() => websocketService.lastError.value)

  // 计算属性：是否有错误
  const hasError = computed(() => status.value === WebSocketConnectionStatus.ERROR)

  // 按类型过滤消息
  const getMessagesByType = (type: WebSocketMessageType) => {
    return messages.value.filter(msg => msg.type === type)
  }

  // 获取最新的特定类型消息
  const getLatestMessageByType = (type: WebSocketMessageType): WebSocketMessage | undefined => {
    const filteredMessages = getMessagesByType(type)
    return filteredMessages.length > 0 ? filteredMessages[filteredMessages.length - 1] : undefined
  }

  // 连接WebSocket
  const connect = async () => {
    if (isConnected.value || isConnecting.value) return

    isConnecting.value = true
    try {
      const connected = await websocketService.connect()
      isConnected.value = connected
    } finally {
      isConnecting.value = false
    }
  }

  // 断开WebSocket连接
  const disconnect = async () => {
    await websocketService.disconnect()
    isConnected.value = false
  }

  // 发送消息
  const send = (destination: string, body: unknown): boolean => {
    return websocketService.send(destination, body)
  }

  // 订阅批次状态更新
  const subscribeToBatchUpdates = () => {
    if (!isConnected.value) return
    websocketService.subscribeToAllBatches()
  }

  // 取消订阅批次状态更新
  const unsubscribeFromBatchUpdates = () => {
    websocketService.unsubscribeFromAllBatches()
  }

  // 订阅特定批次
  const subscribeToBatch = (batchId: number) => {
    if (!isConnected.value) {
      console.warn('WebSocket未连接，无法订阅批次')
      return false
    }
    websocketService.subscribeToBatchUpdates(batchId)
    return true
  }

  // 取消订阅特定批次
  const unsubscribeFromBatch = (batchId: number) => {
    websocketService.unsubscribeFromBatchUpdates(batchId)
  }

  // 添加消息
  const addMessage = (message: WebSocketMessage) => {
    messages.value.push(message)
    // 限制消息数量
    if (messages.value.length > 1000) {
      messages.value = messages.value.slice(-1000)
    }
  }

  // 清除消息
  const clearMessages = () => {
    messages.value = []
  }

  return {
    // 状态
    status,
    lastError,
    messages,
    isConnected,
    isConnecting,
    hasError,

    // 方法
    connect,
    disconnect,
    send,
    subscribeToBatchUpdates,
    unsubscribeFromBatchUpdates,
    subscribeToBatch,
    unsubscribeFromBatch,
    getMessagesByType,
    getLatestMessageByType,
    addMessage,
    clearMessages
  }
})
