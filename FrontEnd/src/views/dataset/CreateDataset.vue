<template>
  <div class="create-dataset-container">
    <!-- 数据集信息卡片 -->
    <el-card class="dataset-info-card">
      <template #header>
        <div class="card-header">
          <h2>创建数据集</h2>
          <div class="header-actions">
            <el-button @click="backToList" :icon="Back">
              返回列表
            </el-button>
          </div>
        </div>
      </template>

      <el-form
        ref="datasetFormRef"
        :model="datasetForm"
        :rules="datasetRules"
        label-width="100px"
        class="dataset-form"
      >
        <el-form-item label="版本号" prop="versionNumber">
          <el-input v-model="datasetForm.versionNumber" placeholder="请输入版本号，如 1.0.0" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="datasetForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="datasetForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入数据集描述"
          />
        </el-form-item>
        <el-form-item label="选中问题">
          <div class="selected-info">
            已选择 <span class="selected-count">{{ selectedQuestions.length }}</span> 个问题
            <el-button
              type="primary"
              @click="submitDataset"
              :disabled="!selectedQuestions.length || isSubmitting"
              :loading="isSubmitting"
            >
              创建数据集
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 标准问题选择卡片 -->
    <el-card class="question-select-card">
      <template #header>
        <div class="card-header">
          <h3>选择标准问题</h3>
          <div class="search-container">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索问题..."
              clearable
              @clear="searchQuestions"
              style="width: 240px;"
            >
              <template #append>
                <el-button :icon="Search" @click="searchQuestions" />
              </template>
            </el-input>
            <el-input
              v-model="searchTags"
              placeholder="标签(用逗号分隔)"
              clearable
              @clear="searchQuestions"
              style="width: 200px; margin-left: 10px;"
            />
            <el-checkbox
              v-model="onlyWithStandardAnswers"
              label="仅显示有标准答案的问题"
              @change="searchQuestions"
              style="margin-left: 10px;"
            />
            <el-checkbox
              v-model="onlyLatestQuestions"
              label="仅显示最新问题"
              @change="searchQuestions"
              style="margin-left: 10px;"
            />
            <el-button type="primary" @click="searchQuestions" :loading="isSearching">搜索</el-button>
          </div>
        </div>
      </template>

      <!-- 加载中状态 -->
      <div v-if="isSearching" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="!searchResults.length" class="empty-container">
        <el-empty description="暂无符合条件的标准问题" />
      </div>

      <!-- 问题列表 -->
      <div v-else>
        <el-table
          ref="questionTableRef"
          :data="searchResults"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
          row-key="id"
          :max-height="600"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="questionText" label="问题文本" min-width="300" show-overflow-tooltip />
          <el-table-column prop="questionType" label="类型" width="120">
            <template #default="scope">
              <el-tag :type="getQuestionTypeTagType(scope.row.questionType)">
                {{ getQuestionTypeDisplay(scope.row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="80">
            <template #default="scope">
              <el-tag :type="getDifficultyTagType(scope.row.difficulty)">
                {{ getDifficultyDisplay(scope.row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标准答案" width="100" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.hasStandardAnswer" type="success">已有</el-tag>
              <el-tag v-else type="info">无</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tags" label="标签" min-width="180">
            <template #default="scope">
              <div class="tag-list">
                <el-tag
                  v-for="tag in scope.row.tags"
                  :key="tag"
                  size="small"
                  style="margin: 2px"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页器 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="searchPage"
            v-model:page-size="searchPageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            :total="searchTotal"
            @size-change="handleSearchSizeChange"
            @current-change="handleSearchPageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type TableInstance } from 'element-plus'
import { searchStandardQuestions } from '@/api/standardData'
import { createDatasetVersion } from '@/api/dataset'
import type { SearchQuestionItem } from '@/types/standardQuestion'
import { useUserStore } from '@/stores/user'

// 路由
const router = useRouter()

// 用户信息
const userStore = useUserStore()

// 数据集表单
const datasetFormRef = ref<FormInstance | null>(null)
const datasetForm = reactive({
  versionNumber: '',
  name: '',
  description: ''
})

// 表单验证规则
const datasetRules = {
  versionNumber: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+(\.\d+)?$/, message: '版本号格式应为 x.y.z', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入数据集描述', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ]
}

// 搜索状态
const searchKeyword = ref('')
const searchTags = ref('')
const searchResults = ref<SearchQuestionItem[]>([])
const selectedQuestions = ref<SearchQuestionItem[]>([])
const isSearching = ref(false)
const isSubmitting = ref(false)
const searchPage = ref(1)
const searchPageSize = ref(10)
const searchTotal = ref(0)
const onlyWithStandardAnswers = ref(true)
const onlyLatestQuestions = ref(true)

// 表格引用
const questionTableRef = ref<TableInstance | null>(null)

// 返回列表页
const backToList = () => {
  router.push('/dataset/list')
}

// 搜索标准问题
const searchQuestions = async () => {
  try {
    isSearching.value = true
    const response = await searchStandardQuestions({
      keyword: searchKeyword.value,
      tags: searchTags.value,
      page: (searchPage.value - 1).toString(), // 后端分页从0开始
      size: searchPageSize.value.toString(),
      onlyLatest: onlyLatestQuestions.value ? 'true' : 'false',
      onlyWithStandardAnswers: onlyWithStandardAnswers.value ? 'true' : undefined
    })

    if (response.success) {
      searchResults.value = response.questions
      searchTotal.value = response.total

      // 恢复之前的选择状态
      nextTick(() => {
        restoreSelection()
      })
    } else {
      ElMessage.error('搜索问题失败')
    }
  } catch (error) {
    console.error('搜索问题失败:', error)
    ElMessage.error('搜索问题失败')
  } finally {
    isSearching.value = false
  }
}

// 恢复选择状态
const restoreSelection = () => {
  if (!questionTableRef.value) return

  searchResults.value.forEach(row => {
    if (selectedQuestions.value.some(q => q.id === row.id)) {
      questionTableRef.value?.toggleRowSelection(row, true)
    }
  })
}

// 处理表格选择变化
const handleSelectionChange = (selection: SearchQuestionItem[]) => {
  // 更新已选列表，但保持之前页面选择的项目
  const currentPageIds = searchResults.value.map(q => q.id)

  // 移除当前页取消选择的项目
  selectedQuestions.value = selectedQuestions.value.filter(
    q => !currentPageIds.includes(q.id) || selection.some(s => s.id === q.id)
  )

  // 添加当前页新选择的项目
  selection.forEach(item => {
    if (!selectedQuestions.value.some(q => q.id === item.id)) {
      selectedQuestions.value.push(item)
    }
  })
}

// 处理搜索分页大小变化
const handleSearchSizeChange = (val: number) => {
  searchPageSize.value = val
  searchQuestions()
}

// 处理搜索页码变化
const handleSearchPageChange = (val: number) => {
  searchPage.value = val
  searchQuestions()
}

// 创建数据集
const submitDataset = async () => {
  if (!datasetFormRef.value) return

  try {
    await datasetFormRef.value.validate()

    if (selectedQuestions.value.length === 0) {
      ElMessage.warning('请至少选择一个标准问题')
      return
    }

    isSubmitting.value = true
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    await createDatasetVersion({
      versionNumber: datasetForm.versionNumber,
      name: datasetForm.name,
      description: datasetForm.description,
      standardQuestionIds: selectedQuestions.value.map(q => q.id),
      userId: currentUser.id
    })

    ElMessage.success('创建数据集成功')
    router.push('/dataset/list')
  } catch (error) {
    console.error('创建数据集失败:', error)
    ElMessage.error('创建数据集失败')
  } finally {
    isSubmitting.value = false
  }
}

// 获取问题类型显示文本
const getQuestionTypeDisplay = (type: string): string => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题',
    'SUBJECTIVE': '主观题'
  }
  return map[type] || type
}

// 获取问题类型标签样式
const getQuestionTypeTagType = (type: string): string => {
  const map: Record<string, string> = {
    'SINGLE_CHOICE': 'success',
    'MULTIPLE_CHOICE': 'warning',
    'SIMPLE_FACT': 'info',
    'SUBJECTIVE': 'primary'
  }
  return map[type] || ''
}

// 获取难度显示文本
const getDifficultyDisplay = (difficulty: string): string => {
  const map: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return map[difficulty] || difficulty
}

// 获取难度标签样式
const getDifficultyTagType = (difficulty: string): string => {
  const map: Record<string, string> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return map[difficulty] || ''
}

// 生命周期钩子
onMounted(() => {
  // 初始化时搜索有标准答案的问题
  searchQuestions()
})
</script>

<style scoped>
.create-dataset-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dataset-info-card,
.question-select-card {
  width: 100%;
}

.dataset-form {
  max-width: 800px;
}

.search-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.selected-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-count {
  font-weight: bold;
  color: #409EFF;
  font-size: 18px;
  margin: 0 4px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-container {
  padding: 20px 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
}
</style>
