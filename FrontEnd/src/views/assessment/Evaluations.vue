<template>
  <div class="evaluations">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <h2>批次评测管理</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 批次列表 -->
      <el-table :data="currentPageData" v-loading="loading" style="width: 100%">
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
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button
                type="primary"
                size="small"
                @click="navigateToObjectiveEvaluation(row)"
                :disabled="!canEvaluate(row)"
              >
                客观题评测
              </el-button>
              <el-button
                type="success"
                size="small"
                @click="navigateToLlmEvaluation(row)"
                :disabled="!canEvaluate(row)"
              >
                主观题大模型评测
              </el-button>
              <el-button
                type="warning"
                size="small"
                @click="navigateToHumanEvaluation(row)"
                :disabled="!canEvaluate(row)"
              >
                主观题人工评测
              </el-button>
            </el-button-group>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getAllAnswerGenerationBatches, BatchStatus, type AnswerGenerationBatch } from '@/api/answerGenerationBatch'

// 路由
const router = useRouter()

// 状态和数据
const loading = ref(false)
const batches = ref<AnswerGenerationBatch[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 计算当前页的数据
const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return batches.value.slice(start, end)
})

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

// 导航到客观题评测页面
const navigateToObjectiveEvaluation = (batch: AnswerGenerationBatch) => {
  router.push({
    name: 'ObjectiveEvaluation',
    params: { batchId: batch.id },
    query: { batchName: batch.name }
  })
}

// 导航到主观题大模型评测页面
const navigateToLlmEvaluation = (batch: AnswerGenerationBatch) => {
  router.push({
    name: 'SubjectiveLlmEvaluation',
    params: { batchId: batch.id },
    query: { batchName: batch.name }
  })
}

// 导航到主观题人工评测页面
const navigateToHumanEvaluation = (batch: AnswerGenerationBatch) => {
  router.push({
    name: 'SubjectiveHumanEvaluation',
    params: { batchId: batch.id },
    query: { batchName: batch.name }
  })
}

// 刷新数据
const refreshData = async () => {
  try {
    loading.value = true

    // 获取所有批次
    const batchList = await getAllAnswerGenerationBatches()
    batches.value = batchList.filter(batch =>
      batch.status === BatchStatus.COMPLETED ||
      batch.status === BatchStatus.GENERATING_ANSWERS
    )
    total.value = batches.value.length
  } catch (error) {
    console.error('获取批次列表失败:', error)
    ElMessage.error('获取批次列表失败')
    batches.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

// 初始化
onMounted(() => {
  refreshData()
})

// 定义组件名称
defineOptions({
  name: 'EvaluationsView'
})
</script>

<style scoped>
.evaluations {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
