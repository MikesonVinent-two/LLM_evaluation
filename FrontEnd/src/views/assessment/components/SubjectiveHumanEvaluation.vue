<template>
  <div class="subjective-human-evaluation">
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
              @click="startHumanEvaluation(row)"
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

    <!-- 人工评测对话框 -->
    <el-dialog
      v-model="humanEvaluationDialog.visible"
      title="主观题人工评测"
      width="800px"
      destroy-on-close
    >
      <div v-if="humanEvaluationDialog.currentAnswer" class="human-evaluation-content">
        <div class="answer-info">
          <h3>问题信息</h3>
          <div class="info-row">
            <span class="info-label">问题:</span>
            <span>{{ humanEvaluationDialog.currentQuestion?.questionText }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">类型:</span>
            <span>{{ getQuestionTypeText(humanEvaluationDialog.currentQuestion?.questionType) }}</span>
          </div>

          <h3>模型回答</h3>
          <div class="info-row">
            <span>{{ humanEvaluationDialog.currentAnswer.answer?.answerText }}</span>
          </div>

          <h3>标准答案</h3>
          <div class="info-row" v-if="humanEvaluationDialog.currentAnswer.standardAnswer">
            <span>{{ humanEvaluationDialog.currentAnswer.standardAnswer.answerText }}</span>
          </div>
          <div class="info-row" v-else>
            <span>暂无标准答案</span>
          </div>
        </div>

        <el-divider>评分</el-divider>

        <div class="evaluation-form">
          <el-form :model="evaluationForm" label-width="120px">
            <el-form-item label="总体评分">
              <el-slider
                v-model="evaluationForm.overallScore"
                :min="0"
                :max="100"
                :step="1"
                show-input
                :marks="{0: '0', 60: '60', 80: '80', 100: '100'}"
              />
            </el-form-item>

            <el-form-item label="评语">
              <el-input
                v-model="evaluationForm.comments"
                type="textarea"
                :rows="3"
                placeholder="请输入评语"
              />
            </el-form-item>

            <el-divider>详细评分项</el-divider>

            <div v-for="(item, index) in evaluationForm.detailScores" :key="index" class="criterion-item">
              <span class="criterion-name">{{ item.criterionName }}</span>
              <el-slider
                v-model="item.score"
                :min="0"
                :max="100"
                :step="1"
                show-input
              />
              <el-input
                v-model="item.comments"
                type="textarea"
                :rows="2"
                :placeholder="`${item.criterionName}评语`"
              />
            </div>
          </el-form>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="humanEvaluationDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="submitHumanEvaluation" :loading="submitting">
            提交评测
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, defineProps, defineEmits } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  submitHumanEvaluation as submitHumanEval,
  getBatchUnevaluatedAnswers,
  type HumanEvaluationRequest,
  type UnevaluatedAnswer,
  type StandardQuestion
} from '@/api/evaluations'
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
const submitting = ref(false)

// 人工评测对话框状态
const humanEvaluationDialog = reactive({
  visible: false,
  currentBatch: null as AnswerGenerationBatch | null,
  currentQuestion: null as StandardQuestion | null,
  currentAnswer: null as UnevaluatedAnswer | null
})

// 评分表单
const evaluationForm = reactive({
  overallScore: 70,
  comments: '',
  detailScores: [
    { criterionId: 1, criterionName: '准确性', score: 70, comments: '' },
    { criterionId: 2, criterionName: '完整性', score: 70, comments: '' },
    { criterionId: 3, criterionName: '逻辑性', score: 70, comments: '' },
    { criterionId: 4, criterionName: '表达清晰度', score: 70, comments: '' }
  ]
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

// 获取问题类型文本
const getQuestionTypeText = (type?: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题',
    'SUBJECTIVE': '主观题'
  }
  return typeMap[type || ''] || type || '未知类型'
}

// 初始化评分表单
const initEvaluationForm = () => {
  evaluationForm.overallScore = 70
  evaluationForm.comments = ''
  evaluationForm.detailScores.forEach(item => {
    item.score = 70
    item.comments = ''
  })
}

// 检查是否可以进行评测
const canEvaluate = (batch: AnswerGenerationBatch) => {
  return batch.status === BatchStatus.COMPLETED
}

// 开始人工评测
const startHumanEvaluation = async (batch: AnswerGenerationBatch) => {
  try {
    const userData = userStore.getCurrentUser()

    const response = await getBatchUnevaluatedAnswers(batch.id, {
      evaluatorId: userData?.evaluatorId,
      page: 0,
      size: 1
    })

    if (response.unevaluatedAnswers && response.unevaluatedAnswers.length > 0) {
      const firstAnswer = response.unevaluatedAnswers[0]
      humanEvaluationDialog.currentBatch = batch
      humanEvaluationDialog.currentQuestion = firstAnswer.standardQuestion
      humanEvaluationDialog.currentAnswer = firstAnswer
      humanEvaluationDialog.visible = true
      initEvaluationForm()
    } else {
      ElMessage.info('该批次暂无需要评测的答案')
    }
  } catch (error) {
    console.error('获取待评测答案失败:', error)
    ElMessage.error('获取待评测答案失败')
  }
}

// 提交人工评测
const submitHumanEvaluation = async () => {
  if (!humanEvaluationDialog.currentAnswer || !humanEvaluationDialog.currentAnswer.answer) {
    ElMessage.error('评测数据不完整')
    return
  }

  try {
    submitting.value = true
    const userData = userStore.getCurrentUser()

    const evaluationData: HumanEvaluationRequest = {
      llmAnswerId: humanEvaluationDialog.currentAnswer.answer.id,
      evaluatorId: userData?.evaluatorId || 0,
      overallScore: evaluationForm.overallScore,
      comments: evaluationForm.comments,
      detailScores: evaluationForm.detailScores.map(item => ({
        criterion: item.criterionName,
        score: item.score,
        comments: item.comments
      })),
      userId: userData?.id || 0
    }

    await submitHumanEval(evaluationData)
    ElMessage.success('评测提交成功')
    humanEvaluationDialog.visible = false

    // 检查是否还有未评测的答案
    await checkNextUnevaluatedAnswer()
  } catch (error) {
    console.error('评测提交失败:', error)
    ElMessage.error('评测提交失败')
  } finally {
    submitting.value = false
  }
}

// 检查下一个未评测的答案
const checkNextUnevaluatedAnswer = async () => {
  if (!humanEvaluationDialog.currentBatch) return

  try {
    const userData = userStore.getCurrentUser()
    const response = await getBatchUnevaluatedAnswers(
      humanEvaluationDialog.currentBatch.id,
      {
        evaluatorId: userData?.evaluatorId,
        page: 0,
        size: 1
      }
    )

    if (response.unevaluatedAnswers && response.unevaluatedAnswers.length > 0) {
      const firstAnswer = response.unevaluatedAnswers[0]
      ElMessageBox.confirm(
        '还有未评测的答案，是否继续评测？',
        '继续评测',
        {
          confirmButtonText: '继续评测',
          cancelButtonText: '稍后再说',
          type: 'info'
        }
      ).then(() => {
        humanEvaluationDialog.currentQuestion = firstAnswer.standardQuestion
        humanEvaluationDialog.currentAnswer = firstAnswer
        humanEvaluationDialog.visible = true
        initEvaluationForm()
      }).catch(() => {
        // 用户选择稍后再说，不做任何操作
      })
    }
  } catch (error) {
    console.error('检查下一个未评测答案失败:', error)
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
.subjective-human-evaluation {
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

.human-evaluation-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.answer-info {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
  background-color: #f8f8f8;
}

.info-row {
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
}

.info-label {
  font-weight: bold;
  width: 80px;
  margin-right: 10px;
}

.criterion-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.criterion-name {
  font-weight: bold;
  margin-bottom: 10px;
  display: block;
}
</style>
