<template>
  <div class="batch-monitor-view">
    <el-page-header @back="goBack" title="返回" content="批次实时监控" />

    <div class="monitor-container">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <h2>WebSocket实时监控</h2>
                <el-tooltip content="WebSocket用于实时接收服务器推送的消息，无需刷新页面即可获取最新数据">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
            </template>
            <div class="card-content">
              <p>WebSocket技术允许在客户端和服务器之间建立持久连接，实现双向通信。在批次监控中，我们使用WebSocket来接收批次状态的实时更新，包括进度、运行状态等信息。</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="monitor-row">
        <el-col :span="24">
          <BatchStatusMonitor />
        </el-col>
      </el-row>

      <el-row :gutter="20" class="monitor-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <h2>批次列表</h2>
                <el-button type="primary" size="small" @click="fetchBatches" :loading="loading">
                  刷新
                </el-button>
              </div>
            </template>
            <div class="batch-list">
              <el-table :data="recentBatches" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="批次ID" width="80" />
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
                <el-table-column label="操作" width="200">
                  <template #default="scope">
                    <el-button
                      type="primary"
                      size="small"
                      @click="monitorBatch(scope.row.id)"
                      :icon="Monitor"
                    >
                      监控
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-empty v-if="!loading && recentBatches.length === 0" description="暂无批次数据" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { QuestionFilled, Monitor } from '@element-plus/icons-vue'
import BatchStatusMonitor from '@/components/BatchStatusMonitor.vue'
import { BatchStatus, getAllAnswerGenerationBatches, type AnswerGenerationBatch } from '@/api/answerGenerationBatch'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 批次数据
const recentBatches = ref<AnswerGenerationBatch[]>([])
const loading = ref(false)

// 获取批次数据
const fetchBatches = async () => {
  loading.value = true
  try {
    const batches = await getAllAnswerGenerationBatches()
    recentBatches.value = batches
  } catch (error) {
    console.error('获取批次数据失败:', error)
    ElMessage.error('获取批次数据失败')
  } finally {
    loading.value = false
  }
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
    [BatchStatus.PENDING]: '等待中',
    [BatchStatus.GENERATING_ANSWERS]: '生成回答中',
    [BatchStatus.COMPLETED]: '已完成',
    [BatchStatus.FAILED]: '失败',
    [BatchStatus.PAUSED]: '已暂停',
    [BatchStatus.RESUMING]: '恢复中'
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
  // 这里可以实现跳转到批次详情页或触发监控组件的批次切换
  console.log(`监控批次 ${batchId}`)

  // 示例：可以通过事件总线或其他方式通知BatchStatusMonitor组件
  // 这里简单通过重新加载页面并传递参数实现
  router.push({
    name: 'batch-monitor',
    query: { batchId }
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  // 加载批次数据
  fetchBatches()
})
</script>

<style scoped>
.batch-monitor-view {
  padding: 20px;
}

.monitor-container {
  margin-top: 20px;
}

.monitor-row {
  margin-top: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.batch-list {
  margin-top: 10px;
}
</style>
