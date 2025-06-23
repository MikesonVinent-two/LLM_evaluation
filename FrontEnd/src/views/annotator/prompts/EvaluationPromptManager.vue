<template>
  <div class="evaluation-prompt-manager">
    <el-tabs v-model="activeTab" class="evaluation-tabs">
      <el-tab-pane label="标签评测提示词" name="tagEvaluation">
        <el-table
          :data="filteredTagPrompts"
          style="width: 100%"
          border
          v-loading="loading.tag"
        >
          <el-table-column prop="name" label="提示词名称" />
          <el-table-column prop="tag.name" label="关联标签" />
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
                @click="handleEditTagPrompt(row)"
              >
                编辑
              </el-button>
              <el-button
                type="success"
                size="small"
                v-if="!row.isActive"
                @click="toggleTagPromptStatus(row, true)"
              >
                启用
              </el-button>
              <el-button
                type="info"
                size="small"
                v-if="row.isActive"
                @click="toggleTagPromptStatus(row, false)"
              >
                禁用
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDeleteTagPrompt(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button
          type="primary"
          icon="el-icon-plus"
          class="create-button"
          @click="openTagPromptCreateDialog"
        >
          创建标签评测提示词
        </el-button>
      </el-tab-pane>

      <el-tab-pane label="主观题评测提示词" name="subjectiveEvaluation">
        <el-table
          :data="filteredSubjectivePrompts"
          style="width: 100%"
          border
          v-loading="loading.subjective"
        >
          <el-table-column prop="name" label="提示词名称" />
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
                @click="handleEditSubjectivePrompt(row)"
              >
                编辑
              </el-button>
              <el-button
                type="success"
                size="small"
                v-if="!row.isActive"
                @click="toggleSubjectivePromptStatus(row, true)"
              >
                启用
              </el-button>
              <el-button
                type="info"
                size="small"
                v-if="row.isActive"
                @click="toggleSubjectivePromptStatus(row, false)"
              >
                禁用
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDeleteSubjectivePrompt(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button
          type="primary"
          icon="el-icon-plus"
          class="create-button"
          @click="openSubjectivePromptCreateDialog"
        >
          创建主观题评测提示词
        </el-button>
      </el-tab-pane>
    </el-tabs>

    <!-- 标签评测提示词编辑对话框 -->
    <el-dialog
      :title="isEditing.tag ? '编辑标签评测提示词' : '创建标签评测提示词'"
      v-model="dialogVisible.tag"
      width="70%"
    >
      <el-form :model="tagForm" label-width="120px" ref="tagFormRef">
        <el-form-item label="提示词名称" prop="name" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
          <el-input v-model="tagForm.name" placeholder="请输入提示词名称" />
        </el-form-item>

        <el-form-item label="关联标签" prop="tagId" :rules="[{ required: true, message: '请选择关联标签', trigger: 'change' }]">
          <el-select v-model="tagForm.tagId" placeholder="请选择关联标签">
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="提示词优先级" prop="promptPriority">
          <el-slider
            v-model="tagForm.promptPriority"
            :min="1"
            :max="100"
            show-input
          />
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="tagForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <el-form-item label="提示词模板" prop="promptTemplate" :rules="[{ required: true, message: '请输入提示词模板', trigger: 'blur' }]">
          <el-input
            v-model="tagForm.promptTemplate"
            type="textarea"
            :rows="8"
            placeholder="请输入提示词模板内容"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入提示词描述"
          />
        </el-form-item>

        <el-form-item label="版本" prop="version">
          <el-input v-model="tagForm.version" placeholder="请输入版本号，例如：1.0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible.tag = false">取消</el-button>
          <el-button type="primary" @click="submitTagForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 主观题评测提示词编辑对话框 -->
    <el-dialog
      :title="isEditing.subjective ? '编辑主观题评测提示词' : '创建主观题评测提示词'"
      v-model="dialogVisible.subjective"
      width="70%"
    >
      <el-form :model="subjectiveForm" label-width="120px" ref="subjectiveFormRef">
        <el-form-item label="提示词名称" prop="name" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
          <el-input v-model="subjectiveForm.name" placeholder="请输入提示词名称" />
        </el-form-item>

        <el-form-item label="提示词优先级" prop="promptPriority">
          <el-slider
            v-model="subjectiveForm.promptPriority"
            :min="1"
            :max="100"
            show-input
          />
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="subjectiveForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <el-form-item label="提示词模板" prop="promptTemplate" :rules="[{ required: true, message: '请输入提示词模板', trigger: 'blur' }]">
          <el-input
            v-model="subjectiveForm.promptTemplate"
            type="textarea"
            :rows="8"
            placeholder="请输入提示词模板内容"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="subjectiveForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入提示词描述"
          />
        </el-form-item>

        <el-form-item label="版本" prop="version">
          <el-input v-model="subjectiveForm.version" placeholder="请输入版本号，例如：1.0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible.subjective = false">取消</el-button>
          <el-button type="primary" @click="submitSubjectiveForm">确定</el-button>
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
  createEvaluationTagPrompt,
  updateEvaluationTagPrompt,
  deleteEvaluationTagPrompt,
  getAllEvaluationTagPrompts
} from '@/api/evaluationTagPrompt'

import {
  createEvaluationSubjectivePrompt,
  updateEvaluationSubjectivePrompt,
  deleteEvaluationSubjectivePrompt,
  getAllEvaluationSubjectivePrompts
} from '@/api/evaluationSubjectivePrompt'

import { getAllTags } from '@/api/tags'

// 从props接收搜索查询
const props = defineProps<{
  searchQuery: string
}>()

// 状态定义
const activeTab = ref('tagEvaluation')
const loading = reactive({
  tag: false,
  subjective: false
})
const tagPrompts = ref<any[]>([])
const subjectivePrompts = ref<any[]>([])
const tags = ref<any[]>([])
const dialogVisible = reactive({
  tag: false,
  subjective: false
})
const isEditing = reactive({
  tag: false,
  subjective: false
})
const tagFormRef = ref<FormInstance>()
const subjectiveFormRef = ref<FormInstance>()

// 表单数据
const tagForm = reactive({
  id: 0,
  name: '',
  tagId: undefined as number | undefined,
  promptTemplate: '',
  description: '',
  isActive: true,
  promptPriority: 50,
  version: '1.0',
  userId: 1 // 应该从用户状态获取
})

const subjectiveForm = reactive({
  id: 0,
  name: '',
  promptTemplate: '',
  description: '',
  isActive: true,
  promptPriority: 50,
  version: '1.0',
  userId: 1 // 应该从用户状态获取
})

// 过滤提示词
const filteredTagPrompts = computed(() => {
  if (!props.searchQuery) return tagPrompts.value

  const query = props.searchQuery.toLowerCase()
  return tagPrompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    prompt.tag.name.toLowerCase().includes(query) ||
    prompt.promptTemplate.toLowerCase().includes(query)
  )
})

const filteredSubjectivePrompts = computed(() => {
  if (!props.searchQuery) return subjectivePrompts.value

  const query = props.searchQuery.toLowerCase()
  return subjectivePrompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    prompt.promptTemplate.toLowerCase().includes(query)
  )
})

// 生命周期钩子
onMounted(async () => {
  await fetchTagData()
  await fetchSubjectiveData()
})

// 获取标签评测提示词数据
const fetchTagData = async () => {
  loading.tag = true
  try {
    // 获取所有标签评测提示词
    const promptsResponse = await getAllEvaluationTagPrompts()
    tagPrompts.value = promptsResponse.data || []

    // 获取所有标签
    const tagsResponse = await getAllTags()
    tags.value = tagsResponse.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取标签评测提示词数据失败')
  } finally {
    loading.tag = false
  }
}

// 获取主观题评测提示词数据
const fetchSubjectiveData = async () => {
  loading.subjective = true
  try {
    // 获取所有主观题评测提示词
    const promptsResponse = await getAllEvaluationSubjectivePrompts()
    subjectivePrompts.value = promptsResponse.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取主观题评测提示词数据失败')
  } finally {
    loading.subjective = false
  }
}

// 标签评测提示词相关方法
const handleEditTagPrompt = (row: any) => {
  isEditing.tag = true
  tagForm.id = row.id
  tagForm.name = row.name
  tagForm.tagId = row.tag.id
  tagForm.promptTemplate = row.promptTemplate
  tagForm.description = row.description
  tagForm.isActive = row.isActive
  tagForm.promptPriority = row.promptPriority
  tagForm.version = row.version
  dialogVisible.tag = true
}

const toggleTagPromptStatus = async (row: any, status: boolean) => {
  try {
    await updateEvaluationTagPrompt(row.id, {
      ...row,
      userId: tagForm.userId,
      tagId: row.tag.id,
      isActive: status
    })
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchTagData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeleteTagPrompt = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该标签评测提示词吗？此操作不可逆。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteEvaluationTagPrompt(row.id)
      ElMessage.success('删除成功')
      await fetchTagData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const submitTagForm = async () => {
  if (!tagFormRef.value) return

  await tagFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.tag) {
          await updateEvaluationTagPrompt(tagForm.id, tagForm)
          ElMessage.success('更新成功')
        } else {
          await createEvaluationTagPrompt(tagForm)
          ElMessage.success('创建成功')
        }
        dialogVisible.tag = false
        await fetchTagData()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

const openTagPromptCreateDialog = () => {
  isEditing.tag = false
  tagForm.id = 0
  tagForm.name = ''
  tagForm.tagId = undefined
  tagForm.promptTemplate = ''
  tagForm.description = ''
  tagForm.isActive = true
  tagForm.promptPriority = 50
  tagForm.version = '1.0'
  dialogVisible.tag = true
}

// 主观题评测提示词相关方法
const handleEditSubjectivePrompt = (row: any) => {
  isEditing.subjective = true
  subjectiveForm.id = row.id
  subjectiveForm.name = row.name
  subjectiveForm.promptTemplate = row.promptTemplate
  subjectiveForm.description = row.description
  subjectiveForm.isActive = row.isActive
  subjectiveForm.promptPriority = row.promptPriority
  subjectiveForm.version = row.version
  dialogVisible.subjective = true
}

const toggleSubjectivePromptStatus = async (row: any, status: boolean) => {
  try {
    await updateEvaluationSubjectivePrompt(row.id, {
      ...row,
      userId: subjectiveForm.userId,
      isActive: status
    })
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchSubjectiveData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeleteSubjectivePrompt = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该主观题评测提示词吗？此操作不可逆。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteEvaluationSubjectivePrompt(row.id)
      ElMessage.success('删除成功')
      await fetchSubjectiveData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const submitSubjectiveForm = async () => {
  if (!subjectiveFormRef.value) return

  await subjectiveFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.subjective) {
          await updateEvaluationSubjectivePrompt(subjectiveForm.id, subjectiveForm)
          ElMessage.success('更新成功')
        } else {
          await createEvaluationSubjectivePrompt(subjectiveForm)
          ElMessage.success('创建成功')
        }
        dialogVisible.subjective = false
        await fetchSubjectiveData()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

const openSubjectivePromptCreateDialog = () => {
  isEditing.subjective = false
  subjectiveForm.id = 0
  subjectiveForm.name = ''
  subjectiveForm.promptTemplate = ''
  subjectiveForm.description = ''
  subjectiveForm.isActive = true
  subjectiveForm.promptPriority = 50
  subjectiveForm.version = '1.0'
  dialogVisible.subjective = true
}

// 导出方法给父组件
defineExpose({
  openCreateDialog: () => {
    if (activeTab.value === 'tagEvaluation') {
      openTagPromptCreateDialog()
    } else {
      openSubjectivePromptCreateDialog()
    }
  }
})
</script>

<style scoped>
.evaluation-prompt-manager {
  margin-bottom: 20px;
}

.evaluation-tabs {
  margin-bottom: 20px;
}

.create-button {
  margin-top: 20px;
}
</style>
