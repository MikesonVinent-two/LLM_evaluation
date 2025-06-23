<template>
  <div class="crowdsourced-answer-review">
    <h1>众包回答审核工作台</h1>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="未审核回答" name="pending">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>待审核众包回答</h3>
              <div class="header-actions">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索回答内容..."
                  clearable
                  style="width: 250px"
                  @input="handleSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="refreshData">刷新</el-button>
              </div>
            </div>
          </template>

          <!-- 未审核回答列表 -->
          <el-table
            :data="filteredPendingAnswers"
            style="width: 100%"
            v-loading="loading.pending"
            border
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userUsername" label="众包用户" width="120" />
            <el-table-column prop="standardQuestionId" label="问题ID" width="100" />
            <el-table-column label="回答内容" min-width="300">
              <template #default="{ row }">
                <div class="answer-text">{{ row.answerText }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="submissionTime" label="提交时间" width="180" />
            <el-table-column prop="taskBatchId" label="任务批次" width="100" />
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="success"
                  size="small"
                  @click="openReviewDialog(row, QualityReviewStatus.ACCEPTED)"
                >
                  通过
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="openReviewDialog(row, QualityReviewStatus.REJECTED)"
                >
                  拒绝
                </el-button>
                <el-button
                  type="warning"
                  size="small"
                  @click="openReviewDialog(row, QualityReviewStatus.FLAGGED)"
                >
                  标记
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.pending.currentPage"
              v-model:page-size="pagination.pending.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.pending.total"
              @size-change="(size: number) => handleSizeChange('pending', size)"
              @current-change="(page: number) => handleCurrentChange(page)"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="已审核回答" name="reviewed">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>已审核众包回答</h3>
              <div class="header-actions">
                <el-select
                  v-model="filterStatus"
                  placeholder="审核状态"
                  clearable
                  style="width: 120px"
                  @change="handleStatusFilterChange"
                >
                  <el-option label="已通过" :value="QualityReviewStatus.ACCEPTED" />
                  <el-option label="已拒绝" :value="QualityReviewStatus.REJECTED" />
                  <el-option label="已标记" :value="QualityReviewStatus.FLAGGED" />
                </el-select>
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索回答内容..."
                  clearable
                  style="width: 250px"
                  @input="handleSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="refreshData">刷新</el-button>
              </div>
            </div>
          </template>

          <!-- 已审核回答列表 -->
          <el-table
            :data="filteredReviewedAnswers"
            style="width: 100%"
            v-loading="loading.reviewed"
            border
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userUsername" label="众包用户" width="120" />
            <el-table-column prop="standardQuestionId" label="问题ID" width="100" />
            <el-table-column label="回答内容" min-width="250">
              <template #default="{ row }">
                <div class="answer-text">{{ row.answerText }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="submissionTime" label="提交时间" width="180" />
            <el-table-column label="审核状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.qualityReviewStatus)">
                  {{ getStatusText(row.qualityReviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewTime" label="审核时间" width="180" />
            <el-table-column prop="reviewFeedback" label="审核反馈" min-width="200" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="openReviewDialog(row, row.qualityReviewStatus)"
                >
                  修改审核
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.reviewed.currentPage"
              v-model:page-size="pagination.reviewed.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.reviewed.total"
              @size-change="(size: number) => handleSizeChange('reviewed', size)"
              @current-change="(page: number) => handleCurrentChange(page)"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialog.visible"
      :title="reviewDialog.isEdit ? '修改审核' : '审核众包回答'"
      width="60%"
    >
      <div v-if="reviewDialog.answer" class="review-dialog-content">
        <div class="answer-details">
          <h4>问题ID: {{ reviewDialog.answer.standardQuestionId }}</h4>
          <h4>众包用户: {{ reviewDialog.answer.userUsername }}</h4>
          <div class="answer-content">
            <h4>回答内容:</h4>
            <div class="answer-text-full">{{ reviewDialog.answer.answerText }}</div>
          </div>
        </div>

        <el-form :model="reviewForm" label-width="120px">
          <el-form-item label="审核状态">
            <el-radio-group v-model="reviewForm.status">
              <el-radio label="ACCEPTED">通过</el-radio>
              <el-radio label="REJECTED">拒绝</el-radio>
              <el-radio label="FLAGGED">标记</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核反馈">
            <el-input
              v-model="reviewForm.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入对这个回答的审核反馈..."
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="submitReview" :loading="loading.submit">
            提交审核
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPendingCrowdsourcedAnswers,
  getReviewedCrowdsourcedAnswers,
  reviewCrowdsourcedAnswer
} from '@/api/crowdsourcedAnswer'
import type { CrowdsourcedAnswerResponse } from '@/types/crowdsourcedAnswer'
import { QualityReviewStatus } from '@/types/crowdsourcedAnswer'

// 用户信息
const userStore = useUserStore()

// 标签页状态
const activeTab = ref('pending')

// 搜索查询
const searchQuery = ref('')

// 状态过滤
const filterStatus = ref('')

// 加载状态
const loading = reactive({
  pending: false,
  reviewed: false,
  submit: false
})

// 分页配置
const pagination = reactive({
  pending: {
    currentPage: 1,
    pageSize: 10,
    total: 0
  },
  reviewed: {
    currentPage: 1,
    pageSize: 10,
    total: 0
  }
})

// 众包回答数据
const pendingAnswers = ref<CrowdsourcedAnswerResponse[]>([])
const reviewedAnswers = ref<CrowdsourcedAnswerResponse[]>([])

// 审核对话框
const reviewDialog = reactive({
  visible: false,
  isEdit: false,
  answer: null as CrowdsourcedAnswerResponse | null
})

// 审核表单
const reviewForm = reactive({
  status: QualityReviewStatus.ACCEPTED,
  feedback: ''
})

// 过滤后的回答列表
const filteredPendingAnswers = computed(() => {
  if (!searchQuery.value) return pendingAnswers.value

  const query = searchQuery.value.toLowerCase()
  return pendingAnswers.value.filter(answer =>
    answer.answerText.toLowerCase().includes(query) ||
    answer.userUsername.toLowerCase().includes(query)
  )
})

const filteredReviewedAnswers = computed(() => {
  let filtered = reviewedAnswers.value

  // 按状态过滤
  if (filterStatus.value) {
    filtered = filtered.filter(answer => answer.qualityReviewStatus === filterStatus.value)
  }

  // 按搜索词过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(answer =>
      answer.answerText.toLowerCase().includes(query) ||
      answer.userUsername.toLowerCase().includes(query) ||
      (answer.reviewFeedback && answer.reviewFeedback.toLowerCase().includes(query))
    )
  }

  return filtered
})

// 获取状态标签类型
const getStatusTagType = (status: QualityReviewStatus) => {
  switch (status) {
    case QualityReviewStatus.ACCEPTED:
      return 'success'
    case QualityReviewStatus.REJECTED:
      return 'danger'
    case QualityReviewStatus.FLAGGED:
      return 'warning'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: QualityReviewStatus) => {
  switch (status) {
    case QualityReviewStatus.ACCEPTED:
      return '已通过'
    case QualityReviewStatus.REJECTED:
      return '已拒绝'
    case QualityReviewStatus.FLAGGED:
      return '已标记'
    case QualityReviewStatus.PENDING:
      return '待审核'
    default:
      return '未知'
  }
}

// 获取未审核的众包回答
const fetchPendingAnswers = async () => {
  loading.pending = true
  try {
    const response = await getPendingCrowdsourcedAnswers({
      page: String(pagination.pending.currentPage - 1),
      size: String(pagination.pending.pageSize),
      sort: 'submissionTime,desc'
    })

    console.log('未审核回答API响应:', response)

    // 处理特定响应格式 - 根据实际API响应进行处理
    if (response && response.status === 'success' && Array.isArray(response.data)) {
      // 处理后端的新格式 {data: Array(1), status: 'success', totalElements: 1, ...}
      console.log('使用新格式响应数据')
      pendingAnswers.value = response.data
      pagination.pending.total = response.totalElements || response.data.length
      pagination.pending.currentPage = (response.currentPage !== undefined) ? response.currentPage + 1 : 1
    }
    // 检查响应格式并适配不同的数据结构
    else if (response.data && response.data.data) {
      // 新的API响应格式
      console.log('使用新的API响应格式处理数据')
      pendingAnswers.value = response.data.data
      pagination.pending.total = response.data.totalElements || response.data.data.length
      pagination.pending.currentPage = response.data.currentPage + 1 || 1
    } else if (response.content) {
      // 旧的API响应格式
      console.log('使用旧的API响应格式处理数据')
      pendingAnswers.value = response.content
      pagination.pending.total = response.totalElements
    } else if (response.data && Array.isArray(response.data)) {
      // 直接返回数据数组的格式
      console.log('使用直接数组返回格式处理数据')
      pendingAnswers.value = response.data
      pagination.pending.total = response.data.length
    } else if (response.status === 'success' && response.data) {
      // 新格式，直接返回的对象
      console.log('使用直接对象返回格式处理数据')
      pendingAnswers.value = response.data
      pagination.pending.total = response.totalElements || 1
      pagination.pending.currentPage = response.currentPage + 1 || 1
    } else {
      console.error('未知的API响应格式:', response)
      pendingAnswers.value = []
      pagination.pending.total = 0
    }
  } catch (error) {
    console.error('获取未审核众包回答失败:', error)
    ElMessage.error('获取未审核众包回答失败')
    pendingAnswers.value = []
    pagination.pending.total = 0
  } finally {
    loading.pending = false
  }
}

// 获取已审核的众包回答
const fetchReviewedAnswers = async () => {
  if (!userStore.currentUser?.id) {
    ElMessage.warning('用户未登录')
    return
  }

  loading.reviewed = true
  try {
    console.log('开始获取已审核回答，用户ID:', userStore.currentUser.id)

    // 获取当前用户审核过的众包回答
    const response = await getReviewedCrowdsourcedAnswers(
      userStore.currentUser.id,
      {
        page: String(pagination.reviewed.currentPage - 1),
        size: String(pagination.reviewed.pageSize),
        sort: 'reviewTime,desc'
      }
    )

    console.log('已审核回答API响应:', response)

    // 处理特定响应格式 - 根据实际API响应进行处理
    if (response && response.status === 'success' && Array.isArray(response.data)) {
      // 处理后端的新格式 {data: Array(1), status: 'success', totalElements: 1, ...}
      console.log('使用新格式响应数据')
      reviewedAnswers.value = response.data
      pagination.reviewed.total = response.totalElements || response.data.length
      pagination.reviewed.currentPage = (response.currentPage !== undefined) ? response.currentPage + 1 : 1
    }
    // 检查响应格式并适配不同的数据结构
    else if (response.data && response.data.data) {
      // 新的API响应格式
      console.log('使用新的API响应格式处理数据')
      reviewedAnswers.value = response.data.data
      pagination.reviewed.total = response.data.totalElements || response.data.data.length
      pagination.reviewed.currentPage = response.data.currentPage + 1 || 1
    } else if (response.content) {
      // 旧的API响应格式
      console.log('使用旧的API响应格式处理数据')
      reviewedAnswers.value = response.content
      pagination.reviewed.total = response.totalElements
    } else if (response.data && Array.isArray(response.data)) {
      // 直接返回数据数组的格式
      console.log('使用直接数组返回格式处理数据')
      reviewedAnswers.value = response.data
      pagination.reviewed.total = response.data.length
    } else if (response.status === 'success' && response.data) {
      // 新格式，直接返回的对象
      console.log('使用直接对象返回格式处理数据')
      reviewedAnswers.value = response.data
      pagination.reviewed.total = response.totalElements || 1
      pagination.reviewed.currentPage = response.currentPage + 1 || 1
    } else {
      console.error('未知的API响应格式:', response)
      reviewedAnswers.value = []
      pagination.reviewed.total = 0
    }

    console.log('处理后的已审核回答数据:', reviewedAnswers.value)
  } catch (error) {
    console.error('获取已审核众包回答失败:', error)
    ElMessage.error('获取已审核众包回答失败')
    reviewedAnswers.value = []
    pagination.reviewed.total = 0
  } finally {
    loading.reviewed = false
  }
}

// 打开审核对话框
const openReviewDialog = (answer: CrowdsourcedAnswerResponse, status: QualityReviewStatus) => {
  reviewDialog.answer = answer
  reviewDialog.isEdit = answer.qualityReviewStatus !== QualityReviewStatus.PENDING
  reviewForm.status = status
  reviewForm.feedback = answer.reviewFeedback || ''
  reviewDialog.visible = true
}

// 提交审核
const submitReview = async () => {
  if (!reviewDialog.answer || !userStore.currentUser?.id) return

  loading.submit = true
  try {
    const response = await reviewCrowdsourcedAnswer(
      reviewDialog.answer.id,
      {
        reviewerUserId: userStore.currentUser.id,
        status: reviewForm.status,
        feedback: reviewForm.feedback
      }
    )

    // 检查是否成功处理
    const isSuccess = response.data?.status === 'success' || !!response.id

    if (isSuccess) {
      ElMessage.success('审核提交成功')
      reviewDialog.visible = false
      // 刷新数据
      refreshData()
    } else {
      console.error('提交审核返回未知格式:', response)
      ElMessage.warning('提交审核可能未成功，请检查后重试')
    }
  } catch (error) {
    console.error('提交审核失败:', error)
    ElMessage.error('提交审核失败')
  } finally {
    loading.submit = false
  }
}

// 刷新数据
const refreshData = () => {
  if (activeTab.value === 'pending') {
    fetchPendingAnswers()
  } else {
    fetchReviewedAnswers()
  }
}

// 处理搜索
const handleSearch = () => {
  // 搜索功能已通过计算属性实现
}

// 处理状态过滤变化
const handleStatusFilterChange = () => {
  // 状态过滤功能已通过计算属性实现
}

// 处理标签页切换
const handleTabClick = () => {
  searchQuery.value = ''
  filterStatus.value = ''
  refreshData()
}

// 处理分页大小变化
const handleSizeChange = (tab: 'pending' | 'reviewed', size: number) => {
  pagination[tab].currentPage = 1
  pagination[tab].pageSize = size
  refreshData()
}

// 处理当前页变化
const handleCurrentChange = (page: number) => {
  if (activeTab.value === 'pending') {
    pagination.pending.currentPage = page
  } else {
    pagination.reviewed.currentPage = page
  }
  refreshData()
}

// 监听标签页变化
watch(activeTab, () => {
  refreshData()
})

// 组件挂载时加载数据
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.crowdsourced-answer-review {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.answer-text {
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap;
}

.answer-text-full {
  white-space: pre-wrap;
  border: 1px solid #eee;
  padding: 10px;
  border-radius: 4px;
  background-color: #f9f9f9;
  max-height: 300px;
  overflow-y: auto;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.review-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.answer-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.answer-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style>
