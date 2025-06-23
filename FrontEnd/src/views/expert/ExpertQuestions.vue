<template>
  <div class="expert-questions-container">
    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <h2>专家问题回答</h2>
          <div class="header-actions">
            <el-input
              v-model="filters.category"
              placeholder="输入问题类别"
              clearable
              style="width: 150px;"
            />
            <el-select v-model="filters.difficulty" placeholder="难度等级" clearable style="width: 150px;">
              <el-option v-for="item in difficulties" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 150px;">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" @click="loadQuestions">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </div>
        </div>
      </template>

      <div class="questions-content">
        <!-- 问题列表 -->
        <el-table
          :data="questions"
          style="width: 100%"
          v-loading="isLoading"
          @row-click="handleRowClick"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="问题标题" min-width="300">
            <template #default="scope">
              <el-tooltip :content="scope.row.title" placement="top" :show-after="1000">
                <div class="question-title">{{ scope.row.title }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="scope">
              <el-tag :type="getDifficultyType(scope.row.difficulty)">
                {{ scope.row.difficulty }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="assignDate" label="分配时间" width="180" />
          <el-table-column prop="dueDate" label="截止时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button
                type="primary"
                link
                @click.stop="handleAnswer(scope.row)"
                :disabled="scope.row.status === 'answered'"
              >
                {{ scope.row.status === 'answered' ? '已回答' : '回答' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 回答对话框 -->
    <el-dialog
      v-model="answerDialogVisible"
      :title="currentQuestion ? `回答问题: ${currentQuestion.title}` : '回答问题'"
      width="70%"
      destroy-on-close
    >
      <div v-if="currentQuestion" class="question-detail">
        <div class="question-info">
          <div class="info-item">
            <span class="info-label">问题ID:</span>
            <span>{{ currentQuestion.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">类别:</span>
            <span>{{ currentQuestion.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">难度:</span>
            <el-tag :type="getDifficultyType(currentQuestion.difficulty)">
              {{ currentQuestion.difficulty }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">截止时间:</span>
            <span :class="{ 'urgent': isUrgent(currentQuestion.dueDate) }">
              {{ currentQuestion.dueDate }}
            </span>
          </div>
        </div>

        <el-divider />

        <div class="question-content">
          <h3>问题内容</h3>
          <div class="content-text">{{ currentQuestion.content }}</div>
        </div>

        <el-divider />

        <div class="answer-form">
          <h3>您的回答</h3>
          <el-form :model="answerForm" ref="answerFormRef" :rules="answerRules">
            <el-form-item prop="content" required>
              <el-input
                v-model="answerForm.content"
                type="textarea"
                :rows="10"
                placeholder="请输入您的专业回答..."
              />
            </el-form-item>

            <el-form-item label="参考资料" prop="references">
              <el-input
                v-model="answerForm.references"
                type="textarea"
                :rows="4"
                placeholder="请输入参考资料或来源（可选）"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="answerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAnswer" :loading="submitting">
            提交回答
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 问题详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentQuestion ? `问题详情: ${currentQuestion.title}` : '问题详情'"
      width="70%"
      destroy-on-close
    >
      <div v-if="currentQuestion" class="question-detail">
        <div class="question-info">
          <div class="info-item">
            <span class="info-label">问题ID:</span>
            <span>{{ currentQuestion.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">类别:</span>
            <span>{{ currentQuestion.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">难度:</span>
            <el-tag :type="getDifficultyType(currentQuestion.difficulty)">
              {{ currentQuestion.difficulty }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">状态:</span>
            <el-tag :type="getStatusType(currentQuestion.status)">
              {{ getStatusLabel(currentQuestion.status) }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="info-label">分配时间:</span>
            <span>{{ currentQuestion.assignDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">截止时间:</span>
            <span :class="{ 'urgent': isUrgent(currentQuestion.dueDate) }">
              {{ currentQuestion.dueDate }}
            </span>
          </div>
        </div>

        <el-divider />

        <div class="question-content">
          <h3>问题内容</h3>
          <div class="content-text">{{ currentQuestion.content }}</div>
        </div>

        <template v-if="currentQuestion.status === 'answered'">
          <el-divider />

          <div class="answer-content">
            <h3>您的回答</h3>
            <div class="content-text">{{ currentQuestion.answer?.content }}</div>

            <h4 v-if="currentQuestion.answer?.references">参考资料</h4>
            <div v-if="currentQuestion.answer?.references" class="content-text">
              {{ currentQuestion.answer.references }}
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="currentQuestion && currentQuestion.status !== 'answered'"
            type="primary"
            @click="handleAnswer(currentQuestion)"
          >
            回答此问题
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { searchStandardQuestions, getQuestionsWithoutAnswers } from '@/api/standardData'
import { createExpertAnswer } from '@/api/expertAnswer'
import type { SearchQuestionItem } from '@/types/standardQuestion'
import { DifficultyLevel } from '@/api/standardData'

// 用户信息
const userStore = useUserStore()

// 加载状态
const isLoading = ref(false)
const submitting = ref(false)

// 过滤条件
const filters = reactive({
  category: '',
  difficulty: '',
  status: ''
})

// 类别选项已改为自由输入文本框

// 难度选项
const difficulties = [
  { value: DifficultyLevel.EASY, label: '简单' },
  { value: DifficultyLevel.MEDIUM, label: '中等' },
  { value: DifficultyLevel.HARD, label: '困难' }
]

// 状态选项
const statusOptions = [
  { value: 'unanswered', label: '待回答' },
  { value: 'answered', label: '已回答' }
]

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 问题列表
const questions = ref<any[]>([])

// 对话框控制
const answerDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentQuestion = ref<any>(null)

// 回答表单
const answerForm = reactive({
  content: '',
  references: ''
})

// 表单引用
const answerFormRef = ref<FormInstance>()

// 表单验证规则
const answerRules = {
  content: [
    { required: true, message: '请输入回答内容', trigger: 'blur' },
    { min: 50, message: '回答内容不能少于50个字符', trigger: 'blur' }
  ]
}

// 获取难度类型
const getDifficultyType = (difficulty: string) => {
  const typeMap: Record<string, string> = {
    [DifficultyLevel.EASY]: 'success',
    [DifficultyLevel.MEDIUM]: 'warning',
    [DifficultyLevel.HARD]: 'danger'
  }
  return typeMap[difficulty] || 'info'
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    'unanswered': '待回答',
    'answered': '已回答',
    'reviewing': '审核中',
    'rejected': '已拒绝'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'unanswered': 'warning',
    'answered': 'success',
    'reviewing': 'info',
    'rejected': 'danger'
  }
  return typeMap[status] || 'info'
}

// 判断是否紧急（截止日期在3天内）
const isUrgent = (dateStr: string) => {
  if (!dateStr) return false
  const dueDate = new Date(dateStr)
  const now = new Date()
  const diffTime = dueDate.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 3 && diffDays >= 0
}

// 加载问题列表
const loadQuestions = async () => {
  isLoading.value = true
  questions.value = []

  try {
    let response

    if (filters.status === 'unanswered') {
      // 从无答案问题列表加载
      response = await getQuestionsWithoutAnswers({
        page: (pagination.currentPage - 1).toString(),
        size: pagination.pageSize.toString()
      })
    } else {
      // 默认搜索所有问题
      const searchParams: any = {
        page: (pagination.currentPage - 1).toString(),
        size: pagination.pageSize.toString(),
        onlyLatest: true // 只返回叶子节点的标准问题
      }

      // 添加过滤条件
      if (filters.category) {
        searchParams.tags = filters.category
      }

      if (userStore.currentUser?.id) {
        searchParams.userId = userStore.currentUser.id.toString()
      }

      response = await searchStandardQuestions(searchParams)

      // 打印返回的数据结构，方便调试
      console.log('API返回数据结构:', response)

      // 更宽松的防御性检查，只要response和response.data存在即可

      // 安全地获取total值，提供默认值0
      pagination.total = response.total || 0

      // 确保questions数组存在
      let questionsList = response.questions || []

      // 确保questionsList是数组
      if (!Array.isArray(questionsList)) {
        console.warn('API返回的questions不是数组，尝试使用替代结构')
        // 尝试其他可能的位置
        if (Array.isArray(response.content)) {
          questionsList = response.content
        } else {
          questionsList = []
        }
      }

      questions.value = questionsList.map((item: any) => {
        // 防御性检查每个字段
        return {
          id: item.id || 0,
          title: item.questionText || '未知标题',
          category: (item.tags && Array.isArray(item.tags)) ? item.tags.join(', ') : '未分类',
          difficulty: item.difficulty || 'MEDIUM',
          assignDate: (item.creationTime && typeof item.creationTime === 'string')
            ? item.creationTime.replace('T', ' ').substring(0, 19) || '-'
            : '-',
          dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19).replace('T', ' '),
          status: item.hasExpertAnswer ? 'answered' : 'unanswered',
          content: item.questionText || '未知内容',
          tags: (item.tags && Array.isArray(item.tags)) ? item.tags : []
        }
      })

      if (filters.status === 'answered') {
        questions.value = questions.value.filter(q => q.status === 'answered')
      }
    }

    // 根据难度筛选
    if (filters.difficulty) {
      questions.value = questions.value.filter(q => q.difficulty === filters.difficulty)
    }

    ElMessage.success('数据已更新')
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取问题列表失败，请重试')
  } finally {
    isLoading.value = false
  }
}

// 处理分页大小变化
const handleSizeChange = (val: number) => {
  pagination.pageSize = val
  loadQuestions()
}

// 处理页码变化
const handleCurrentChange = (val: number) => {
  pagination.currentPage = val
  loadQuestions()
}

// 处理行点击事件
const handleRowClick = (row: any) => {
  currentQuestion.value = { ...row }
  detailDialogVisible.value = true
}

// 处理回答问题
const handleAnswer = (question: any) => {
  currentQuestion.value = { ...question }
  answerForm.content = ''
  answerForm.references = ''
  detailDialogVisible.value = false
  answerDialogVisible.value = true
}

// 提交回答
const submitAnswer = async () => {
  if (!answerFormRef.value || !currentQuestion.value || !userStore.currentUser?.id) return

  await answerFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        // 调用API提交专家候选回答
        const response = await createExpertAnswer({
          standardQuestionId: currentQuestion.value.id.toString(),
          userId: userStore.currentUser.id.toString(),
          candidateAnswerText: answerForm.content
        })

        // 更新问题状态
        const index = questions.value.findIndex(q => q.id === currentQuestion.value.id)
        if (index !== -1) {
          questions.value[index].status = 'answered'
          questions.value[index].answer = {
            content: answerForm.content,
            references: answerForm.references
          }
        }

        ElMessage.success('回答已提交')
        answerDialogVisible.value = false

        // 重新加载数据以更新状态
        loadQuestions()
      } catch (error) {
        console.error('提交回答失败:', error)
        ElMessage.error('提交回答失败，请重试')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.expert-questions-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.questions-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.questions-content {
  margin-top: 20px;
}

.question-title {
  max-width: 450px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.question-detail {
  padding: 0 20px;
}

.question-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-label {
  color: var(--light-text);
  min-width: 80px;
}

.urgent {
  color: #f56c6c;
  font-weight: bold;
}

.question-content,
.answer-content {
  margin: 20px 0;
}

.question-content h3,
.answer-content h3,
.answer-content h4 {
  margin-top: 0;
  margin-bottom: 15px;
  font-weight: 600;
}

.content-text {
  line-height: 1.6;
  white-space: pre-line;
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
  border-left: 4px solid var(--primary-color);
}

.answer-form {
  margin: 20px 0;
}

.answer-form h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    margin-top: 10px;
  }

  .question-info {
    grid-template-columns: 1fr;
  }
}
</style>
