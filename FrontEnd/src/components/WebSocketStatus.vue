<template>
  <div class="websocket-status">
    <div class="status-indicator">
      <el-tag
        :type="statusType"
        :effect="isConnected ? 'light' : 'plain'"
        class="status-tag"
      >
        <el-icon class="status-icon">
          <component :is="statusIcon" />
        </el-icon>
        {{ statusText }}
      </el-tag>

      <el-tooltip
        v-if="lastError"
        :content="lastError"
        placement="top"
        effect="dark"
      >
        <el-icon class="error-icon"><WarningFilled /></el-icon>
      </el-tooltip>
    </div>

    <div class="controls">
      <el-button
        v-if="!isConnected && !isConnecting"
        type="success"
        size="small"
        @click="connect"
        :icon="Connection"
      >
        连接
      </el-button>
      <el-button
        v-else-if="isConnected"
        type="danger"
        size="small"
        @click="disconnect"
        :icon="CloseBold"
      >
        断开
      </el-button>
      <el-button
        v-else
        type="info"
        size="small"
        disabled
        :loading="true"
      >
        连接中...
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import { WebSocketConnectionStatus } from '@/types/websocketTypes'
import {
  Connection,
  CloseBold,
  Loading,
  WarningFilled,
  CircleCheck,
  CircleClose,
  Watch
} from '@element-plus/icons-vue'

const wsStore = useWebSocketStore()

// 计算属性
const status = computed(() => wsStore.status)
const isConnected = computed(() => wsStore.isConnected)
const isConnecting = computed(() => wsStore.isConnecting)
const lastError = computed(() => wsStore.lastError)

// 状态类型
const statusType = computed(() => {
  switch (status.value) {
    case WebSocketConnectionStatus.CONNECTED:
      return 'success'
    case WebSocketConnectionStatus.CONNECTING:
      return 'info'
    case WebSocketConnectionStatus.ERROR:
      return 'danger'
    default:
      return 'info'
  }
})

// 状态文本
const statusText = computed(() => {
  switch (status.value) {
    case WebSocketConnectionStatus.CONNECTED:
      return '已连接'
    case WebSocketConnectionStatus.CONNECTING:
      return '连接中...'
    case WebSocketConnectionStatus.ERROR:
      return '连接错误'
    default:
      return '未连接'
  }
})

// 状态图标
const statusIcon = computed(() => {
  switch (status.value) {
    case WebSocketConnectionStatus.CONNECTED:
      return CircleCheck
    case WebSocketConnectionStatus.CONNECTING:
      return Loading
    case WebSocketConnectionStatus.ERROR:
      return CircleClose
    default:
      return Watch
  }
})

// 连接方法
const connect = () => {
  wsStore.connect(`${import.meta.env.VITE_API_BASE_URL}/api/ws`)
}

// 断开连接方法
const disconnect = () => {
  wsStore.disconnect()
}
</script>

<style scoped>
.websocket-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #eaeaea;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-icon {
  margin-right: 4px;
}

.error-icon {
  color: #f56c6c;
  cursor: pointer;
}

.controls {
  display: flex;
  gap: 8px;
}
</style>
