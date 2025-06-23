<template>
  <div class="subjective-human-evaluation-page">
    <!-- 批次ID无效时的提示 -->
    <el-card v-if="!isValidBatchId" class="error-card">
      <el-result
        icon="error"
        title="无效的批次ID"
        sub-title="未找到有效的批次信息，请返回评测页面重新选择批次。"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回评测页面</el-button>
        </template>
      </el-result>
    </el-card>

    <template v-else>
      <el-card class="page-header">
        <template #header>
          <div class="card-header">
            <div class="title-section">
              <h2>主观题人工评测</h2>
              <el-tag class="batch-tag" type="info">批次: {{ batchName }}</el-tag>
            </div>
            <div class="header-actions">
              <el-button @click="goBack">
                <el-icon><Back /></el-icon>
                返回
              </el-button>
              <el-button @click="configureCriteria" :disabled="availableCriteria.length === 0">
                <el-icon><Setting /></el-icon>
                配置评测标准
              </el-button>
              <el-button type="primary" @click="currentAnswer ? handleNextQuestion() : handleStartEvaluation()" :loading="loading" :disabled="!criteriaConfigured">
                <el-icon><RefreshRight /></el-icon>
                {{ currentAnswer ? '下一题' : '开始评测' }}
              </el-button>
            </div>
          </div>
        </template>

        <!-- 评测进度统计 -->
        <el-row class="evaluation-progress" :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover" class="progress-card">
              <template #header>
                <div class="progress-header">
                  <el-icon><Histogram /></el-icon>
                  <span>评测进度</span>
                </div>
              </template>
              <el-progress
                :percentage="evaluationProgress"
                :format="progressFormat"
                :status="evaluationProgress === 100 ? 'success' : ''"
                :stroke-width="18"
              />
              <div class="progress-stats">
                <div class="stat-item">
                  <span class="stat-label">已评测:</span>
                  <span class="stat-value">{{ evaluationStats.completed }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">未评测:</span>
                  <span class="stat-value">{{ evaluationStats.pending }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">总数量:</span>
                  <span class="stat-value">{{ evaluationStats.total }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="hover" class="progress-card">
              <template #header>
                <div class="progress-header">
                  <el-icon><DataAnalysis /></el-icon>
                  <span>评分统计</span>
                </div>
              </template>
              <div class="score-stats">
                <div class="stat-item">
                  <span class="stat-label">平均分:</span>
                  <span class="stat-value">{{ (evaluationStats.averageScore || 0).toFixed(1) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">最高分:</span>
                  <span class="stat-value">{{ evaluationStats.maxScore }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">最低分:</span>
                  <span class="stat-value">{{ evaluationStats.minScore }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 评测员身份检查提示 -->
        <el-alert
          v-if="!userStore.getCurrentUser()?.isEvaluator || !userStore.getCurrentUser()?.evaluatorId"
          type="error"
          :closable="false"
          show-icon
          style="margin: 20px 0;"
        >
          <template #title>
            无评测员权限
          </template>
          <p>当前用户不是评测员，无法进行人工评测。</p>
          <p>请联系管理员将您设置为评测员，或使用具有评测员权限的账户登录。</p>
        </el-alert>

        <!-- 评测标准配置提示 -->
        <el-alert
          v-else-if="!criteriaConfigured"
          type="warning"
          :closable="false"
          show-icon
          style="margin: 20px 0;"
        >
          <template #title>
            请先配置评测标准
          </template>
          <p>在开始评测前，请点击"配置评测标准"按钮选择要使用的评测标准。</p>
          <p>已加载 {{ availableCriteria.length }} 个可用评测标准。</p>
        </el-alert>

        <!-- 已配置标准显示 -->
        <el-card v-if="criteriaConfigured" style="margin: 20px 0;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>当前评测标准</span>
              <el-button size="small" @click="configureCriteria">重新配置</el-button>
            </div>
          </template>
          <el-tag
            v-for="criterionId in selectedCriteriaIds"
            :key="criterionId"
            style="margin-right: 8px; margin-bottom: 8px;"
            type="success"
          >
            {{ availableCriteria.find(c => c.id === criterionId)?.name }}
            (最高{{ availableCriteria.find(c => c.id === criterionId)?.maxScore }}分)
          </el-tag>
        </el-card>

        <!-- 评测内容 -->
        <div v-if="currentAnswer && criteriaConfigured" class="evaluation-content">
          <el-card class="answer-card" shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 600;">题目 {{ currentAnswerIndex + 1 }}</span>
                <el-tag type="info" size="small">{{ getQuestionTypeText(currentQuestion?.questionType) }}</el-tag>
              </div>
            </template>

            <div class="question-section">
              <h4>问题</h4>
              <p class="question-text">{{ currentQuestion?.questionText }}</p>
            </div>

            <div class="answer-section">
              <h4>模型回答</h4>
              <div class="answer-text">{{ currentAnswer.answer?.answerText }}</div>
            </div>

            <div class="reference-section" v-if="currentAnswer.standardAnswer">
              <h4>参考答案</h4>
              <div class="reference-text">{{ currentAnswer.standardAnswer.answerText }}</div>
            </div>
            <div class="reference-section" v-else>
              <h4>参考答案</h4>
              <div class="no-reference">暂无参考答案</div>
            </div>
          </el-card>

          <el-divider>评分</el-divider>

          <div class="evaluation-form">
            <el-form :model="evaluationForm" label-width="100px" class="compact-form">
              <el-form-item label="总体评分">
                <div style="display: flex; align-items: center; gap: 12px;">
                  <el-input-number
                    v-model="evaluationForm.overallScore"
                    :min="0"
                    :max="100"
                    :step="1"
                    style="width: 120px;"
                  />
                  <span style="color: #909399;">/ 100分</span>
                  <el-tag
                    :type="getScoreTagType(evaluationForm.overallScore)"
                    size="small"
                  >
                    {{ getScoreLevel(evaluationForm.overallScore) }}
                  </el-tag>
                </div>
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
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                  <span class="criterion-name">{{ item.criterionName }}</span>
                  <el-tag size="small" type="info">最高{{ item.maxScore }}分</el-tag>
                </div>

                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
                  <el-input-number
                    v-model="item.score"
                    :min="0"
                    :max="item.maxScore"
                    :step="1"
                    style="width: 120px;"
                  />
                  <span style="color: #909399;">/ {{ item.maxScore }}分</span>
                  <el-tag
                    :type="getScoreTagType(item.score, item.maxScore)"
                    size="small"
                  >
                    {{ getScoreLevel(item.score, item.maxScore) }}
                  </el-tag>
                </div>

                <el-input
                  v-model="item.comments"
                  type="textarea"
                  :rows="2"
                  :placeholder="`${item.criterionName}评语`"
                />
              </div>

              <div class="form-actions">
                <el-button type="primary" @click="submitEvaluation" :loading="submitting">
                  提交评测
                </el-button>
              </div>
            </el-form>
          </div>
        </div>

        <!-- 无题目提示 -->
        <div v-else-if="noMoreAnswers" class="no-answers">
          <el-empty description="该批次暂无需要评测的答案" />
        </div>

        <!-- 加载中 -->
        <div v-else-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>

        <!-- 已评测答案列表 -->
        <el-divider>已评测答案</el-divider>
        <div class="evaluated-answers">
          <el-table
            v-loading="loadingEvaluated"
            :data="evaluatedAnswers"
            stripe
            style="width: 100%"
            max-height="400"
          >
            <el-table-column prop="questionText" label="问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="answerText" label="回答" min-width="200" show-overflow-tooltip />
            <el-table-column prop="score" label="评分" width="100">
              <template #default="scope">
                <el-tag :type="getScoreTagType(scope.row.score)">
                  {{ scope.row.score }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="comments" label="评语" min-width="150" show-overflow-tooltip />
            <el-table-column prop="evaluationTime" label="评测时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.evaluationTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button type="primary" link @click="viewEvaluationDetail(scope.row)">
                  查看详情
                </el-button>
                <el-button type="warning" link @click="editEvaluation(scope.row)">
                  修改评测
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="evaluatedAnswersPage"
              v-model:page-size="evaluatedAnswersPageSize"
              :page-sizes="[5, 10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="evaluatedAnswersTotal"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </el-card>

      <!-- 评测详情对话框 -->
      <el-dialog
        v-model="evaluationDetailDialog.visible"
        title="评测详情"
        width="60%"
        destroy-on-close
      >
        <div v-if="evaluationDetailDialog.data" class="evaluation-detail">
          <div class="detail-section">
            <h3>问题</h3>
            <p>{{ evaluationDetailDialog.data.questionText }}</p>
          </div>
          <div class="detail-section">
            <h3>模型回答</h3>
            <p>{{ evaluationDetailDialog.data.answerText }}</p>
          </div>
          <div class="detail-section">
            <h3>评分: {{ evaluationDetailDialog.data.score }}</h3>
            <p>{{ evaluationDetailDialog.data.comments }}</p>
          </div>
          <div class="detail-section">
            <h3>详细评分</h3>
            <el-table :data="evaluationDetailDialog.data.evaluationDetails || []" border>
              <el-table-column prop="criterionName" label="评分项" />
              <el-table-column prop="score" label="分数" width="100" />
              <el-table-column prop="comments" label="评语" min-width="200" show-overflow-tooltip />
            </el-table>
          </div>
        </div>
      </el-dialog>

      <!-- 修改评测对话框 -->
      <el-dialog
        v-model="editEvaluationDialog.visible"
        title="修改评测结果"
        width="80%"
        :close-on-click-modal="false"
        destroy-on-close
      >
        <div v-if="editEvaluationDialog.data" class="edit-evaluation-content">
          <!-- 题目信息 -->
          <el-card class="question-card" shadow="never" style="margin-bottom: 20px;">
            <template #header>
              <span style="font-weight: 600;">题目信息</span>
            </template>

            <div class="question-section">
              <h4>问题</h4>
              <p class="question-text">{{ editEvaluationDialog.data.questionText }}</p>
            </div>

            <div class="answer-section">
              <h4>模型回答</h4>
              <div class="answer-text">{{ editEvaluationDialog.data.answerText }}</div>
            </div>
          </el-card>

          <!-- 评分表单 -->
          <el-card class="evaluation-form-card" shadow="never">
            <template #header>
              <span style="font-weight: 600;">重新评分</span>
            </template>

            <el-form :model="editEvaluationDialog.form" label-width="100px" class="compact-form">
              <el-form-item label="总体评分">
                <div style="display: flex; align-items: center; gap: 12px;">
                  <el-input-number
                    v-model="editEvaluationDialog.form.overallScore"
                    :min="0"
                    :max="100"
                    :step="1"
                    style="width: 120px;"
                  />
                  <span style="color: #909399;">/ 100分</span>
                  <el-tag
                    :type="getScoreTagType(editEvaluationDialog.form.overallScore)"
                    size="small"
                  >
                    {{ getScoreLevel(editEvaluationDialog.form.overallScore) }}
                  </el-tag>
                </div>
              </el-form-item>

              <el-form-item label="评语">
                <el-input
                  v-model="editEvaluationDialog.form.comments"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入评语"
                />
              </el-form-item>

              <el-divider>详细评分项</el-divider>

              <div v-for="(item, index) in editEvaluationDialog.form.detailScores" :key="index" class="criterion-item">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                  <span class="criterion-name">{{ item.criterionName }}</span>
                  <el-tag size="small" type="info">最高{{ item.maxScore }}分</el-tag>
                </div>

                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
                  <el-input-number
                    v-model="item.score"
                    :min="0"
                    :max="item.maxScore"
                    :step="1"
                    style="width: 120px;"
                  />
                  <span style="color: #909399;">/ {{ item.maxScore }}分</span>
                  <el-tag
                    :type="getScoreTagType(item.score, item.maxScore)"
                    size="small"
                  >
                    {{ getScoreLevel(item.score, item.maxScore) }}
                  </el-tag>
                </div>

                <el-input
                  v-model="item.comments"
                  type="textarea"
                  :rows="2"
                  :placeholder="`${item.criterionName}评语`"
                />
              </div>
            </el-form>
          </el-card>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="editEvaluationDialog.visible = false">取消</el-button>
            <el-button type="primary" @click="submitEditedEvaluation" :loading="editEvaluationDialog.submitting">
              保存修改
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 评测标准配置对话框 -->
      <el-dialog
        v-model="criteriaDialogVisible"
        title="配置评测标准"
        width="70%"
        :close-on-click-modal="false"
      >
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        >
          <p>请选择要使用的评测标准。每个标准都有不同的最大分值，请根据实际需要选择。</p>
          <p>建议选择3-5个标准进行综合评测。</p>
        </el-alert>

        <el-checkbox-group v-model="selectedCriteriaIds">
          <div class="criteria-grid">
            <el-card
              v-for="criterion in availableCriteria"
              :key="criterion.id"
              class="criterion-card"
              shadow="hover"
            >
              <template #header>
                <el-checkbox :label="criterion.id" style="width: 100%;">
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <strong>{{ criterion.name }}</strong>
                    <el-tag size="small" type="primary">{{ criterion.maxScore }}分</el-tag>
                  </div>
                </el-checkbox>
              </template>
              <div class="criterion-info">
                <p><strong>描述：</strong>{{ criterion.description }}</p>
                <p><strong>数据类型：</strong>{{ criterion.dataType }}</p>
                <p><strong>分数范围：</strong>{{ criterion.scoreRange }}</p>
                <p v-if="criterion.questionType"><strong>适用题型：</strong>{{ criterion.questionType }}</p>
              </div>
            </el-card>
          </div>
        </el-checkbox-group>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="criteriaDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmCriteriaSelection">
              确认选择 ({{ selectedCriteriaIds.length }})
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, RefreshRight, Histogram, DataAnalysis, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  submitHumanEvaluation as submitHumanEval,
  getBatchUnevaluatedAnswers,
  getCompletedHumanEvaluations,
  type HumanEvaluationRequest,
  type UnevaluatedAnswer,
  type StandardQuestion
} from '@/api/evaluations'
import { getAllEvaluationCriteria, type EvaluationCriterion } from '@/api/evaluationCriteria'

// 路由
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 批次信息
const batchId = computed(() => route.params.batchId as string)
const batchName = computed(() => route.query.batchName as string || '未命名批次')

// 检查批次ID是否有效
const isValidBatchId = computed(() => {
  const id = batchId.value
  return !!(id && id.trim() && id !== 'undefined' && id !== 'null' && id !== '' && !isNaN(Number(id)))
})

// 状态
const loading = ref(false)
const submitting = ref(false)
const noMoreAnswers = ref(false)
const loadingEvaluated = ref(false)
const currentAnswerIndex = ref(0) // 当前答案索引，用于分页

// 当前评测数据
const currentQuestion = ref<StandardQuestion | null>(null)
const currentAnswer = ref<UnevaluatedAnswer | null>(null)

// 评测标准相关
const availableCriteria = ref<EvaluationCriterion[]>([])
const selectedCriteriaIds = ref<number[]>([])
const criteriaConfigured = ref(false)
const criteriaDialogVisible = ref(false)

// 评分表单
const evaluationForm = reactive({
  overallScore: 70,
  comments: '',
  detailScores: [] as Array<{
    criterionId: number
    criterionName: string
    maxScore: number
    score: number
    comments: string
  }>
})

// 评测统计
const evaluationStats = reactive({
  completed: 0,
  pending: 0,
  total: 0,
  averageScore: 0,
  maxScore: 0,
  minScore: 0
})

// 已评测答案分页
const evaluatedAnswers = ref<any[]>([])
const evaluatedAnswersPage = ref(1)
const evaluatedAnswersPageSize = ref(10)
const evaluatedAnswersTotal = ref(0)

// 评测详情对话框
const evaluationDetailDialog = reactive({
  visible: false,
  data: null as any
})

// 修改评测对话框
const editEvaluationDialog = reactive({
  visible: false,
  data: null as any,
  form: {
    overallScore: 70,
    comments: '',
    detailScores: [] as Array<{
      criterionId: number
      criterionName: string
      maxScore: number
      score: number
      comments: string
    }>
  },
  submitting: false
})

// 计算评测进度百分比
const evaluationProgress = computed(() => {
  if (evaluationStats.total === 0) return 0
  return Math.round((evaluationStats.completed / evaluationStats.total) * 100)
})

// 格式化进度显示
const progressFormat = (percentage: number) => {
  return `${percentage}%`
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

// 根据分数获取标签类型
const getScoreTagType = (score: number, maxScore = 100) => {
  const percentage = (score / maxScore) * 100
  if (percentage >= 90) return 'success'
  if (percentage >= 80) return 'primary'
  if (percentage >= 60) return 'warning'
  return 'danger'
}

// 获取评分等级
const getScoreLevel = (score: number, maxScore = 100) => {
  const percentage = (score / maxScore) * 100
  if (percentage >= 90) return '优秀'
  if (percentage >= 80) return '良好'
  if (percentage >= 60) return '及格'
  return '不及格'
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()
}



// 加载评测标准
const loadEvaluationCriteria = async () => {
  try {
    const criteria = await getAllEvaluationCriteria()
    console.log('获取到的评测标准:', criteria)

    // 过滤主观题相关的评测标准，如果没有questionType字段则显示所有
    if (criteria.length > 0) {
      if (criteria[0].hasOwnProperty('questionType')) {
        availableCriteria.value = criteria.filter(criterion =>
          criterion.questionType === 'SUBJECTIVE' || !criterion.questionType
        )
      } else {
        availableCriteria.value = criteria
      }

      console.log('可用的评测标准:', availableCriteria.value)
    }
  } catch (error) {
    console.error('加载评测标准失败:', error)
    ElMessage.error('加载评测标准失败')
  }
}

// 配置评测标准
const configureCriteria = () => {
  criteriaDialogVisible.value = true
}

// 确认选择评测标准
const confirmCriteriaSelection = () => {
  if (selectedCriteriaIds.value.length === 0) {
    ElMessage.warning('请至少选择一个评测标准')
    return
  }

  // 根据选择的标准初始化评分表单
  initEvaluationFormWithCriteria()
  criteriaConfigured.value = true
  criteriaDialogVisible.value = false

  ElMessage.success(`已选择 ${selectedCriteriaIds.value.length} 个评测标准`)
}

// 根据选择的评测标准初始化评分表单
const initEvaluationFormWithCriteria = () => {
  evaluationForm.detailScores = selectedCriteriaIds.value.map(criterionId => {
    const criterion = availableCriteria.value.find(c => c.id === criterionId)
    const defaultScore = Math.floor(criterion?.maxScore ? criterion.maxScore * 0.7 : 70)

    return {
      criterionId: criterionId,
      criterionName: criterion?.name || '未知标准',
      maxScore: criterion?.maxScore || 100,
      score: defaultScore,
      comments: ''
    }
  })
}

// 初始化评分表单
const initEvaluationForm = () => {
  evaluationForm.overallScore = 70
  evaluationForm.comments = ''
  evaluationForm.detailScores.forEach(item => {
    const defaultScore = Math.floor(item.maxScore * 0.7)
    item.score = defaultScore
    item.comments = ''
  })
}

// 返回上一页
const goBack = () => {
  router.push({ name: 'Evaluations' })
}

// 加载下一个待评测答案
const loadNextAnswer = async (resetIndex = false) => {
  try {
    loading.value = true
    noMoreAnswers.value = false

    const userData = userStore.getCurrentUser()

    // 检查用户是否为评测员
    if (!userData?.isEvaluator || !userData?.evaluatorId) {
      ElMessage.error('当前用户不是评测员，无法获取待评测答案')
      noMoreAnswers.value = true
      return
    }

    // 如果是重置索引（比如首次加载），则从0开始
    if (resetIndex) {
      currentAnswerIndex.value = 0
    }

    console.log(`正在加载第 ${currentAnswerIndex.value + 1} 个待评测答案，page=${currentAnswerIndex.value}`)

    const response = await getBatchUnevaluatedAnswers(batchId.value, {
      evaluatorId: userData.evaluatorId,
      page: currentAnswerIndex.value,
      size: 1
    })

    console.log('API响应:', response)

    if (response.unevaluatedAnswers && response.unevaluatedAnswers.length > 0) {
      const firstAnswer = response.unevaluatedAnswers[0]
      currentQuestion.value = firstAnswer.standardQuestion
      currentAnswer.value = firstAnswer
      initEvaluationForm()

      console.log(`✅ 成功加载第 ${currentAnswerIndex.value + 1} 个待评测答案`)
    } else {
      currentQuestion.value = null
      currentAnswer.value = null
      noMoreAnswers.value = true
      ElMessage.info('该批次暂无需要评测的答案')
      console.log('❌ 没有更多待评测答案')
    }
  } catch (error) {
    console.error('获取待评测答案失败:', error)
    ElMessage.error('获取待评测答案失败')
  } finally {
    loading.value = false
  }
}

// 处理"下一题"按钮点击
const handleNextQuestion = () => {
  currentAnswerIndex.value++
  console.log(`点击下一题，索引递增到: ${currentAnswerIndex.value}`)
  loadNextAnswer()
}

// 处理"开始评测"按钮点击
const handleStartEvaluation = () => {
  console.log('开始评测，重置索引到0')
  loadNextAnswer(true)
}

// 提交评测
const submitEvaluation = async () => {
  if (!currentAnswer.value || !currentAnswer.value.answer) {
    ElMessage.error('评测数据不完整')
    return
  }

  try {
    submitting.value = true
    const userData = userStore.getCurrentUser()

    console.log('当前用户数据:', userData)
    console.log('用户是否为评测员:', userData?.isEvaluator)
    console.log('评测员ID:', userData?.evaluatorId)

    // 检查用户是否为评测员
    if (!userData?.isEvaluator || !userData?.evaluatorId) {
      ElMessage.error('当前用户不是评测员，无法提交评测结果')
      return
    }

    const evaluationData: HumanEvaluationRequest = {
      llmAnswerId: currentAnswer.value.answer.id,
      evaluatorId: userData.evaluatorId, // 使用evaluatorId而不是userId
      overallScore: evaluationForm.overallScore,
      comments: evaluationForm.comments,
      detailScores: evaluationForm.detailScores.map(item => ({
        criterionId: item.criterionId,
        criterion: item.criterionName,
        score: item.score,
        maxScore: item.maxScore,
        comments: item.comments
      })),
      userId: userData.id // userId用于记录操作者
    }

    console.log('提交的评测数据:', evaluationData)

    await submitHumanEval(evaluationData)
    ElMessage.success('评测提交成功')

    // 更新评测统计和已评测答案列表
    if (isValidBatchId.value) {
      await loadEvaluationStats()
      await loadEvaluatedAnswers()
    }

    // 自动加载下一个答案
    console.log('评测提交成功，自动加载下一题')
    handleNextQuestion()
  } catch (error) {
    console.error('评测提交失败:', error)
    ElMessage.error('评测提交失败')
  } finally {
    submitting.value = false
  }
}



// 加载评测统计
const loadEvaluationStats = async () => {
  // 验证batchId是否有效
  if (!isValidBatchId.value) {
    console.warn('batchId无效，跳过加载评测统计')
    return
  }

  try {
    const userData = userStore.getCurrentUser()

    // 检查用户是否为评测员
    if (!userData?.isEvaluator || !userData?.evaluatorId) {
      console.warn('当前用户不是评测员，跳过加载评测统计')
      return
    }

    // 获取已评测答案数量
    const completedResponse = await getCompletedHumanEvaluations({
      batchId: batchId.value,
      evaluatorId: userData.evaluatorId,
      userId: userData.id,
      page: 0,
      size: 10
    })

    // 获取未评测答案数量
    const pendingResponse = await getBatchUnevaluatedAnswers(batchId.value, {
      evaluatorId: userData.evaluatorId,
      page: 0,
      size: 10
    })

    // 更新统计数据
    evaluationStats.completed = completedResponse.totalItems || 0
    evaluationStats.pending = pendingResponse.totalElements || 0
    evaluationStats.total = evaluationStats.completed + evaluationStats.pending

    // 计算平均分、最高分和最低分
    if (completedResponse.items && completedResponse.items.length > 0) {
      // 如果有评测数据，计算统计信息
      const scores = completedResponse.items.map(item => item.score)
      evaluationStats.averageScore = scores.reduce((a, b) => a + b, 0) / scores.length
      evaluationStats.maxScore = Math.max(...scores)
      evaluationStats.minScore = Math.min(...scores)
    } else {
      // 无评测数据时的默认值
      evaluationStats.averageScore = 0
      evaluationStats.maxScore = 0
      evaluationStats.minScore = 0
    }
  } catch (error) {
    console.error('加载评测统计失败:', error)
  }
}

// 加载已评测答案
const loadEvaluatedAnswers = async () => {
  // 验证batchId是否有效
  if (!isValidBatchId.value) {
    console.warn('batchId无效，跳过加载已评测答案')
    return
  }

  try {
    loadingEvaluated.value = true
    const userData = userStore.getCurrentUser()

    // 检查用户是否为评测员
    if (!userData?.isEvaluator || !userData?.evaluatorId) {
      console.warn('当前用户不是评测员，跳过加载已评测答案')
      return
    }

    const response = await getCompletedHumanEvaluations({
      batchId: batchId.value,
      evaluatorId: userData.evaluatorId,
      userId: userData.id,
      page: evaluatedAnswersPage.value - 1,
      size: evaluatedAnswersPageSize.value
    })

    evaluatedAnswers.value = response.items || []
    evaluatedAnswersTotal.value = response.totalItems || 0
  } catch (error) {
    console.error('加载已评测答案失败:', error)
    ElMessage.error('加载已评测答案失败')
  } finally {
    loadingEvaluated.value = false
  }
}

// 查看评测详情
const viewEvaluationDetail = (row: any) => {
  evaluationDetailDialog.data = row
  evaluationDetailDialog.visible = true
}

// 修改评测
const editEvaluation = (row: any) => {
  console.log('修改评测数据:', row)

  // 设置对话框数据
  editEvaluationDialog.data = row

  // 初始化表单数据
  editEvaluationDialog.form.overallScore = row.score || 70
  editEvaluationDialog.form.comments = row.comments || ''

  // 初始化详细评分项
  if (row.evaluationDetails && row.evaluationDetails.length > 0) {
    editEvaluationDialog.form.detailScores = row.evaluationDetails.map((detail: any) => ({
      criterionId: detail.criterionId || 0,
      criterionName: detail.criterionName || detail.criterion || '',
      maxScore: detail.maxScore || 100,
      score: detail.score || 0,
      comments: detail.comments || ''
    }))
  } else {
    // 如果没有详细评分项，使用当前配置的评测标准
    editEvaluationDialog.form.detailScores = selectedCriteriaIds.value.map(criterionId => {
      const criterion = availableCriteria.value.find(c => c.id === criterionId)
      return {
        criterionId: criterionId,
        criterionName: criterion?.name || '',
        maxScore: criterion?.maxScore || 100,
        score: 70,
        comments: ''
      }
    })
  }

  editEvaluationDialog.visible = true
}

// 提交修改的评测结果
const submitEditedEvaluation = async () => {
  if (!editEvaluationDialog.data) {
    ElMessage.error('评测数据不完整')
    return
  }

  try {
    editEvaluationDialog.submitting = true
    const userData = userStore.getCurrentUser()

    console.log('修改评测 - 当前用户数据:', userData)
    console.log('修改评测 - 用户是否为评测员:', userData?.isEvaluator)
    console.log('修改评测 - 评测员ID:', userData?.evaluatorId)

    // 检查用户是否为评测员
    if (!userData?.isEvaluator || !userData?.evaluatorId) {
      ElMessage.error('当前用户不是评测员，无法修改评测结果')
      return
    }

    // 构造评测数据，使用原有的llmAnswerId
    const evaluationData: HumanEvaluationRequest = {
      llmAnswerId: editEvaluationDialog.data.llmAnswerId || editEvaluationDialog.data.id,
      evaluatorId: userData.evaluatorId,
      overallScore: editEvaluationDialog.form.overallScore,
      comments: editEvaluationDialog.form.comments,
      detailScores: editEvaluationDialog.form.detailScores.map(item => ({
        criterionId: item.criterionId,
        criterion: item.criterionName,
        score: item.score,
        maxScore: item.maxScore,
        comments: item.comments
      })),
      userId: userData.id
    }

    console.log('提交修改的评测数据:', evaluationData)

    // 调用提交评测接口，后端会覆盖原有结果
    await submitHumanEval(evaluationData)
    ElMessage.success('评测结果修改成功')

    // 关闭对话框
    editEvaluationDialog.visible = false

    // 刷新已评测答案列表和统计数据
    if (isValidBatchId.value) {
      await loadEvaluationStats()
      await loadEvaluatedAnswers()
    }
  } catch (error) {
    console.error('修改评测结果失败:', error)
    ElMessage.error('修改评测结果失败')
  } finally {
    editEvaluationDialog.submitting = false
  }
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  evaluatedAnswersPageSize.value = size
  if (isValidBatchId.value) {
    loadEvaluatedAnswers()
  }
}

// 当前页变化
const handleCurrentChange = (page: number) => {
  evaluatedAnswersPage.value = page
  if (isValidBatchId.value) {
    loadEvaluatedAnswers()
  }
}

// 初始化
onMounted(async () => {
  // 等待路由参数完全加载
  await nextTick()

  console.log('主观题人工评测页面初始化 - batchId:', batchId.value, 'isValid:', isValidBatchId.value)

  // 检查用户是否为评测员
  const userData = userStore.getCurrentUser()
  console.log('当前用户数据:', userData)

  if (!userData?.isEvaluator || !userData?.evaluatorId) {
    ElMessage.warning('当前用户不是评测员，无法进行人工评测')
    console.warn('用户不是评测员，isEvaluator:', userData?.isEvaluator, 'evaluatorId:', userData?.evaluatorId)
  }

  // 加载评测标准（无论批次ID是否有效都需要加载）
  await loadEvaluationCriteria()

  // 只有在批次ID有效且用户是评测员时才加载评测统计和已评测答案
  if (isValidBatchId.value && batchId.value && batchId.value !== 'undefined' && batchId.value !== 'null') {
    console.log('batchId有效，开始加载评测数据')
    if (userData?.isEvaluator && userData?.evaluatorId) {
      loadEvaluationStats()
      loadEvaluatedAnswers()
    } else {
      console.log('用户不是评测员，跳过加载评测数据')
    }
  } else {
    console.log('batchId无效，跳过加载评测数据')
  }
})

// 定义组件名称
defineOptions({
  name: 'SubjectiveHumanEvaluationPage'
})
</script>

<style scoped>
.subjective-human-evaluation-page {
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

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-tag {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.evaluation-progress {
  margin-bottom: 20px;
}

.progress-card {
  height: 100%;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.progress-stats, .score-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 15px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  display: block;
  margin-top: 5px;
}

.evaluation-content {
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

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.no-answers {
  padding: 40px 0;
  text-align: center;
}

.loading-container {
  padding: 20px;
}

.evaluated-answers {
  margin-top: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.evaluation-detail {
  padding: 10px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h3 {
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid #ebeef5;
}

.error-card {
  margin: 100px auto;
  max-width: 600px;
}

.criteria-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.criterion-card {
  height: fit-content;
}

.criterion-info {
  font-size: 14px;
  line-height: 1.6;
}

.criterion-info p {
  margin: 8px 0;
  color: #606266;
}

.criterion-info strong {
  color: #303133;
}

.compact-form .el-form-item {
  margin-bottom: 20px;
}

.criterion-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e9ecef;
}

.criterion-name {
  font-weight: 600;
  color: #495057;
  font-size: 15px;
}

/* 修改评测对话框样式 */
.edit-evaluation-content {
  max-height: 70vh;
  overflow-y: auto;
}

.edit-evaluation-content .question-card,
.edit-evaluation-content .evaluation-form-card {
  border: 1px solid #e4e7ed;
}

.edit-evaluation-content .question-section,
.edit-evaluation-content .answer-section {
  margin-bottom: 16px;
}

.edit-evaluation-content .question-section h4,
.edit-evaluation-content .answer-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.edit-evaluation-content .question-text,
.edit-evaluation-content .answer-text {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
  color: #303133;
}

.edit-evaluation-content .criterion-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e9ecef;
}

.edit-evaluation-content .criterion-name {
  font-weight: 600;
  color: #495057;
  font-size: 15px;
}

.evaluation-form {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e9ecef;
}

.answer-card {
  margin-bottom: 20px;
  border: 1px solid #e9ecef;
}

.question-section, .answer-section, .reference-section {
  margin-bottom: 20px;
}

.question-section h4, .answer-section h4, .reference-section h4 {
  margin: 0 0 8px 0;
  color: #495057;
  font-size: 14px;
  font-weight: 600;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  color: #212529;
  margin: 0;
  font-weight: 500;
}

.answer-text, .reference-text {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  line-height: 1.6;
  color: #495057;
  border-left: 3px solid #007bff;
}

.reference-text {
  border-left-color: #28a745;
}

.no-reference {
  color: #6c757d;
  font-style: italic;
}
</style>
