<template>
  <div class="answer-type-prompt">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>回答阶段 - 题型提示词管理</h3>
          <el-button type="primary" @click="handleCreate">创建提示词</el-button>
        </div>
      </template>

      <el-input
        v-model="searchQuery"
        placeholder="搜索提示词..."
        prefix-icon="Search"
        clearable
        style="margin-bottom: 20px; width: 300px;"
      />

      <el-table
        :data="filteredPrompts"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="name" label="提示词名称" />
        <el-table-column prop="questionType" label="题型" width="120">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeTag(row.questionType)">
              {{ getQuestionTypeLabel(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="success"
              size="small"
              v-if="!row.isActive"
              @click="toggleStatus(row, true)"
            >
              启用
            </el-button>
            <el-button
              type="info"
              size="small"
              v-if="row.isActive"
              @click="toggleStatus(row, false)"
            >
              禁用
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 编辑对话框 -->
      <el-dialog
        :title="isEditing ? '编辑题型提示词' : '创建题型提示词'"
        v-model="dialogVisible"
        width="70%"
      >
        <el-form :model="form" label-width="120px" ref="formRef">
          <el-form-item label="提示词名称" prop="name" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
            <el-input v-model="form.name" placeholder="请输入提示词名称" />
          </el-form-item>

          <el-form-item label="题型" prop="questionType" :rules="[{ required: true, message: '请选择题型', trigger: 'change' }]">
            <el-select v-model="form.questionType" placeholder="请选择题型">
              <el-option
                v-for="type in questionTypeOptions"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="状态" prop="isActive">
            <el-switch
              v-model="form.isActive"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <el-form-item label="提示词模板" prop="promptTemplate" :rules="[{ required: true, message: '请输入提示词模板', trigger: 'blur' }]">
            <el-input
              v-model="form.promptTemplate"
              type="textarea"
              :rows="8"
              placeholder="请输入提示词模板内容"
            />
          </el-form-item>

          <el-form-item label="回答格式要求" prop="responseFormatInstruction">
            <el-input
              v-model="form.responseFormatInstruction"
              type="textarea"
              :rows="4"
              placeholder="请输入回答格式要求"
            />
          </el-form-item>

          <el-form-item label="回答示例" prop="responseExample">
            <el-input
              v-model="form.responseExample"
              type="textarea"
              :rows="4"
              placeholder="请输入回答示例"
            />
          </el-form-item>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入提示词描述"
            />
          </el-form-item>

          <el-form-item label="版本" prop="version">
            <el-input v-model="form.version" placeholder="请输入版本号，例如：1.0" />
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

// 导入相关API
import {
  createAnswerTypePrompt,
  updateAnswerTypePrompt,
  deleteAnswerTypePrompt,
  getAllAnswerTypePrompts,
  QuestionType
} from '@/api/promptApis'
import { useUserStore } from '@/stores/user'

// 状态定义
const loading = ref(false)
const prompts = ref<any[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const searchQuery = ref('')
const userStore = useUserStore()

// 题型选项
const questionTypeOptions = [
  { value: QuestionType.SINGLE_CHOICE, label: '单选题' },
  { value: QuestionType.MULTIPLE_CHOICE, label: '多选题' },
  { value: QuestionType.SIMPLE_FACT, label: '简单事实题' },
  { value: QuestionType.SUBJECTIVE, label: '主观题' }
]

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  questionType: '' as QuestionType,
  promptTemplate: '',
  responseFormatInstruction: '',
  responseExample: '',
  description: '',
  isActive: true,
  version: '1.0',
  userId: 0
})

// 过滤提示词
const filteredPrompts = computed(() => {
  if (!searchQuery.value) return prompts.value

  const query = searchQuery.value.toLowerCase()
  return prompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    getQuestionTypeLabel(prompt.questionType).toLowerCase().includes(query) ||
    prompt.description.toLowerCase().includes(query) ||
    prompt.promptTemplate.toLowerCase().includes(query)
  )
})

// 获取题型标签类型
const getQuestionTypeTag = (type: QuestionType) => {
  switch (type) {
    case QuestionType.SINGLE_CHOICE:
      return 'success'
    case QuestionType.MULTIPLE_CHOICE:
      return 'warning'
    case QuestionType.SIMPLE_FACT:
      return 'info'
    case QuestionType.SUBJECTIVE:
      return 'primary'
    default:
      return 'info'
  }
}

// 获取题型显示标签
const getQuestionTypeLabel = (type: QuestionType) => {
  switch (type) {
    case QuestionType.SINGLE_CHOICE:
      return '单选题'
    case QuestionType.MULTIPLE_CHOICE:
      return '多选题'
    case QuestionType.SIMPLE_FACT:
      return '简单事实题'
    case QuestionType.SUBJECTIVE:
      return '主观题'
    default:
      return type
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 生命周期钩子
onMounted(async () => {
  await fetchData()
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const response = await getAllAnswerTypePrompts()
    prompts.value = response || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取题型提示词数据失败')
  } finally {
    loading.value = false
  }
}

// 创建提示词
const handleCreate = () => {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑提示词
const handleEdit = (row: any) => {
  isEditing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    questionType: row.questionType,
    promptTemplate: row.promptTemplate,
    responseFormatInstruction: row.responseFormatInstruction,
    responseExample: row.responseExample,
    description: row.description,
    isActive: row.isActive,
    version: row.version,
    userId: userStore.currentUser?.id || 0
  })
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any, status: boolean) => {
  try {
    const updatedData = {
      ...row,
      isActive: status,
      userId: userStore.currentUser?.id || 0
    }
    await updateAnswerTypePrompt(row.id, updatedData)
    ElMessage.success(`${status ? '启用' : '禁用'}提示词成功`)
    await fetchData()
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error(`${status ? '启用' : '禁用'}提示词失败`)
  }
}

// 删除提示词
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该提示词吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteAnswerTypePrompt(row.id, { userId: userStore.currentUser?.id || 0 })
      ElMessage.success('删除提示词成功')
      await fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除提示词失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 重置表单
const resetForm = () => {
  form.id = 0
  form.name = ''
  form.questionType = '' as QuestionType
  form.promptTemplate = ''
  form.responseFormatInstruction = ''
  form.responseExample = ''
  form.description = ''
  form.isActive = true
  form.version = '1.0'
  form.userId = userStore.currentUser?.id || 0
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        form.userId = userStore.currentUser?.id || 0

        if (isEditing.value) {
          await updateAnswerTypePrompt(form.id, form)
          ElMessage.success('更新提示词成功')
        } else {
          await createAnswerTypePrompt(form)
          ElMessage.success('创建提示词成功')
        }

        dialogVisible.value = false
        await fetchData()
      } catch (error) {
        console.error('提交表单失败:', error)
        ElMessage.error(isEditing.value ? '更新提示词失败' : '创建提示词失败')
      }
    }
  })
}
</script>

<style scoped>
.answer-type-prompt {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-tag {
  margin-right: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
