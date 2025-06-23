<template>
  <div class="crowdsource-dashboard">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-header">
              <h2>众包用户工作台</h2>
              <el-button type="primary" @click="navigateTo('crowdsource-questions')">浏览标准问题</el-button>
            </div>
          </template>
          <div class="dashboard-content">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :md="8">
                <div class="stat-card">
                  <div class="stat-icon">
                    <el-icon><ChatDotRound /></el-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-title">我的回答</div>
                    <div class="stat-value">{{ stats.totalAnswers }}</div>
                  </div>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <div class="stat-card">
                  <div class="stat-icon accepted">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-title">已接受</div>
                    <div class="stat-value">{{ stats.acceptedAnswers }}</div>
                  </div>
                </div>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <div class="stat-card">
                  <div class="stat-icon pending">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-title">待审核</div>
                    <div class="stat-value">{{ stats.pendingAnswers }}</div>
                  </div>
                </div>
              </el-col>
            </el-row>

            <h3 class="section-title">快速操作</h3>
            <div class="quick-actions">
              <el-button type="primary" @click="navigateTo('crowdsource-questions')">
                <el-icon><Search /></el-icon>
                浏览标准问题
              </el-button>
              <el-button type="success" @click="navigateTo('crowdsource-tasks')">
                <el-icon><Document /></el-icon>
                管理我的回答
              </el-button>
            </div>

            <h3 class="section-title">最新回答</h3>
            <div v-if="loading" class="loading-container">
              <el-icon class="loading-icon"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <el-empty v-else-if="recentAnswers.length === 0" description="暂无回答记录" />
            <div v-else class="recent-answers">
              <el-card v-for="answer in recentAnswers" :key="answer.id" class="recent-answer-item" shadow="hover">
                <div class="answer-header">
                  <h4 class="answer-title">{{ getQuestionText(answer.standardQuestionId) }}</h4>
                  <el-tag :type="getStatusType(answer.qualityReviewStatus)" size="small">
                    {{ getStatusLabel(answer.qualityReviewStatus) }}
                  </el-tag>
                </div>
                <p class="answer-preview">{{ truncateText(answer.answerText, 100) }}</p>
                <div class="answer-meta">
                  <span class="meta-date">{{ formatDate(answer.submissionTime) }}</span>
                </div>
              </el-card>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="guide-card">
          <template #header>
            <div class="card-header">
              <h2>众包指南</h2>
            </div>
          </template>
          <div class="guide-content">
            <h3>如何参与众包回答</h3>
            <ol class="guide-steps">
              <li>
                <strong>浏览标准问题：</strong>
                在"标准问题"页面查看可回答的问题列表
              </li>
              <li>
                <strong>提交回答：</strong>
                选择感兴趣的问题，点击"提交回答"按钮
              </li>
              <li>
                <strong>等待审核：</strong>
                您的回答将由审核员进行审核
              </li>
              <li>
                <strong>查看结果：</strong>
                在"我的回答"页面查看回答状态和反馈
              </li>
            </ol>

            <h3>回答质量要求</h3>
            <ul class="quality-list">
              <li>
                <el-icon><InfoFilled /></el-icon>
                <span>回答应当准确、完整、清晰</span>
              </li>
              <li>
                <el-icon><InfoFilled /></el-icon>
                <span>避免冗余或无关的内容</span>
              </li>
              <li>
                <el-icon><InfoFilled /></el-icon>
                <span>尊重知识产权，注明引用来源</span>
              </li>
              <li>
                <el-icon><InfoFilled /></el-icon>
                <span>保持客观，避免个人偏见</span>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCrowdsourcedAnswersByUser } from '@/api/crowdsourcedAnswer'
import type { CrowdsourcedAnswerResponse } from '@/types/crowdsourcedAnswer'
import { QualityReviewStatus } from '@/types/crowdsourcedAnswer'
import { useUserStore } from '@/stores/user'
import {
  Loading,
  ChatDotRound,
  CircleCheck,
  Clock,
  Search,
  Document,
  InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => userStore.currentUser?.id || 0)

// 页面状态
const loading = ref(false)
const recentAnswers = ref<CrowdsourcedAnswerResponse[]>([])
const questionTextMap = ref<Record<number, string>>({})

// 统计数据
const stats = reactive({
  totalAnswers: 0,
  acceptedAnswers: 0,
  pendingAnswers: 0,
  rejectedAnswers: 0
})

// 初始加载
onMounted(async () => {
  if (userId.value) {
    await loadDashboardData()
  }
})

// 加载仪表盘数据
async function loadDashboardData() {
  loading.value = true

  try {
    // 获取用户最近的回答
    const response = await getCrowdsourcedAnswersByUser(userId.value.toString(), {
      page: '0',
      size: '5',
      sort: '-submissionTime' // 按提交时间倒序
    })

    recentAnswers.value = response.content || []

    // 获取统计数据
    await calculateStats()

    // 加载问题文本
    await loadQuestionTexts()

  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算统计数据
async function calculateStats() {
  try {
    // 获取所有回答
    const allAnswersResponse = await getCrowdsourcedAnswersByUser(userId.value.toString(), {
      page: '0',
      size: '1000' // 较大的值以获取所有数据
    })

    const allAnswers = allAnswersResponse.content || []

    // 计算各状态的数量
    stats.totalAnswers = allAnswers.length
    stats.acceptedAnswers = allAnswers.filter(a => a.qualityReviewStatus === QualityReviewStatus.ACCEPTED).length
    stats.pendingAnswers = allAnswers.filter(a => a.qualityReviewStatus === QualityReviewStatus.PENDING).length
    stats.rejectedAnswers = allAnswers.filter(a => a.qualityReviewStatus === QualityReviewStatus.REJECTED).length

  } catch (error) {
    console.error('计算统计数据失败:', error)
  }
}

// 加载问题文本
async function loadQuestionTexts() {
  if (recentAnswers.value.length === 0) return

  const questionIds = [...new Set(recentAnswers.value.map(a => a.standardQuestionId))]

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

        const question = response.questions.find(q => q.id === questionId)
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

// 导航到指定页面
function navigateTo(route: string) {
  router.push({ name: route })
}

// 获取问题文本
function getQuestionText(questionId: number) {
  return questionTextMap.value[questionId] || `问题 #${questionId}`
}

// 截断文本
function truncateText(text: string, maxLength: number) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
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
.crowdsource-dashboard {
  padding: 20px;
}

.dashboard-card,
.guide-card {
  margin-bottom: 20px;
  height: calc(100% - 20px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-content,
.guide-content {
  padding: 20px 0;
}

.section-title {
  margin: 30px 0 15px;
  font-size: 18px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.stat-card {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 24px;
  width: 50px;
  height: 50px;
  background-color: #409eff;
  color: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
}

.stat-icon.accepted {
  background-color: #67c23a;
}

.stat-icon.pending {
  background-color: #e6a23c;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.recent-answers {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.recent-answer-item {
  border-left: 3px solid #409eff;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.answer-title {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.answer-preview {
  margin: 10px 0;
  color: #666;
  line-height: 1.5;
}

.answer-meta {
  font-size: 12px;
  color: #999;
}

.guide-steps {
  padding-left: 20px;
  margin: 15px 0 25px;
}

.guide-steps li {
  margin-bottom: 15px;
  line-height: 1.5;
}

.quality-list {
  list-style: none;
  padding: 0;
  margin: 15px 0;
}

.quality-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #555;
}

.quality-list li .el-icon {
  color: #409eff;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #999;
}

.loading-icon {
  font-size: 24px;
  margin-right: 10px;
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
