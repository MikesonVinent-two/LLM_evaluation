<template>
  <div class="batch-evaluation">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <h2>批次评测</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshBatchList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 批次列表 -->
    <el-card class="batch-list">
      <el-table :data="batches" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="批次ID" width="100" />
        <el-table-column prop="name" label="批次名称" />
        <el-table-column prop="totalAnswers" label="总答案数" width="120" />
        <el-table-column prop="evaluatedCount" label="已评测数" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button
                type="primary"
                @click="evaluateObjectiveQuestions(row)"
                :disabled="!canEvaluateObjective(row)"
              >
                客观题评测
              </el-button>
              <el-button
                type="success"
                @click="evaluateSubjectiveQuestions(row)"
                :disabled="!canEvaluateSubjective(row)"
              >
                主观题评测
              </el-button>
              <el-button
                type="warning"
                @click="startHumanEvaluation(row)"
                :disabled="!canStartHumanEvaluation(row)"
              >
                人工评测
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 人工评测对话框 -->
    <el-dialog
      v-model="humanEvaluationDialog.visible"
      title="人工评测"
      width="80%"
      destroy-on-close
    >
      <div class="human-evaluation-content">
        <div class="answer-info">
          <h3>问题内容</h3>
          <p>{{ humanEvaluationDialog.currentQuestion?.content }}</p>
          <h3>答案内容</h3>
          <p>{{ humanEvaluationDialog.currentAnswer?.content }}</p>
        </div>

        <div class="evaluation-form">
          <el-form ref="evaluationForm" :model="evaluationForm" label-width="120px">
            <el-form-item label="总体评分" required>
              <el-slider
                v-model="evaluationForm.overallScore"
                :min="0"
                :max="100"
                :step="1"
                show-input
              />
            </el-form-item>

            <el-form-item label="评语">
              <el-input
                v-model="evaluationForm.comments"
                type="textarea"
                :rows="4"
                placeholder="请输入评语"
              />
            </el-form-item>

            <el-form-item label="详细评分" required>
              <div v-for="(criterion, index) in evaluationCriteria" :key="index" class="criterion-item">
                <span class="criterion-name">{{ criterion.name }}</span>
                <el-slider
                  v-model="evaluationForm.detailScores[index].score"
                  :min="0"
                  :max="100"
                  :step="1"
                  show-input
                />
                <el-input
                  v-model="evaluationForm.detailScores[index].comments"
                  type="textarea"
                  :rows="2"
                  :placeholder="'请输入' + criterion.name + '的评语'"
                />
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="humanEvaluationDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="submitHumanEvaluation">
            提交评测
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  evaluateBatchObjectiveQuestions,
  evaluateBatchSubjective,
  submitHumanEvaluation,
  getBatchUnevaluatedAnswers
} from '@/api/evaluations'
import type { HumanEvaluationRequest } from '@/api/evaluations'

// 状态和数据
const loading = ref(false)
const batches = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 人工评测对话框状态
const humanEvaluationDialog = ref({
  visible: false,
  currentBatch: null,
  currentQuestion: null,
  currentAnswer: null
})

// 评分表单
const evaluationForm = ref({
  overallScore: 0,
  comments: '',
  detailScores: []
})

// 评分标准
const evaluationCriteria = [
  { name: '准确性', id: 1 },
  { name: '完整性', id: 2 },
  { name: '逻辑性', id: 3 },
  { name: '表达清晰度', id: 4 }
]

// 初始化评分表单的详细评分数组
const initEvaluationForm = () => {
  evaluationForm.value.detailScores = evaluationCriteria.map(criterion => ({
    criterionId: criterion.id,
    criterionName: criterion.name,
    score: 0,
    comments: ''
  }))
}

// 获取状态类型
const getStatusType = (status: string) => {
  const statusMap = {
    PENDING: '',
    EVALUATING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return statusMap[status] || ''
}

// 检查是否可以进行客观题评测
const canEvaluateObjective = (batch: any) => {
  return batch.status === 'PENDING' || batch.status === 'EVALUATING'
}

// 检查是否可以进行主观题评测
const canEvaluateSubjective = (batch: any) => {
  return batch.status === 'PENDING' || batch.status === 'EVALUATING'
}

// 检查是否可以开始人工评测
const canStartHumanEvaluation = (batch: any) => {
  return batch.status === 'PENDING' || batch.status === 'EVALUATING'
}

// 客观题评测
const evaluateObjectiveQuestions = async (batch: any) => {
  try {
    loading.value = true
    await evaluateBatchObjectiveQuestions(batch.id)
    ElMessage.success('客观题评测已启动')
    await refreshBatchList()
  } catch (error) {
    ElMessage.error('启动客观题评测失败')
    console.error('启动客观题评测失败:', error)
  } finally {
    loading.value = false
  }
}

// 主观题评测
const evaluateSubjectiveQuestions = async (batch: any) => {
  try {
    loading.value = true
    const userData = JSON.parse(localStorage.getItem('user') || '{}')
    await evaluateBatchSubjective({
      batchId: batch.id,
      evaluatorId: userData.evaluatorId,
      userId: userData.id
    })
    ElMessage.success('主观题评测已启动')
    await refreshBatchList()
  } catch (error) {
    ElMessage.error('启动主观题评测失败')
    console.error('启动主观题评测失败:', error)
  } finally {
    loading.value = false
  }
}

// 开始人工评测
const startHumanEvaluation = async (batch: any) => {
  try {
    loading.value = true
    const userData = JSON.parse(localStorage.getItem('user') || '{}')
    const response = await getBatchUnevaluatedAnswers(batch.id, {
      evaluatorId: userData.evaluatorId
    })

    if (response && response.content && response.content.length > 0) {
      const firstAnswer = response.content[0]
      humanEvaluationDialog.value = {
        visible: true,
        currentBatch: batch,
        currentQuestion: firstAnswer.question,
        currentAnswer: firstAnswer
      }
      initEvaluationForm()
    } else {
      ElMessage.info('该批次暂无需要评测的答案')
    }
  } catch (error) {
    ElMessage.error('获取待评测答案失败')
    console.error('获取待评测答案失败:', error)
  } finally {
    loading.value = false
  }
}

// 提交人工评测
const submitHumanEvaluation = async () => {
  try {
    loading.value = true
    const userData = JSON.parse(localStorage.getItem('user') || '{}')

    const evaluationData: HumanEvaluationRequest = {
      llmAnswerId: humanEvaluationDialog.value.currentAnswer.id,
      evaluatorId: userData.evaluatorId,
      overallScore: evaluationForm.value.overallScore,
      comments: evaluationForm.value.comments,
      detailScores: evaluationForm.value.detailScores,
      userId: userData.id
    }

    await submitHumanEvaluation(evaluationData)
    ElMessage.success('评测提交成功')
    humanEvaluationDialog.value.visible = false
    await refreshBatchList()
  } catch (error) {
    ElMessage.error('评测提交失败')
    console.error('评测提交失败:', error)
  } finally {
    loading.value = false
  }
}

// 刷新批次列表
const refreshBatchList = async () => {
  // TODO: 实现获取批次列表的API调用
  // const response = await getBatchList({
  //   page: currentPage.value - 1,
  //   size: pageSize.value
  // })
  // batches.value = response.content
  // total.value = response.totalElements
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  refreshBatchList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  refreshBatchList()
}

// 组件挂载时获取数据
onMounted(() => {
  refreshBatchList()
})
</script>

<style scoped>
.batch-evaluation {
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
  padding: 20px;
  background-color: #f5f7fa;
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
