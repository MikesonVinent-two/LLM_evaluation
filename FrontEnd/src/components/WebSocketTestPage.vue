<template>
  <div class="websocket-test-page">
    <el-card>
      <template #header>
        <h2>WebSocket 消息测试</h2>
      </template>

      <div class="test-controls">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>
                <h3>连接状态</h3>
              </template>
              <WebSocketStatus />
              <div class="connection-info">
                <p>连接URL: {{ wsUrl }}</p>
                <p>消息数量: {{ wsStore.messages.length }}</p>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card>
              <template #header>
                <h3>测试控制</h3>
              </template>
              <div class="test-buttons">
                <el-button
                  type="primary"
                  @click="triggerQuestionCompleted"
                  :disabled="!wsStore.isConnected"
                >
                  触发 Question Completed 测试
                </el-button>
                <el-button @click="clearMessages">清空消息</el-button>
                <el-button @click="subscribeToTestMessages">订阅测试消息</el-button>
              </div>

              <div class="test-params">
                <el-form :model="testParams" label-width="100px">
                  <el-form-item label="批次ID:">
                    <el-input-number v-model="testParams.batchId" :min="1" />
                  </el-form-item>
                  <el-form-item label="问题ID:">
                    <el-input-number v-model="testParams.questionId" :min="1" />
                  </el-form-item>
                  <el-form-item label="完成数量:">
                    <el-input-number v-model="testParams.completedCount" :min="1" />
                  </el-form-item>
                </el-form>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="message-display">
        <el-card>
          <template #header>
            <div class="message-header">
              <h3>接收到的消息</h3>
              <el-tag :type="questionCompletedCount > 0 ? 'success' : 'info'">
                QUESTION_COMPLETED: {{ questionCompletedCount }}
              </el-tag>
            </div>
          </template>

          <el-scrollbar height="400px">
            <div class="message-list">
              <div
                v-for="(message, index) in wsStore.messages"
                :key="index"
                class="message-item"
                :class="{ 'question-completed': message.type === 'QUESTION_COMPLETED' }"
              >
                <div class="message-header-info">
                  <el-tag
                    :type="getMessageTypeColor(message.type)"
                    size="small"
                  >
                    {{ message.type }}
                  </el-tag>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <div class="message-payload">
                  <pre>{{ JSON.stringify(message.payload, null, 2) }}</pre>
                </div>
              </div>

              <div v-if="wsStore.messages.length === 0" class="no-messages">
                暂无消息
              </div>
            </div>
          </el-scrollbar>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import { WebSocketMessageType } from '@/types/websocketTypes'
import WebSocketStatus from './WebSocketStatus.vue'
import { ElMessage } from 'element-plus'

const wsStore = useWebSocketStore()

// 测试参数
const testParams = ref({
  batchId: 8,
  questionId: 999,
  completedCount: 10
})

// WebSocket URL
const wsUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/ws`)

// 统计 QUESTION_COMPLETED 消息数量
const questionCompletedCount = computed(() => {
  return wsStore.messages.filter(msg => msg.type === WebSocketMessageType.QUESTION_COMPLETED).length
})

// 触发问题完成测试
const triggerQuestionCompleted = async () => {
  try {
    const url = `http://localhost:8080/api/websocket-test/question-completed/${testParams.value.batchId}?questionId=${testParams.value.questionId}&completedCount=${testParams.value.completedCount}`

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      ElMessage.success('测试请求已发送')
    } else {
      ElMessage.error(`测试请求失败: ${response.status}`)
    }
  } catch (error) {
    console.error('发送测试请求失败:', error)
    ElMessage.error('发送测试请求失败')
  }
}

// 清空消息
const clearMessages = () => {
  wsStore.clearMessages()
  ElMessage.info('消息已清空')
}

// 订阅测试消息
const subscribeToTestMessages = () => {
  if (!wsStore.isConnected) {
    ElMessage.warning('WebSocket未连接')
    return
  }

  // 订阅批次更新
  wsStore.subscribeToBatch(testParams.value.batchId)
  ElMessage.success(`已订阅批次 ${testParams.value.batchId} 的消息`)
}

// 获取消息类型颜色
const getMessageTypeColor = (type: string) => {
  switch (type) {
    case WebSocketMessageType.QUESTION_COMPLETED:
      return 'success'
    case WebSocketMessageType.QUESTION_FAILED:
      return 'danger'
    case WebSocketMessageType.PROGRESS_UPDATE:
      return 'primary'
    case WebSocketMessageType.STATUS_CHANGE:
      return 'warning'
    case WebSocketMessageType.ERROR:
      return 'danger'
    default:
      return 'info'
  }
}

// 格式化时间
const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

// 组件挂载时连接WebSocket
onMounted(async () => {
  if (!wsStore.isConnected) {
    await wsStore.connect()
  }
})

// 组件卸载时清理
onUnmounted(() => {
  // 可以在这里进行清理工作
})
</script>

<style scoped>
.websocket-test-page {
  padding: 20px;
}

.test-controls {
  margin-bottom: 20px;
}

.connection-info {
  margin-top: 16px;
}

.connection-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}

.test-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.test-params {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.message-display {
  margin-top: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-list {
  padding: 8px;
}

.message-item {
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafafa;
}

.message-item.question-completed {
  border-color: #67c23a;
  background: #f0f9ff;
}

.message-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-payload {
  background: white;
  border-radius: 4px;
  padding: 8px;
  font-size: 12px;
}

.message-payload pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
