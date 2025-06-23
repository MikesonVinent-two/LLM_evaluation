<template>
  <div class="standard-questions">
    <el-card class="search-card">
      <template #header>
        <div class="card-header">
          <h2>标准问题搜索</h2>
        </div>
      </template>
      <div class="search-content">
        <el-form :inline="true" class="search-form">
          <el-form-item label="关键词">
            <el-input v-model="searchParams.keyword" placeholder="请输入关键词" clearable />
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="searchParams.tags"
              multiple
              filterable
              allow-create
              default-first-option
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择或输入标签，回车确认"
              clearable
              style="width: 240px"
              @keyup.enter="handleTagEnter"
            >
              <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchQuestions">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <h2>标准问题列表</h2>
          <el-radio-group v-model="filterType" @change="searchQuestions">
            <el-radio-button label="all">全部问题</el-radio-button>
            <el-radio-button label="unanswered">未回答</el-radio-button>
            <el-radio-button label="answered">已回答</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="questions-content">
        <el-empty v-if="loading" description="加载中...">
          <el-icon class="loading-icon"><Loading /></el-icon>
        </el-empty>
        <el-empty v-else-if="questions.length === 0" description="暂无数据" />
        <div v-else class="question-list">
          <el-card v-for="question in questions" :key="question.id" class="question-item" shadow="hover">
            <div class="question-header">
              <h3 class="question-title">{{ question.questionText }}</h3>
              <div class="question-tags">
                <el-tag v-for="tag in question.tags" :key="tag" size="small" class="tag-item">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="question-info">
              <span class="question-type">
                {{ getQuestionTypeLabel(question.questionType) }}
              </span>
              <span class="question-difficulty">
                <el-tag :type="getDifficultyType(question.difficulty)" size="small">
                  {{ getDifficultyLabel(question.difficulty) }}
                </el-tag>
              </span>
              <span class="question-date">{{ formatDate(question.creationTime) }}</span>
            </div>
            <div class="question-status">
              <el-tag v-if="question.hasCrowdsourcedAnswer" type="success" effect="dark" size="small">
                已回答
              </el-tag>
              <el-tag v-else type="info" effect="plain" size="small">
                未回答
              </el-tag>
            </div>
            <div class="question-actions">
              <el-button type="primary" size="small" @click="viewQuestion(question)">查看</el-button>
              <el-button
                type="success"
                size="small"
                @click="answerQuestion(question)"
                :disabled="question.hasCrowdsourcedAnswer"
              >
                {{ question.hasCrowdsourcedAnswer ? '修改回答' : '提交回答' }}
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

    <!-- 查看问题详情对话框 -->
    <el-dialog
      v-model="questionDetailVisible"
      title="问题详情"
      width="60%"
      destroy-on-close
    >
      <template v-if="selectedQuestion">
        <div class="question-detail">
          <h2>{{ selectedQuestion.questionText }}</h2>
          <div class="question-meta">
            <div class="meta-item">
              <span class="meta-label">类型：</span>
              <span class="meta-value">{{ getQuestionTypeLabel(selectedQuestion.questionType) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">难度：</span>
              <span class="meta-value">
                <el-tag :type="getDifficultyType(selectedQuestion.difficulty)" size="small">
                  {{ getDifficultyLabel(selectedQuestion.difficulty) }}
                </el-tag>
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间：</span>
              <span class="meta-value">{{ formatDate(selectedQuestion.creationTime) }}</span>
            </div>
          </div>
          <div class="question-tags-section">
            <span class="meta-label">标签：</span>
            <div class="tags-container">
              <el-tag
                v-for="tag in selectedQuestion.tags"
                :key="tag"
                class="tag-item"
                size="small"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 提交回答对话框 -->
    <el-dialog
      v-model="answerDialogVisible"
      :title="selectedQuestion?.hasCrowdsourcedAnswer ? '修改回答' : '提交回答'"
      width="60%"
      destroy-on-close
    >
      <template v-if="selectedQuestion">
        <div class="question-answer-section">
          <h3>{{ selectedQuestion.questionText }}</h3>
          <el-form :model="answerForm" label-position="top">
            <el-form-item label="您的回答" required>
              <el-input
                v-model="answerForm.answerText"
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
          <el-button @click="answerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAnswer" :loading="submitting">
            {{ selectedQuestion?.hasCrowdsourcedAnswer ? '更新回答' : '提交回答' }}
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
  searchStandardQuestions,
  QuestionType,
  DifficultyLevel
} from '@/api/standardData'
import {
  createCrowdsourcedAnswer,
  updateCrowdsourcedAnswer,
  getCrowdsourcedAnswersByQuestion
} from '@/api/crowdsourcedAnswer'
import { getAllTags } from '@/api/tags'
import type {
  SearchQuestionItem,
  SearchQuestionResponse
} from '@/types/standardQuestion'
import type {
  CreateCrowdsourcedAnswerRequest,
  UpdateCrowdsourcedAnswerRequest,
  CrowdsourcedAnswerResponse
} from '@/types/crowdsourcedAnswer'
import { useUserStore } from '@/stores/user'
import { Loading } from '@element-plus/icons-vue'

// 用户信息
const userStore = useUserStore()
const userId = computed(() => userStore.currentUser?.id || 0)

// 问题列表状态
const questions = ref<SearchQuestionItem[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const filterType = ref('all')
const availableTags = ref<string[]>([])

// 搜索相关
const searchParams = reactive({
  keyword: '',
  tags: [] as string[],
  page: '0',
  size: '10'
})

// 问题详情
const selectedQuestion = ref<SearchQuestionItem | null>(null)
const questionDetailVisible = ref(false)
const answerDialogVisible = ref(false)
const submitting = ref(false)

// 回答表单
const answerForm = reactive({
  answerText: '',
  standardQuestionId: '',
  userId: '',
  answerId: ''
})

// 当前回答
const currentAnswer = ref<CrowdsourcedAnswerResponse | null>(null)

// 初始加载
onMounted(async () => {
  // 获取所有标签
  await fetchAllTags()
  // 搜索问题
  await searchQuestions()
})

// 获取所有标签
async function fetchAllTags() {
  try {
    const response = await getAllTags()
    if (response && Array.isArray(response)) {
      // 从返回的对象数组中提取tagName属性
      availableTags.value = response.map(tag => tag.tagName)
    } else {
      console.error('获取标签列表格式不正确:', response)
      ElMessage.warning('获取标签列表格式不正确')
    }
  } catch (error) {
    console.error('获取标签列表失败:', error)
    ElMessage.warning('获取标签列表失败，将使用默认标签')
  }
}

// 处理标签回车事件
function handleTagEnter(event: KeyboardEvent) {
  // 获取输入内容
  const input = (event.target as HTMLInputElement).value.trim()

  if (input && !searchParams.tags.includes(input)) {
    // 添加新标签
    searchParams.tags.push(input)

    // 如果这是一个新标签，也添加到可用标签列表中
    if (!availableTags.value.includes(input)) {
      availableTags.value.push(input)
    }
  }
}

// 搜索问题
async function searchQuestions() {
  loading.value = true
  try {
    // 准备搜索参数
    const params = {
      keyword: searchParams.keyword,
      tags: searchParams.tags.length > 0 ? searchParams.tags.join(',') : undefined,
      page: (currentPage.value - 1).toString(), // 后端分页从0开始
      size: pageSize.value.toString(),
      userId: userId.value.toString(), // 添加当前用户ID作为参数
      onlyLatest: true // 只返回叶子节点的标准问题
    }

    const response = await searchStandardQuestions(params)
    questions.value = response.questions || []
    totalItems.value = response.total || 0

    // 如果筛选已回答的问题
    if (filterType.value === 'unanswered') {
      questions.value = questions.value.filter(q => !q.hasCrowdsourcedAnswer)
    } else if (filterType.value === 'answered') {
      questions.value = questions.value.filter(q => q.hasCrowdsourcedAnswer)
    }

  } catch (error) {
    console.error('搜索标准问题失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
    questions.value = []
    totalItems.value = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
function resetSearch() {
  searchParams.keyword = ''
  searchParams.tags = []
  filterType.value = 'all'
  currentPage.value = 1
  searchQuestions()
}

// 分页处理
function handleSizeChange(size: number) {
  pageSize.value = size
  searchQuestions()
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  searchQuestions()
}

// 查看问题详情
function viewQuestion(question: SearchQuestionItem) {
  selectedQuestion.value = question
  questionDetailVisible.value = true
}

// 回答问题
async function answerQuestion(question: SearchQuestionItem) {
  selectedQuestion.value = question
  answerForm.standardQuestionId = question.id.toString()
  answerForm.userId = userId.value.toString()
  answerForm.answerText = ''

  // 如果已经回答过，获取已有回答内容
  if (question.hasCrowdsourcedAnswer) {
    try {
      const response = await getCrowdsourcedAnswersByQuestion(
        question.id.toString(),
        { userId: userId.value.toString(), page: '0', size: '1' }
      )

      if (response.content && response.content.length > 0) {
        currentAnswer.value = response.content[0]
        answerForm.answerText = response.content[0].answerText
        answerForm.answerId = response.content[0].id.toString()
      }
    } catch (error) {
      console.error('获取已有回答失败:', error)
      ElMessage.warning('无法获取已有回答，将创建新回答')
    }
  } else {
    currentAnswer.value = null
  }

  answerDialogVisible.value = true
}

// 提交回答
async function submitAnswer() {
  if (!answerForm.answerText.trim()) {
    ElMessage.warning('回答内容不能为空')
    return
  }

  submitting.value = true
  try {
    if (selectedQuestion.value?.hasCrowdsourcedAnswer && currentAnswer.value) {
      // 更新已有回答
      const updateData: UpdateCrowdsourcedAnswerRequest = {
        userId: Number(answerForm.userId),
        answerText: answerForm.answerText,
        standardQuestionId: Number(answerForm.standardQuestionId)
      }

      await updateCrowdsourcedAnswer(answerForm.answerId, updateData)
      ElMessage.success('回答更新成功')
    } else {
      // 创建新回答
      const createData: CreateCrowdsourcedAnswerRequest = {
        standardQuestionId: answerForm.standardQuestionId,
        userId: answerForm.userId,
        answerText: answerForm.answerText
      }

      await createCrowdsourcedAnswer(createData)
      ElMessage.success('回答提交成功')
    }

    // 关闭对话框并刷新问题列表
    answerDialogVisible.value = false
    await searchQuestions()
  } catch (error) {
    console.error('提交回答失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
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

// 获取问题类型显示名称
function getQuestionTypeLabel(type: string) {
  const typeMap: Record<string, string> = {
    [QuestionType.SINGLE_CHOICE]: '单选题',
    [QuestionType.MULTIPLE_CHOICE]: '多选题',
    [QuestionType.SIMPLE_FACT]: '简单事实题',
    [QuestionType.SUBJECTIVE]: '主观题'
  }
  return typeMap[type] || type
}

// 获取难度显示名称
function getDifficultyLabel(difficulty: string) {
  const difficultyMap: Record<string, string> = {
    [DifficultyLevel.EASY]: '简单',
    [DifficultyLevel.MEDIUM]: '中等',
    [DifficultyLevel.HARD]: '困难'
  }
  return difficultyMap[difficulty] || difficulty
}

// 获取难度标签样式类型
function getDifficultyType(difficulty: string) {
  const typeMap: Record<string, string> = {
    [DifficultyLevel.EASY]: 'success',
    [DifficultyLevel.MEDIUM]: 'warning',
    [DifficultyLevel.HARD]: 'danger'
  }
  return typeMap[difficulty] || 'info'
}
</script>

<style scoped>
.standard-questions {
  padding: 20px;
}

.search-card,
.questions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-content,
.questions-content {
  padding: 20px 0;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  position: relative;
}

.question-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.question-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-item {
  margin-right: 5px;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.question-status {
  position: absolute;
  top: 16px;
  right: 16px;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.question-detail {
  padding: 0 20px;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin: 20px 0;
}

.meta-item {
  display: flex;
  align-items: center;
}

.meta-label {
  font-weight: bold;
  margin-right: 8px;
  color: #666;
}

.question-tags-section {
  margin-top: 16px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
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
