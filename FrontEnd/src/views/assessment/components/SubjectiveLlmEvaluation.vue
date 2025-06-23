<template>
  <div class="subjective-llm-evaluation">
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="openEvaluationDialog(row)"
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

    <!-- 评测对话框 -->
    <el-dialog
      v-model="evaluationDialog.visible"
      title="主观题大模型评测"
      width="600px"
      destroy-on-close
    >
      <div class="evaluation-form">
        <el-form :model="evaluationForm" label-width="120px">
          <el-form-item label="评测模型">
            <el-select v-model="evaluationForm.modelId" placeholder="请选择评测模型">
              <el-option
                v-for="model in availableModels"
                :key="model.id"
                :label="model.name"
                :value="model.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="评测提示词">
            <el-input
              v-model="evaluationForm.prompt"
              type="textarea"
              :rows="5"
              placeholder="请输入评测提示词"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="evaluationDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="startEvaluation" :loading="submitting">
            开始评测
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { evaluateBatchSubjective } from '@/api/evaluations'
import { BatchStatus, type AnswerGenerationBatch } from '@/api/answerGenerationBatch'
import { getEvaluators, type Evaluator } from '@/api/evaluator'
import { testSingleModelConnectivity } from '@/api/llmModel'

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
const submitting = ref(false)

// 可用模型
const availableModels = ref<Evaluator[]>([])

// 评测对话框状态
const evaluationDialog = reactive({
  visible: false,
  currentBatch: null as AnswerGenerationBatch | null
})

// 评测表单
const evaluationForm = reactive({
  modelId: '',
  prompt: '请评估以下回答的质量，考虑准确性、完整性、逻辑性和表达清晰度。给出1-100的分数和详细评价。'
})

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

// 打开评测对话框
const openEvaluationDialog = async (batch: AnswerGenerationBatch) => {
  try {
    // 获取可用模型
    const evaluators = await getEvaluators()
    // 过滤出LLM类型的评测器作为可用模型
    availableModels.value = evaluators.filter(evaluator => evaluator.type === 'LLM')

    if (availableModels.value.length > 0) {
      evaluationForm.modelId = availableModels.value[0].id.toString()
    }

    evaluationDialog.currentBatch = batch
    evaluationDialog.visible = true
  } catch (error) {
    console.error('获取可用模型失败:', error)
    ElMessage.error('获取可用模型失败')
  }
}

// 测试模型连通性
const testSelectedModel = async () => {
  if (!evaluationForm.modelId) {
    ElMessage.warning('请先选择评测模型')
    return false
  }

  try {
    const result = await testSingleModelConnectivity(parseInt(evaluationForm.modelId))
    if (result.success) {
      ElMessage.success('模型连接测试成功')
      return true
    } else {
      ElMessage.error(`模型连接测试失败: ${result.message || '未知错误'}`)
      return false
    }
  } catch (error) {
    console.error('测试模型连通性失败:', error)
    ElMessage.error('测试模型连通性失败')
    return false
  }
}

// 开始评测
const startEvaluation = async () => {
  if (!evaluationDialog.currentBatch) {
    ElMessage.error('批次数据不完整')
    return
  }

  if (!evaluationForm.modelId) {
    ElMessage.warning('请选择评测模型')
    return
  }

  // 先测试模型连通性
  const isConnected = await testSelectedModel()
  if (!isConnected) {
    return
  }

  try {
    submitting.value = true
    const userData = userStore.getCurrentUser()

    await evaluateBatchSubjective({
      batchId: evaluationDialog.currentBatch.id,
      evaluatorId: parseInt(evaluationForm.modelId),
      userId: userData?.id || 0,
      prompt: evaluationForm.prompt
    })

    ElMessage.success('主观题评测已启动')
    evaluationDialog.visible = false
    emit('refresh')
  } catch (error) {
    console.error('启动主观题评测失败:', error)
    ElMessage.error('启动主观题评测失败')
  } finally {
    submitting.value = false
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
.subjective-llm-evaluation {
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

.evaluation-form {
  padding: 20px;
}
</style>
