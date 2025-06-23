<template>
  <div class="answer-review">
    <el-row :gutter="20">
      <el-col :span="7">
        <!-- 左侧待评审回答列表 -->
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <h2>待评审回答</h2>
              <div class="header-actions">
                <el-select
                  v-model="filterType"
                  placeholder="来源筛选"
                  @change="loadAnswers"
                  style="width: 120px;"
                >
                  <el-option label="全部" value="ALL" />
                  <el-option label="众包回答" value="CROWDSOURCED" />
                  <el-option label="专家回答" value="EXPERT" />
                  <el-option label="自动生成" value="AUTO" />
                </el-select>
              </div>
            </div>
          </template>

          <div class="filter-bar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索问题..."
              clearable
              @clear="loadAnswers"
              @input="handleSearchInputChange"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <el-table
            :data="answers"
            style="width: 100%"
            @row-click="selectAnswer"
            highlight-current-row
            v-loading="loading.list"
            :row-class-name="getRowClassName"
          >
            <el-table-column label="问题" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.questionText }}
              </template>
            </el-table-column>
            <el-table-column label="来源" width="100" prop="source">
              <template #default="{ row }">
                <el-tag :type="getSourceTagType(row.source)">
                  {{ getSourceLabel(row.source) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="totalItems"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="17">
        <!-- 右侧内容区域 -->
        <div v-if="!selectedAnswer" class="empty-state">
          <el-empty description="请从左侧选择一个待评审回答" />
        </div>

        <template v-else>
          <!-- 回答详情 -->
          <el-card class="review-card">
            <template #header>
              <div class="card-header">
                <h2>回答评审</h2>
                <div class="header-actions">
                  <el-button-group>
                    <el-button type="success" @click="approveAnswer" :disabled="reviewing">接受</el-button>
                    <el-button type="danger" @click="rejectAnswer" :disabled="reviewing">拒绝</el-button>
                    <el-button type="warning" @click="flagAnswer" :disabled="reviewing">标记</el-button>
                  </el-button-group>
                </div>
              </div>
            </template>

            <div class="answer-details">
              <div class="question-section">
                <h3>问题</h3>
                <div class="question-content">
                  <p>{{ selectedAnswer.questionText }}</p>
                  <div class="question-meta">
                    <el-tag size="small">{{ getQuestionTypeText(selectedAnswer.questionType) }}</el-tag>
                    <el-tag size="small" :type="getDifficultyType(selectedAnswer.difficulty)">
                      {{ getDifficultyText(selectedAnswer.difficulty) }}
                    </el-tag>
                    <el-tag
                      v-for="tag in selectedAnswer.tags"
                      :key="tag"
                      size="small"
                      effect="plain"
                      class="tag-item"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <el-divider />

              <div class="answer-section">
                <h3>回答内容</h3>
                <div class="answer-content">
                  <p v-if="selectedAnswer.source === 'EXPERT' || selectedAnswer.source === 'AUTO'">
                    {{ selectedAnswer.candidateAnswerText }}
                  </p>
                  <p v-else>{{ selectedAnswer.answerText }}</p>
                  <div class="answer-meta">
                    <span>
                      <strong>提交者:</strong> {{ selectedAnswer.submitterName || '未知' }}
                    </span>
                    <span>
                      <strong>提交时间:</strong> {{ formatDate(selectedAnswer.submissionTime) }}
                    </span>
                  </div>
                </div>
              </div>

              <el-divider />

              <div class="review-section">
                <h3>评审信息</h3>
                <el-form :model="reviewForm" label-position="top">
                  <el-form-item label="评分 (1-10分)">
                    <el-rate
                      v-model="reviewForm.score"
                      :max="10"
                      show-score
                      score-template="{value}"
                    />
                  </el-form-item>
                  <el-form-item label="评审反馈">
                    <el-input
                      v-model="reviewForm.feedback"
                      type="textarea"
                      :rows="4"
                      placeholder="请输入评审反馈信息..."
                    />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-card>

          <!-- 参考回答 -->
          <el-card class="reference-card">
            <template #header>
              <div class="card-header">
                <h3>参考回答</h3>
                <el-switch
                  v-model="showReference"
                  active-text="显示参考"
                  inactive-text="隐藏参考"
                />
              </div>
            </template>

            <div v-if="showReference" class="reference-content">
              <div v-if="!referenceAnswer" class="empty-reference">
                <el-empty description="暂无参考回答" />
              </div>
              <div v-else>
                <p>{{ referenceAnswer.content }}</p>
                <div class="reference-meta">
                  <p><strong>提供者:</strong> {{ referenceAnswer.provider }}</p>
                  <p><strong>最后更新:</strong> {{ formatDate(referenceAnswer.updatedAt) }}</p>
                </div>
              </div>
            </div>
            <div v-else class="reference-hidden">
              <el-empty description="参考回答已隐藏" />
            </div>
          </el-card>
        </template>
      </el-col>
    </el-row>

    <!-- 评审确认对话框 -->
    <el-dialog
      v-model="confirmDialogVisible"
      :title="confirmDialogTitle"
      width="30%"
    >
      <span>{{ confirmDialogMessage }}</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview" :loading="reviewing">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { QuestionType, DifficultyLevel } from '@/api/standardData'

// 用户信息
const userStore = useUserStore()
const currentUser = computed(() => userStore.currentUser)

// 列表状态
const answers = ref([])
const loading = reactive({
  list: false,
  detail: false,
  submit: false
})
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const filterType = ref('ALL')
const searchQuery = ref('')
const searchTimeout = ref(null)

// 选中的回答
const selectedAnswer = ref(null)
const showReference = ref(true)
const referenceAnswer = ref(null)

// 评审表单
const reviewForm = reactive({
  score: 0,
  feedback: ''
})

// 评审确认对话框
const confirmDialogVisible = ref(false)
const confirmDialogTitle = ref('')
const confirmDialogMessage = ref('')
const confirmAction = ref('')
const reviewing = ref(false)

// 初始化
onMounted(() => {
  loadAnswers()
})

// 加载回答列表
const loadAnswers = async () => {
  if (!currentUser.value) return

  loading.list = true

  try {
    // 这里应该调用API获取数据
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 800))

    answers.value = Array.from({ length: 20 }, (_, i) => ({
      id: i + 1,
      questionId: 100 + i,
      questionText: `这是一个${i % 3 === 0 ? '简单' : i % 3 === 1 ? '中等' : '困难'}的${
        i % 4 === 0 ? '单选题' : i % 4 === 1 ? '多选题' : i % 4 === 2 ? '简单事实题' : '主观题'
      }问题示例 ${i + 1}`,
      questionType: i % 4 === 0 ? 'SINGLE_CHOICE' : i % 4 === 1 ? 'MULTIPLE_CHOICE' : i % 4 === 2 ? 'SIMPLE_FACT' : 'SUBJECTIVE',
      difficulty: i % 3 === 0 ? 'EASY' : i % 3 === 1 ? 'MEDIUM' : 'HARD',
      tags: [`标签${i % 5 + 1}`, `标签${i % 3 + 6}`],
      source: i % 3 === 0 ? 'CROWDSOURCED' : i % 3 === 1 ? 'EXPERT' : 'AUTO',
      status: 'PENDING',
      submitterName: `用户${i + 100}`,
      submissionTime: new Date(Date.now() - i * 86400000).toISOString(),
      answerText: i % 3 === 0 ? `这是来自众包用户的回答内容示例 ${i + 1}。包含详细解释和必要的信息。` : undefined,
      candidateAnswerText: i % 3 !== 0 ? `这是来自${i % 3 === 1 ? '专家' : 'AI'}的回答内容示例 ${i + 1}。包含详细解释和必要的信息。` : undefined
    }))

    totalItems.value = 50
  } catch (error) {
    console.error('加载回答失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.list = false
  }
}

// 选择回答
const selectAnswer = (row) => {
  selectedAnswer.value = row
  resetReviewForm()
  loadReferenceAnswer(row.questionId)
}

// 加载参考回答
const loadReferenceAnswer = async (questionId) => {
  loading.detail = true

  try {
    // 这里应该调用API获取数据
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))

    // 随机返回参考回答或null
    if (Math.random() > 0.3) {
      referenceAnswer.value = {
        id: questionId + 1000,
        content: `这是问题 #${questionId} 的参考标准回答。包含详细解释、示例和必要的信息，确保用户能够理解问题的解决方案。`,
        provider: '系统管理员',
        updatedAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 86400000).toISOString()
      }
    } else {
      referenceAnswer.value = null
    }
  } catch (error) {
    console.error('加载参考回答失败:', error)
    ElMessage.error('加载参考回答失败')
    referenceAnswer.value = null
  } finally {
    loading.detail = false
  }
}

// 重置评审表单
const resetReviewForm = () => {
  reviewForm.score = 0
  reviewForm.feedback = ''
}

// 审批回答 - 接受
const approveAnswer = () => {
  confirmDialogTitle.value = '确认接受回答'
  confirmDialogMessage.value = '您确定要接受这个回答吗？'
  confirmAction.value = 'APPROVE'
  confirmDialogVisible.value = true
}

// 审批回答 - 拒绝
const rejectAnswer = () => {
  if (!reviewForm.feedback) {
    ElMessage.warning('拒绝回答时必须提供反馈意见')
    return
  }

  confirmDialogTitle.value = '确认拒绝回答'
  confirmDialogMessage.value = '您确定要拒绝这个回答吗？'
  confirmAction.value = 'REJECT'
  confirmDialogVisible.value = true
}

// 审批回答 - 标记
const flagAnswer = () => {
  confirmDialogTitle.value = '确认标记回答'
  confirmDialogMessage.value = '您确定要标记这个回答以便进一步审核吗？'
  confirmAction.value = 'FLAG'
  confirmDialogVisible.value = true
}

// 提交评审
const submitReview = async () => {
  if (!selectedAnswer.value) return

  reviewing.value = true

  try {
    // 这里应该调用API提交评审
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 更新本地数据
    const index = answers.value.findIndex(a => a.id === selectedAnswer.value.id)
    if (index !== -1) {
      const newStatus =
        confirmAction.value === 'APPROVE' ? 'APPROVED' :
        confirmAction.value === 'REJECT' ? 'REJECTED' : 'FLAGGED'

      answers.value[index].status = newStatus
    }

    // 显示成功消息
    const actionMap = {
      'APPROVE': '接受',
      'REJECT': '拒绝',
      'FLAG': '标记'
    }
    ElMessage.success(`已成功${actionMap[confirmAction.value]}回答`)

    // 关闭对话框并重置表单
    confirmDialogVisible.value = false

    // 可选：如果要在评审后自动选择下一个回答
    if (answers.value.length > 0) {
      const nextIndex = Math.min(index + 1, answers.value.length - 1)
      if (nextIndex >= 0 && nextIndex < answers.value.length) {
        selectAnswer(answers.value[nextIndex])
      } else {
        selectedAnswer.value = null
        resetReviewForm()
      }
    }
  } catch (error) {
    console.error('提交评审失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    reviewing.value = false
  }
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  loadAnswers()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadAnswers()
}

// 搜索处理
const handleSearchInputChange = () => {
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }

  searchTimeout.value = setTimeout(() => {
    loadAnswers()
  }, 500)
}

// 获取行样式
const getRowClassName = ({ row }) => {
  if (row.status === 'APPROVED') return 'row-approved'
  if (row.status === 'REJECTED') return 'row-rejected'
  if (row.status === 'FLAGGED') return 'row-flagged'
  return ''
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知'

  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取问题类型文本
const getQuestionTypeText = (type) => {
  const typeMap = {
    [QuestionType.SINGLE_CHOICE]: '单选题',
    [QuestionType.MULTIPLE_CHOICE]: '多选题',
    [QuestionType.SIMPLE_FACT]: '简单事实题',
    [QuestionType.SUBJECTIVE]: '主观题'
  }
  return typeMap[type] || type
}

// 获取难度文本
const getDifficultyText = (difficulty) => {
  const difficultyMap = {
    [DifficultyLevel.EASY]: '简单',
    [DifficultyLevel.MEDIUM]: '中等',
    [DifficultyLevel.HARD]: '困难'
  }
  return difficultyMap[difficulty] || difficulty
}

// 获取难度标签类型
const getDifficultyType = (difficulty) => {
  const typeMap = {
    [DifficultyLevel.EASY]: 'success',
    [DifficultyLevel.MEDIUM]: 'warning',
    [DifficultyLevel.HARD]: 'danger'
  }
  return typeMap[difficulty] || ''
}

// 获取来源标签
const getSourceLabel = (source) => {
  const sourceMap = {
    'CROWDSOURCED': '众包',
    'EXPERT': '专家',
    'AUTO': '自动'
  }
  return sourceMap[source] || source
}

// 获取来源标签类型
const getSourceTagType = (source) => {
  const typeMap = {
    'CROWDSOURCED': 'info',
    'EXPERT': 'success',
    'AUTO': 'warning'
  }
  return typeMap[source] || ''
}

// 获取状态标签
const getStatusLabel = (status) => {
  const statusMap = {
    'PENDING': '待审核',
    'APPROVED': '已接受',
    'REJECTED': '已拒绝',
    'FLAGGED': '已标记'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'FLAGGED': 'warning'
  }
  return typeMap[status] || ''
}
</script>

<style scoped>
.answer-review {
  padding: 20px;
}

.list-card, .review-card, .reference-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-bar {
  margin-bottom: 15px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.question-section, .answer-section, .review-section {
  margin-bottom: 20px;
}

.question-content, .answer-content {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
}

.question-meta, .answer-meta, .reference-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  color: #666;
}

.tag-item {
  margin-right: 5px;
}

.reference-content {
  padding: 15px;
}

.reference-hidden, .empty-reference {
  padding: 20px;
  text-align: center;
}

/* 行状态样式 */
:deep(.row-approved) {
  background-color: rgba(103, 194, 58, 0.1);
}

:deep(.row-rejected) {
  background-color: rgba(245, 108, 108, 0.1);
}

:deep(.row-flagged) {
  background-color: rgba(230, 162, 60, 0.1);
}
</style>
