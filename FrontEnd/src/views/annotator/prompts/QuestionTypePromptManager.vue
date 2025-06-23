<template>
  <div class="question-type-prompt-manager">
    <el-table
      :data="filteredPrompts"
      style="width: 100%"
      border
      v-loading="loading"
    >
      <el-table-column prop="name" label="提示词名称" />
      <el-table-column prop="questionType" label="题型" />
      <el-table-column label="优先级">
        <template #default="{ row }">
          <el-progress
            :percentage="row.promptPriority"
            :format="(percentage) => `${percentage}%`"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">
            {{ row.isActive ? '启用' : '禁用' }}
          </el-tag>
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
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="简单事实题" value="SIMPLE_FACT" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>

        <el-form-item label="提示词优先级" prop="promptPriority">
          <el-slider
            v-model="form.promptPriority"
            :min="1"
            :max="100"
            show-input
          />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'

// 导入相关API
import {
  createQuestionTypePrompt,
  updateQuestionTypePrompt,
  deleteQuestionTypePrompt,
  getAllQuestionTypePrompts,
  getSupportedQuestionTypes
} from '@/api/questionTypePrompt'

// 从props接收搜索查询
const props = defineProps<{
  searchQuery: string
}>()

// 状态定义
const loading = ref(false)
const prompts = ref<any[]>([])
const questionTypes = ref<string[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  questionType: '',
  promptTemplate: '',
  description: '',
  isActive: true,
  promptPriority: 50,
  version: '1.0',
  userId: 1 // 应该从用户状态获取
})

// 过滤提示词
const filteredPrompts = computed(() => {
  if (!props.searchQuery) return prompts.value

  const query = props.searchQuery.toLowerCase()
  return prompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    prompt.questionType.toLowerCase().includes(query) ||
    prompt.promptTemplate.toLowerCase().includes(query)
  )
})

// 生命周期钩子
onMounted(async () => {
  await fetchData()
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 获取所有题型提示词
    const promptsResponse = await getAllQuestionTypePrompts()
    prompts.value = promptsResponse.data || []

    // 获取所有支持的题型
    const typesResponse = await getSupportedQuestionTypes()
    questionTypes.value = typesResponse.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 编辑提示词
const handleEdit = (row: any) => {
  isEditing.value = true
  form.id = row.id
  form.name = row.name
  form.questionType = row.questionType
  form.promptTemplate = row.promptTemplate
  form.description = row.description
  form.isActive = row.isActive
  form.promptPriority = row.promptPriority
  form.version = row.version
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any, status: boolean) => {
  try {
    await updateQuestionTypePrompt(row.id, {
      ...row,
      userId: form.userId,
      isActive: status
    })
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除提示词
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该提示词吗？此操作不可逆。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteQuestionTypePrompt(row.id)
      ElMessage.success('删除成功')
      await fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.value) {
          await updateQuestionTypePrompt(form.id, form)
          ElMessage.success('更新成功')
        } else {
          await createQuestionTypePrompt(form)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        await fetchData()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

// 导出方法给父组件
defineExpose({
  openCreateDialog: () => {
    isEditing.value = false
    form.id = 0
    form.name = ''
    form.questionType = ''
    form.promptTemplate = ''
    form.description = ''
    form.isActive = true
    form.promptPriority = 50
    form.version = '1.0'
    dialogVisible.value = true
  }
})
</script>

<style scoped>
.question-type-prompt-manager {
  margin-bottom: 20px;
}
</style>
