<template>
  <div class="expert-dashboard-container">
    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <h2>专家仪表盘</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <div class="dashboard-content">
        <!-- 统计数据卡片 -->
        <div class="stats-cards">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-value">{{ stats.totalAssigned || 0 }}</div>
            <div class="stat-label">待回答问题</div>
          </el-card>

          <el-card class="stat-card" shadow="hover">
            <div class="stat-value">{{ stats.totalAnswered || 0 }}</div>
            <div class="stat-label">已回答问题</div>
          </el-card>

          <el-card class="stat-card" shadow="hover">
            <div class="stat-value">{{ stats.reviewedCount || 0 }}</div>
            <div class="stat-label">已审核回答</div>
          </el-card>

          <el-card class="stat-card" shadow="hover">
            <div class="stat-value">{{ (stats.averageScore || 0).toFixed(1) }}</div>
            <div class="stat-label">平均评分</div>
          </el-card>
        </div>

        <!-- 图表 -->
        <div class="charts-container">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <h3>回答统计</h3>
              </div>
            </template>
            <div class="chart-placeholder">
              <el-empty description="图表数据加载中..." v-if="isLoading"></el-empty>
              <div v-else class="chart-content">
                <!-- 这里放图表组件，目前用文本代替 -->
                <ul class="chart-data-list">
                  <li>本周回答: {{ stats.weekAnswers || 0 }} 个问题</li>
                  <li>本月回答: {{ stats.monthAnswers || 0 }} 个问题</li>
                  <li>被采纳数: {{ stats.acceptedAnswers || 0 }} 个回答</li>
                  <li>好评率: {{ stats.positiveRate || 0 }}%</li>
                </ul>
              </div>
            </div>
          </el-card>

          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <h3>专业领域分布</h3>
              </div>
            </template>
            <div class="chart-placeholder">
              <el-empty description="图表数据加载中..." v-if="isLoading"></el-empty>
              <div v-else class="chart-content">
                <!-- 这里放图表组件，目前用文本代替 -->
                <ul class="chart-data-list">
                  <li v-for="(count, domain) in domainStats" :key="domain">
                    {{ domain }}: {{ count }} 个问题
                  </li>
                </ul>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 最近回答列表 -->
        <el-card class="recent-answers-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <h3>最近回答</h3>
              <el-button type="primary" text @click="viewAllAnswers">
                查看全部
              </el-button>
            </div>
          </template>

          <el-table :data="recentAnswers" stripe style="width: 100%" v-loading="isLoading">
            <el-table-column prop="questionTitle" label="问题" min-width="300">
              <template #default="scope">
                <el-tooltip :content="scope.row.questionTitle" placement="top" :show-after="1000">
                  <div class="question-title">{{ scope.row.questionTitle }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="answerDate" label="回答时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusLabel(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="评分" width="100">
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
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button type="primary" link @click="viewAnswer(scope.row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getExpertAnswersByUser } from '@/api/expertAnswer'
import type { ExpertAnswerResponse } from '@/types/expertAnswer'

// 定义领域统计类型
interface DomainStats {
  [key: string]: number;
}

// 定义回答数据类型
interface AnswerItem {
  id: number;
  questionId: number;
  questionTitle: string;
  answerDate: string;
  status: 'reviewed' | 'pending_review' | 'rejected';
  score: number | null;
}

const router = useRouter()
const userStore = useUserStore()
const isLoading = ref(true)

// 统计数据
const stats = reactive({
  totalAssigned: 0,
  totalAnswered: 0,
  reviewedCount: 0,
  averageScore: 0,
  weekAnswers: 0,
  monthAnswers: 0,
  acceptedAnswers: 0,
  positiveRate: 0
})

// 专业领域统计
const domainStats = reactive<DomainStats>({
  '计算机科学': 0,
  '医学健康': 0,
  '金融经济': 0,
  '法律': 0,
  '教育': 0
})

// 最近回答列表
const recentAnswers = ref<AnswerItem[]>([])

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

// 刷新数据
const refreshData = async () => {
  if (!userStore.currentUser?.id) {
    ElMessage.warning('用户未登录或无法获取用户ID')
    return
  }

  isLoading.value = true
  try {
    // 调用API获取专家回答数据
    const userId = userStore.currentUser.id
    const response = await getExpertAnswersByUser(userId, {
      page: '0',
      size: '10',
      sort: 'submissionTime,desc'
    })



    // 检查并提取content数据
    const content = response.content || []
    const totalElements = response.totalElements || 0

    // 计算统计数据
    stats.totalAnswered = totalElements

    // 评审统计
    let reviewedCount = 0
    let scoreSum = 0
    let acceptedCount = 0

    // 最近一周和一个月的回答数量
    const now = new Date()
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    const oneMonthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)

    stats.weekAnswers = 0
    stats.monthAnswers = 0

    // 清零领域统计
    Object.keys(domainStats).forEach(key => {
      domainStats[key] = 0
    })

    // 转换为显示格式并计算统计数据
    recentAnswers.value = content.map((item: ExpertAnswerResponse) => {
      // 计算统计数据
      if (item.qualityScore > 0) {
        reviewedCount++
        scoreSum += item.qualityScore

        // 超过80分算接受
        if (item.qualityScore >= 80) {
          acceptedCount++
        }
      }

      // 统计时间
      const submissionDate = new Date(item.submissionTime)
      if (submissionDate > oneWeekAgo) {
        stats.weekAnswers++
      }
      if (submissionDate > oneMonthAgo) {
        stats.monthAnswers++
      }

      // 计算领域统计 (使用标签的第一个标签作为领域)
      const category = item.standardQuestionCategory || '其他'
      if (domainStats[category] !== undefined) {
        domainStats[category]++
      }

      // 返回格式化的回答数据
      return {
        id: item.id,
        questionId: item.standardQuestionId,
        questionTitle: `问题 #${item.standardQuestionId}`, // API中可能没有返回标题
        answerDate: formatDateTime(item.submissionTime),
        status: item.qualityScore ? 'reviewed' : 'pending_review',
        score: item.qualityScore ? item.qualityScore / 20 : null, // 假设API返回的分数是0-100，转换为0-5星
      } as AnswerItem
    })

    // 更新统计数据
    stats.reviewedCount = reviewedCount
    stats.averageScore = reviewedCount > 0 ? scoreSum / reviewedCount : 0
    stats.acceptedAnswers = acceptedCount
    stats.positiveRate = reviewedCount > 0 ? (acceptedCount / reviewedCount) * 100 : 0

    // 待分配的问题数量 - 由于API中没有这个数据，暂时设为0
    stats.totalAssigned = 0

    ElMessage.success('数据已更新')
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败，请重试')
  } finally {
    isLoading.value = false
  }
}

// 查看回答详情
const viewAnswer = (answer: AnswerItem) => {
  router.push(`/expert/expert-history/${answer.id}`)
}

// 查看所有回答
const viewAllAnswers = () => {
  router.push('/expert/expert-history')
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

// 组件挂载时获取数据
onMounted(async () => {
  await refreshData()
})
</script>

<style scoped>
.expert-dashboard-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.dashboard-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: var(--light-text);
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
}

.chart-card {
  min-height: 300px;
}

.chart-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 250px;
}

.chart-content {
  width: 100%;
  height: 100%;
  padding: 10px;
}

.chart-data-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.chart-data-list li {
  padding: 10px;
  border-bottom: 1px solid var(--border-color);
  font-size: 16px;
}

.chart-data-list li:last-child {
  border-bottom: none;
}

.recent-answers-card {
  margin-top: 20px;
}

.question-title {
  max-width: 500px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .stats-cards,
  .charts-container {
    grid-template-columns: 1fr;
  }
}
</style>
