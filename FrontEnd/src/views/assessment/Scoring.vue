<template>
  <div class="scoring-page">
    <!-- 页面标题 -->
    <div class="page-title">
      <h1>批次评分详情</h1>
      <p v-if="batchData?.batchInfo" class="subtitle">
        {{ batchData.batchInfo.name }} - {{ batchData.batchInfo.description }}
      </p>
    </div>

    <!-- 批次选择 -->
    <el-card class="selection-card" shadow="hover">
      <div class="selection-row">
        <div class="selection-item">
          <label>选择批次：</label>
          <el-select
            v-model="selectedBatchId"
            placeholder="请选择批次"
            @change="onBatchChange"
            style="width: 300px"
          >
            <el-option
              v-for="batch in availableBatches"
              :key="batch.id"
              :label="`${batch.name} (${batch.description})`"
              :value="batch.id"
            />
          </el-select>
        </div>
        <!-- <div class="selection-item">
          <label>筛选模型：</label>
          <el-select
            v-model="selectedModelIds"
            placeholder="选择模型（不选则显示全部）"
            multiple
            collapse-tags
            style="width: 400px"
            @change="onModelFilterChange"
          >
            <el-option
              v-for="model in availableModels"
              :key="model.id"
              :label="`${model.name} (${model.provider})`"
              :value="model.id"
            />
          </el-select>
        </div> -->
        <div class="selection-actions">
          <el-button type="primary" @click="refreshData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
          <el-button type="success" @click="viewEvaluationDetails" :disabled="!selectedBatchId">
            <el-icon><Document /></el-icon>
            查看评分详情
          </el-button>
          <el-button @click="exportData" :disabled="!batchData">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 主要内容 -->
    <template v-else-if="batchData">
      <!-- 概览统计 -->
      <div class="overview-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-icon models">
                  <el-icon><Monitor /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ batchData.overview.total_models }}</div>
                  <div class="stat-label">参与模型</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-icon evaluations">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ batchData.overview.total_evaluations }}</div>
                  <div class="stat-label">总评测数</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-icon evaluators">
                  <el-icon><User /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ batchData.overview.total_evaluators }}</div>
                  <div class="stat-label">评测员数</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <div class="stat-icon score">
                  <el-icon><DataAnalysis /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ safeNumber(overallAverageScore).toFixed(1) }}</div>
                  <div class="stat-label">平均分数</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>



      <!-- 模型排名 -->
      <div class="ranking-section">
        <el-card>
          <template #header>
            <div class="ranking-header">
              <span>模型排名</span>
              <!-- <el-radio-group v-model="sortBy" size="small" @change="onSortChange">
                <el-radio-button label="overall">综合评分</el-radio-button>
                <el-radio-button label="objective">客观题</el-radio-button>
                <el-radio-button label="subjective">主观题</el-radio-button>
              </el-radio-group> -->
            </div>
          </template>

          <el-table :data="sortedModelScores" stripe>
            <el-table-column prop="rank" label="排名" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getRankTagType(row.rank)" size="small">
                  #{{ row.rank }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="模型信息" min-width="200">
              <template #default="{ row }">
                <div>
                  <div class="model-name">{{ row.modelInfo.name }}</div>
                  <div class="model-meta">
                    <el-tag size="small" type="info">{{ row.modelInfo.provider }}</el-tag>
                    <span class="model-version">{{ row.modelInfo.version }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="overallScore" label="综合评分" width="120" align="center">
              <template #default="{ row }">
                <div class="score-display">
                  <div class="score-number">{{ safeNumber(row.overallScore).toFixed(1) }}</div>
                  <el-progress
                    :percentage="Math.round(safeNumber(row.overallScore))"
                    :color="getScoreColor(row.overallScore)"
                    :show-text="false"
                    :stroke-width="4"
                  />
                </div>
              </template>
            </el-table-column>

            <el-table-column label="客观题" width="100" align="center">
              <template #default="{ row }">
                <div class="score-display">
                  <div class="score-number">{{ safeNumber(row.objectiveScores?.average_score).toFixed(1) }}</div>
                  <div class="score-count">{{ row.objectiveScores?.total_answers || 0 }}题</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="主观题(AI)" width="100" align="center">
              <template #default="{ row }">
                <div class="score-display">
                  <div class="score-number">{{ safeNumber(row.subjectiveAiScores?.average_score).toFixed(1) }}</div>
                  <div class="score-count">{{ row.subjectiveAiScores?.total_answers || 0 }}题</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="主观题(人工)" width="100" align="center">
              <template #default="{ row }">
                <div class="score-display">
                  <div class="score-number">{{ safeNumber(row.subjectiveHumanScores?.average_score).toFixed(1) }}</div>
                  <div class="score-count">{{ row.subjectiveHumanScores?.total_answers || 0 }}题</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="viewModelDetails(row)">
                  查看详情
                </el-button>
                <el-button size="small" @click="compareModel(row)">
                  对比分析
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-section">
      <el-empty description="暂无数据">
        <el-button type="primary" @click="loadAvailableBatches">
          加载批次数据
        </el-button>
      </el-empty>
    </div>

    <!-- 模型详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      title="模型详细评分"
      width="80%"
      :before-close="handleDetailDialogClose"
    >
      <ModelDetailView
        v-if="selectedModelForDetail"
        :batch-id="selectedBatchId"
        :model-info="selectedModelForDetail"
        @close="showDetailDialog = false"
      />
    </el-dialog>

    <!-- 模型对比弹窗 -->
    <el-dialog
      v-model="showCompareDialog"
      title="模型对比分析"
      width="90%"
      :before-close="handleCompareDialogClose"
    >
      <ModelCompareView
        v-if="selectedModelsForCompare.length > 0"
        :batch-id="selectedBatchId"
        :models="selectedModelsForCompare"
        @close="showCompareDialog = false"
      />
    </el-dialog>

    <!-- 评分详情弹窗 -->
    <el-dialog
      v-model="showEvaluationDetailDialog"
      title="评分详情"
      width="90%"
      :before-close="handleEvaluationDetailDialogClose"
      top="2vh"
      :modal="true"
      :append-to-body="true"
      :z-index="3000"
      class="evaluation-detail-dialog"
    >
      <div class="evaluation-detail-container">
        <!-- 筛选条件 -->
        <el-card class="filter-card" shadow="never">
          <div class="filter-row">
            <!-- <div class="filter-item">
              <label>题型筛选：</label>
              <el-select
                v-model="selectedFilters.questionType"
                placeholder="选择题型"
                clearable
                @change="handleEvaluationDetailFilterChange"
                style="width: 150px"
              >
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="简单事实题" value="SIMPLE_FACT" />
                <el-option label="主观题" value="SUBJECTIVE" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>模型筛选：</label>
              <el-select
                v-model="selectedFilters.modelIds"
                placeholder="选择模型"
                multiple
                collapse-tags
                clearable
                @change="handleEvaluationDetailFilterChange"
                style="width: 250px"
              >
                <el-option
                  v-for="model in availableModels"
                  :key="model.id"
                  :label="`${model.name} (${model.provider})`"
                  :value="model.id"
                />
              </el-select>
            </div> -->
          </div>
        </el-card>

        <!-- 评分详情表格 -->
        <el-card class="detail-table-card">
                     <el-table
             :data="evaluationDetailData?.items || []"
             v-loading="evaluationDetailLoading"
             stripe
             border
             :height="400"
             :max-height="500"
           >
            <el-table-column prop="questionText" label="问题" min-width="200" show-overflow-tooltip />

            <el-table-column label="题型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="getQuestionTypeTagType(row.questionType)">
                  {{ getQuestionTypeText(row.questionType) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="模型" width="150">
              <template #default="{ row }">
                <div>
                  <div class="model-name">{{ row.modelName }}</div>
                  <div class="model-provider">{{ row.modelProvider }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="answerText" label="回答内容" min-width="250" show-overflow-tooltip />

            <el-table-column label="评测员" width="120">
              <template #default="{ row }">
                <div>
                  <div class="evaluator-name">{{ row.evaluatorName }}</div>
                  <el-tag size="small" :type="row.evaluatorType === 'AI_MODEL' ? 'primary' : 'success'">
                    {{ getEvaluationTypeText(row.evaluationType) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="总分" width="80" align="center">
              <template #default="{ row }">
                <div class="score-cell">
                  <div class="score-number">{{ safeNumber(row.overallScore).toFixed(1) }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="详细评分" width="200">
              <template #default="{ row }">
                <div class="detail-scores">
                  <div
                    v-for="detail in row.evaluationDetails?.slice(0, 3) || []"
                    :key="detail.id"
                    class="score-item"
                  >
                    <span class="criterion-name">{{ detail.criterionName }}:</span>
                    <span class="criterion-score">{{ detail.score.toFixed(1) }}</span>
                  </div>
                  <div v-if="(row.evaluationDetails?.length || 0) > 3" class="more-scores">
                    +{{ (row.evaluationDetails?.length || 0) - 3 }}项...
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="evaluationComments" label="评语" min-width="200" show-overflow-tooltip />

            <el-table-column label="评测时间" width="150" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.evaluationTime) }}
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="evaluationDetailPagination.currentPage"
              v-model:page-size="evaluationDetailPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="evaluationDetailPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleEvaluationDetailSizeChange"
              @current-change="handleEvaluationDetailPageChange"
            />
          </div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Monitor,
  Document,
  User,
  DataAnalysis,
  Sort
} from '@element-plus/icons-vue'

// API imports
import {
  getBatchComprehensiveScores,
  type BatchComprehensiveScoresResponse,
  type ModelScore,
  type BatchInfo,
  type ModelInfo,
  getAnswerEvaluationDetails,
  type AnswerEvaluationDetailResponse,
  type AnswerEvaluationDetailItem
} from '@/api/evaluations'
import { getAllAnswerGenerationBatches, type AnswerGenerationBatch } from '@/api/answerGenerationBatch'

// 组件导入
import ModelDetailView from './components/ModelDetailView.vue'
import ModelCompareView from './components/ModelCompareView.vue'

// 响应式数据
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const selectedBatchId = ref<number | null>(null)
const selectedModelIds = ref<number[]>([])
const batchData = ref<BatchComprehensiveScoresResponse | null>(null)
const availableBatches = ref<AnswerGenerationBatch[]>([])
const availableModels = ref<ModelInfo[]>([])
const sortBy = ref('overall')

// 弹窗相关
const showDetailDialog = ref(false)
const showCompareDialog = ref(false)
const selectedModelForDetail = ref<ModelScore | null>(null)
const selectedModelsForCompare = ref<ModelScore[]>([])

// 评分详情相关
const showEvaluationDetailDialog = ref(false)
const evaluationDetailLoading = ref(false)
const evaluationDetailData = ref<AnswerEvaluationDetailResponse | null>(null)
const evaluationDetailPagination = ref({
  currentPage: 0,
  pageSize: 10,
  total: 0
})
const selectedFilters = ref({
  questionType: '',
  modelIds: [] as number[],
  evaluatorIds: [] as number[]
})



// 工具函数
const safeNumber = (value: any): number => {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return 0
  }
  return Number(value)
}

// 计算属性
const overallAverageScore = computed(() => {
  if (!batchData.value?.modelScores?.length) return 0
  const total = batchData.value.modelScores.reduce((sum, model) => sum + safeNumber(model.overallScore), 0)
  return total / batchData.value.modelScores.length
})



const sortedModelScores = computed(() => {
  if (!batchData.value?.modelScores) return []

  let scores = [...batchData.value.modelScores]

  // 根据选择的排序方式排序
  scores.sort((a, b) => {
    let scoreA = 0, scoreB = 0

    switch (sortBy.value) {
      case 'objective':
        scoreA = safeNumber(a.objectiveScores?.average_score)
        scoreB = safeNumber(b.objectiveScores?.average_score)
        break
      case 'subjective':
        scoreA = (safeNumber(a.subjectiveAiScores?.average_score) + safeNumber(a.subjectiveHumanScores?.average_score)) / 2
        scoreB = (safeNumber(b.subjectiveAiScores?.average_score) + safeNumber(b.subjectiveHumanScores?.average_score)) / 2
        break
      default:
        scoreA = safeNumber(a.overallScore)
        scoreB = safeNumber(b.overallScore)
    }

    return scoreB - scoreA
  })

  // 添加排名
  scores.forEach((score, index) => {
    score.rank = index + 1
  })

  return scores
})

// 样式相关函数
const getScoreColor = (score: number): string => {
  const safeScore = safeNumber(score)
  if (safeScore >= 90) return '#67C23A'
  if (safeScore >= 80) return '#E6A23C'
  if (safeScore >= 70) return '#F56C6C'
  return '#909399'
}

const getRankTagType = (rank: number): string => {
  if (rank === 1) return 'danger'
  if (rank === 2) return 'warning'
  if (rank === 3) return 'success'
  return 'info'
}



// 数据加载函数
const loadAvailableBatches = async () => {
  try {
    loading.value = true
    const response = await getAllAnswerGenerationBatches()
    availableBatches.value = response || []
    console.log('成功加载批次列表:', availableBatches.value.length, '个批次')
  } catch (error) {
    console.error('加载批次失败:', error)
    ElMessage.error('加载批次失败')
    availableBatches.value = []
  } finally {
    loading.value = false
  }
}

const loadBatchData = async () => {
  if (!selectedBatchId.value) return

  try {
    loading.value = true
    const response = await getBatchComprehensiveScores(selectedBatchId.value)
    batchData.value = response

    // 更新可用模型列表
    if (response.modelScores) {
      availableModels.value = response.modelScores.map(score => score.modelInfo)
    }
  } catch (error) {
    console.error('加载批次数据失败:', error)
    ElMessage.error('加载批次数据失败')
  } finally {
    loading.value = false
  }
}

// 事件处理函数
const onBatchChange = () => {
  loadBatchData()
}

const onModelFilterChange = () => {
  // 实现模型筛选逻辑
}

const onSortChange = () => {
  // 排序逻辑已在计算属性中实现
}

const refreshData = () => {
  loadBatchData()
}

const exportData = () => {
  ElMessage.info('导出功能开发中...')
}

const viewModelDetails = (model: ModelScore) => {
  selectedModelForDetail.value = model
  showDetailDialog.value = true
}

const compareModel = (model: ModelScore) => {
  selectedModelsForCompare.value = [model]
  showCompareDialog.value = true
}

const handleDetailDialogClose = () => {
  showDetailDialog.value = false
  selectedModelForDetail.value = null
}

const handleCompareDialogClose = () => {
  showCompareDialog.value = false
  selectedModelsForCompare.value = []
}

// 评分详情相关函数
const loadEvaluationDetails = async (page = 0) => {
  if (!selectedBatchId.value) return

  try {
    evaluationDetailLoading.value = true
    const params: any = {
      batchId: selectedBatchId.value,
      page,
      size: evaluationDetailPagination.value.pageSize
    }

    // 添加筛选条件
    if (selectedFilters.value.questionType) {
      params.questionType = selectedFilters.value.questionType
    }
    if (selectedFilters.value.modelIds.length > 0) {
      params.modelIds = selectedFilters.value.modelIds
    }
    if (selectedFilters.value.evaluatorIds.length > 0) {
      params.evaluatorIds = selectedFilters.value.evaluatorIds
    }

    const response = await getAnswerEvaluationDetails(params)
    evaluationDetailData.value = response
    evaluationDetailPagination.value = {
      currentPage: response.currentPage,
      pageSize: response.pageSize,
      total: response.totalItems
    }
  } catch (error) {
    console.error('加载评分详情失败:', error)
    ElMessage.error('加载评分详情失败')
  } finally {
    evaluationDetailLoading.value = false
  }
}

const viewEvaluationDetails = () => {
  showEvaluationDetailDialog.value = true
  loadEvaluationDetails()
}

const handleEvaluationDetailPageChange = (page: number) => {
  loadEvaluationDetails(page - 1) // Element Plus 分页从1开始，API从0开始
}

const handleEvaluationDetailSizeChange = (size: number) => {
  evaluationDetailPagination.value.pageSize = size
  loadEvaluationDetails()
}

const handleEvaluationDetailFilterChange = () => {
  loadEvaluationDetails()
}

const handleEvaluationDetailDialogClose = () => {
  showEvaluationDetailDialog.value = false
  evaluationDetailData.value = null
  selectedFilters.value = {
    questionType: '',
    modelIds: [],
    evaluatorIds: []
  }
}

const getQuestionTypeText = (type: string): string => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题',
    'SUBJECTIVE': '主观题'
  }
  return typeMap[type] || type
}

const getQuestionTypeTagType = (type: string): string => {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': 'success',
    'MULTIPLE_CHOICE': 'warning',
    'SIMPLE_FACT': 'info',
    'SUBJECTIVE': 'danger'
  }
  return typeMap[type] || 'info'
}

const getEvaluationTypeText = (type: string): string => {
  return type === 'AI_MODEL' ? 'AI评测' : '人工评测'
}

const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}



// 生命周期
onMounted(async () => {
  const batchId = route.params.batchId || route.query.batchId
  if (batchId) {
    selectedBatchId.value = Number(batchId)
  }

  await loadAvailableBatches()

  if (selectedBatchId.value) {
    await loadBatchData()
  }
})


</script>

<style scoped>
.scoring-page {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.page-title h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.selection-card {
  margin-bottom: 20px;
}

.selection-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.selection-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selection-item label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.selection-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.loading-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.overview-section {
  margin-bottom: 20px;
}

.stat-card {
  height: 100px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.stat-icon.models {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.evaluations {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.evaluators {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.score {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}



.ranking-section {
  margin-bottom: 20px;
}

.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.model-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.model-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-version {
  font-size: 12px;
  color: #909399;
}

.score-display {
  text-align: center;
}

.score-number {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.score-count {
  font-size: 12px;
  color: #909399;
}

.empty-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: white;
  border-radius: 8px;
}

/* 评分详情弹窗样式 */
.evaluation-detail-dialog {
  z-index: 3000 !important;
}

.evaluation-detail-dialog .el-dialog {
  margin: 0 !important;
  position: fixed !important;
  top: 2vh !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  max-height: 96vh !important;
  overflow: hidden !important;
}

.evaluation-detail-dialog .el-dialog__body {
  padding: 10px 20px !important;
  max-height: calc(96vh - 120px) !important;
  overflow-y: auto !important;
}

/* 评分详情容器样式 */
.evaluation-detail-container {
  max-height: calc(96vh - 140px);
  overflow-y: auto;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.detail-table-card {
  margin-bottom: 16px;
}

.model-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.model-provider {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.evaluator-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.score-cell {
  text-align: center;
}

.score-number {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.detail-scores {
  font-size: 12px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}

.criterion-name {
  color: #606266;
  flex: 1;
}

.criterion-score {
  color: #303133;
  font-weight: 500;
}

.more-scores {
  color: #909399;
  font-style: italic;
  text-align: center;
  margin-top: 4px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 确保弹窗遮罩层正确显示 */
.evaluation-detail-dialog .el-overlay {
  z-index: 2999 !important;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .evaluation-detail-dialog .el-dialog {
    width: 95% !important;
  }
}

@media (max-width: 768px) {
  .evaluation-detail-dialog .el-dialog {
    width: 98% !important;
    top: 1vh !important;
    max-height: 98vh !important;
  }

  .evaluation-detail-container {
    max-height: calc(98vh - 120px);
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .scoring-page {
    padding: 16px;
  }

  .selection-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .selection-actions {
    margin-left: 0;
    width: 100%;
  }
}
</style>
