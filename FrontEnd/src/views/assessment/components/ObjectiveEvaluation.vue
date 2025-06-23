<template>
  <div class="objective-evaluation">
    <!-- 操作按钮 -->
    <div class="actions">
      <el-button type="primary" @click="$emit('refresh')" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 批次列表 -->
    <el-card class="batch-list">
      <el-table :data="batches" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="批次ID" width="80" />
        <el-table-column prop="name" label="批次名称" min-width="120" />
        <el-table-column prop="description" label="批次描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="datasetVersionName" label="数据集" width="120" />
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progressPercentage || 0"
              :status="getProgressStatus(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="evaluateObjectiveQuestions(row)"
              :disabled="!canEvaluate(row)"
            >
              开始评测
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { evaluateBatchObjectiveQuestions } from '@/api/evaluations'
import { BatchStatus, type AnswerGenerationBatch } from '@/api/answerGenerationBatch'

const props = defineProps<{
  batches: AnswerGenerationBatch[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
}>()

// 用户存储
const userStore = useUserStore()

// 分页
const currentPage = ref(1)
const pageSize = ref(10)

// 计算分页后的数据
const paginatedBatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return props.batches.slice(start, end)
})

// 总数
const total = computed(() => props.batches.length)

// 获取状态类型对应的Tag类型
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'info',
    'GENERATING_ANSWERS': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'PAUSED': 'info',
    'RESUMING': 'warning'
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'GENERATING_ANSWERS': '生成中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'PAUSED': '已暂停',
    'RESUMING': '恢复中'
  }
  return statusMap[status] || status
}

// 获取进度条状态
const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'PAUSED') return 'warning'
  return ''
}

// 检查是否可以进行评测
const canEvaluate = (batch: AnswerGenerationBatch) => {
  return batch.status === BatchStatus.COMPLETED
}

// 客观题评测
const evaluateObjectiveQuestions = async (batch: AnswerGenerationBatch) => {
  try {
    const userData = userStore.getCurrentUser()

    await evaluateBatchObjectiveQuestions(batch.id, {
      userId: userData?.id,
      evaluatorId: userData?.evaluatorId
    })

    ElMessage.success('客观题评测已启动')
    emit('refresh')
  } catch (error) {
    console.error('启动客观题评测失败:', error)
    ElMessage.error('启动客观题评测失败')
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}
</script>

<style scoped>
.objective-evaluation {
  padding: 20px 0;
}

.actions {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.batch-list {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
