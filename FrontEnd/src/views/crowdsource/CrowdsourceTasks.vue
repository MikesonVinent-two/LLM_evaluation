<template>
  <div class="crowdsource-answers">
    <el-card class="answers-card">
      <template #header>
        <div class="card-header">
          <h2>我的众包回答</h2>
          <div class="header-actions">
            <el-select v-model="statusFilter" placeholder="状态筛选" @change="loadAnswers">
              <el-option label="全部" value="" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已接受" value="ACCEPTED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已标记" value="FLAGGED" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="answers-content">
        <el-empty v-if="loading" description="加载中...">
          <el-icon class="loading-icon"><Loading /></el-icon>
        </el-empty>
        <el-empty v-else-if="myAnswers.length === 0" description="暂无回答记录" />
        <div v-else class="answers-list">
          <el-card v-for="answer in myAnswers" :key="answer.id" class="answer-item" shadow="hover">
            <div class="answer-header">
              <h3 class="answer-title">{{ getQuestionText(answer.standardQuestionId) }}</h3>
              <el-tag :type="getStatusType(answer.qualityReviewStatus)" effect="dark" size="small">
                {{ getStatusLabel(answer.qualityReviewStatus) }}
              </el-tag>
            </div>
            <div class="answer-content">
              <p class="answer-text">{{ answer.answerText }}</p>
            </div>
            <div class="answer-meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                提交时间: {{ formatDate(answer.submissionTime) }}
              </span>
              <span v-if="answer.reviewTime" class="meta-item">
                <el-icon><Timer /></el-icon>
                审核时间: {{ formatDate(answer.reviewTime) }}
              </span>
            </div>
            <div v-if="answer.reviewFeedback" class="answer-feedback">
              <p class="feedback-label">审核反馈:</p>
              <p class="feedback-content">{{ answer.reviewFeedback }}</p>
            </div>
            <div class="answer-actions">
              <el-button
                type="primary"
                size="small"
                @click="editAnswer(answer)"
                :disabled="answer.qualityReviewStatus !== 'PENDING'"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="confirmDeleteAnswer(answer)"
                :disabled="answer.qualityReviewStatus !== 'PENDING'"
              >
                删除
              </el-button>
            </div>
          </el-card>
        </div>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalItems"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 编辑回答对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑回答"
      width="60%"
      destroy-on-close
    >
      <template v-if="selectedAnswer">
        <div class="edit-answer-section">
          <h3>{{ getQuestionText(selectedAnswer.standardQuestionId) }}</h3>
          <el-form :model="editForm" label-position="top">
            <el-form-item label="您的回答" required>
              <el-input
                v-model="editForm.answerText"
                type="textarea"
                :rows="8"
                placeholder="请输入您的回答..."
              />
            </el-form-item>
          </el-form>
        </div>
      </template>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateAnswer" :loading="submitting">
            更新回答
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="30%"
    >
      <p>确定要删除此回答吗？此操作无法撤销。</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="deleteAnswer" :loading="submitting">
            确认删除
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCrowdsourcedAnswersByUser,
  updateCrowdsourcedAnswer,
  deleteCrowdsourcedAnswer
} from '@/api/crowdsourcedAnswer'
import { searchStandardQuestions } from '@/api/standardData'
import type {
  CrowdsourcedAnswerResponse,
  UpdateCrowdsourcedAnswerRequest
} from '@/types/crowdsourcedAnswer'
import { QualityReviewStatus } from '@/types/crowdsourcedAnswer'
import { useUserStore } from '@/stores/user'
import { Loading, Calendar, Timer } from '@element-plus/icons-vue'

// 用户信息
const userStore = useUserStore()
const userId = computed(() => userStore.currentUser?.id || 0)

// 回答列表状态
const myAnswers = ref<CrowdsourcedAnswerResponse[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const statusFilter = ref('')
const questionTextMap = ref<Record<number, string>>({})

// 编辑对话框
const editDialogVisible = ref(false)
const selectedAnswer = ref<CrowdsourcedAnswerResponse | null>(null)
const editForm = reactive({
  answerText: '',
})
const submitting = ref(false)

// 删除对话框
const deleteDialogVisible = ref(false)

// 初始加载
onMounted(async () => {
  await loadAnswers()
})

// 加载用户的众包回答
async function loadAnswers() {
  if (!userId.value) return

  loading.value = true
  try {
    const params = {
      page: (currentPage.value - 1).toString(), // 后端分页从0开始
      size: pageSize.value.toString(),
      status: statusFilter.value || undefined
    }

    const response = await getCrowdsourcedAnswersByUser(userId.value.toString(), params)
    myAnswers.value = response.content || []
    totalItems.value = response.totalElements || 0

    // 获取问题文本
    await loadQuestionTexts()
  } catch (error) {
    console.error('加载回答失败:', error)
    ElMessage.error('加载失败，请稍后重试')
    myAnswers.value = []
    totalItems.value = 0
  } finally {
    loading.value = false
  }
}

// 加载问题文本
async function loadQuestionTexts() {
  if (myAnswers.value.length === 0) return

  const questionIds = [...new Set(myAnswers.value.map(a => a.standardQuestionId))]

  try {
    for (const questionId of questionIds) {
      if (!questionTextMap.value[questionId]) {
        // 这里可以改为批量获取，为简化代码使用单个查询
        const response = await searchStandardQuestions({
          keyword: '',
          page: '0',
          size: '1',
          userId: userId.value.toString(), // 添加用户ID参数
          onlyLatest: true // 只返回叶子节点的标准问题
        })

        const question = response.data.questions.find((q) => q.id === questionId)
        if (question) {
          questionTextMap.value[questionId] = question.questionText
        } else {
          questionTextMap.value[questionId] = `问题 #${questionId}`
        }
      }
    }
  } catch (error) {
    console.error('获取问题文本失败:', error)
  }
}

// 获取问题文本
function getQuestionText(questionId: number) {
  return questionTextMap.value[questionId] || `问题 #${questionId}`
}

// 编辑回答
function editAnswer(answer: CrowdsourcedAnswerResponse) {
  selectedAnswer.value = answer
  editForm.answerText = answer.answerText
  editDialogVisible.value = true
}

// 更新回答
async function updateAnswer() {
  if (!selectedAnswer.value || !editForm.answerText.trim()) {
    ElMessage.warning('回答内容不能为空')
    return
  }

  submitting.value = true
  try {
    const updateData: UpdateCrowdsourcedAnswerRequest = {
      userId: userId.value,
      answerText: editForm.answerText,
      standardQuestionId: selectedAnswer.value.standardQuestionId
    }

    await updateCrowdsourcedAnswer(selectedAnswer.value.id.toString(), updateData)
    ElMessage.success('回答更新成功')

    // 关闭对话框并刷新列表
    editDialogVisible.value = false
    await loadAnswers()
  } catch (error) {
    console.error('更新回答失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 确认删除回答
function confirmDeleteAnswer(answer: CrowdsourcedAnswerResponse) {
  selectedAnswer.value = answer
  deleteDialogVisible.value = true
}

// 删除回答
async function deleteAnswer() {
  if (!selectedAnswer.value) return

  submitting.value = true
  try {
    await deleteCrowdsourcedAnswer(selectedAnswer.value.id.toString(), userId.value.toString())
    ElMessage.success('回答删除成功')

    // 关闭对话框并刷新列表
    deleteDialogVisible.value = false
    await loadAnswers()
  } catch (error) {
    console.error('删除回答失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 分页处理
function handleSizeChange(size: number) {
  pageSize.value = size
  loadAnswers()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  loadAnswers()
}

// 格式化日期
function formatDate(dateString: string) {
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

// 获取状态显示名称
function getStatusLabel(status: string) {
  const statusMap: Record<string, string> = {
    [QualityReviewStatus.PENDING]: '待审核',
    [QualityReviewStatus.ACCEPTED]: '已接受',
    [QualityReviewStatus.REJECTED]: '已拒绝',
    [QualityReviewStatus.FLAGGED]: '已标记'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
function getStatusType(status: string) {
  const typeMap: Record<string, string> = {
    [QualityReviewStatus.PENDING]: 'info',
    [QualityReviewStatus.ACCEPTED]: 'success',
    [QualityReviewStatus.REJECTED]: 'danger',
    [QualityReviewStatus.FLAGGED]: 'warning'
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped>
.crowdsource-answers {
  padding: 20px;
}

.answers-card {
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

.answers-content {
  padding: 20px 0;
}

.answers-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.answer-item {
  position: relative;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.answer-title {
  margin: 0;
  font-size: 18px;
  color: #333;
  flex: 1;
}

.answer-content {
  margin-bottom: 16px;
}

.answer-text {
  margin: 0;
  color: #555;
  line-height: 1.6;
  white-space: pre-line;
}

.answer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.answer-feedback {
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.feedback-label {
  font-weight: bold;
  margin: 0 0 4px 0;
  color: #555;
}

.feedback-content {
  margin: 0;
  color: #666;
  white-space: pre-line;
}

.answer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.loading-icon {
  font-size: 24px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
