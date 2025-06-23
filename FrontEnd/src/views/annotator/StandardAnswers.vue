<template>
  <div class="standard-answers">
    <el-row :gutter="20">
      <el-col :span="6">
        <!-- 左侧未回答标准问题列表 -->
        <el-card class="left-panel">
      <template #header>
            <div class="panel-header">
              <h3>未回答标准问题</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索问题..."
                prefix-icon="el-icon-search"
                clearable
                style="width: 180px; margin-left: 10px;"
              />
            </div>
          </template>

          <div class="filter-bar">
            <el-select v-model="filterQuestionType" placeholder="题型筛选" style="width: 150px; margin-right: 10px;">
              <el-option label="全部" value="" />
              <el-option label="单选题" :value="QuestionType.SINGLE_CHOICE" />
              <el-option label="多选题" :value="QuestionType.MULTIPLE_CHOICE" />
              <el-option label="简单事实题" :value="QuestionType.SIMPLE_FACT" />
              <el-option label="主观题" :value="QuestionType.SUBJECTIVE" />
            </el-select>

            <el-switch
              v-model="onlyLatest"
              active-text="仅显示最新版本"
              inactive-text="显示所有版本"
              @change="handleOnlyLatestChange"
            />
          </div>

          <el-table
            :data="filteredQuestions"
            style="width: 100%"
            @row-click="handleQuestionClick"
            border
            highlight-current-row
            v-loading="loading.questions"
          >
            <el-table-column label="问题文本" prop="questionText" show-overflow-tooltip />
            <el-table-column label="题型" width="90">
              <template #default="{ row }">
                {{ getQuestionTypeText(row.questionType) }}
              </template>
            </el-table-column>
            <el-table-column label="难度" width="70">
              <template #default="{ row }">
                <el-tag :type="getDifficultyTagType(row.difficulty)">
                  {{ getDifficultyText(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              layout="prev, pager, next"
              :total="totalQuestions"
              :page-size="pageSize"
              :current-page="currentPage"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="18">
        <!-- 右侧内容区域 -->
        <div v-if="!selectedQuestion" class="empty-state">
          <el-empty description="请从左侧选择一个标准问题" />
        </div>

        <template v-else>
          <!-- 标准问题详情 -->
          <el-card class="question-card">
            <template #header>
              <div class="panel-header">
                <h3>标准问题详情</h3>
                <el-tag :type="getDifficultyTagType(selectedQuestion.difficulty)">
                  {{ getDifficultyText(selectedQuestion.difficulty) }}
                </el-tag>
              </div>
            </template>

            <div class="question-details">
              <p><strong>问题：</strong> {{ selectedQuestion.questionText }}</p>
              <p><strong>题型：</strong> {{ getQuestionTypeText(selectedQuestion.questionType) }}</p>
              <p>
                <strong>标签：</strong>
                <el-tag
                  v-for="tag in selectedQuestion.tags"
                  :key="tag"
                  style="margin-right: 5px;"
                  size="small"
                >
                  {{ tag }}
                </el-tag>
              </p>
            </div>
          </el-card>

          <!-- 三栏对比区域 -->
          <el-card class="comparison-card">
            <template #header>
              <div class="panel-header">
                <h3>回答参考</h3>
              </div>
            </template>

            <el-tabs type="border-card">
              <el-tab-pane label="原始回答">
                <div v-loading="loading.rawAnswers">
                  <div v-if="rawAnswers.length === 0" class="empty-answers">
                    <el-empty description="暂无原始回答" />
                  </div>

                  <el-collapse v-else accordion>
                    <el-collapse-item
                      v-for="(answer, index) in rawAnswers"
                      :key="`raw-${answer.id}`"
                      :title="`回答 ${index + 1} - ${answer.respondent || '未知回答者'}`"
                      :name="index"
                    >
                      <div class="answer-content">
                        <p>{{ answer.answerText }}</p>
                        <div class="answer-meta">
                          <p><strong>回答时间：</strong> {{ formatDate(answer.answerTime) }}</p>
                          <p v-if="answer.score !== undefined"><strong>分数：</strong> {{ answer.score }}</p>
                          <el-button
                            type="primary"
                            size="small"
                            @click="copyToEditor(answer.answerText)"
                            plain
                          >
                            引用到编辑器
                          </el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-tab-pane>

              <el-tab-pane label="众包回答">
                <div v-loading="loading.crowdsourcedAnswers">
                  <div v-if="crowdsourcedAnswers.length === 0" class="empty-answers">
                    <el-empty description="暂无众包回答" />
                  </div>

                  <el-collapse v-else accordion>
                    <el-collapse-item
                      v-for="(answer, index) in crowdsourcedAnswers"
                      :key="`crowd-${answer.id}`"
                      :title="`回答 ${index + 1} - ${answer.userUsername || '未知用户'}`"
                      :name="index"
                    >
                      <div class="answer-content">
                        <p>{{ answer.answerText }}</p>
                        <div class="answer-meta">
                          <p><strong>提交时间：</strong> {{ formatDate(answer.submissionTime) }}</p>
                          <p><strong>审核状态：</strong> {{ getReviewStatusText(answer.qualityReviewStatus) }}</p>
                          <p v-if="answer.reviewFeedback"><strong>反馈：</strong> {{ answer.reviewFeedback }}</p>
                          <el-button
                            type="primary"
                            size="small"
                            @click="copyToEditor(answer.answerText)"
                            plain
                          >
                            引用到编辑器
                          </el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-tab-pane>

              <el-tab-pane label="专家回答">
                <div v-loading="loading.expertAnswers">
                  <div v-if="expertAnswers.length === 0" class="empty-answers">
                    <el-empty description="暂无专家回答" />
                  </div>

                  <el-collapse v-else accordion>
                    <el-collapse-item
                      v-for="(answer, index) in expertAnswers"
                      :key="`expert-${answer.id}`"
                      :title="`回答 ${index + 1} - ${answer.userUsername || '未知专家'}`"
                      :name="index"
                    >
                      <div class="answer-content">
                        <p>{{ answer.candidateAnswerText }}</p>
                        <div class="answer-meta">
                          <p><strong>提交时间：</strong> {{ formatDate(answer.submissionTime) }}</p>
                          <p v-if="answer.qualityScore"><strong>质量评分：</strong> {{ answer.qualityScore }}</p>
                          <p v-if="answer.feedback"><strong>反馈：</strong> {{ answer.feedback }}</p>
                          <el-button
                            type="primary"
                            size="small"
                            @click="copyToEditor(answer.candidateAnswerText)"
                            plain
                          >
                            引用到编辑器
                          </el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>

          <!-- 标准答案编辑区域 -->
          <el-card class="editor-card">
            <template #header>
              <div class="panel-header">
                <h3>标准答案编辑</h3>
                <el-button type="primary" @click="submitStandardAnswer">提交标准答案</el-button>
              </div>
            </template>

            <div class="editor-content">
              <el-form label-position="top">
                <el-form-item label="提交说明 (简要说明此答案的特点或与其他答案的区别)">
                  <el-input v-model="answerForm.commitMessage" placeholder="例如：这是一个详细的解答，包含了完整的推导过程和结果解释" />
                </el-form-item>
              </el-form>

              <!-- 根据题型显示不同的编辑器 -->
              <div v-if="selectedQuestion.questionType === QuestionType.SINGLE_CHOICE">
                <SingleChoiceEditor
                  v-model:options="answerForm.options"
                  v-model:correctIds="answerForm.correctIds"
                  v-model:answerText="answerForm.answerText"
                  ref="answerEditorRef"
                />
              </div>

              <div v-else-if="selectedQuestion.questionType === QuestionType.MULTIPLE_CHOICE">
                <MultipleChoiceEditor
                  v-model:options="answerForm.options"
                  v-model:correctIds="answerForm.correctIds"
                  v-model:answerText="answerForm.answerText"
                  ref="answerEditorRef"
                />
              </div>

              <div v-else-if="selectedQuestion.questionType === QuestionType.SIMPLE_FACT">
                <SimpleFactEditor
                  v-model:alternativeAnswers="answerForm.alternativeAnswers"
                  v-model:answerText="answerForm.answerText"
                  ref="answerEditorRef"
                />
              </div>

              <div v-else-if="selectedQuestion.questionType === QuestionType.SUBJECTIVE">
                <SubjectiveEditor
                  v-model:answerText="answerForm.answerText"
                  v-model:scoringGuidance="answerForm.scoringGuidance"
                  ref="answerEditorRef"
                />
              </div>
            </div>
          </el-card>
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, markRaw, onUnmounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import type { TableInstance } from 'element-plus'
import { Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'

// 导入编辑器组件
import SingleChoiceEditor from './standard-answers/SingleChoiceEditor.vue'
import MultipleChoiceEditor from './standard-answers/MultipleChoiceEditor.vue'
import SimpleFactEditor from './standard-answers/SimpleFactEditor.vue'
import SubjectiveEditor from './standard-answers/SubjectiveEditor.vue'

// 导入相关API
import { getQuestionsWithoutAnswers, getQuestionOriginalData, createStandardAnswer, QuestionType, getStandardQuestions } from '@/api/standardData'
import { getCrowdsourcedAnswersByQuestion } from '@/api/crowdsourcedAnswer'
import { getExpertAnswersByQuestion } from '@/api/expertAnswer'
import { websocketService } from '@/services/websocket'
import { WebSocketMessageType } from '@/types/websocketTypes'

// 引入接口类型
import type { ChoiceAnswerDto, SimpleFactAnswerDto, SubjectiveAnswerDto } from '@/api/standardData'

// 外部组件和状态
const router = useRouter()
const searchQuery = ref('')
const tableData = ref<any[]>([])
const questionTypes = ['简答题', '单选题', '多选题', '判断题']
const tableRef = ref<TableInstance>()
const pageSize = ref(10)
const currentPage = ref(1)
const total = ref(0)

// WebSocket相关状态和处理
let removeConnectionListener: (() => void) | null = null
const websocketConnected = ref(false)
const realTimeUpdates = ref(true)

// 状态定义
const loading = reactive({
  questions: false,
  rawAnswers: false,
  crowdsourcedAnswers: false,
  expertAnswers: false,
  submitting: false
})
const filterQuestionType = ref('')
const onlyLatest = ref(true) // 默认只显示最新版本
const questions = ref<any[]>([])
const selectedQuestion = ref<any>(null)
const rawAnswers = ref<any[]>([])
const crowdsourcedAnswers = ref<any[]>([])
const expertAnswers = ref<any[]>([])
const totalQuestions = ref(0)
const answerEditorRef = ref<any>(null)

// 答案表单数据
const answerForm = reactive({
  standardQuestionId: 0,
  userId: 1, // 应该从用户状态获取
  questionType: '',
  answerText: '',
  commitMessage: '',
  options: [] as any[],
  correctIds: [] as (number | string)[],
  alternativeAnswers: [] as string[],
  scoringGuidance: ''
})

// 过滤标准问题
const filteredQuestions = computed(() => {
  if (!searchQuery.value && !filterQuestionType.value) return questions.value

  return questions.value.filter(question => {
    const matchesSearch = !searchQuery.value ||
      question.questionText.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesType = !filterQuestionType.value ||
      question.questionType === filterQuestionType.value

    return matchesSearch && matchesType
  })
})

// 获取标准问题数据
const fetchData = async () => {
  loading.questions = true
  try {
    const params = {
      page: (currentPage.value - 1).toString(), // 后端分页从0开始
      size: pageSize.value.toString(),
      onlyLatest: onlyLatest.value, // 是否只获取最新版本的问题
      onlyLatestVersion: true
    }

    const response = await getQuestionsWithoutAnswers(params)

    console.log('response:', response)

    if (response && response.success) {
      questions.value = response.questions || []
      totalQuestions.value = response.total || 0
    } else {
      questions.value = []
      totalQuestions.value = 0
      console.error('获取未回答标准问题响应格式不符合预期:', response)
    }
  } catch (error) {
    console.error('获取标准问题失败:', error)
    ElMessage.error('获取标准问题失败')
    questions.value = []
    totalQuestions.value = 0
  } finally {
    loading.questions = false
  }
}

// 刷新数据
const refreshData = () => {
  fetchData()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// 处理页面变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchData()
}

// 处理每页数量变化
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  fetchData()
}

// WebSocket连接状态变更处理
const handleConnectionStateChange = (status: string) => {
  websocketConnected.value = status === 'CONNECTED'
  console.log('WebSocket连接状态:', status)
}

// 处理WebSocket接收到的消息
const handleWebSocketMessage = (message: any) => {
  // 仅当启用实时更新时处理
  if (!realTimeUpdates.value) return

  // 根据消息类型处理不同的业务逻辑
  if (message.type === WebSocketMessageType.STATUS_CHANGE &&
      message.data && message.data.entity === 'standardQuestion') {
    // 标准问题状态变更，刷新数据
    ElMessage.info({
      message: `收到标准问题更新通知: ${message.data.message || '数据已更新'}`,
      duration: 3000
    })
    // 延迟刷新数据，避免频繁刷新
    setTimeout(() => {
      refreshData()
    }, 300)
  }
}

// 根据问题类型获取图标
const getTypeIcon = (type: string) => {
  switch (type) {
    case '简答题':
      return 'el-icon-document-checked'
    case '单选题':
      return 'el-icon-select'
    case '多选题':
      return 'el-icon-finished'
    case '判断题':
      return 'el-icon-check'
    default:
      return 'el-icon-document'
  }
}

// 这个函数会与导入的API函数冲突，重命名为navigateToCreateAnswer
const navigateToCreateAnswer = (questionId: number) => {
  // 如果需要导航到创建答案页面，应该确保路由存在
  ElMessage.warning('创建标准答案功能已集成到当前页面')
}

// 启用或禁用实时更新
const toggleRealTimeUpdates = () => {
  realTimeUpdates.value = !realTimeUpdates.value
  ElMessage.info(realTimeUpdates.value ? '已启用实时更新' : '已禁用实时更新')
}

// 删除标准问题
const deleteQuestion = (questionId: number) => {
  ElMessageBox.confirm(
    '此操作将永久删除该标准问题, 是否继续?',
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        // 调用删除API
        // await deleteStandardQuestion(questionId)
        ElMessage.success('删除成功')
        fetchData() // 刷新数据
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

// 生命周期钩子
onMounted(async () => {
  // 获取初始数据
  await fetchData()

  // 检查并确保WebSocket连接
  const connected = await websocketService.checkConnectionAndReconnect()
  websocketConnected.value = connected

  // 添加WebSocket连接状态监听器
  removeConnectionListener = websocketService.addConnectionListener(handleConnectionStateChange)

  // 订阅标准问题更新主题
  websocketService.subscribeToQueue('standard-questions', handleWebSocketMessage)
})

// 组件卸载前
onBeforeUnmount(() => {
  // 取消订阅并移除监听器
  websocketService.unsubscribeFromQueue('standard-questions')
  if (removeConnectionListener) {
    removeConnectionListener()
  }
})

// 处理"只显示最新版本"开关变化
const handleOnlyLatestChange = () => {
  fetchData() // 重新获取数据
}

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 获取题型文本
const getQuestionTypeText = (type: string) => {
  switch (type) {
    case QuestionType.SINGLE_CHOICE:
      return '单选题'
    case QuestionType.MULTIPLE_CHOICE:
      return '多选题'
    case QuestionType.SIMPLE_FACT:
      return '简单事实题'
    case QuestionType.SUBJECTIVE:
      return '主观题'
    default:
      return '未知'
  }
}

// 获取难度文本
const getDifficultyText = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY':
      return '简单'
    case 'MEDIUM':
      return '中等'
    case 'HARD':
      return '困难'
    default:
      return '未知'
  }
}

// 获取难度标签类型
const getDifficultyTagType = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY':
      return 'success'
    case 'MEDIUM':
      return 'warning'
    case 'HARD':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取审核状态文本
const getReviewStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已拒绝'
    default:
      return '未知'
  }
}

// 处理标准问题点击
const handleQuestionClick = async (row: any) => {
  selectedQuestion.value = row

  // 重置答案表单
  resetAnswerForm(row)

  // 获取相关回答数据
  await Promise.all([
    fetchOriginalData(row.id),
    fetchCrowdsourcedAnswers(row.id),
    fetchExpertAnswers(row.id)
  ])
}

// 重置答案表单
const resetAnswerForm = (question: any) => {
  answerForm.standardQuestionId = question.id
  answerForm.questionType = question.questionType
  answerForm.answerText = ''
  answerForm.commitMessage = ''
  answerForm.options = []
  answerForm.correctIds = []
  answerForm.alternativeAnswers = []
  answerForm.scoringGuidance = ''

  // 为选择题添加默认选项
  if (question.questionType === QuestionType.SINGLE_CHOICE || question.questionType === QuestionType.MULTIPLE_CHOICE) {
    answerForm.options = [
      { id: 1, text: '' },
      { id: 2, text: '' },
      { id: 3, text: '' },
      { id: 4, text: '' }
    ]
  }
}

// 获取原始问题和回答
const fetchOriginalData = async (questionId: number) => {
  loading.rawAnswers = true
  rawAnswers.value = []

  try {
    const response = await getQuestionOriginalData(questionId)
    rawAnswers.value = response.rawAnswers || []
  } catch (error) {
    console.error('获取原始数据失败:', error)
    ElMessage.error('获取原始数据失败')
  } finally {
    loading.rawAnswers = false
  }
}

// 获取众包回答
const fetchCrowdsourcedAnswers = async (questionId: number) => {
  loading.crowdsourcedAnswers = true
  crowdsourcedAnswers.value = []

  try {
    const response = await getCrowdsourcedAnswersByQuestion(questionId)
    crowdsourcedAnswers.value = response.content || []
  } catch (error) {
    console.error('获取众包回答失败:', error)
    ElMessage.error('获取众包回答失败')
  } finally {
    loading.crowdsourcedAnswers = false
  }
}

// 获取专家回答
const fetchExpertAnswers = async (questionId: number) => {
  loading.expertAnswers = true
  expertAnswers.value = []

  try {
    const response = await getExpertAnswersByQuestion(questionId)
    expertAnswers.value = response.content || []
  } catch (error) {
    console.error('获取专家回答失败:', error)
    ElMessage.error('获取专家回答失败')
  } finally {
    loading.expertAnswers = false
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchData()
}

// 复制文本到编辑器
const copyToEditor = (text: string) => {
  if (!text) return

  answerForm.answerText = text

  // 如果编辑器有自定义的复制方法，调用它
  if (answerEditorRef.value && typeof answerEditorRef.value.setAnswerText === 'function') {
    answerEditorRef.value.setAnswerText(text)
  }

  ElMessage.success('已复制到编辑器')
}

// 提交标准答案
const submitStandardAnswer = async () => {
  // 验证表单
  if (!answerForm.answerText) {
    ElMessage.error('请输入答案文本')
    return
  }

  if (!answerForm.commitMessage) {
    ElMessage.error('请输入提交说明')
    return
  }

  // 根据题型验证特定字段
  if (selectedQuestion.value.questionType === QuestionType.SINGLE_CHOICE || selectedQuestion.value.questionType === QuestionType.MULTIPLE_CHOICE) {
    if (answerForm.options.some(option => !option.text.trim())) {
      ElMessage.error('所有选项都必须填写')
      return
    }

    if (answerForm.correctIds.length === 0) {
      ElMessage.error('请至少选择一个正确答案')
      return
    }

    if (selectedQuestion.value.questionType === QuestionType.SINGLE_CHOICE && answerForm.correctIds.length > 1) {
      ElMessage.error('单选题只能有一个正确答案')
      return
    }
  } else if (selectedQuestion.value.questionType === QuestionType.SUBJECTIVE && !answerForm.scoringGuidance) {
    ElMessage.error('请输入评分指导')
    return
  }

  // 构建请求数据
  let requestData: ChoiceAnswerDto | SimpleFactAnswerDto | SubjectiveAnswerDto

  if (selectedQuestion.value.questionType === QuestionType.SINGLE_CHOICE || selectedQuestion.value.questionType === QuestionType.MULTIPLE_CHOICE) {
    requestData = {
      standardQuestionId: answerForm.standardQuestionId,
      userId: answerForm.userId,
      questionType: answerForm.questionType,
      answerText: answerForm.answerText,
      commitMessage: answerForm.commitMessage,
      options: answerForm.options,
      correctIds: answerForm.correctIds
    } as ChoiceAnswerDto
  } else if (selectedQuestion.value.questionType === QuestionType.SIMPLE_FACT) {
    requestData = {
      standardQuestionId: answerForm.standardQuestionId,
      userId: answerForm.userId,
      questionType: answerForm.questionType,
      answerText: answerForm.answerText,
      commitMessage: answerForm.commitMessage,
      alternativeAnswers: answerForm.alternativeAnswers
    } as SimpleFactAnswerDto
  } else {
    requestData = {
      standardQuestionId: answerForm.standardQuestionId,
      userId: answerForm.userId,
      questionType: answerForm.questionType,
      answerText: answerForm.answerText,
      commitMessage: answerForm.commitMessage,
      scoringGuidance: answerForm.scoringGuidance
    } as SubjectiveAnswerDto
  }

  // 提交答案
  loading.submitting = true
  try {
    // 使用导入的API函数创建标准答案
    await createStandardAnswer(requestData)
    ElMessage.success('标准答案提交成功')

    // 刷新问题列表
    await fetchData()
    selectedQuestion.value = null

    // 重置表单
    resetAnswerForm({
      id: 0,
      questionType: ''
    })
  } catch (error) {
    console.error('提交标准答案失败:', error)
    ElMessage.error('提交标准答案失败')
  } finally {
    loading.submitting = false
  }
}


</script>

<style scoped>
.standard-answers {
  padding: 20px;
}

.left-panel {
  height: calc(100vh - 140px);
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 15px;
}

.pagination {
  margin-top: 15px;
  text-align: center;
}

.empty-state {
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.question-card, .comparison-card, .editor-card {
  margin-bottom: 20px;
}

.question-details {
  padding: 10px 0;
}

.empty-answers {
  height: 150px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.answer-content {
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.answer-meta {
  margin-top: 10px;
  border-top: 1px solid #eee;
  padding-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.answer-meta p {
  margin-right: 20px;
}
</style>
