<template>
  <div class="batch-status-monitor">
    <div class="monitor-header">
      <h3>批次状态实时监控</h3>
      <WebSocketStatus />
    </div>

    <div class="batch-info" v-if="batchId">
      <div class="batch-header">
        <span class="batch-id">批次ID: {{ batchId }}</span>
        <el-tag :type="batchStatusType">{{ batchStatusText }}</el-tag>
      </div>

      <el-progress
        :percentage="progressPercentage"
        :status="progressStatus"
        :stroke-width="18"
        :format="progressFormat"
      />

      <div class="batch-stats">
        <el-statistic title="总运行数" :value="totalRuns" />
        <el-statistic title="已完成" :value="completedRuns" />
        <el-statistic title="待处理" :value="pendingRuns" />
        <el-statistic title="失败" :value="failedRuns" />
      </div>

      <div class="last-update" v-if="lastUpdateTime">
        <span>最后更新: {{ formatTime(lastUpdateTime) }}</span>
      </div>
    </div>

    <div class="no-batch" v-else>
      <el-empty description="未选择批次" />
      <el-input-number
        v-model="inputBatchId"
        :min="1"
        placeholder="输入批次ID"
        class="batch-input"
      />
      <el-button
        type="primary"
        @click="subscribeToBatch"
        :disabled="!inputBatchId || !wsStore.isConnected"
      >
        监控批次
      </el-button>
    </div>

    <div class="activity-log">
      <h4>活动日志</h4>
      <el-scrollbar height="200px">
        <div class="log-list">
          <div
            v-for="(log, index) in activityLogs"
            :key="index"
            class="log-item"
            :class="{'log-error': log.type === 'error'}"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="activityLogs.length === 0" class="empty-log">
            暂无活动日志
          </div>
        </div>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import WebSocketStatus from './WebSocketStatus.vue'
import { BatchStatus } from '@/api/answerGenerationBatch'
import {
  WebSocketMessageType,
  WebSocketConnectionStatus
} from '@/types/websocketTypes'
import type { WebSocketMessage } from '@/types/websocketTypes'

// 日志项类型
interface LogItem {
  timestamp: number
  message: string
  type: 'info' | 'success' | 'warning' | 'error'
}

const wsStore = useWebSocketStore()
const batchId = ref<number | null>(null)
const inputBatchId = ref<number | null>(null)
const lastUpdateTime = ref<number | null>(null)
const activityLogs = ref<LogItem[]>([])

// 批次状态数据
const batchStatus = ref<BatchStatus | null>(null)
const progressPercentage = ref(0)
const totalRuns = ref(0)
const completedRuns = ref(0)
const pendingRuns = ref(0)
const failedRuns = ref(0)

// 计算属性：批次状态类型
const batchStatusType = computed(() => {
  if (!batchStatus.value) return 'info'

  switch (batchStatus.value) {
    case BatchStatus.COMPLETED:
      return 'success'
    case BatchStatus.FAILED:
      return 'danger'
    case BatchStatus.PAUSED:
      return 'warning'
    case BatchStatus.GENERATING_ANSWERS:
    case BatchStatus.RESUMING:
      return 'primary'
    default:
      return 'info'
  }
})

// 计算属性：批次状态文本
const batchStatusText = computed(() => {
  if (!batchStatus.value) return '未知'

  const statusMap: Record<string, string> = {
    'PENDING': '等待中',
    'GENERATING_ANSWERS': '生成回答中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'PAUSED': '已暂停',
    'RESUMING': '恢复中'
  }

  return statusMap[batchStatus.value] || '未知'
})

// 计算属性：进度状态
const progressStatus = computed(() => {
  if (!batchStatus.value) return ''

  switch (batchStatus.value) {
    case BatchStatus.COMPLETED:
      return 'success'
    case BatchStatus.FAILED:
      return 'exception'
    case BatchStatus.PAUSED:
      return 'warning'
    default:
      return ''
  }
})

// 进度格式化
const progressFormat = (percentage: number) => {
  return `${percentage.toFixed(1)}%`
}

// 格式化时间
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 添加日志
const addLog = (message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') => {
  activityLogs.value.unshift({
    timestamp: Date.now(),
    message,
    type
  })

  // 限制日志数量
  if (activityLogs.value.length > 50) {
    activityLogs.value = activityLogs.value.slice(0, 50)
  }
}

// 订阅批次状态
const subscribeToBatch = () => {
  if (!inputBatchId.value) return

  batchId.value = inputBatchId.value
  wsStore.subscribeToBatch(batchId.value)
  addLog(`开始监控批次 ${batchId.value}`, 'info')
}

// 处理批次状态更新
const handleBatchStatusUpdate = (data: WebSocketMessage['data']) => {
  if (!data || data.batchId !== batchId.value) return

  batchStatus.value = data.status as BatchStatus || batchStatus.value
  progressPercentage.value = data.progressPercentage || 0
  totalRuns.value = data.totalRuns || 0
  completedRuns.value = data.completedRuns || 0
  lastUpdateTime.value = Date.now()

  addLog(`批次状态更新: ${batchStatusText.value}`, 'info')
}

// 处理回答生成进度

// 监听WebSocket消息
watch(() => wsStore.messages, (newMessages, oldMessages) => {
  if (!newMessages || newMessages.length === 0) return
  if (!oldMessages || oldMessages.length === 0) {
    // 首次获取消息，检查所有消息
    newMessages.forEach(msg => {
      if (msg.type === WebSocketMessageType.STATUS_CHANGE ||
          msg.type === WebSocketMessageType.TASK_COMPLETED ||
          msg.type === WebSocketMessageType.TASK_FAILED) {
        handleBatchStatusUpdate(msg.data)
      }
    })
    return
  }

  // 只处理新增的消息
  const newlyAddedMessages = newMessages.slice(oldMessages.length)
  newlyAddedMessages.forEach(msg => {
    if (msg.type === WebSocketMessageType.STATUS_CHANGE ||
        msg.type === WebSocketMessageType.TASK_COMPLETED ||
        msg.type === WebSocketMessageType.TASK_FAILED) {
      handleBatchStatusUpdate(msg.data)
    }
  })
}, { deep: true })

// 监听WebSocket连接状态
watch(() => wsStore.status, (newStatus) => {
  if (newStatus === WebSocketConnectionStatus.CONNECTED) {
    addLog('WebSocket连接成功', 'success')
    // 如果有批次ID，重新订阅
    if (batchId.value) {
      wsStore.subscribeToBatch(batchId.value)
      addLog(`重新订阅批次 ${batchId.value}`, 'info')
    }
  } else if (newStatus === WebSocketConnectionStatus.DISCONNECTED) {
    addLog('WebSocket连接已断开', 'warning')
  } else if (newStatus === WebSocketConnectionStatus.ERROR) {
    addLog(`WebSocket连接错误: ${wsStore.lastError || '未知错误'}`, 'error')
  }
})

onMounted(() => {
  // 如果WebSocket已连接，自动连接
  if (!wsStore.isConnected && !wsStore.isConnecting) {
    wsStore.connect()
    addLog('正在连接WebSocket...', 'info')
  }
})

onUnmounted(() => {
  // 如果有批次ID，取消订阅
  if (batchId.value) {
    wsStore.unsubscribeFromBatch(batchId.value)
  }
})
</script>

<style scoped>
.batch-status-monitor {
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.monitor-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.batch-info {
  margin-bottom: 20px;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.batch-id {
  font-size: 16px;
  font-weight: 500;
}

.batch-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.last-update {
  margin-top: 12px;
  text-align: right;
  color: #909399;
  font-size: 12px;
}

.no-batch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 0;
}

.batch-input {
  width: 200px;
}

.activity-log {
  margin-top: 20px;
}

.activity-log h4 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 500;
}

.log-list {
  padding: 8px;
}

.log-item {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  display: flex;
  gap: 12px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #909399;
  white-space: nowrap;
}

.log-message {
  flex: 1;
}

.log-error {
  color: #f56c6c;
}

.empty-log {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}
</style>
