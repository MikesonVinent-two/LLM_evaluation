<template>
  <div class="websocket-demo">
    <h2>WebSocket连接演示</h2>

    <div class="status-panel">
      <div class="status-indicator" :class="statusClass"></div>
      <span>状态: {{ statusText }}</span>
    </div>

    <div class="button-group">
      <button class="btn" @click="connect" :disabled="isConnecting || isConnected">连接</button>
      <button class="btn" @click="disconnect" :disabled="!isConnected">断开</button>
      <button class="btn" @click="subscribeToGlobal" :disabled="!isConnected">订阅全局消息</button>
      <button class="btn" @click="subscribeToBatch" :disabled="!isConnected">订阅批次消息</button>
      <button class="btn" @click="sendTestMessage" :disabled="!isConnected">发送测试消息</button>
      <button class="btn" @click="clearMessages">清除消息</button>
    </div>

    <div class="connection-info" v-if="connectionStats">
      <h3>连接统计</h3>
      <p>重连尝试次数: {{ connectionStats.reconnectAttempts }}</p>
      <p>消息数量: {{ connectionStats.messageCount }}</p>
      <p v-if="connectionStats.lastError">最后错误: {{ connectionStats.lastError }}</p>
    </div>

    <div class="message-container">
      <h3>消息记录 ({{ messages.length }})</h3>
      <div class="message-list">
        <div v-for="(message, index) in messages" :key="index" class="message-item" :class="messageClass(message)">
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          <div class="message-type">{{ message.type }}</div>
          <div class="message-content">{{ formatMessageContent(message) }}</div>
        </div>
        <div v-if="messages.length === 0" class="no-messages">
          暂无消息
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { websocketService } from '@/services/websocket'
import { WebSocketConnectionStatus, WebSocketMessageType } from '@/types/websocketTypes'
import type { WebSocketMessage } from '@/types/websocketTypes'

export default defineComponent({
  name: 'WebSocketDemo',

  setup() {
    // 响应式状态
    const messages = ref<WebSocketMessage[]>([])
    const connectionStats = ref<{
      status: WebSocketConnectionStatus,
      reconnectAttempts: number,
      lastError: string | null,
      messageCount: number
    } | null>(null)
    const batchId = ref<number>(1) // 示例批次ID
    const isComponentMounted = ref(true) // 跟踪组件是否已挂载

    // 创建一个取消监听函数的引用
    let removeConnectionListener: (() => void) | null = null

    // 连接状态计算属性
    const status = computed(() => websocketService.status.value)
    const isConnected = computed(() => status.value === WebSocketConnectionStatus.CONNECTED)
    const isConnecting = computed(() => status.value === WebSocketConnectionStatus.CONNECTING)
    const isDisconnected = computed(() => status.value === WebSocketConnectionStatus.DISCONNECTED)
    const hasError = computed(() => status.value === WebSocketConnectionStatus.ERROR)

    const statusText = computed(() => {
      switch (status.value) {
        case WebSocketConnectionStatus.CONNECTED:
          return '已连接'
        case WebSocketConnectionStatus.CONNECTING:
          return '正在连接...'
        case WebSocketConnectionStatus.DISCONNECTED:
          return '已断开'
        case WebSocketConnectionStatus.ERROR:
          return '连接错误'
        default:
          return '未知状态'
      }
    })

    const statusClass = computed(() => {
      switch (status.value) {
        case WebSocketConnectionStatus.CONNECTED:
          return 'status-connected'
        case WebSocketConnectionStatus.CONNECTING:
          return 'status-connecting'
        case WebSocketConnectionStatus.DISCONNECTED:
          return 'status-disconnected'
        case WebSocketConnectionStatus.ERROR:
          return 'status-error'
        default:
          return ''
      }
    })

    // 更新消息列表
    const updateMessages = () => {
      if (!isComponentMounted.value) return // 组件卸载后不更新
      messages.value = [...websocketService.messages.value]
    }

    // 更新连接统计
    const updateStats = () => {
      if (!isComponentMounted.value) return // 组件卸载后不更新
      connectionStats.value = websocketService.getConnectionStats()
    }

    // 连接WebSocket
    const connect = async () => {
      if (!isComponentMounted.value) return false // 组件卸载后不连接
      const connected = await websocketService.connect()
      if (connected) {
        console.log('连接成功')
      } else {
        console.error('连接失败')
      }
      updateStats()
      return connected
    }

    // 断开连接
    const disconnect = async () => {
      await websocketService.disconnect()
      if (isComponentMounted.value) {
      updateStats()
      }
    }

    // 订阅全局消息
    const subscribeToGlobal = () => {
      if (!isComponentMounted.value) return // 组件卸载后不订阅
      // 全局消息已在连接时自动订阅，这里只是一个演示按钮
      websocketService.send('/app/global/subscribe', {})
      addLocalMessage('已发送全局订阅请求')
    }

    // 订阅批次消息
    const subscribeToBatch = () => {
      if (!isComponentMounted.value) return // 组件卸载后不订阅
      websocketService.subscribeToBatchUpdates(batchId.value)
      addLocalMessage(`已订阅批次 ${batchId.value} 消息`)
    }

    // 发送测试消息
    const sendTestMessage = () => {
      if (!isComponentMounted.value) return // 组件卸载后不发送
      const testMessage = {
        content: '测试消息',
        timestamp: Date.now()
      }

      const sent = websocketService.send('/app/test', testMessage)
      if (sent) {
        addLocalMessage(`已发送测试消息: ${JSON.stringify(testMessage)}`)
      } else {
        addLocalMessage('发送测试消息失败，WebSocket未连接')
      }
    }

    // 添加本地消息（不经过WebSocket，仅用于UI显示）
    const addLocalMessage = (text: string) => {
      if (!isComponentMounted.value) return // 组件卸载后不添加
      messages.value.push({
        type: WebSocketMessageType.CONNECTION_ESTABLISHED,
        payload: { message: text },
        timestamp: new Date().toISOString()
      })
    }

    // 清除消息
    const clearMessages = () => {
      if (!isComponentMounted.value) return // 组件卸载后不清除
      websocketService.clearMessages()
      updateMessages()
    }

    // 格式化消息内容
    const formatMessageContent = (message: WebSocketMessage) => {
      if (message.payload) {
        if (message.payload.message) {
          return message.payload.message
        } else if (typeof message.payload === 'object' && message.payload !== null) {
          return JSON.stringify(message.payload)
        }
      }
      return '无内容'
    }

    // 根据消息类型返回CSS类
    const messageClass = (message: WebSocketMessage) => {
      switch (message.type) {
        case WebSocketMessageType.ERROR:
          return 'message-error'
        case WebSocketMessageType.CONNECTION_ESTABLISHED:
          return 'message-success'
        case WebSocketMessageType.CONNECTION_CLOSED:
          return 'message-warning'
        default:
          return ''
      }
    }

    // 格式化时间戳
    const formatTime = (timestamp: string) => {
      return new Date(timestamp).toLocaleTimeString()
    }

    // 组件挂载时
    onMounted(() => {
      isComponentMounted.value = true

      // 添加连接状态监听
      removeConnectionListener = websocketService.addConnectionListener((_newStatus) => {
        if (isComponentMounted.value) {
        updateStats()
        updateMessages()
        }
      })

      // 初始连接
      updateMessages()
      updateStats()
    })

    // 组件卸载前
    onBeforeUnmount(() => {
      // 标记组件已卸载
      isComponentMounted.value = false

      // 移除状态监听器
      if (removeConnectionListener) {
        removeConnectionListener()
        removeConnectionListener = null
      }

      // 确保不再有批次订阅
      try {
        websocketService.unsubscribeFromBatchUpdates(batchId.value)
      } catch (e) {
        console.error('取消批次订阅失败:', e)
      }
    })

    return {
      messages,
      connectionStats,
      status,
      statusText,
      statusClass,
      isConnected,
      isConnecting,
      isDisconnected,
      hasError,
      connect,
      disconnect,
      subscribeToGlobal,
      subscribeToBatch,
      sendTestMessage,
      clearMessages,
      formatTime,
      formatMessageContent,
      messageClass
    }
  }
})
</script>

<style scoped>
.websocket-demo {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  font-family: Arial, sans-serif;
}

.status-panel {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
}

.status-connected {
  background-color: #4caf50;
  box-shadow: 0 0 8px #4caf50;
}

.status-connecting {
  background-color: #2196f3;
  animation: blink 1s infinite;
}

.status-disconnected {
  background-color: #9e9e9e;
}

.status-error {
  background-color: #f44336;
  box-shadow: 0 0 8px #f44336;
}

.button-group {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  background-color: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn:hover:not(:disabled) {
  background-color: #0b7dda;
}

.btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.connection-info {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.connection-info h3 {
  margin-top: 0;
}

.message-container {
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.message-container h3 {
  margin: 0;
  padding: 10px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.message-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 0;
}

.message-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  flex-wrap: wrap;
}

.message-time {
  flex: 0 0 100px;
  color: #666;
}

.message-type {
  flex: 0 0 180px;
  font-weight: bold;
}

.message-content {
  flex: 1;
  word-break: break-word;
}

.message-error {
  background-color: #ffebee;
}

.message-success {
  background-color: #e8f5e9;
}

.message-warning {
  background-color: #fff8e1;
}

.no-messages {
  padding: 20px;
  text-align: center;
  color: #666;
}

@keyframes blink {
  0% { opacity: 0.4; }
  50% { opacity: 1; }
  100% { opacity: 0.4; }
}
</style>
