<template>
  <div class="subjective-llm-evaluation-page">
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
              <h2>主观题大模型评测</h2>
              <el-tag class="batch-tag" type="info">批次: {{ batchName }}</el-tag>
            </div>
            <div class="header-actions">
              <el-button @click="goBack">
                <el-icon><Back /></el-icon>
                返回
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
                :percentage="evaluationStats.progress"
                :format="progressFormat"
                :status="evaluationStats.progress === 100 ? 'success' : ''"
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

        <!-- 模型选择和评测设置 -->
        <div v-if="!evaluating && !evaluationCompleted" class="model-selection">
          <h3>选择评测模型</h3>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 20px;"
          >
            <p>请选择评测模型和评测配置。系统将使用选定的模型对主观题答案进行自动评分。</p>
          </el-alert>

          <div class="model-selection-form">
            <el-form :model="evaluationForm" label-width="120px">
              <el-form-item label="评测模型" required>
                <el-select v-model="evaluationForm.modelId" placeholder="请选择评测模型" style="width: 100%">
                  <el-option
                    v-for="model in availableModels"
                    :key="model.id"
                    :label="model.name"
                    :value="model.id"
                  >
                    <div class="model-option">
                      <span>{{ model.name }}</span>
                      <el-tag size="small" type="info" v-if="model.aiEvaluator">AI模型</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="评测配置">
                <el-radio-group v-model="evaluationForm.useCustomPrompt">
                  <el-radio :label="false">使用系统评测配置</el-radio>
                  <el-radio :label="true" disabled>自定义提示词（不可用）</el-radio>
                </el-radio-group>
              </el-form-item>

              <template v-if="!evaluationForm.useCustomPrompt">
                <el-form-item label="提示词模板" required>
                  <el-select v-model="evaluationForm.subjectivePromptId" placeholder="请选择提示词模板" style="width: 100%">
                    <el-option
                      v-for="prompt in availablePrompts"
                      :key="prompt.id"
                      :label="prompt.name"
                      :value="prompt.id"
                    >
                      <div class="prompt-option">
                        <span>{{ prompt.name }}</span>
                        <el-tooltip :content="prompt.description" placement="top">
                          <el-icon><InfoFilled /></el-icon>
                        </el-tooltip>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>

                <el-form-item label="评测组装配置" required>
                  <el-select v-model="evaluationForm.evaluationAssemblyConfigId" placeholder="请选择评测组装配置" style="width: 100%">
                    <el-option
                      v-for="config in availableConfigs"
                      :key="config.id"
                      :label="config.name"
                      :value="config.id"
                    >
                      <div class="config-option">
                        <span>{{ config.name }}</span>
                        <el-tooltip :content="config.description" placement="top">
                          <el-icon><InfoFilled /></el-icon>
                        </el-tooltip>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>

                <el-form-item label="评测标准" required>
                  <el-select v-model="evaluationForm.criteriaIds" multiple placeholder="请选择评测标准" style="width: 100%">
                    <el-option
                      v-for="criterion in availableCriteria"
                      :key="criterion.id"
                      :label="criterion.name"
                      :value="criterion.id"
                    >
                      <div class="criterion-option">
                        <span>{{ criterion.name }}</span>
                        <el-tag size="small" :type="criterion.questionType ? getQuestionTypeTagType(criterion.questionType) : 'info'">
                          {{ criterion.questionType ? getQuestionTypeDisplay(criterion.questionType) : (criterion.dataType || '评分') }}
                        </el-tag>
                        <el-tag size="small" type="warning" v-if="criterion.scoreRange">
                          {{ criterion.scoreRange }}
                        </el-tag>
                        <el-tooltip :content="criterion.description" placement="top">
                          <el-icon><InfoFilled /></el-icon>
                        </el-tooltip>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
              </template>

              <el-form-item>
                <el-button type="primary" @click="startEvaluation" :loading="submitting">
                  <el-icon><VideoPlay /></el-icon>
                  开始评测
                </el-button>
                <el-button type="info" @click="testSelectedModel" :loading="testing">
                  <el-icon><Connection /></el-icon>
                  测试模型连通性
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 评测状态 -->
        <div v-if="evaluating" class="evaluation-status">
          <el-alert
            title="评测进行中..."
            type="success"
            :closable="false"
            show-icon
          >
            <template #description>
              <div class="status-description">
                <p>系统正在使用大模型对主观题进行评测，请耐心等待。</p>
                <p>评测模型: {{ selectedModelName }}</p>
                <el-progress :percentage="evaluationProgress" :status="evaluationStatus"></el-progress>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 评测结果提示 -->
        <div v-if="evaluationCompleted" class="evaluation-result">
          <el-result
            icon="success"
            title="评测任务已提交"
            sub-title="系统正在后台处理评测任务，完成后可在评测结果页面查看详细信息。"
          >
            <template #extra>
              <el-button type="primary" @click="goBack">返回批次列表</el-button>
            </template>
          </el-result>
        </div>

        <!-- 答案列表 -->
        <el-divider>批次答案列表</el-divider>

        <div style="margin-bottom: 16px;">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          >
            <p>该区域显示该批次的所有答案及其评测状态。</p>
            <p>绿色标签表示已评测，灰色表示未评测。点击"刷新评测结果"按钮查看最新数据。</p>
          </el-alert>
          <div style="display: flex; justify-content: flex-end;">
            <el-button type="primary" @click="refreshEvaluationResults" :loading="loadingEvaluated">
              <el-icon><Refresh /></el-icon>
              刷新评测结果
            </el-button>
          </div>
        </div>
        <div class="evaluated-answers">
          <el-table
            v-loading="loadingEvaluated"
            :data="evaluatedAnswers"
            stripe
            style="width: 100%"
            max-height="600"
          >
            <template #empty>
              <div style="padding: 40px; text-align: center; color: #909399;">
                <el-icon size="48" style="margin-bottom: 16px;"><DataAnalysis /></el-icon>
                <p>暂无数据</p>
                <p style="font-size: 12px; margin-top: 8px;">
                  该批次可能还没有答案数据，或者答案尚未进行评测。<br/>
                  点击上方"刷新评测结果"按钮查看最新数据
                </p>
              </div>
            </template>
            <el-table-column prop="questionText" label="问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="answerText" label="回答" min-width="200" show-overflow-tooltip />
            <el-table-column prop="modelName" label="模型" width="120" />
            <el-table-column label="评测状态" width="100">
              <template #default="scope">
                <el-tag v-if="scope.row.evaluations && scope.row.evaluations.length > 0" type="success">
                  已评测
                </el-tag>
                <el-tag v-else type="info">
                  未评测
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="100">
              <template #default="scope">
                <el-tag v-if="scope.row.evaluations && scope.row.evaluations.length > 0" :type="getScoreTagType(scope.row.evaluations[0].score)">
                  {{ scope.row.evaluations[0].score }}
                </el-tag>
                <span v-else style="color: #909399;">-</span>
              </template>
            </el-table-column>
            <el-table-column label="评语" min-width="150" show-overflow-tooltip>
              <template #default="scope">
                <span v-if="scope.row.evaluations && scope.row.evaluations.length > 0">
                  {{ scope.row.evaluations[0].comments || '无评语' }}
                </span>
                <span v-else style="color: #909399;">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button type="primary" link @click="viewEvaluationDetail(scope.row)">
                  查看详情
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
        width="70%"
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
          <div class="detail-section" v-if="evaluationDetailDialog.data.evaluations && evaluationDetailDialog.data.evaluations.length > 0">
            <h3>评分: {{ evaluationDetailDialog.data.evaluations[0].score }}</h3>
            <p>{{ evaluationDetailDialog.data.evaluations[0].comments || '无评语' }}</p>
          </div>
          <div class="detail-section" v-else>
            <h3>评分状态</h3>
            <p class="text-gray-400">该答案尚未进行评测</p>
          </div>
          <div class="detail-section" v-if="evaluationDetailDialog.data.evaluation_results?.criteriaScores?.length">
            <h3>详细评分</h3>
            <el-table :data="evaluationDetailDialog.data.evaluation_results.criteriaScores" border>
              <el-table-column prop="criterionName" label="评分项" />
              <el-table-column prop="score" label="分数" width="100" />
              <el-table-column prop="comments" label="评语" min-width="200" show-overflow-tooltip />
            </el-table>
          </div>
          <div class="detail-section" v-if="evaluationDetailDialog.data.evaluation_results?.strengths">
            <h3>优点</h3>
            <p>{{ evaluationDetailDialog.data.evaluation_results.strengths }}</p>
          </div>
          <div class="detail-section" v-if="evaluationDetailDialog.data.evaluation_results?.weaknesses">
            <h3>缺点</h3>
            <p>{{ evaluationDetailDialog.data.evaluation_results.weaknesses }}</p>
          </div>
          <div class="detail-section" v-if="evaluationDetailDialog.data.evaluation_results?.suggestions">
            <h3>建议</h3>
            <p>{{ evaluationDetailDialog.data.evaluation_results.suggestions }}</p>
          </div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, VideoPlay, Connection, InfoFilled, Histogram, DataAnalysis, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  evaluateBatchSubjective,
  getSubjectiveEvaluationResults,
  getBatchUnevaluatedAnswers,
  type SubjectiveEvaluationResultItem
} from '@/api/evaluations'
import { getEvaluators, type EvaluatorInfo, testAiEvaluatorConnectivity } from '@/api/evaluator'
import { getActiveEvaluationSubjectivePrompts, type EvaluationSubjectivePromptInfo } from '@/api/evaluationSubjectivePrompt'
import { getAllActiveEvaluationConfigs, type EvaluationConfigInfo } from '@/api/evaluationPromptAssembly'
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
const testing = ref(false)
const evaluating = ref(false)
const evaluationCompleted = ref(false)
const evaluationProgress = ref(0)
const selectedModelName = ref('')
const loadingEvaluated = ref(false)

// 可用模型
const availableModels = ref<EvaluatorInfo[]>([])
// 可用提示词模板
const availablePrompts = ref<EvaluationSubjectivePromptInfo[]>([])
// 可用评测组装配置
const availableConfigs = ref<EvaluationConfigInfo[]>([])
// 可用评测标准
const availableCriteria = ref<EvaluationCriterion[]>([])

// 评测表单
const evaluationForm = reactive({
  modelId: '',
  prompt: '请评估以下回答的质量，考虑准确性、完整性、逻辑性和表达清晰度。给出1-100的分数和详细评价。',
  subjectivePromptId: null as number | null,
  evaluationAssemblyConfigId: null as number | null,
  useCustomPrompt: false,  // 默认使用系统评测配置
  criteriaIds: [] as number[]
})

// 评测统计
const evaluationStats = reactive({
  completed: 0,
  pending: 0,
  total: 0,
  progress: 0,
  averageScore: 0,
  maxScore: 0,
  minScore: 0
})

// 已评测答案分页
const evaluatedAnswers = ref<SubjectiveEvaluationResultItem[]>([])
const evaluatedAnswersPage = ref(1)
const evaluatedAnswersPageSize = ref(10)
const evaluatedAnswersTotal = ref(0)

// 评测详情对话框
const evaluationDetailDialog = reactive({
  visible: false,
  data: null as SubjectiveEvaluationResultItem | null
})

// 评测状态
const evaluationStatus = computed(() => {
  if (evaluationProgress.value < 100) return ''
  return 'success'
})

// 格式化进度显示
const progressFormat = (percentage: number) => {
  return `${percentage}%`
}

// 根据分数获取标签类型
const getScoreTagType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 返回上一页
const goBack = () => {
  router.push({ name: 'Evaluations' })
}

// 加载数据
const loadData = async () => {
  try {
    loading.value = true

    // 加载可用模型
    await loadAvailableModels()

    // 加载提示词模板
    await loadPromptTemplates()

    // 加载评测组装配置
    await loadAssemblyConfigs()

    // 加载评测标准
    await loadEvaluationCriteria()

    // 加载评测统计和已评测答案
    await loadEvaluationStats()
    await loadEvaluatedAnswers()
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载可用模型
const loadAvailableModels = async () => {
  try {
    const evaluators = await getEvaluators()
    // 过滤出LLM类型的评测器作为可用模型
    availableModels.value = evaluators.filter(evaluator => evaluator.evaluatorType === 'AI_MODEL')

    if (availableModels.value.length > 0) {
      evaluationForm.modelId = availableModels.value[0].id.toString()
    } else {
      ElMessage.warning('没有可用的评测模型')
    }
  } catch (error) {
    console.error('加载可用模型失败:', error)
    ElMessage.error('加载可用模型失败')
  }
}

// 加载提示词模板
const loadPromptTemplates = async () => {
  try {
    const prompts = await getActiveEvaluationSubjectivePrompts()
    availablePrompts.value = prompts
    if (prompts.length > 0) {
      evaluationForm.subjectivePromptId = prompts[0].id
    }
  } catch (error) {
    console.error('加载提示词模板失败:', error)
    ElMessage.warning('加载提示词模板失败')
  }
}

// 加载评测组装配置
const loadAssemblyConfigs = async () => {
  try {
    const response = await getAllActiveEvaluationConfigs()
    availableConfigs.value = response.configs
    if (response.configs.length > 0) {
      evaluationForm.evaluationAssemblyConfigId = response.configs[0].id
    }
  } catch (error) {
    console.error('加载评测组装配置失败:', error)
    ElMessage.warning('加载评测组装配置失败')
  }
}

// 加载评测标准
const loadEvaluationCriteria = async () => {
  try {
    const criteria = await getAllEvaluationCriteria()
    console.log('获取到的评测标准数据:', criteria)

    // 检查数据结构，如果没有questionType字段，则显示所有标准
    if (criteria.length > 0) {
      console.log('第一个评测标准的结构:', criteria[0])

      // 如果数据有questionType字段，则过滤主观题；否则显示所有
      if (criteria[0].hasOwnProperty('questionType')) {
        availableCriteria.value = criteria.filter(criterion => criterion.questionType === 'SUBJECTIVE')
        console.log('过滤后的主观题评测标准:', availableCriteria.value)
      } else {
        // 没有questionType字段，显示所有标准
        availableCriteria.value = criteria
        console.log('没有questionType字段，显示所有评测标准:', availableCriteria.value)
      }

      if (availableCriteria.value.length > 0) {
        // 默认选择前3个评测标准
        evaluationForm.criteriaIds = availableCriteria.value.slice(0, 3).map(c => c.id)
        console.log('默认选择的评测标准ID:', evaluationForm.criteriaIds)
      }
    }
  } catch (error) {
    console.error('加载评测标准失败:', error)
    ElMessage.warning('加载评测标准失败')
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
    // 获取已评测答案数量
    const completedResponse = await getSubjectiveEvaluationResults({
      batchId: batchId.value,
      page: 0,
      size: 1
    })

    // 获取未评测答案数量
    const pendingResponse = await getBatchUnevaluatedAnswers(batchId.value, {
      page: 0,
      size: 1
    })

    // 更新统计数据
    evaluationStats.completed = completedResponse.totalItems || 0
    evaluationStats.pending = pendingResponse.totalElements || 0
    evaluationStats.total = evaluationStats.completed + evaluationStats.pending
    evaluationStats.progress = evaluationStats.total > 0
      ? Math.round((evaluationStats.completed / evaluationStats.total) * 100)
      : 0

    // 计算平均分、最高分和最低分
    if (completedResponse.items && completedResponse.items.length > 0) {
      // 获取所有评测结果以计算统计信息
      const allResults = await getSubjectiveEvaluationResults({
        batchId: batchId.value,
        page: 0,
        size: 1000 // 获取足够多的结果以计算统计
      })

      if (allResults.items && allResults.items.length > 0) {
        // 提取有评测结果的项目的分数
        const scores = allResults.items
          .filter(item => item.evaluations && item.evaluations.length > 0)
          .map(item => item.evaluations[0].score)

        if (scores.length > 0) {
          evaluationStats.averageScore = scores.reduce((a, b) => a + b, 0) / scores.length
          evaluationStats.maxScore = Math.max(...scores)
          evaluationStats.minScore = Math.min(...scores)
        } else {
          resetEvaluationStats()
        }
      } else {
        resetEvaluationStats()
      }
    } else {
      resetEvaluationStats()
    }
  } catch (error) {
    console.error('加载评测统计失败:', error)
    resetEvaluationStats()
  }
}

// 重置评测统计
const resetEvaluationStats = () => {
  evaluationStats.averageScore = 0
  evaluationStats.maxScore = 0
  evaluationStats.minScore = 0
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

    const response = await getSubjectiveEvaluationResults({
      batchId: batchId.value,
      page: evaluatedAnswersPage.value - 1,
      size: evaluatedAnswersPageSize.value
    })

    console.log('获取到的评测结果数据:', response)
    console.log('评测结果项目数量:', response.items?.length || 0)
    if (response.items && response.items.length > 0) {
      console.log('第一个评测结果项目结构:', response.items[0])
    }

    evaluatedAnswers.value = response.items || []
    evaluatedAnswersTotal.value = response.totalItems || 0

    console.log('设置到表格的数据:', evaluatedAnswers.value)
    console.log('总数量:', evaluatedAnswersTotal.value)
  } catch (error) {
    console.error('加载已评测答案失败:', error)
    ElMessage.error('加载已评测答案失败')
  } finally {
    loadingEvaluated.value = false
  }
}

// 查看评测详情
const viewEvaluationDetail = (row: SubjectiveEvaluationResultItem) => {
  evaluationDetailDialog.data = row
  evaluationDetailDialog.visible = true
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

// 刷新评测结果
const refreshEvaluationResults = async () => {
  if (!isValidBatchId.value) {
    ElMessage.warning('批次ID无效，无法加载评测结果')
    return
  }

  console.log('手动刷新评测结果')
  await Promise.all([
    loadEvaluationStats(),
    loadEvaluatedAnswers()
  ])
  ElMessage.success('评测结果已刷新')
}

// 测试模型连通性
const testSelectedModel = async () => {
  if (!evaluationForm.modelId) {
    ElMessage.warning('请先选择评测模型')
    return false
  }

  try {
    testing.value = true
    const result = await testAiEvaluatorConnectivity(parseInt(evaluationForm.modelId))
    if (result.success) {
      ElMessage.success(`模型连接测试成功: ${result.message}`)
      ElMessage.info(`响应示例: ${result.response}`)
      ElMessage.info(`响应时间: ${result.responseTime}ms`)
      return true
    } else {
      ElMessage.error(`模型连接测试失败: ${result.message || '未知错误'}`)
      return false
    }
  } catch (error) {
    console.error('测试模型连通性失败:', error)
    ElMessage.error('测试模型连通性失败')
    return false
  } finally {
    testing.value = false
  }
}

// 开始评测
const startEvaluation = async () => {
  if (!evaluationForm.modelId) {
    ElMessage.warning('请选择评测模型')
    return
  }

  if (!evaluationForm.useCustomPrompt) {
    if (!evaluationForm.subjectivePromptId) {
      ElMessage.warning('请选择提示词模板')
      return
    }
    if (!evaluationForm.evaluationAssemblyConfigId) {
      ElMessage.warning('请选择评测组装配置')
      return
    }
    if (!evaluationForm.criteriaIds || evaluationForm.criteriaIds.length === 0) {
      ElMessage.warning('请选择至少一个评测标准')
      return
    }
  } else {
    // 使用自定义提示词时，需要提示用户这个功能可能不可用
    ElMessage.warning('注意：自定义提示词功能可能不再支持，建议选择系统提示词模板')
  }

  // 先测试模型连通性
  testing.value = true
  const isConnected = await testSelectedModel()
  testing.value = false

  if (!isConnected) {
    return
  }

  try {
    submitting.value = true
    const userData = userStore.getCurrentUser()

    // 保存选中的模型名称用于显示
    const selectedModel = availableModels.value.find(m => m.id.toString() === evaluationForm.modelId)
    if (selectedModel) {
      selectedModelName.value = selectedModel.name
    }

    // 准备API请求参数
    const requestParams = {
      batchId: parseInt(batchId.value),
      evaluatorId: parseInt(evaluationForm.modelId),
      userId: userData?.id || 0
    } as any

    // 如果选择了系统提示词模板和评测组装配置，则添加相应参数
    if (!evaluationForm.useCustomPrompt && evaluationForm.subjectivePromptId) {
      requestParams.subjectivePromptId = evaluationForm.subjectivePromptId
    }

    if (!evaluationForm.useCustomPrompt && evaluationForm.evaluationAssemblyConfigId) {
      requestParams.evaluationAssemblyConfigId = evaluationForm.evaluationAssemblyConfigId
    }

    // 添加评测标准ID列表
    if (!evaluationForm.useCustomPrompt && evaluationForm.criteriaIds && evaluationForm.criteriaIds.length > 0) {
      requestParams.criteriaIds = evaluationForm.criteriaIds
    }

    // 调用评测API
    await evaluateBatchSubjective(requestParams)

    // 显示评测进度
    evaluating.value = true

    // 模拟进度
    startProgressSimulation()

    ElMessage.success('主观题评测已启动')
  } catch (error) {
    console.error('启动主观题评测失败:', error)
    ElMessage.error('启动主观题评测失败')
  } finally {
    submitting.value = false
  }
}

// 模拟进度
const startProgressSimulation = () => {
  let progress = 0
  const interval = setInterval(() => {
    if (progress >= 100) {
      clearInterval(interval)
      evaluationCompleted.value = true

      // 评测完成后，自动刷新评测结果
      setTimeout(() => {
        refreshEvaluationResults()
      }, 2000)

      return
    }

    // 随机增加进度，模拟评测过程
    progress += Math.random() * 2
    evaluationProgress.value = Math.min(Math.round(progress), 100)

    if (evaluationProgress.value >= 100) {
      clearInterval(interval)
      evaluationCompleted.value = true

      // 评测完成后，自动刷新评测结果
      setTimeout(() => {
        refreshEvaluationResults()
      }, 2000)
    }
  }, 1000)
}

// 获取题型显示名称
const getQuestionTypeDisplay = (type: string) => {
  const typeMap: Record<string, string> = {
    'SUBJECTIVE': '主观题',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题'
  }
  return typeMap[type] || type
}

// 获取题型标签类型
const getQuestionTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'SUBJECTIVE': 'danger',
    'SINGLE_CHOICE': 'success',
    'MULTIPLE_CHOICE': 'warning',
    'SIMPLE_FACT': 'info'
  }
  return typeMap[type] || ''
}

// 初始化
onMounted(async () => {
  // 等待路由参数完全加载
  await nextTick()

  console.log('主观题大模型评测页面初始化 - batchId:', batchId.value, 'isValid:', isValidBatchId.value)

  // 加载基础数据（无论批次ID是否有效）
  loadAvailableModels()
  loadPromptTemplates()
  loadAssemblyConfigs()
  loadEvaluationCriteria()

  // 对于主观题大模型评测页面，不在初始化时自动加载已有结果
  // 用户需要先选择模型和配置，或者手动刷新查看已有结果
  console.log('主观题大模型评测页面已初始化，等待用户选择评测配置')
})

// 定义组件名称
defineOptions({
  name: 'SubjectiveLlmEvaluationPage'
})
</script>

<style scoped>
.subjective-llm-evaluation-page {
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

.model-selection {
  margin: 20px 0;
}

.model-selection h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: #606266;
}

.model-selection-form {
  max-width: 800px;
}

.model-option, .prompt-option, .config-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.criterion-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.criterion-option .el-tag {
  margin: 0 8px;
}

.evaluation-status {
  margin: 20px 0;
}

.status-description {
  margin-top: 10px;
}

.evaluation-result {
  margin-top: 30px;
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
</style>
