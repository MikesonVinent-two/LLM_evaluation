<template>
  <div class="expert-answer-review">
    <h1>专家回答评分工作台</h1>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="未评分回答" name="unrated">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>待评分专家回答</h3>
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

          <!-- 未评分回答列表 -->
          <el-table
            :data="filteredUnratedAnswers"
            style="width: 100%"
            v-loading="loading.unrated"
            border
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userUsername" label="专家用户名" width="120" />
            <el-table-column prop="standardQuestionId" label="问题ID" width="100" />
            <el-table-column label="回答内容" min-width="300">
              <template #default="{ row }">
                <div class="answer-text">{{ row.candidateAnswerText }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="submissionTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="openRatingDialog(row)"
                >
                  评分
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.unrated.currentPage"
              v-model:page-size="pagination.unrated.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.unrated.total"
              @size-change="(size: number) => handleSizeChange('unrated', size)"
              @current-change="(page: number) => handleCurrentChange(page)"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="已评分回答" name="rated">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>已评分专家回答</h3>
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

          <!-- 已评分回答列表 -->
          <el-table
            :data="filteredRatedAnswers"
            style="width: 100%"
            v-loading="loading.rated"
            border
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userUsername" label="专家用户名" width="120" />
            <el-table-column prop="standardQuestionId" label="问题ID" width="100" />
            <el-table-column label="回答内容" min-width="250">
              <template #default="{ row }">
                <div class="answer-text">{{ row.candidateAnswerText }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="submissionTime" label="提交时间" width="180" />
            <el-table-column label="评分" width="100">
              <template #default="{ row }">
                <el-tag :type="getScoreTagType(row.qualityScore)">
                  {{ row.qualityScore }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="feedback" label="评价反馈" min-width="200" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="openRatingDialog(row)"
                >
                  修改评分
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.rated.currentPage"
              v-model:page-size="pagination.rated.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.rated.total"
              @size-change="(size: number) => handleSizeChange('rated', size)"
              @current-change="(page: number) => handleCurrentChange(page)"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 评分对话框 -->
    <el-dialog
      v-model="ratingDialog.visible"
      :title="ratingDialog.isEdit ? '修改评分' : '评分专家回答'"
      width="60%"
    >
      <div v-if="ratingDialog.answer" class="rating-dialog-content">
        <div class="answer-details">
          <h4>问题ID: {{ ratingDialog.answer.standardQuestionId }}</h4>
          <h4>专家用户: {{ ratingDialog.answer.userUsername }}</h4>
          <div class="answer-content">
            <h4>回答内容:</h4>
            <div class="answer-text-full">{{ ratingDialog.answer.candidateAnswerText }}</div>
          </div>
        </div>

        <el-form :model="ratingForm" label-width="120px">
          <el-form-item label="质量评分">
            <el-slider
              v-model="ratingForm.qualityScore"
              :min="0"
              :max="100"
              :step="1"
              show-input
            />
          </el-form-item>
          <el-form-item label="评价反馈">
            <el-input
              v-model="ratingForm.feedback"
              type="textarea"
              :rows="4"
              placeholder="请输入对这个回答的评价反馈..."
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="ratingDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="submitRating" :loading="loading.submit">
            提交评分
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
  getUnratedExpertAnswers,
  getExpertAnswersByUser,
  rateExpertAnswer
} from '@/api/expertAnswer'
import type { ExpertAnswerResponse } from '@/types/expertAnswer'

// 用户信息
const userStore = useUserStore()

// 标签页状态
const activeTab = ref('unrated')

// 搜索查询
const searchQuery = ref('')

// 加载状态
const loading = reactive({
  unrated: false,
  rated: false,
  submit: false
})

// 分页配置
const pagination = reactive({
  unrated: {
    currentPage: 1,
    pageSize: 10,
    total: 0
  },
  rated: {
    currentPage: 1,
    pageSize: 10,
    total: 0
  }
})

// 专家回答数据
const unratedAnswers = ref<ExpertAnswerResponse[]>([])
const ratedAnswers = ref<ExpertAnswerResponse[]>([])

// 评分对话框
const ratingDialog = reactive({
  visible: false,
  isEdit: false,
  answer: null as ExpertAnswerResponse | null
})

// 评分表单
const ratingForm = reactive({
  qualityScore: 80,
  feedback: ''
})

// 过滤后的回答列表
const filteredUnratedAnswers = computed(() => {
  if (!searchQuery.value) return unratedAnswers.value

  const query = searchQuery.value.toLowerCase()
  return unratedAnswers.value.filter(answer =>
    answer.candidateAnswerText.toLowerCase().includes(query) ||
    answer.userUsername.toLowerCase().includes(query)
  )
})

const filteredRatedAnswers = computed(() => {
  if (!searchQuery.value) return ratedAnswers.value

  const query = searchQuery.value.toLowerCase()
  return ratedAnswers.value.filter(answer =>
    answer.candidateAnswerText.toLowerCase().includes(query) ||
    answer.userUsername.toLowerCase().includes(query) ||
    (answer.feedback && answer.feedback.toLowerCase().includes(query))
  )
})

// 获取评分标签类型
const getScoreTagType = (score: number) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 获取未评分的专家回答
const fetchUnratedAnswers = async () => {
  loading.unrated = true
  try {
    const response = await getUnratedExpertAnswers({
      page: String(pagination.unrated.currentPage - 1),
      size: String(pagination.unrated.pageSize),
      sort: 'submissionTime,desc'
    })

    unratedAnswers.value = response.content
    pagination.unrated.total = response.totalElements
  } catch (error) {
    console.error('获取未评分专家回答失败:', error)
    ElMessage.error('获取未评分专家回答失败')
  } finally {
    loading.unrated = false
  }
}

// 获取已评分的专家回答
const fetchRatedAnswers = async () => {
  if (!userStore.currentUser?.id) {
    ElMessage.warning('用户未登录')
    return
  }

  loading.rated = true
  try {
    const response = await getExpertAnswersByUser(
      userStore.currentUser.id,
      {
        page: String(pagination.rated.currentPage - 1),
        size: String(pagination.rated.pageSize),
        sort: 'submissionTime,desc'
      }
    )

    // 过滤出已评分的回答
    ratedAnswers.value = response.content.filter(answer => answer.qualityScore > 0)
    pagination.rated.total = ratedAnswers.value.length
  } catch (error) {
    console.error('获取已评分专家回答失败:', error)
    ElMessage.error('获取已评分专家回答失败')
  } finally {
    loading.rated = false
  }
}

// 打开评分对话框
const openRatingDialog = (answer: ExpertAnswerResponse) => {
  ratingDialog.answer = answer
  ratingDialog.isEdit = answer.qualityScore > 0
  ratingForm.qualityScore = answer.qualityScore || 80
  ratingForm.feedback = answer.feedback || ''
  ratingDialog.visible = true
}

// 提交评分
const submitRating = async () => {
  if (!ratingDialog.answer) return

  loading.submit = true
  try {
    await rateExpertAnswer(
      ratingDialog.answer.id,
      {
        qualityScore: ratingForm.qualityScore,
        feedback: ratingForm.feedback
      }
    )

    ElMessage.success('评分提交成功')
    ratingDialog.visible = false

    // 刷新数据
    refreshData()
  } catch (error) {
    console.error('提交评分失败:', error)
    ElMessage.error('提交评分失败')
  } finally {
    loading.submit = false
  }
}

// 刷新数据
const refreshData = () => {
  if (activeTab.value === 'unrated') {
    fetchUnratedAnswers()
  } else {
    fetchRatedAnswers()
  }
}

// 处理搜索
const handleSearch = () => {
  // 搜索功能已通过计算属性实现
}

// 处理标签页切换
const handleTabClick = () => {
  searchQuery.value = ''
  refreshData()
}

// 处理分页大小变化
const handleSizeChange = (tab: 'unrated' | 'rated', size: number) => {
  pagination[tab].currentPage = 1
  pagination[tab].pageSize = size
  refreshData()
}

// 处理当前页变化
const handleCurrentChange = (page: number) => {
  if (activeTab.value === 'unrated') {
    pagination.unrated.currentPage = page
  } else {
    pagination.rated.currentPage = page
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
.expert-answer-review {
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

.rating-dialog-content {
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
 