<template>
  <div class="question-standardization">
    <el-row :gutter="20">
      <el-col :span="8">
        <!-- 左侧原始问题列表 -->
        <el-card class="left-panel">
          <template #header>
            <div class="panel-header">
              <h3>原始问题列表</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索原始问题..."
                prefix-icon="el-icon-search"
                clearable
                style="width: 220px; margin-left: 10px;"
              />
            </div>
          </template>

          <div class="filter-bar">
            <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 150px; margin-right: 10px;">
              <el-option label="全部" value="" />
              <el-option label="未处理" value="PENDING" />
              <el-option label="已标准化" value="STANDARDIZED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>

            <el-switch
              v-model="onlyLatest"
              active-text="仅显示最新版本"
              inactive-text="显示所有版本"
              style="margin-right: 10px;"
              @change="fetchRawQuestions"
            />

            <el-select v-model="sortOrder" placeholder="排序方式" style="width: 150px;">
              <el-option label="收集时间 (新→旧)" value="collectionTime,desc" />
              <el-option label="收集时间 (旧→新)" value="collectionTime,asc" />
              <el-option label="问题文本 (A→Z)" value="questionText,asc" />
              <el-option label="问题文本 (Z→A)" value="questionText,desc" />
            </el-select>
          </div>

          <el-table
            :data="filteredRawQuestions"
            style="width: 100%"
            @row-click="handleRawQuestionClick"
            border
            highlight-current-row
            v-loading="loading.rawQuestions"
          >
            <el-table-column label="问题文本" prop="questionText" show-overflow-tooltip />
            <el-table-column label="来源" prop="source" width="100" />
            <el-table-column label="收集时间" width="100">
              <template #default="{ row }">
                {{ formatDate(row.collectionTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              layout="prev, pager, next"
              :total="totalRawQuestions"
              :page-size="pageSize"
              :current-page="currentPage"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <!-- 右侧标准问题编辑区域 -->
        <el-card class="right-panel">
          <template #header>
            <div class="panel-header">
              <h3>标准问题创建</h3>
              <el-button type="primary" @click="createStandardQuestion" :disabled="!selectedRawQuestion">创建标准问题</el-button>
            </div>
          </template>

          <div v-if="!selectedRawQuestion" class="empty-state">
            <el-empty description="请从左侧选择一个原始问题" />
          </div>

          <div v-else class="standard-question-form">
            <div class="original-question-display">
              <h4>原始问题</h4>
              <div class="question-content">
                <p><strong>问题文本：</strong> {{ selectedRawQuestion.questionText }}</p>
                <p><strong>来源：</strong> {{ selectedRawQuestion.source }}</p>
                <p><strong>收集时间：</strong> {{ formatDate(selectedRawQuestion.collectionTime) }}</p>
                <p v-if="selectedRawQuestion.tags && selectedRawQuestion.tags.length > 0">
                  <strong>标签：</strong>
                  <el-tag
                    v-for="tag in selectedRawQuestion.tags"
                    :key="tag"
                    size="small"
                    style="margin-right: 5px;"
                  >
                    {{ tag }}
                  </el-tag>
                </p>
              </div>
            </div>

            <el-divider />

            <el-form :model="standardQuestionForm" label-width="120px" ref="standardQuestionFormRef">
              <el-form-item label="问题文本" prop="questionText" :rules="[{ required: true, message: '请输入标准问题文本', trigger: 'blur' }]">
                <el-input
                  v-model="standardQuestionForm.questionText"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入标准问题文本"
                />
              </el-form-item>

              <el-form-item label="题型" prop="questionType" :rules="[{ required: true, message: '请选择题型', trigger: 'change' }]">
                <el-select v-model="standardQuestionForm.questionType" placeholder="请选择题型">
                  <el-option label="单选题" value="SINGLE_CHOICE" />
                  <el-option label="多选题" value="MULTIPLE_CHOICE" />
                  <el-option label="简单事实题" value="SIMPLE_FACT" />
                  <el-option label="主观题" value="SUBJECTIVE" />
                </el-select>
              </el-form-item>

              <el-form-item label="难度" prop="difficulty" :rules="[{ required: true, message: '请选择难度', trigger: 'change' }]">
                <el-select v-model="standardQuestionForm.difficulty" placeholder="请选择难度">
                  <el-option label="简单" value="EASY" />
                  <el-option label="中等" value="MEDIUM" />
                  <el-option label="困难" value="HARD" />
                </el-select>
              </el-form-item>

              <el-form-item label="标签" prop="tags" :rules="[{ required: true, message: '请至少添加一个标签', trigger: 'change' }]">
                <el-tag
                  v-for="tag in standardQuestionForm.tags"
                  :key="tag"
                  closable
                  @close="handleRemoveTag(tag)"
                  style="margin-right: 5px; margin-bottom: 5px;"
                >
                  {{ tag }}
                </el-tag>

                <el-input
                  v-if="tagInputVisible"
                  ref="tagInputRef"
                  v-model="tagInputValue"
                  class="tag-input"
                  size="mini"
                  @keyup.enter="handleAddTag"
                  @blur="handleAddTag"
                />

                <el-button v-else class="button-new-tag" size="small" @click="showTagInput">
                  + 新标签
                </el-button>

                <el-button size="small" type="info" @click="handleCopyOriginalTags" v-if="selectedRawQuestion.tags && selectedRawQuestion.tags.length > 0">
                  引用原始标签
                </el-button>
              </el-form-item>

              <el-form-item label="推荐标签">
                <div class="recommended-tags">
                  <el-tag
                    v-for="tag in recommendedTags"
                    :key="tag"
                    @click="handleAddRecommendedTag(tag)"
                    style="margin-right: 5px; margin-bottom: 5px; cursor: pointer;"
                    effect="plain"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </el-form-item>

              <el-form-item label="提交说明" prop="commitMessage" :rules="[{ required: true, message: '请输入提交说明', trigger: 'blur' }]">
                <el-input
                  v-model="standardQuestionForm.commitMessage"
                  placeholder="请简要描述此标准问题的创建原因"
                />
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  searchStandardQuestions,
  createStandardQuestion as apiCreateStandardQuestion
} from '@/api/standardData'
import { recommendTags } from '@/api/tags'
import { getRawQuestionsByStatus } from '@/api/rawData'
import type { RecommendTagsRequest, } from '@/api/tags'
import type { QuestionType, DifficultyLevel } from '@/api/standardData'
import type { RawQuestionSearchItem, RawQuestionPageResponse } from '@/types/rawData'
import type { StandardQuestionDto } from '@/types/standardQuestion'

// 定义后端API返回的原始问题结构
interface RawQuestionApiItem {
  id: number
  title: string
  content: string
  sourceSite: string
  sourceUrl: string
  crawlTime: string
  tags: string[]
  standardized: boolean
  standardQuestionId?: number | null
  otherMetadata?: string
}

// 定义后端API分页响应结构
interface RawQuestionApiResponse {
  content: RawQuestionApiItem[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
}

// 组件状态
const loading = reactive({
  rawQuestions: false,
  creating: false
})

const currentPage = ref(1)
const pageSize = ref(10)
const totalRawQuestions = ref(0)
const searchQuery = ref('')
const filterStatus = ref('')
const sortOrder = ref('collectionTime,desc')
const rawQuestions = ref<RawQuestionSearchItem[]>([])
const selectedRawQuestion = ref<RawQuestionSearchItem | null>(null)
const standardQuestionFormRef = ref<FormInstance>()
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref<HTMLInputElement>()
const recommendedTags = ref<string[]>([])
const onlyLatest = ref(true)

// 表单数据
const standardQuestionForm = reactive<StandardQuestionDto>({
  questionText: '',
  questionType: 'SINGLE_CHOICE' as QuestionType,
  difficulty: 'MEDIUM' as DifficultyLevel,
  tags: [],
  commitMessage: '',
  originalRawQuestionId: 0,
  userId: 1 // 应该从用户状态获取
})

// 过滤原始问题
const filteredRawQuestions = computed(() => {
  if (!searchQuery.value) return rawQuestions.value

  const query = searchQuery.value.toLowerCase()
  return rawQuestions.value.filter(question =>
    question.questionText.toLowerCase().includes(query) ||
    question.source.toLowerCase().includes(query)
  )
})

// 生命周期钩子
onMounted(() => {
  fetchRawQuestions()
})

// 获取原始问题列表
const fetchRawQuestions = async () => {
  loading.rawQuestions = true
  try {
    const params = {
      standardized: filterStatus.value === 'STANDARDIZED' ? true :
                   filterStatus.value === 'PENDING' ? false : undefined,
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize.value,
      sort: sortOrder.value
    }

    const response = await getRawQuestionsByStatus(params)

    // 映射后端返回的数据结构到组件期望的结构
    if (response && response.content) {
      rawQuestions.value = response.content.map((item) => ({
        id: item.id,
        questionText: item.title || item.content,
        source: item.sourceSite || '未知',
        collectionTime: item.crawlTime,
        tags: item.tags || [],
        status: item.standardized ? 'STANDARDIZED' : 'PENDING'
      }));
      totalRawQuestions.value = response.totalElements || 0;
    } else {
      rawQuestions.value = [];
      totalRawQuestions.value = 0;
    }
  } catch (error) {
    console.error('获取原始问题失败:', error)
    ElMessage.error('获取原始问题失败')
  } finally {
    loading.rawQuestions = false
  }
}

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'PENDING':
      return 'info'
    case 'STANDARDIZED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return '未处理'
    case 'STANDARDIZED':
      return '已标准化'
    case 'REJECTED':
      return '已拒绝'
    default:
      return '未知'
  }
}

// 处理原始问题点击
const handleRawQuestionClick = (row: RawQuestionSearchItem) => {
  selectedRawQuestion.value = row
  standardQuestionForm.questionText = row.questionText
  standardQuestionForm.originalRawQuestionId = row.id

  // 获取推荐标签
  fetchRecommendedTags(row.questionText)
}

// 获取推荐标签
const fetchRecommendedTags = async (questionText: string) => {
  try {
    const request: RecommendTagsRequest = {
      text: questionText,
      questionType: standardQuestionForm.questionType,
      existingTags: standardQuestionForm.tags,
      onlyLatest: onlyLatest.value
    }
    const response = await recommendTags(request)
    recommendedTags.value = response.tags
  } catch (error) {
    console.error('获取推荐标签失败:', error)
    recommendedTags.value = []
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchRawQuestions()
}

// 标签相关方法
const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

const handleAddTag = () => {
  if (tagInputValue.value) {
    if (!standardQuestionForm.tags.includes(tagInputValue.value)) {
      standardQuestionForm.tags.push(tagInputValue.value)
    }
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

const handleRemoveTag = (tag: string) => {
  standardQuestionForm.tags = standardQuestionForm.tags.filter((t: string) => t !== tag)
}

const handleAddRecommendedTag = (tag: string) => {
  if (!standardQuestionForm.tags.includes(tag)) {
    standardQuestionForm.tags.push(tag)
  }
}

const handleCopyOriginalTags = () => {
  if (selectedRawQuestion.value && selectedRawQuestion.value.tags) {
    selectedRawQuestion.value.tags.forEach((tag: string) => {
      if (!standardQuestionForm.tags.includes(tag)) {
        standardQuestionForm.tags.push(tag)
      }
    })
  }
}

// 创建标准问题
const createStandardQuestion = async () => {
  if (!standardQuestionFormRef.value) return

  await standardQuestionFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.creating = true
      try {
        await apiCreateStandardQuestion(standardQuestionForm)
        ElMessage.success('标准问题创建成功')

        // 重置表单
        standardQuestionForm.questionText = ''
        standardQuestionForm.questionType = 'SINGLE_CHOICE' as QuestionType
        standardQuestionForm.difficulty = 'MEDIUM' as DifficultyLevel
        standardQuestionForm.tags = []
        standardQuestionForm.commitMessage = ''
        standardQuestionForm.originalRawQuestionId = 0

        // 刷新原始问题列表
        fetchRawQuestions()
        selectedRawQuestion.value = null
      } catch (error) {
        console.error('创建标准问题失败:', error)
        ElMessage.error('创建标准问题失败')
      } finally {
        loading.creating = false
      }
    }
  })
}
</script>

<style scoped>
.question-standardization {
  padding: 20px;
}

.left-panel, .right-panel {
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
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 15px;
  text-align: center;
}

.empty-state {
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.original-question-display {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.question-content {
  margin-top: 10px;
}

.tag-input {
  width: 100px;
  margin-right: 5px;
  vertical-align: bottom;
}

.recommended-tags {
  margin-top: 5px;
}
</style>
