<template>
  <div class="evaluation-tag-prompt-manager">
    <el-table
      :data="filteredPrompts"
      style="width: 100%"
      border
      v-loading="loading"
    >
      <el-table-column prop="name" label="提示词名称" />
      <el-table-column prop="tag.tagName" label="关联标签" />
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
      :title="isEditing ? '编辑评测标签提示词' : '创建评测标签提示词'"
      v-model="dialogVisible"
      width="70%"
    >
      <el-form :model="form" label-width="120px" ref="formRef">
        <el-form-item label="提示词名称" prop="name" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
          <el-input v-model="form.name" placeholder="请输入提示词名称" />
        </el-form-item>

        <el-form-item label="关联标签" prop="tagId" :rules="[{ required: true, message: '请选择关联标签', trigger: 'change' }]">
          <el-select v-model="form.tagId" placeholder="请选择关联标签">
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.tagName"
              :value="tag.id"
            />
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
import { useUserStore } from '@/stores/user'

// 导入相关API
import {
  createEvaluationTagPrompt,
  updateEvaluationTagPrompt,
  deleteEvaluationTagPrompt,
  getAllEvaluationTagPrompts,
  EvaluationTagPromptInfo
} from '@/api/evaluationTagPrompt'
import { getAllTags } from '@/api/tags'

// 从props接收搜索查询
const props = defineProps<{
  searchQuery: string
}>()

// 状态定义
const loading = ref(false)
const prompts = ref<EvaluationTagPromptInfo[]>([])
const tags = ref<any[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const userStore = useUserStore()

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  tagId: undefined as number | undefined,
  promptTemplate: '',
  description: '',
  isActive: true,
  promptPriority: 50,
  version: '1.0',
  userId: 0 // 会从用户状态获取
})

// 过滤提示词
const filteredPrompts = computed(() => {
  if (!props.searchQuery) return prompts.value

  const query = props.searchQuery.toLowerCase()
  return prompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    (prompt.tag && prompt.tag.tagName.toLowerCase().includes(query)) ||
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
    // 获取所有评测标签提示词
    const promptsResponse = await getAllEvaluationTagPrompts()
    prompts.value = promptsResponse || []

    // 获取所有标签
    const tagsResponse = await getAllTags()
    tags.value = tagsResponse || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
const openCreateDialog = () => {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

// 处理编辑按钮点击
const handleEdit = (row: EvaluationTagPromptInfo) => {
  isEditing.value = true
  resetForm()

  form.id = row.id
  form.name = row.name
  form.tagId = row.tag.id
  form.promptTemplate = row.promptTemplate
  form.description = row.description
  form.isActive = row.isActive
  form.promptPriority = row.promptPriority
  form.version = row.version

  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: EvaluationTagPromptInfo, status: boolean) => {
  try {
    loading.value = true

    const userId = userStore.currentUser?.id
    if (!userId) {
      ElMessage.error('用户未登录')
      return
    }

    // 使用更新API切换状态
    const updatedData = {
      userId,
      tagId: row.tag.id,
      name: row.name,
      promptTemplate: row.promptTemplate,
      description: row.description,
      isActive: status,
      promptPriority: row.promptPriority,
      version: row.version
    }

    await updateEvaluationTagPrompt(row.id, updatedData)
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchData() // 刷新数据
  } catch (error) {
    console.error(`${status ? '启用' : '禁用'}失败:`, error)
    ElMessage.error(`${status ? '启用' : '禁用'}失败`)
  } finally {
    loading.value = false
  }
}

// 处理删除按钮点击
const handleDelete = (row: EvaluationTagPromptInfo) => {
  ElMessageBox.confirm(
    '确定要删除该提示词吗？此操作不可逆。',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      loading.value = true

      const userId = userStore.currentUser?.id
      if (!userId) {
        ElMessage.error('用户未登录')
        return
      }

      await deleteEvaluationTagPrompt(row.id, { userId })
      ElMessage.success('删除成功')
      await fetchData() // 刷新数据
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {
    // 取消删除，不做任何操作
  })
}

// 重置表单
const resetForm = () => {
  form.id = 0
  form.name = ''
  form.tagId = undefined
  form.promptTemplate = ''
  form.description = ''
  form.isActive = true
  form.promptPriority = 50
  form.version = '1.0'

  const userId = userStore.currentUser?.id
  if (userId) {
    form.userId = userId
  }

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true

        if (!form.userId) {
          const userId = userStore.currentUser?.id
          if (!userId) {
            ElMessage.error('用户未登录')
            return
          }
          form.userId = userId
        }

        if (isEditing.value) {
          // 更新现有提示词
          await updateEvaluationTagPrompt(form.id, form)
          ElMessage.success('更新成功')
        } else {
          // 创建新提示词
          await createEvaluationTagPrompt(form)
          ElMessage.success('创建成功')
        }

        dialogVisible.value = false
        await fetchData() // 刷新数据
      } catch (error) {
        console.error(isEditing.value ? '更新失败:' : '创建失败:', error)
        ElMessage.error(isEditing.value ? '更新失败' : '创建失败')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请完善表单信息')
    }
  })
}

// 导出公共方法供父组件调用
defineExpose({
  openCreateDialog
})
</script>

<style scoped>
.evaluation-tag-prompt-manager {
  padding: 10px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
