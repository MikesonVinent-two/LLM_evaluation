<template>
  <div class="objective-evaluation-page">
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
              <h2>客观题评测</h2>
              <el-tag class="batch-tag" type="info">批次: {{ batchName }}</el-tag>
            </div>
            <div class="header-actions">
              <el-button @click="goBack">
                <el-icon><Back /></el-icon>
                返回
              </el-button>
              <el-button type="primary" @click="startEvaluation" :loading="loading" :disabled="evaluating">
                <el-icon><VideoPlay /></el-icon>
                开始评测
              </el-button>
            </div>
          </div>
        </template>

        <!-- 评测状态 -->
        <div v-if="evaluating" class="evaluation-status">
          <el-alert
            title="评测进行中..."
            type="info"
            :closable="false"
            show-icon
          >
            <template #description>
              <div class="status-description">
                <p>系统正在对客观题进行自动评测，请耐心等待。</p>
                <el-progress :percentage="evaluationProgress" :status="evaluationStatus"></el-progress>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 评测结果 -->
        <div v-if="evaluationResult" class="evaluation-result">
          <el-descriptions title="评测结果统计" :column="2" border>
            <el-descriptions-item label="总答案数量">{{ evaluationResult.totalAnswers }}</el-descriptions-item>
            <el-descriptions-item label="平均得分">{{ (evaluationResult.averageScore || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="成功数量">{{ evaluationResult.successCount }}</el-descriptions-item>
            <el-descriptions-item label="失败数量">{{ evaluationResult.failedCount }}</el-descriptions-item>
          </el-descriptions>

          <h3>各题型统计</h3>
          <el-table :data="questionTypeStats" style="width: 100%">
            <el-table-column prop="type" label="题型" />
            <el-table-column prop="count" label="数量" />
            <el-table-column prop="averageScore" label="平均分" />
          </el-table>
        </div>

        <!-- 详细评测结果 -->
        <div v-if="detailResults" class="detail-results">
          <el-divider>
            <h3>详细评测结果</h3>
          </el-divider>

          <!-- 模型平均分 -->
          <h4>各模型平均分</h4>
          <div class="model-averages">
            <el-card v-for="(score, modelId) in detailResults.modelAverages" :key="modelId" class="average-card">
              <template #header>
                <div class="average-header">
                  {{ getModelName(modelId) }}
                </div>
              </template>
              <div class="average-score">{{ (score || 0).toFixed(2) }}</div>
            </el-card>
          </div>

          <!-- 题型平均分 -->
          <h4>各题型平均分</h4>
          <div class="type-averages">
            <el-card v-for="(score, type) in detailResults.typeAverages" :key="type" class="average-card">
              <template #header>
                <div class="average-header">
                  {{ getQuestionTypeText(type) }}
                </div>
              </template>
              <div class="average-score">{{ (score || 0).toFixed(2) }}</div>
            </el-card>
          </div>

          <!-- 答案详情表格 -->
          <h4>答案详情</h4>
          <el-table :data="detailResults.items" style="width: 100%" border stripe>
            <el-table-column prop="questionText" label="问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="questionType" label="题型" width="120">
              <template #default="{ row }">
                {{ getQuestionTypeText(row.questionType) }}
              </template>
            </el-table-column>
            <el-table-column prop="modelName" label="模型" width="120" />
            <el-table-column prop="answerText" label="答案" min-width="150" show-overflow-tooltip />
            <el-table-column prop="score" label="得分" width="80" />
            <el-table-column prop="isCorrect" label="是否正确" width="100">
              <template #default="{ row }">
                <el-tag :type="getCorrectTagType(row.score)">
                  {{ getCorrectText(row.score) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页器 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="detailPage"
              v-model:page-size="detailPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="detailResults.totalItems"
              @size-change="handleDetailSizeChange"
              @current-change="handleDetailPageChange"
            />
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  evaluateBatchObjectiveQuestions,
  getObjectiveEvaluationResults,
  type BatchObjectiveEvaluationResult,
  type ObjectiveEvaluationDetailResponse,
  type QuestionTypeStatistics
} from '@/api/evaluations'

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
const evaluating = ref(false)
const evaluationProgress = ref(0)
const evaluationResult = ref<BatchObjectiveEvaluationResult | null>(null)
const detailResults = ref<ObjectiveEvaluationDetailResponse | null>(null)
const detailPage = ref(1)
const detailPageSize = ref(10)
const loadingDetails = ref(false)

// 评测状态
const evaluationStatus = computed(() => {
  if (evaluationProgress.value < 100) return ''
  return 'success'
})

// 题型统计数据
const questionTypeStats = computed(() => {
  if (!evaluationResult.value) return []

  return Object.entries(evaluationResult.value.typeStatistics).map(([type, stats]) => ({
    type,
    count: stats.count,
    averageScore: (stats.averageScore || 0).toFixed(2)
  }))
})

// 获取题型文本
const getQuestionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题',
    'SUBJECTIVE': '主观题'
  }
  return typeMap[type] || type
}

// 获取模型名称
const getModelName = (modelId: string) => {
  if (!detailResults.value) return `模型 ${modelId}`

  const model = detailResults.value.items.find(item => item.modelId.toString() === modelId)
  return model ? model.modelName : `模型 ${modelId}`
}

// 返回上一页
const goBack = () => {
  router.push({ name: 'Evaluations' })
}

// 获取详细评测结果
const fetchDetailResults = async () => {
  // 验证batchId是否有效
  if (!isValidBatchId.value) {
    console.warn('batchId无效，跳过获取详细评测结果')
    return
  }

  try {
    loadingDetails.value = true

    const result = await getObjectiveEvaluationResults({
      batchId: batchId.value,
      page: detailPage.value - 1, // API页码从0开始
      size: detailPageSize.value
    })

    detailResults.value = result
  } catch (error) {
    console.error('获取客观题详细评测结果失败:', error)
    ElMessage.warning('获取客观题详细评测结果失败')
  } finally {
    loadingDetails.value = false
  }
}

// 开始评测
const startEvaluation = async () => {
  try {
    loading.value = true
    evaluating.value = true
    evaluationProgress.value = 0
    detailResults.value = null // 清除之前的详细结果

    // 启动进度模拟
    startProgressSimulation()

    const userData = userStore.getCurrentUser()

    // 调用评测API
    const result = await evaluateBatchObjectiveQuestions(batchId.value, {
      userId: userData?.id,
      evaluatorId: userData?.evaluatorId
    })

    // 评测完成
    evaluationProgress.value = 100
    evaluationResult.value = result
    ElMessage.success('客观题评测完成')

    // 获取详细评测结果
    if (isValidBatchId.value) {
      await fetchDetailResults()
    }
  } catch (error) {
    console.error('客观题评测失败:', error)
    ElMessage.error('客观题评测失败')
    evaluating.value = false
    evaluationProgress.value = 0
  } finally {
    loading.value = false
  }
}

// 模拟进度
const startProgressSimulation = () => {
  let progress = 0
  const interval = setInterval(() => {
    if (evaluationProgress.value >= 95) {
      clearInterval(interval)
      return
    }

    // 随机增加进度，模拟评测过程
    progress += Math.random() * 5
    evaluationProgress.value = Math.min(Math.round(progress), 95)
  }, 1000)
}

// 详细结果分页处理
const handleDetailSizeChange = (val: number) => {
  detailPageSize.value = val
  if (isValidBatchId.value) {
    fetchDetailResults()
  }
}

const handleDetailPageChange = (val: number) => {
  detailPage.value = val
  if (isValidBatchId.value) {
    fetchDetailResults()
  }
}

// 获取正确状态的标签类型
const getCorrectTagType = (score: number) => {
  if (score === 100) return 'success'
  if (score === 0) return 'danger'
  return 'warning'
}

// 获取正确状态的文本
const getCorrectText = (score: number) => {
  if (score === 100) return '正确'
  if (score === 0) return '错误'
  return '部分正确'
}

// 获取评测结果
const fetchEvaluationResults = async () => {
  // 验证batchId是否有效
  if (!isValidBatchId.value) {
    console.warn('batchId无效，跳过获取评测结果')
    return
  }

  try {
    loading.value = true

    // 尝试获取已有的评测结果
    const hasExistingResults = await tryFetchExistingResults()

    if (!hasExistingResults) {
      ElMessage.info('未找到现有评测结果，请点击"开始评测"按钮进行评测')
    }
  } catch (error) {
    console.error('获取评测结果失败:', error)
    ElMessage.warning('获取评测结果失败')
  } finally {
    loading.value = false
  }
}

// 尝试获取已有的评测结果
const tryFetchExistingResults = async () => {
  // 验证batchId是否有效
  if (!isValidBatchId.value) {
    console.warn('batchId无效，跳过获取已有评测结果')
    return false
  }

  try {
    loading.value = true

    // 尝试获取详细评测结果
    const result = await getObjectiveEvaluationResults({
      batchId: batchId.value,
      page: 0,
      size: detailPageSize.value
    })

    if (result && result.items && result.items.length > 0) {
      // 如果有评测结果，显示结果
      detailResults.value = result

      // 构造评测结果统计信息
      const totalAnswers = result.totalItems
      const successCount = result.items.filter(item => item.score === 100).length
      const failedCount = result.items.filter(item => item.score === 0).length

      // 计算平均分
      const totalScore = result.items.reduce((sum, item) => sum + item.score, 0)
      const averageScore = totalScore / result.items.length

      // 按题型分组统计
      const typeStats: Record<string, QuestionTypeStatistics> = {}
      result.items.forEach(item => {
        if (!typeStats[item.questionType]) {
          typeStats[item.questionType] = { count: 0, averageScore: 0 }
        }
        typeStats[item.questionType].count++
        typeStats[item.questionType].averageScore += item.score
      })

      // 计算各题型平均分
      Object.keys(typeStats).forEach(type => {
        typeStats[type].averageScore = typeStats[type].averageScore / typeStats[type].count
      })

      // 构造评测结果对象
      evaluationResult.value = {
        totalAnswers,
        successCount,
        failedCount,
        averageScore,
        typeStatistics: typeStats,
        repeatIndexStatistics: {} // 这个字段可能不需要完全匹配
      }

      ElMessage.success('已加载现有评测结果')
      return true
    }

    return false
  } catch (error) {
    console.error('获取现有评测结果失败:', error)
    return false
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(async () => {
  // 等待路由参数完全加载
  await nextTick()

  console.log('客观题评测页面初始化 - batchId:', batchId.value, 'isValid:', isValidBatchId.value)

  // 只有在批次ID有效时才加载评测结果
  if (isValidBatchId.value && batchId.value && batchId.value !== 'undefined' && batchId.value !== 'null') {
    console.log('batchId有效，开始检查评测结果')
    // 检查是否已有评测结果
    fetchEvaluationResults()
  } else {
    console.log('batchId无效，跳过加载评测结果')
  }
})

// 定义组件名称
defineOptions({
  name: 'ObjectiveEvaluationPage'
})
</script>

<style scoped>
.objective-evaluation-page {
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

.evaluation-status {
  margin: 20px 0;
}

.status-description {
  margin-top: 10px;
}

.evaluation-result {
  margin-top: 30px;
}

h3 {
  margin: 20px 0 10px;
}

h4 {
  margin: 15px 0 10px;
  color: #606266;
}

.detail-results {
  margin-top: 30px;
}

.model-averages,
.type-averages {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.average-card {
  width: 180px;
}

.average-header {
  font-size: 14px;
  font-weight: bold;
}

.average-score {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  text-align: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.error-card {
  margin: 100px auto;
  max-width: 600px;
}
</style>
