<template>
  <div class="answer-batch-dashboard">
    <el-page-header @back="goBack" title="返回" content="回答批次仪表盘" />

    <div class="page-container">
      <!-- 统计卡片 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <template #header>
              <div class="stat-header">
                <span>总批次数</span>
                <el-icon><DataLine /></el-icon>
              </div>
            </template>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.totalBatches }}</div>
              <div class="stat-label">个批次</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <template #header>
              <div class="stat-header">
                <span>运行中批次</span>
                <el-icon><Loading /></el-icon>
              </div>
            </template>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.runningBatches }}</div>
              <div class="stat-label">个批次</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <template #header>
              <div class="stat-header">
                <span>已完成批次</span>
                <el-icon><Select /></el-icon>
              </div>
            </template>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.completedBatches }}</div>
              <div class="stat-label">个批次</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card" shadow="hover">
            <template #header>
              <div class="stat-header">
                <span>总生成回答数</span>
                <el-icon><ChatDotRound /></el-icon>
              </div>
            </template>
            <div class="stat-content">
              <div class="stat-number">{{ statistics.totalAnswers }}</div>
              <div class="stat-label">个回答</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- WebSocket状态 -->
      <el-row :gutter="20" class="content-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <h2>实时状态监控</h2>
                <el-tag :type="wsStore.isConnected ? 'success' : 'danger'">
                  {{ wsStore.isConnected ? '已连接' : '未连接' }}
                </el-tag>
              </div>
            </template>
            <div class="websocket-status-container">
              <WebSocketStatus />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 批次列表 -->
      <el-row :gutter="20" class="content-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <h2>批次列表</h2>
                  <el-radio-group v-model="filterStatus" size="small" class="status-filter">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button label="GENERATING_ANSWERS">运行中</el-radio-button>
                    <el-radio-button label="COMPLETED">已完成</el-radio-button>
                    <el-radio-button label="FAILED">失败</el-radio-button>
                    <el-radio-button label="PAUSED">已暂停</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="header-right">
                  <el-button type="primary" @click="refreshBatches">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                  <el-button type="success" @click="router.push('/runtime/create-answer-batch')">
                    <el-icon><Plus /></el-icon>
                    创建批次
                  </el-button>
                </div>
              </div>
            </template>

            <el-table :data="filteredBatches" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="批次名称" min-width="200">
                <template #default="scope">
                  <div class="batch-name-cell">
                    <span>{{ scope.row.name }}</span>
                    <el-tooltip v-if="scope.row.description" :content="scope.row.description">
                      <el-icon><InfoFilled /></el-icon>
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getBatchStatusType(scope.row.status)">
                    {{ getBatchStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="progressPercentage" label="进度" width="200">
                <template #default="scope">
                  <div class="progress-cell">
                    <el-progress
                      :percentage="scope.row.progressPercentage"
                      :status="getBatchProgressStatus(scope.row.status)"
                      :stroke-width="10"
                    />
                    <span class="progress-text">
                      {{ scope.row.completedRuns }}/{{ scope.row.totalRuns }}
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="creationTime" label="创建时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.creationTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="scope">
                  <div class="action-buttons">
                    <el-button
                      v-if="scope.row.status === 'PENDING'"
                      type="primary"
                      size="small"
                      @click="startBatch(scope.row.id)"
                    >
                      <el-icon><VideoPlay /></el-icon>
                      启动
                    </el-button>
                    <el-button
                      v-if="scope.row.status === 'GENERATING_ANSWERS'"
                      type="warning"
                      size="small"
                      @click="pauseBatch(scope.row.id)"
                    >
                      <el-icon><VideoPause /></el-icon>
                      暂停
                    </el-button>
                    <el-button
                      v-if="scope.row.status === 'PAUSED'"
                      type="success"
                      size="small"
                      @click="resumeBatch(scope.row.id)"
                    >
                      <el-icon><VideoPlay /></el-icon>
                      恢复
                    </el-button>
                    <el-button
                      type="primary"
                      size="small"
                      @click="monitorBatch(scope.row.id)"
                    >
                      <el-icon><Monitor /></el-icon>
                      监控
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  InfoFilled,
  Plus,
  Monitor,
  VideoPlay,
  VideoPause,
  Refresh,
  DataLine,
  Loading,
  Select,
  ChatDotRound
} from '@element-plus/icons-vue'
import WebSocketStatus from '@/components/WebSocketStatus.vue'
import { useWebSocketStore } from '@/stores/websocket'
import { WebSocketMessageType } from '@/types/websocketTypes'
import type { BatchStatusUpdateData } from '@/types/websocketTypes'
import {
  startAnswerGenerationBatch,
  pauseAnswerGenerationBatch,
  resumeAnswerGenerationBatch,
  getUserAnswerGenerationBatches,
  BatchStatus,
  type AnswerGenerationBatch
} from '@/api/answerGenerationApis'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const wsStore = useWebSocketStore()
const userStore = useUserStore()

// 批次列表数据
const batches = ref<AnswerGenerationBatch[]>([])
const loading = ref(false)
const filterStatus = ref('')

// 统计数据
const statistics = reactive({
  totalBatches: 0,
  runningBatches: 0,
  completedBatches: 0,
  totalAnswers: 0
})

// 过滤后的批次列表
const filteredBatches = computed(() => {
  const batchList = batches.value || []
  if (!filterStatus.value) return batchList
  return batchList.filter(batch => batch.status === filterStatus.value)
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
    refreshBatches()
  } catch (error) {
    ElMessage.error('启动批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 暂停批次
const pauseBatch = async (batchId: number) => {
  try {
    await pauseAnswerGenerationBatch(batchId)
    ElMessage.success('批次已暂停')
    refreshBatches()
  } catch (error) {
    ElMessage.error('暂停批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 恢复批次
const resumeBatch = async (batchId: number) => {
  try {
    await resumeAnswerGenerationBatch(batchId)
    ElMessage.success('批次已恢复')
    refreshBatches()
  } catch (error) {
    ElMessage.error('恢复批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 刷新批次列表
const refreshBatches = async () => {
  const userId = userStore.currentUser?.id
  if (!userId) {
    ElMessage.error('用户未登录')
    return
  }

  loading.value = true
  try {
    const response = await getUserAnswerGenerationBatches(userId, {
      page: 0,
      size: 100,
      status: filterStatus.value as BatchStatus || undefined
    })
    batches.value = response || []
    updateStatistics()
  } catch (error) {
    ElMessage.error('加载批次列表失败: ' + (error instanceof Error ? error.message : '未知错误'))
    batches.value = []
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = () => {
  const batchList = batches.value || []

  statistics.totalBatches = batchList.length
  statistics.runningBatches = batchList.filter(
    batch => batch.status === BatchStatus.GENERATING_ANSWERS
  ).length
  statistics.completedBatches = batchList.filter(
    batch => batch.status === BatchStatus.COMPLETED
  ).length
  statistics.totalAnswers = batchList.reduce(
    (sum, batch) => sum + (batch.completedRuns || 0),
    0
  )
}

// 处理WebSocket批次状态更新
const handleBatchStatusUpdate = (data: BatchStatusUpdateData) => {
  if (!data || !data.batchId) return

  if (!Array.isArray(batches.value)) {
    batches.value = []
  }

  const batchIndex = batches.value.findIndex(b => b.id === data.batchId)
  if (batchIndex >= 0) {
    batches.value[batchIndex] = {
      ...batches.value[batchIndex],
      ...data,
      status: data.status as BatchStatus
    }
    updateStatistics()
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 监听WebSocket消息
const setupWebSocketListeners = () => {
  if (!wsStore.isConnected && !wsStore.isConnecting) {
    wsStore.connect()
  }

  const unsubscribe = watch(() => wsStore.messages, (newMessages, oldMessages) => {
    if (!newMessages || newMessages.length === 0) return
    if (!oldMessages || oldMessages.length === 0) {
      newMessages.forEach((msg) => {
        if (msg.type === WebSocketMessageType.STATUS_CHANGE ||
            msg.type === WebSocketMessageType.TASK_COMPLETED ||
            msg.type === WebSocketMessageType.TASK_FAILED) {
          handleBatchStatusUpdate(msg.data as BatchStatusUpdateData)
        }
      })
      return
    }

    const oldLength = oldMessages ? oldMessages.length : 0
    const newlyAddedMessages = newMessages.slice(oldLength)

    newlyAddedMessages.forEach((msg) => {
      if (msg.type === WebSocketMessageType.STATUS_CHANGE ||
          msg.type === WebSocketMessageType.TASK_COMPLETED ||
          msg.type === WebSocketMessageType.TASK_FAILED) {
        handleBatchStatusUpdate(msg.data as BatchStatusUpdateData)
      }
    })
  }, { deep: true })

  return unsubscribe
}

onMounted(() => {
  refreshBatches()
  const unsubscribe = setupWebSocketListeners()
  onUnmounted(() => {
    unsubscribe()
  })
})
</script>

<style scoped>
.answer-batch-dashboard {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  gap: 8px;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.status-filter {
  margin-left: 16px;
}

.stat-card {
  height: 100%;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}

.stat-content {
  text-align: center;
  padding: 12px 0;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-color-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.batch-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.websocket-status-container {
  min-height: 100px;
}
</style>
