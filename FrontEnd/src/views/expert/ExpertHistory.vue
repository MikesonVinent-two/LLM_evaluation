<template>
  <div class="expert-history-container">
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <h2>历史回答记录</h2>
          <div class="header-actions">
            <el-input
              v-model="filters.category"
              placeholder="输入问题类别"
              clearable
              style="width: 150px;"
            />
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 150px;">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 260px;"
            />
            <el-button type="primary" @click="loadAnswers">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </div>
        </div>
      </template>

      <div class="history-content">
        <!-- 回答历史列表 -->
        <el-table
          :data="answers"
          style="width: 100%"
          v-loading="isLoading"
          @row-click="handleRowClick"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="questionTitle" label="问题标题" min-width="300">
            <template #default="scope">
              <el-tooltip :content="scope.row.questionTitle" placement="top" :show-after="1000">
                <div class="question-title">{{ scope.row.questionTitle }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="answerDate" label="回答时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="120">
            <template #default="scope">
              <div v-if="scope.row.score">
                <el-rate
                  v-model="scope.row.score"
                  disabled
                  text-color="#ff9900"
                  score-template="{value}"
                />
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click.stop="viewDetail(scope.row)">
                查看详情
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

    <!-- 回答详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentAnswer ? `回答详情: ${currentAnswer.questionTitle}` : '回答详情'"
      width="70%"
      destroy-on-close
    >
      <div v-if="currentAnswer" class="answer-detail">
        <div class="answer-info">
          <div class="info-item">
            <span class="info-label">问题ID:</span>
            <span>{{ currentAnswer.questionId }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">类别:</span>
            <span>{{ currentAnswer.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">回答时间:</span>
            <span>{{ currentAnswer.answerDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">状态:</span>
            <el-tag :type="getStatusType(currentAnswer.status)">
              {{ getStatusLabel(currentAnswer.status) }}
            </el-tag>
          </div>
          <div class="info-item" v-if="currentAnswer.score">
            <span class="info-label">评分:</span>
            <el-rate
              v-model="currentAnswer.score"
              disabled
              text-color="#ff9900"
            />
          </div>
        </div>

        <el-divider />

        <div class="question-content">
          <h3>问题内容</h3>
          <div class="content-text">{{ currentAnswer.questionContent }}</div>
        </div>

        <el-divider />

        <div class="answer-content">
          <h3>我的回答</h3>
          <div class="content-text">{{ currentAnswer.answerContent }}</div>

          <h4 v-if="currentAnswer.references">参考资料</h4>
          <div v-if="currentAnswer.references" class="content-text">
            {{ currentAnswer.references }}
          </div>
        </div>

        <el-divider v-if="currentAnswer.feedback" />

        <div class="feedback-content" v-if="currentAnswer.feedback">
          <h3>审核反馈</h3>
          <div class="feedback-item">
            <span class="feedback-label">审核人:</span>
            <span>{{ currentAnswer.feedback.reviewer }}</span>
          </div>
          <div class="feedback-item">
            <span class="feedback-label">审核时间:</span>
            <span>{{ currentAnswer.feedback.reviewDate }}</span>
          </div>
          <div class="feedback-item">
            <span class="feedback-label">评分:</span>
            <el-rate
              v-model="currentAnswer.score"
              disabled
              text-color="#ff9900"
            />
          </div>
          <div class="feedback-item" v-if="currentAnswer.feedback.comment">
            <span class="feedback-label">评语:</span>
            <div class="feedback-comment">{{ currentAnswer.feedback.comment }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getExpertAnswersByUser } from '@/api/expertAnswer'
import type { ExpertAnswerResponse } from '@/types/expertAnswer'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 加载状态
const isLoading = ref(false)

// 过滤条件
const filters = reactive({
  category: '',
  status: '',
  dateRange: null
})

// 类别选项已改为自由输入文本框

// 状态选项
const statusOptions = [
  { value: 'reviewed', label: '已审核' },
  { value: 'pending_review', label: '待审核' },
  { value: 'rejected', label: '已拒绝' }
]

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 回答列表
const answers = ref<any[]>([])

// 对话框控制
const detailDialogVisible = ref(false)
const currentAnswer = ref<any>(null)

// 获取状态标签
const getStatusLabel = (status: string) => {
  const statusMap: Record<string, string> = {
    'reviewed': '已审核',
    'pending_review': '待审核',
    'rejected': '已拒绝'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'reviewed': 'success',
    'pending_review': 'warning',
    'rejected': 'danger'
  }
  return typeMap[status] || 'info'
}

// 处理行点击事件
const handleRowClick = (row: any) => {
  viewDetail(row)
}

// 查看详情
const viewDetail = (answer: any) => {
  // 直接使用行数据作为详情
  currentAnswer.value = { ...answer }
  detailDialogVisible.value = true
}

// 加载回答列表
const loadAnswers = async () => {
  if (!userStore.currentUser?.id) {
    ElMessage.warning('用户未登录或无法获取用户ID')
    return
  }

  isLoading.value = true
  try {
    // 调用API获取当前登录用户的专家回答
    const userId = userStore.currentUser.id
    const response = await getExpertAnswersByUser(userId, {
      page: (pagination.currentPage - 1).toString(),
      size: pagination.pageSize.toString(),
      sort: 'submissionTime,desc'
    })

    // 处理返回的数据
    const { content, totalElements } = response

    // 转换API返回的数据格式为组件需要的格式
    answers.value = content.map(item => ({
      id: item.id,
      questionId: item.standardQuestionId,
      questionTitle: `问题 #${item.standardQuestionId}`, // API中可能没有返回标题，这里使用ID代替
      category: '专业领域', // API中可能没有返回类别
      answerDate: formatDateTime(item.submissionTime),
      status: item.qualityScore ? 'reviewed' : 'pending_review',
      score: item.qualityScore ? item.qualityScore / 20 : null, // 假设API返回的分数是0-100，转换为0-5星
      answerContent: item.candidateAnswerText,
      questionContent: '问题详情将在查看详情时加载', // 可能需要额外的API调用获取问题详情
      references: '', // API中可能没有返回参考资料
      feedback: item.feedback ? {
        comment: item.feedback,
        reviewer: '评审员',
        reviewDate: formatDateTime(item.submissionTime) // API中可能没有返回审核时间
      } : null
    }))

    pagination.total = totalElements

    ElMessage.success('数据已更新')
  } catch (error) {
    console.error('获取回答列表失败:', error)
    ElMessage.error('获取回答列表失败，请重试')
  } finally {
    isLoading.value = false
  }
}

// 处理分页大小变化
const handleSizeChange = (val: number) => {
  pagination.pageSize = val
  loadAnswers()
}

// 处理页码变化
const handleCurrentChange = (val: number) => {
  pagination.currentPage = val
  loadAnswers()
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    console.error('日期格式化失败:', e)
    return dateStr
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadAnswers()
})
</script>

<style scoped>
.expert-history-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.history-card {
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

.history-content {
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

.answer-detail {
  padding: 0 20px;
}

.answer-info {
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

.question-content,
.answer-content,
.feedback-content {
  margin: 20px 0;
}

.question-content h3,
.answer-content h3,
.answer-content h4,
.feedback-content h3 {
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

.feedback-item {
  margin: 10px 0;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.feedback-label {
  color: var(--light-text);
  min-width: 80px;
}

.feedback-comment {
  line-height: 1.6;
  white-space: pre-line;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  flex: 1;
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

  .answer-info {
    grid-template-columns: 1fr;
  }
}
</style>
