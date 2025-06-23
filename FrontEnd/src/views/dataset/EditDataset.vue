<template>
  <div class="edit-dataset-container">
    <!-- 无效ID提示页面 -->
    <el-card v-if="!isValidId" class="empty-dataset-card">
      <template #header>
        <div class="card-header">
          <h2>数据集编辑</h2>
          <div class="header-actions">
            <el-button @click="backToList" :icon="Back">
              返回列表
            </el-button>
          </div>
        </div>
      </template>
      <div class="empty-dataset-content">
        <div class="empty-illustration">
          <el-empty description="未找到有效的数据集ID" :image-size="200" />
        </div>
        <div class="guide-content">
          <h3 class="guide-title">数据集编辑功能说明</h3>
          <p class="guide-text">该页面用于编辑数据集中的问题，您可以：</p>
          <ul class="feature-list">
            <li><el-icon><Plus /></el-icon> 添加或删除数据集中的问题</li>
            <li><el-icon><Edit /></el-icon> 修改数据集的基本信息</li>
            <li><el-icon><Files /></el-icon> 管理数据集版本</li>
          </ul>
          <div class="action-buttons">
            <el-button type="primary" size="large" @click="goToDatasetList">
              <el-icon><List /></el-icon> 查看数据集列表
            </el-button>
            <el-button type="success" size="large" @click="goToCreateDataset">
              <el-icon><Plus /></el-icon> 创建新数据集
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <template v-else>
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <h2>编辑数据集: {{ datasetName }}</h2>
          <div class="header-actions">
            <el-button @click="backToList" :icon="Back">
              返回列表
            </el-button>
            <el-button type="primary" @click="saveChanges" :loading="isSaving">
              保存更改
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="datasetForm" label-width="100px" class="dataset-form">
        <el-form-item label="版本号">
          <el-input v-model="datasetForm.versionNumber" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="datasetForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="datasetForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入数据集描述"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 问题管理区域 -->
    <div class="questions-container">
      <!-- 左侧：数据集内问题 -->
      <el-card class="questions-card included-questions">
        <template #header>
          <div class="card-header">
            <h3>数据集内问题 ({{ includedQuestions.length }})</h3>
            <div class="search-container">
              <el-input
                v-model="includedKeyword"
                placeholder="搜索已选问题..."
                clearable
                @clear="searchIncludedQuestions"
                style="width: 200px;"
              >
                <template #append>
                  <el-button :icon="Search" @click="searchIncludedQuestions" />
                </template>
              </el-input>
              <el-button
                type="danger"
                :disabled="!selectedIncludedQuestions.length"
                @click="removeSelectedQuestions"
              >
                移除所选 ({{ selectedIncludedQuestions.length }})
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="isLoadingIncluded" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="!displayedIncludedQuestions.length" class="empty-container">
          <el-empty description="暂无数据集内问题" />
        </div>
        <div v-else>
          <el-table
            ref="includedTableRef"
            :data="displayedIncludedQuestions"
            border
            stripe
            style="width: 100%"
            @selection-change="handleIncludedSelectionChange"
            height="500"
            :row-class-name="getIncludedRowClassName"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="questionText" label="问题文本" min-width="250" show-overflow-tooltip />
            <el-table-column prop="questionType" label="类型" width="100">
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
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.toBeRemoved" type="danger">将要移除</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="!scope.row.toBeRemoved"
                  size="small"
                  type="danger"
                  plain
                  @click="removeQuestion(scope.row)"
                >
                  移除
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="success"
                  plain
                  @click="cancelRemoveQuestion(scope.row)"
                >
                  取消移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页器 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="includedPage"
              v-model:page-size="includedPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              :total="includedTotal"
              @size-change="handleIncludedSizeChange"
              @current-change="handleIncludedPageChange"
            />
          </div>
        </div>
      </el-card>

      <!-- 右侧：可添加问题 -->
      <el-card class="questions-card available-questions">
        <template #header>
          <div class="card-header">
            <h3>可添加问题</h3>
            <div class="search-container">
              <el-input
                v-model="availableKeyword"
                placeholder="搜索问题..."
                clearable
                @clear="searchAvailableQuestions"
                style="width: 180px;"
              >
                <template #append>
                  <el-button :icon="Search" @click="searchAvailableQuestions" />
                </template>
              </el-input>
              <el-input
                v-model="availableTags"
                placeholder="标签(用逗号分隔)"
                clearable
                @clear="searchAvailableQuestions"
                style="width: 180px; margin-left: 10px;"
              />
              <el-checkbox
                v-model="onlyWithStandardAnswers"
                label="仅显示有标准答案"
                @change="searchAvailableQuestions"
                style="margin-left: 10px;"
              />
                <el-checkbox
                  v-model="onlyLatestQuestions"
                  label="仅显示最新问题"
                  @change="searchAvailableQuestions"
                  style="margin-left: 10px;"
                />
              <el-button
                type="primary"
                :disabled="!selectedAvailableQuestions.length"
                @click="addSelectedQuestions"
              >
                添加所选 ({{ selectedAvailableQuestions.length }})
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="isLoadingAvailable" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="!availableQuestions.length" class="empty-container">
          <el-empty description="暂无可添加的问题" />
        </div>
        <div v-else>
          <el-table
            ref="availableTableRef"
            :data="availableQuestions"
            border
            stripe
            style="width: 100%"
            @selection-change="handleAvailableSelectionChange"
            height="500"
            :row-class-name="getAvailableRowClassName"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="questionText" label="问题文本" min-width="250" show-overflow-tooltip />
            <el-table-column prop="questionType" label="类型" width="100">
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
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.toBeAdded" type="success">将要添加</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="!scope.row.toBeAdded"
                  size="small"
                  type="primary"
                  plain
                  @click="addQuestion(scope.row)"
                >
                  添加
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="warning"
                  plain
                  @click="cancelAddQuestion(scope.row)"
                >
                  取消添加
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页器 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="availablePage"
              v-model:page-size="availablePageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              :total="availableTotal"
              @size-change="handleAvailableSizeChange"
              @current-change="handleAvailablePageChange"
            />
          </div>
        </div>
      </el-card>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Back, Search, Plus, Edit, Files, List } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import {
  getQuestionsInDataset,
  getQuestionsOutsideDataset,
  updateDatasetVersion,
  getAllDatasetVersions,
  type StandardQuestionsByDatasetResponse
} from '@/api/dataset'
import { useUserStore } from '@/stores/user'

// 路由
const router = useRouter()
const route = useRoute()
const datasetId = ref<number>(Number(route.params.id))
const isValidId = computed(() => !!datasetId.value && !isNaN(datasetId.value))
const datasetName = ref<string>('')

// 用户信息
const userStore = useUserStore()

// 数据集表单
const datasetForm = reactive({
  versionNumber: '',
  name: '',
  description: ''
})

// 加载状态
const isLoadingIncluded = ref(false)
const isLoadingAvailable = ref(false)
const isSaving = ref(false)
const isLoadingDataset = ref(false)

// 已包含问题状态
const includedQuestions = ref<any[]>([])
const displayedIncludedQuestions = ref<any[]>([])
const includedKeyword = ref('')
const includedPage = ref(1)
const includedPageSize = ref(10)
const includedTotal = ref(0)
const selectedIncludedQuestions = ref<any[]>([])
const includedTableRef = ref<TableInstance | null>(null)

// 可用问题状态
const availableQuestions = ref<any[]>([])
const availableKeyword = ref('')
const availableTags = ref('')
const availablePage = ref(1)
const availablePageSize = ref(10)
const availableTotal = ref(0)
const selectedAvailableQuestions = ref<any[]>([])
const availableTableRef = ref<TableInstance | null>(null)
const onlyWithStandardAnswers = ref(true)
const onlyLatestQuestions = ref(true)

// 问题变更跟踪
const questionsToAdd = ref<number[]>([])
const questionsToRemove = ref<number[]>([])

// 导航函数
const goToDatasetList = () => {
  router.push('/dataset/list')
}

const goToCreateDataset = () => {
  router.push('/dataset/create')
}

// 返回列表页
const backToList = () => {
  router.push('/dataset/list')
}

// 获取数据集信息
const fetchDatasetInfo = async () => {
  try {
    isLoadingDataset.value = true
    const response = await getAllDatasetVersions()
    const dataset = response.find(item => item.id === datasetId.value)

    if (dataset) {
      datasetName.value = dataset.name
      datasetForm.versionNumber = dataset.versionNumber
      datasetForm.name = dataset.name
      datasetForm.description = dataset.description
    } else {
      ElMessage.error('未找到数据集信息')
    }
  } catch (error) {
    console.error('获取数据集信息失败:', error)
    ElMessage.error('获取数据集信息失败')
  } finally {
    isLoadingDataset.value = false
  }
}

// 获取数据集内问题
const fetchIncludedQuestions = async () => {
  try {
    isLoadingIncluded.value = true
    const response = await getQuestionsInDataset(datasetId.value, {
      keyword: includedKeyword.value,
      pageNumber: includedPage.value - 1, // 后端分页从0开始
      pageSize: includedPageSize.value
    })

    if (response.success) {
      // 为每个问题添加状态标记
      const questions = response.questions.map(q => ({
        ...q,
        toBeRemoved: questionsToRemove.value.includes(q.id)
      }))

      includedQuestions.value = questions
      displayedIncludedQuestions.value = filterIncludedQuestions()
      includedTotal.value = response.total

      // 如果数据集信息还未加载，从响应中获取数据集信息
      if (response.datasetName && !datasetName.value) {
        datasetName.value = response.datasetName
      }
      if (response.versionNumber && !datasetForm.versionNumber) {
        datasetForm.versionNumber = response.versionNumber
      }
    } else {
      ElMessage.error('获取数据集内问题失败')
    }
  } catch (error) {
    console.error('获取数据集内问题失败:', error)
    ElMessage.error('获取数据集内问题失败')
  } finally {
    isLoadingIncluded.value = false
  }
}

// 获取可添加问题
const fetchAvailableQuestions = async () => {
  try {
    isLoadingAvailable.value = true
    const response = await getQuestionsOutsideDataset(datasetId.value, {
      keyword: availableKeyword.value,
      tags: availableTags.value,
      pageNumber: availablePage.value - 1, // 后端分页从0开始
      pageSize: availablePageSize.value,
      onlyWithStandardAnswers: onlyWithStandardAnswers.value,
      onlyLatest: onlyLatestQuestions.value
    })

    if (response.success) {
      // 为每个问题添加状态标记
      const questions = response.questions.map(q => ({
        ...q,
        toBeAdded: questionsToAdd.value.includes(q.id)
      }))

      availableQuestions.value = questions
      availableTotal.value = response.total
    } else {
      ElMessage.error('获取可添加问题失败')
    }
  } catch (error) {
    console.error('获取可添加问题失败:', error)
    ElMessage.error('获取可添加问题失败')
  } finally {
    isLoadingAvailable.value = false
  }
}

// 过滤已包含问题（本地搜索）
const filterIncludedQuestions = () => {
  if (!includedKeyword.value) {
    return includedQuestions.value
  }
  const keyword = includedKeyword.value.toLowerCase()
  return includedQuestions.value.filter(q =>
    q.questionText.toLowerCase().includes(keyword) ||
    q.id.toString().includes(keyword)
  )
}

// 搜索已包含问题
const searchIncludedQuestions = () => {
  fetchIncludedQuestions()
}

// 搜索可添加问题
const searchAvailableQuestions = () => {
  availablePage.value = 1 // 重置页码
  fetchAvailableQuestions()
}

// 处理已包含问题分页大小变化
const handleIncludedSizeChange = (val: number) => {
  includedPageSize.value = val
  fetchIncludedQuestions()
}

// 处理已包含问题页码变化
const handleIncludedPageChange = (val: number) => {
  includedPage.value = val
  fetchIncludedQuestions()
}

// 处理可添加问题分页大小变化
const handleAvailableSizeChange = (val: number) => {
  availablePageSize.value = val
  fetchAvailableQuestions()
}

// 处理可添加问题页码变化
const handleAvailablePageChange = (val: number) => {
  availablePage.value = val
  fetchAvailableQuestions()
}

// 处理已包含问题选择变化
const handleIncludedSelectionChange = (selection: any[]) => {
  selectedIncludedQuestions.value = selection
}

// 处理可添加问题选择变化
const handleAvailableSelectionChange = (selection: any[]) => {
  selectedAvailableQuestions.value = selection
}

// 移除问题
const removeQuestion = (question: any) => {
  // 添加到待移除列表
  if (!questionsToRemove.value.includes(question.id)) {
    questionsToRemove.value.push(question.id)
  }

  // 如果之前计划添加，则从待添加列表中移除
  const addIndex = questionsToAdd.value.indexOf(question.id)
  if (addIndex !== -1) {
    questionsToAdd.value.splice(addIndex, 1)
  }

  // 更新问题状态而不是从列表中移除
  const index = includedQuestions.value.findIndex(q => q.id === question.id)
  if (index !== -1) {
    includedQuestions.value[index].toBeRemoved = true
    displayedIncludedQuestions.value = filterIncludedQuestions()
  }
}

// 取消移除问题
const cancelRemoveQuestion = (question: any) => {
  // 从待移除列表中移除
  const removeIndex = questionsToRemove.value.indexOf(question.id)
  if (removeIndex !== -1) {
    questionsToRemove.value.splice(removeIndex, 1)
  }

  // 更新问题状态
  const index = includedQuestions.value.findIndex(q => q.id === question.id)
  if (index !== -1) {
    includedQuestions.value[index].toBeRemoved = false
    displayedIncludedQuestions.value = filterIncludedQuestions()
  }
}

// 添加问题
const addQuestion = (question: any) => {
  // 添加到待添加列表
  if (!questionsToAdd.value.includes(question.id)) {
    questionsToAdd.value.push(question.id)
  }

  // 如果之前计划移除，则从待移除列表中移除
  const removeIndex = questionsToRemove.value.indexOf(question.id)
  if (removeIndex !== -1) {
    questionsToRemove.value.splice(removeIndex, 1)
  }

  // 更新问题状态
  const index = availableQuestions.value.findIndex(q => q.id === question.id)
  if (index !== -1) {
    availableQuestions.value[index].toBeAdded = true
  }
}

// 取消添加问题
const cancelAddQuestion = (question: any) => {
  // 从待添加列表中移除
  const addIndex = questionsToAdd.value.indexOf(question.id)
  if (addIndex !== -1) {
    questionsToAdd.value.splice(addIndex, 1)
  }

  // 更新问题状态
  const index = availableQuestions.value.findIndex(q => q.id === question.id)
  if (index !== -1) {
    availableQuestions.value[index].toBeAdded = false
  }
}

// 批量移除选中问题
const removeSelectedQuestions = () => {
  if (!selectedIncludedQuestions.value.length) return

  selectedIncludedQuestions.value.forEach(question => {
    removeQuestion(question)
  })

  selectedIncludedQuestions.value = []
}

// 批量添加选中问题
const addSelectedQuestions = () => {
  if (!selectedAvailableQuestions.value.length) return

  selectedAvailableQuestions.value.forEach(question => {
    addQuestion(question)
  })

  selectedAvailableQuestions.value = []
}

// 保存更改
const saveChanges = async () => {
  try {
    isSaving.value = true
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    await updateDatasetVersion(datasetId.value, {
      name: datasetForm.name,
      description: datasetForm.description,
      standardQuestionsToAdd: questionsToAdd.value,
      standardQuestionsToRemove: questionsToRemove.value,
      userId: currentUser.id
    })

    ElMessage.success('保存数据集更改成功')

    // 重置变更跟踪
    questionsToAdd.value = []
    questionsToRemove.value = []

    // 重置页码以确保能看到所有更改
    includedPage.value = 1
    availablePage.value = 1

    // 刷新数据
    await fetchIncludedQuestions()
    await fetchAvailableQuestions()
  } catch (error) {
    console.error('保存数据集更改失败:', error)
    ElMessage.error('保存数据集更改失败')
  } finally {
    isSaving.value = false
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

// 设置已包含问题行的类名
const getIncludedRowClassName = ({ row }: { row: any }): string => {
  if (row.toBeRemoved) {
    return 'to-be-removed'
  }
  return ''
}

// 设置可添加问题行的类名
const getAvailableRowClassName = ({ row }: { row: any }): string => {
  if (row.toBeAdded) {
    return 'to-be-added'
  }
  return ''
}

// 生命周期钩子
onMounted(() => {
  if (!isValidId.value) {
    console.warn('无效的数据集ID:', route.params.id)
    return
  }

  // 首先获取数据集基本信息
  fetchDatasetInfo()

  // 然后加载问题数据
  fetchIncludedQuestions()
  fetchAvailableQuestions()
})
</script>

<style scoped>
.edit-dataset-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.dataset-form {
  max-width: 800px;
}

.questions-container {
  display: flex;
  gap: 20px;
  width: 100%;
}

.questions-card {
  flex: 1;
  min-width: 0; /* 允许卡片在flex容器中缩小 */
}

.search-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.loading-container {
  padding: 20px 0;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 自定义滚动条样式 */
.el-table {
  --el-scrollbar-width: 8px;
  --el-scrollbar-hover-width: 10px;
}

/* 自定义行样式 */
:deep(.to-be-removed) {
  background-color: rgba(245, 108, 108, 0.1); /* 淡红色背景 */
}

:deep(.to-be-added) {
  background-color: rgba(103, 194, 58, 0.1); /* 淡绿色背景 */
}

@media (max-width: 1200px) {
  .questions-container {
    flex-direction: column;
  }
}

.empty-dataset-card {
  width: 100%;
  margin: 0;
  height: calc(100vh - 100px);
}

.empty-dataset-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 60px;
  height: 100%;
}

.empty-illustration {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.guide-content {
  flex: 1;
  text-align: left;
  max-width: 500px;
}

.guide-title {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.guide-text {
  margin: 20px 0;
  font-size: 16px;
  color: #606266;
}

.feature-list {
  margin: 30px 0;
  padding-left: 0;
  list-style: none;
}

.feature-list li {
  margin-bottom: 15px;
  color: #606266;
  font-size: 16px;
  display: flex;
  align-items: center;
}

.feature-list li .el-icon {
  margin-right: 10px;
  color: var(--el-color-primary);
  font-size: 18px;
}

.action-buttons {
  margin-top: 40px;
  display: flex;
  gap: 20px;
}

@media (max-width: 768px) {
  .empty-dataset-content {
    flex-direction: column;
    padding: 20px;
    gap: 30px;
  }

  .guide-content {
    text-align: center;
  }

  .feature-list li {
    justify-content: center;
  }
}
</style>
