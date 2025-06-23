<template>
  <div class="answer-generation-batches">
    <el-page-header @back="goBack" title="返回" content="回答生成批次管理" />

    <div class="page-container">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <h2>批次管理</h2>
                <el-tooltip content="创建和管理回答生成批次，监控批次状态">
                  <el-icon><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
            </template>
            <div class="card-content">
              <p>在此页面中，您可以创建新的回答生成批次，查看现有批次的状态，并管理批次的执行。通过WebSocket实时连接，您可以监控批次的进度和状态变化。</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="content-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <h2>WebSocket连接状态</h2>
              </div>
            </template>
            <div class="websocket-status-container">
              <WebSocketStatus />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="content-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <h2>批次列表</h2>
                <el-button type="primary" @click="router.push('/runtime/create-answer-batch')" :icon="Plus">
                  创建批次
                </el-button>
              </div>
            </template>
            <div class="batch-list">
              <el-table :data="batches" style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="批次名称" />
                <el-table-column prop="status" label="状态">
                  <template #default="scope">
                    <el-tag :type="getBatchStatusType(scope.row.status)">
                      {{ getBatchStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="progressPercentage" label="进度">
                  <template #default="scope">
                    <el-progress
                      :percentage="scope.row.progressPercentage"
                      :status="getBatchProgressStatus(scope.row.status)"
                      :stroke-width="10"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="creationTime" label="创建时间" width="180">
                  <template #default="scope">
                    {{ formatDate(scope.row.creationTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="280">
                  <template #default="scope">
                    <el-button-group>
                      <el-button
                        type="primary"
                        size="small"
                        @click="monitorBatch(scope.row.id)"
                        :icon="Monitor"
                      >
                        监控
                      </el-button>
                      <el-button
                        type="success"
                        size="small"
                        @click="startBatch(scope.row.id)"
                        :icon="VideoPlay"
                        :disabled="scope.row.status !== 'PENDING'"
                      >
                        启动
                      </el-button>
                      <el-button
                        type="warning"
                        size="small"
                        @click="pauseBatch(scope.row.id)"
                        :icon="VideoPause"
                        :disabled="scope.row.status !== 'GENERATING_ANSWERS'"
                      >
                        暂停
                      </el-button>
                      <el-button
                        type="info"
                        size="small"
                        @click="resumeBatch(scope.row.id)"
                        :icon="VideoPlay"
                        :disabled="scope.row.status !== 'PAUSED'"
                      >
                        恢复
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 不再需要创建批次对话框，已经创建了独立页面 -->
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  InfoFilled,
  Plus,
  Monitor,
  VideoPlay,
  VideoPause
} from '@element-plus/icons-vue'
import WebSocketStatus from '@/components/WebSocketStatus.vue'
import { useWebSocketStore } from '@/stores/websocket'
import {
  startAnswerGenerationBatch,
  pauseAnswerGenerationBatch,
  resumeAnswerGenerationBatch,
  BatchStatus,
  type AnswerGenerationBatch
} from '@/api/answerGenerationBatch'
import { WebSocketMessageType } from '@/types/websocketTypes'
import type { BatchStatusUpdateData } from '@/types/websocketTypes'
import { useUserStore } from '@/stores/user'
import { getUserAnswerGenerationBatches, getAllAnswerGenerationBatches } from '@/api/answerGenerationBatch'

const router = useRouter()
const wsStore = useWebSocketStore()
const userStore = useUserStore()

// 批次列表数据
const batches = ref<AnswerGenerationBatch[]>([])
const loading = ref(false)
const filterStatus = ref<BatchStatus | null>(null)
const statistics = ref({
  totalBatches: 0,
  runningBatches: 0,
  completedBatches: 0,
  totalAnswers: 0
})

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 获取批次状态类型
const getBatchStatusType = (status: string) => {
  switch (status) {
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
}

// 获取批次状态文本
const getBatchStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '等待中',
    'GENERATING_ANSWERS': '生成回答中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'PAUSED': '已暂停',
    'RESUMING': '恢复中'
  }

  return statusMap[status] || '未知'
}

// 获取批次进度状态
const getBatchProgressStatus = (status: string) => {
  switch (status) {
    case BatchStatus.COMPLETED:
      return 'success'
    case BatchStatus.FAILED:
      return 'exception'
    case BatchStatus.PAUSED:
      return 'warning'
    default:
      return ''
  }
}

// 监控批次
const monitorBatch = (batchId: number) => {
  router.push({
    name: 'batch-monitor',
    query: { batchId: batchId.toString() }
  })
}

// 启动批次
const startBatch = async (batchId: number) => {
  try {
    await startAnswerGenerationBatch(batchId)
    ElMessage.success('批次已启动')
    loadBatches()
  } catch (error) {
    ElMessage.error('启动批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 暂停批次
const pauseBatch = async (batchId: number) => {
  try {
    await pauseAnswerGenerationBatch(batchId)
    ElMessage.success('批次已暂停')
    loadBatches()
  } catch (error) {
    ElMessage.error('暂停批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 恢复批次
const resumeBatch = async (batchId: number) => {
  try {
    await resumeAnswerGenerationBatch(batchId)
    ElMessage.success('批次已恢复')
    loadBatches()
  } catch (error) {
    ElMessage.error('恢复批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 加载批次列表
const loadBatches = async () => {
  loading.value = true
  try {
    const response = await getAllAnswerGenerationBatches()

    batches.value = response || []

    // 更新统计信息
    statistics.value.totalBatches = batches.value.length
    statistics.value.runningBatches = batches.value.filter(
      batch => batch.status === BatchStatus.GENERATING_ANSWERS
    ).length
    statistics.value.completedBatches = batches.value.filter(
      batch => batch.status === BatchStatus.COMPLETED
    ).length
    statistics.value.totalAnswers = batches.value.reduce(
      (sum, batch) => sum + (batch.completedRuns || 0),
      0
    )
  } catch (error) {
    ElMessage.error('加载批次列表失败: ' + (error instanceof Error ? error.message : '未知错误'))
    batches.value = []
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理WebSocket批次状态更新
const handleBatchStatusUpdate = (data: WebSocketMessage['payload']) => {
  if (!data.batchId) return

  // 查找并更新批次状态
  const batchIndex = batches.value.findIndex(b => b.id === data.batchId)
  if (batchIndex >= 0) {
    // 更新批次信息
    const updatedBatch = {
      ...batches.value[batchIndex]
    }

    // 更新状态（如果有）
    if (data.status) {
      updatedBatch.status = data.status
    }

    // 更新进度信息（如果有）
    if (data.progressPercentage !== undefined) {
      updatedBatch.progressPercentage = data.progressPercentage
    }
    if (data.completedRuns !== undefined) {
      updatedBatch.completedRuns = data.completedRuns
    }
    if (data.pendingRuns !== undefined) {
      updatedBatch.pendingRuns = data.pendingRuns
    }
    if (data.failedRuns !== undefined) {
      updatedBatch.failedRuns = data.failedRuns
    }
    if (data.lastActivityTime) {
      updatedBatch.lastActivityTime = data.lastActivityTime
    }

    // 更新批次
    batches.value[batchIndex] = updatedBatch

    // 更新统计信息
    statistics.value.totalBatches = batches.value.length
    statistics.value.runningBatches = batches.value.filter(
      batch => batch.status === BatchStatus.GENERATING_ANSWERS
    ).length
    statistics.value.completedBatches = batches.value.filter(
      batch => batch.status === BatchStatus.COMPLETED
    ).length
    statistics.value.totalAnswers = batches.value.reduce(
      (sum, batch) => sum + (batch.completedRuns || 0),
      0
    )
  }
}

// 监听WebSocket消息
const setupWebSocketListeners = () => {
  // 如果WebSocket未连接，则连接
  if (!wsStore.isConnected && !wsStore.isConnecting) {
    wsStore.connect()
  }

  // 订阅批次状态更新
  wsStore.subscribeToBatchUpdates()

  // 监听批次状态更新消息
  const unsubscribe = watch(() => wsStore.messages, (newMessages, oldMessages) => {
    if (!newMessages || newMessages.length === 0) return

    const processMessage = (msg: WebSocketMessage) => {
      if (msg.type === WebSocketMessageType.STATUS_CHANGE ||
          msg.type === WebSocketMessageType.PROGRESS_UPDATE ||
          msg.type === WebSocketMessageType.TASK_COMPLETED ||
          msg.type === WebSocketMessageType.TASK_FAILED) {
        handleBatchStatusUpdate(msg.payload)
      }
    }

    if (!oldMessages || oldMessages.length === 0) {
      // 首次获取消息，处理所有消息
      newMessages.forEach(processMessage)
    } else {
      // 只处理新增的消息
      const newlyAddedMessages = newMessages.slice(oldMessages.length)
      newlyAddedMessages.forEach(processMessage)
    }
  }, { deep: true })

  return () => {
    unsubscribe()
    wsStore.unsubscribeFromBatchUpdates()
  }
}

// 组件挂载时
onMounted(async () => {
  // 加载批次列表
  await loadBatches()

  // 设置WebSocket监听
  const cleanup = setupWebSocketListeners()

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })
})
</script>

<style scoped>
.answer-generation-batches {
  padding: 20px;
}

.page-container {
  margin-top: 20px;
}

.content-row {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.card-content {
  color: #606266;
  line-height: 1.6;
}

.websocket-status-container {
  margin-bottom: 16px;
}

.batch-list {
  margin-top: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
