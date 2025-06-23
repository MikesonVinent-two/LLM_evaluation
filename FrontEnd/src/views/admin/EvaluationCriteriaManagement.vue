<template>
  <div class="evaluation-criteria-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>评测标准管理</h2>
          <el-button type="primary" @click="handleCreateCriterion">
            <el-icon><Plus /></el-icon> 创建评测标准
          </el-button>
        </div>
      </template>

      <div class="management-content">
        <!-- 搜索区域 -->
        <div class="search-area">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索评测标准名称或描述"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>

          <div class="filter-area">
            <el-select v-model="questionTypeFilter" placeholder="题型筛选" clearable @change="handleSearch">
              <el-option label="主观题" value="SUBJECTIVE" />
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="简单事实题" value="SIMPLE_FACT" />
            </el-select>
          </div>
        </div>

        <!-- 数据表格 -->
        <el-table
          v-loading="loading"
          :data="filteredCriteria"
          border
          stripe
          style="width: 100%; margin-top: 15px;"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="questionType" label="适用题型" width="120">
            <template #default="scope">
              <el-tag :type="getQuestionTypeTagType(scope.row.questionType)">
                {{ getQuestionTypeDisplay(scope.row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicableQuestionTypes" label="适用问题类型" min-width="200">
            <template #default="scope">
              <div class="tag-list">
                <el-tag
                  v-for="type in scope.row.applicableQuestionTypes"
                  :key="type"
                  size="small"
                  :type="getQuestionTypeTagType(type)"
                  style="margin: 2px"
                >
                  {{ getQuestionTypeDisplay(type) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="scoreRange" label="分值范围" width="100" />
          <el-table-column prop="maxScore" label="最大分值" width="100" />
          <el-table-column prop="weight" label="权重" width="80" />
          <el-table-column prop="isRequired" label="必填" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.isRequired ? 'danger' : 'info'">
                {{ scope.row.isRequired ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="180">
            <template #default="scope">
              <el-button size="small" @click="handleViewCriterion(scope.row)">查看</el-button>
              <el-button size="small" type="primary" @click="handleEditCriterion(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteCriterion(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页器 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalElements"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 查看评测标准对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="评测标准详情"
      width="600px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ currentCriterion.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ currentCriterion.name }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ currentCriterion.version }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentCriterion.description }}</el-descriptions-item>
        <el-descriptions-item label="题型">
          <el-tag :type="getQuestionTypeTagType(currentCriterion.questionType)">
            {{ getQuestionTypeDisplay(currentCriterion.questionType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据类型">{{ currentCriterion.dataType }}</el-descriptions-item>
        <el-descriptions-item label="分值范围">{{ currentCriterion.scoreRange }}</el-descriptions-item>
        <el-descriptions-item label="最大分值">{{ currentCriterion.maxScore }}</el-descriptions-item>
        <el-descriptions-item label="适用问题类型">
          <div class="tag-list">
            <el-tag
              v-for="type in currentCriterion.applicableQuestionTypes"
              :key="type"
              size="small"
              :type="getQuestionTypeTagType(type)"
              style="margin: 2px"
            >
              {{ getQuestionTypeDisplay(type) }}
            </el-tag>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="权重">{{ currentCriterion.weight }}</el-descriptions-item>
        <el-descriptions-item label="是否必填">
          <el-tag :type="currentCriterion.isRequired ? 'danger' : 'info'">
            {{ currentCriterion.isRequired ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="排序索引">{{ currentCriterion.orderIndex }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(currentCriterion.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建者">
          {{ currentCriterion.createdByUser?.username || '未知' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 创建/编辑评测标准对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isEditMode ? '编辑评测标准' : '创建评测标准'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入评测标准名称" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号，如1.0" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入评测标准描述" />
        </el-form-item>
        <el-form-item label="题型" prop="questionType">
          <el-select v-model="form.questionType" placeholder="请选择题型">
            <el-option label="主观题" value="SUBJECTIVE" />
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="简单事实题" value="SIMPLE_FACT" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据类型" prop="dataType">
          <el-select v-model="form.dataType" placeholder="请选择数据类型">
            <el-option label="分数" value="SCORE" />
            <el-option label="布尔值" value="BOOLEAN" />
            <el-option label="文本" value="TEXT" />
          </el-select>
        </el-form-item>
        <el-form-item label="分值范围" prop="scoreRange">
          <el-input v-model="form.scoreRange" placeholder="请输入分值范围，如0-5" />
        </el-form-item>
        <el-form-item label="最大分值" prop="maxScore">
          <el-input-number v-model="form.maxScore" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="适用问题类型" prop="applicableQuestionTypes">
          <el-select v-model="form.applicableQuestionTypes" multiple placeholder="请选择适用问题类型">
            <el-option label="主观题" value="SUBJECTIVE" />
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="简单事实题" value="SIMPLE_FACT" />
          </el-select>
          <div class="form-help-text">可以选择多个适用的问题类型</div>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-slider v-model="form.weight" :min="0" :max="1" :step="0.05" :format-tooltip="formatWeight" />
        </el-form-item>
        <el-form-item label="是否必填" prop="isRequired">
          <el-switch v-model="form.isRequired" />
        </el-form-item>
        <el-form-item label="排序索引" prop="orderIndex">
          <el-input-number v-model="form.orderIndex" :min="1" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAllEvaluationCriteria,
  getEvaluationCriterionDetail,
  createEvaluationCriterion,
  updateEvaluationCriterion,
  deleteEvaluationCriterion,
  type EvaluationCriterion,
  type CreateEvaluationCriterionRequest,
  type UpdateEvaluationCriterionRequest
} from '@/api/evaluationCriteria'

// 搜索关键词
const searchKeyword = ref('')
const questionTypeFilter = ref('')

// 评测标准列表数据
const criteriaList = ref<EvaluationCriterion[]>([])
const loading = ref(false)

// 分页相关
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

// 过滤后的评测标准列表
const filteredCriteria = computed(() => {
  let result = criteriaList.value

  // 关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(
      item => item.name.toLowerCase().includes(keyword) ||
              item.description.toLowerCase().includes(keyword)
    )
  }

  // 题型过滤
  if (questionTypeFilter.value) {
    result = result.filter(item => item.questionType === questionTypeFilter.value)
  }

  return result
})

// 查看评测标准相关
const viewDialogVisible = ref(false)
const currentCriterion = ref<EvaluationCriterion>({
  id: 0,
  name: '',
  version: '',
  description: '',
  questionType: '',
  dataType: '',
  scoreRange: '',
  applicableQuestionTypes: [],
  weight: 0,
  isRequired: false,
  orderIndex: 0,
  options: {},
  createdAt: '',
  createdByUser: { id: 0, username: '' },
  parentCriterion: null,
  createdChangeLog: null,
  deletedAt: null
})

// 编辑评测标准相关
const editDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEditMode = ref(false)

// 表单数据
const form = reactive<CreateEvaluationCriterionRequest & { id?: number }>({
  name: '',
  version: '1.0',
  description: '',
  questionType: 'SUBJECTIVE',
  dataType: 'SCORE',
  scoreRange: '0-5',
  maxScore: 5,
  applicableQuestionTypes: ['SUBJECTIVE'],
  weight: 0.2,
  isRequired: true,
  orderIndex: 1,
  options: {}
})

// 表单验证规则
const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入评测标准名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度应为2-50个字符', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入评测标准描述', trigger: 'blur' }
  ],
  questionType: [
    { required: true, message: '请选择题型', trigger: 'change' }
  ],
  dataType: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ],
  scoreRange: [
    { required: true, message: '请输入分值范围', trigger: 'blur' }
  ],
  applicableQuestionTypes: [
    { required: true, message: '请选择适用问题类型', trigger: 'change' }
  ],
  weight: [
    { required: true, message: '请设置权重', trigger: 'change' }
  ],
  orderIndex: [
    { required: true, message: '请设置排序索引', trigger: 'change' }
  ]
})

// 初始化加载评测标准数据
onMounted(() => {
  fetchCriteriaList()
})

// 获取评测标准列表
const fetchCriteriaList = async () => {
  loading.value = true
  try {
    const response = await getAllEvaluationCriteria({
      page: currentPage.value,
      size: pageSize.value
    })
    criteriaList.value = response
    totalElements.value = response.length // 假设后端暂时没有返回总数，使用当前页的数量
  } catch (error) {
    console.error('获取评测标准列表失败', error)
    ElMessage.error('获取评测标准列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 0
  fetchCriteriaList()
}

// 分页大小变化处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchCriteriaList()
}

// 页码变化处理
const handleCurrentChange = (page: number) => {
  currentPage.value = page - 1 // API从0开始计数
  fetchCriteriaList()
}

// 查看评测标准详情
const handleViewCriterion = async (criterion: EvaluationCriterion) => {
  try {
    // 获取最新的评测标准详情
    const detail = await getEvaluationCriterionDetail(criterion.id)
    currentCriterion.value = detail
    viewDialogVisible.value = true
  } catch (error) {
    console.error('获取评测标准详情失败', error)
    ElMessage.error('获取评测标准详情失败')
  }
}

// 创建评测标准
const handleCreateCriterion = () => {
  isEditMode.value = false
  // 重置表单
  Object.assign(form, {
    name: '',
    version: '1.0',
    description: '',
    questionType: 'SUBJECTIVE',
    dataType: 'SCORE',
    scoreRange: '0-5',
    maxScore: 5,
    applicableQuestionTypes: ['SUBJECTIVE'],
    weight: 0.2,
    isRequired: true,
    orderIndex: 1,
    options: {}
  })
  editDialogVisible.value = true
}

// 编辑评测标准
const handleEditCriterion = async (criterion: EvaluationCriterion) => {
  isEditMode.value = true
  try {
    // 获取最新的评测标准详情
    const detail = await getEvaluationCriterionDetail(criterion.id)

    // 填充表单数据
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      version: detail.version,
      description: detail.description,
      questionType: detail.questionType,
      dataType: detail.dataType,
      scoreRange: detail.scoreRange,
      maxScore: detail.maxScore || 5,
      applicableQuestionTypes: [...detail.applicableQuestionTypes],
      weight: detail.weight,
      isRequired: detail.isRequired,
      orderIndex: detail.orderIndex,
      options: { ...detail.options }
    })

    editDialogVisible.value = true
  } catch (error) {
    console.error('获取评测标准详情失败', error)
    ElMessage.error('获取评测标准详情失败')
  }
}

// 删除评测标准
const handleDeleteCriterion = (criterion: EvaluationCriterion) => {
  ElMessageBox.confirm(
    `确定要删除评测标准 "${criterion.name}" 吗？此操作不可恢复！`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        // 获取当前用户ID
        const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
        const userId = currentUser.id

        // 传入userId参数
        const result = await deleteEvaluationCriterion(criterion.id, userId)
        if (result.success) {
          ElMessage({
            type: 'success',
            message: `评测标准 "${criterion.name}" 已成功删除`,
          })
          // 重新加载评测标准列表
          fetchCriteriaList()
        } else {
          ElMessage.error(`删除失败: ${result.message}`)
        }
      } catch (error) {
        console.error('删除评测标准失败', error)
        ElMessage.error('删除评测标准失败')
      }
    })
    .catch(() => {
      ElMessage({
        type: 'info',
        message: '已取消删除',
      })
    })
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true

      try {
        if (isEditMode.value && form.id) {
          // 编辑模式
          const updateData: UpdateEvaluationCriterionRequest = {
            name: form.name,
            version: form.version,
            description: form.description,
            questionType: form.questionType,
            dataType: form.dataType,
            scoreRange: form.scoreRange,
            maxScore: form.maxScore,
            applicableQuestionTypes: form.applicableQuestionTypes,
            weight: form.weight,
            isRequired: form.isRequired,
            orderIndex: form.orderIndex,
            options: form.options
          }

          // 获取当前用户ID
          const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
          const userId = currentUser.id

          // 传入userId参数
          await updateEvaluationCriterion(form.id, updateData, userId)
          ElMessage.success('评测标准更新成功')
        } else {
          // 创建模式
          const createData: CreateEvaluationCriterionRequest = {
            name: form.name,
            version: form.version,
            description: form.description,
            questionType: form.questionType,
            dataType: form.dataType,
            scoreRange: form.scoreRange,
            maxScore: form.maxScore,
            applicableQuestionTypes: form.applicableQuestionTypes,
            weight: form.weight,
            isRequired: form.isRequired,
            orderIndex: form.orderIndex,
            options: form.options
          }

          // 获取当前用户ID
          const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
          const userId = currentUser.id

          // 传入userId参数
          await createEvaluationCriterion(createData, userId)
          ElMessage.success('评测标准创建成功')
        }

        editDialogVisible.value = false
        // 重新加载评测标准列表
        fetchCriteriaList()
      } catch (error) {
        console.error('保存评测标准失败', error)
        ElMessage.error('保存评测标准失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 格式化日期
const formatDate = (dateString: string) => {
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

// 格式化权重提示
const formatWeight = (val: number) => {
  return val.toFixed(2)
}

// 获取题型显示名称
const getQuestionTypeDisplay = (type: string) => {
  const typeMap: Record<string, string> = {
    'SUBJECTIVE': '主观题',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SIMPLE_FACT': '简单事实题'
  }
  return typeMap[type] || type
}

// 获取题型标签类型
const getQuestionTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'SUBJECTIVE': 'danger',
    'SINGLE_CHOICE': 'success',
    'MULTIPLE_CHOICE': 'warning',
    'SIMPLE_FACT': 'info'
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped>
.evaluation-criteria-management {
  padding: 20px;
}

.management-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.management-content {
  padding: 20px 0;
}

.search-area {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-input {
  width: 400px;
}

.filter-area {
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.form-help-text {
  margin-top: 10px;
  font-size: 0.8em;
  color: #909399;
}
</style>
