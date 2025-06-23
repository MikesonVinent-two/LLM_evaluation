<template>
  <div class="evaluation-assembly-config">
    <el-table
      :data="configsList"
      style="width: 100%"
      border
      v-loading="loading"
    >
      <el-table-column prop="name" label="配置名称" />
      <el-table-column prop="createdByUsername" label="创建者" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="更新时间">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">
            {{ row.isActive ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
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
            type="warning"
            size="small"
            @click="handlePreview(row)"
          >
            预览
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog
      :title="isEditing ? '编辑评测提示词组装配置' : '创建评测提示词组装配置'"
      v-model="dialogVisible"
      width="80%"
    >
      <el-form :model="form" label-width="150px" ref="formRef">
        <el-form-item label="配置名称" prop="name" :rules="[{ required: true, message: '请输入配置名称', trigger: 'blur' }]">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>

        <el-form-item label="配置描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
          />
        </el-form-item>

        <el-form-item label="基础系统提示词" prop="baseSystemPrompt" :rules="[{ required: true, message: '请输入基础系统提示词', trigger: 'blur' }]">
          <el-input
            v-model="form.baseSystemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入基础系统提示词"
          />
        </el-form-item>

        <el-form-item label="标签提示词部分标题" prop="tagPromptsSectionHeader">
          <el-input v-model="form.tagPromptsSectionHeader" placeholder="请输入标签提示词部分标题" />
        </el-form-item>

        <el-form-item label="主观题部分标题" prop="subjectiveSectionHeader">
          <el-input v-model="form.subjectiveSectionHeader" placeholder="请输入主观题部分标题" />
        </el-form-item>

        <el-form-item label="标签提示词分隔符" prop="tagPromptSeparator">
          <el-input v-model="form.tagPromptSeparator" placeholder="请输入标签提示词分隔符" />
        </el-form-item>

        <el-form-item label="各部分分隔符" prop="sectionSeparator">
          <el-input v-model="form.sectionSeparator" placeholder="请输入各部分分隔符" />
        </el-form-item>

        <el-form-item label="最终指令" prop="finalInstruction">
          <el-input
            v-model="form.finalInstruction"
            type="textarea"
            :rows="3"
            placeholder="请输入最终指令"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      title="提示词组装预览"
      v-model="previewVisible"
      width="80%"
    >
      <div class="preview-content">
        <pre>{{ previewContent }}</pre>
      </div>

      <div class="preview-options">
        <el-form :inline="true">
          <el-form-item label="标签">
            <el-select v-model="previewOptions.tagIds" multiple placeholder="选择标签" @change="generatePreview">
              <el-option
                v-for="tag in availableTags"
                :key="tag.id"
                :label="tag.tagName"
                :value="tag.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="使用主观题提示词">
            <el-switch v-model="previewOptions.includeSubjective" @change="generatePreview" />
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 导入相关API
import {
  createEvaluationConfig,
  getEvaluationConfigById,
  getAllActiveEvaluationConfigs,
  EvaluationConfigInfo
} from '@/api/evaluationPromptAssembly'

import { getAllTags } from '@/api/tags'
import {
  getActiveEvaluationTagPromptsByTagId,
  EvaluationTagPromptInfo
} from '@/api/evaluationTagPrompt'
import {
  getActiveEvaluationSubjectivePrompts,
  EvaluationSubjectivePromptInfo
} from '@/api/evaluationSubjectivePrompt'

// 状态定义
const loading = ref(false)
const configsList = ref<EvaluationConfigInfo[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const userStore = useUserStore()

// 预览相关
const previewVisible = ref(false)
const previewContent = ref('')
const availableTags = ref<any[]>([])
const previewOptions = reactive({
  tagIds: [] as number[],
  includeSubjective: true
})
const currentConfig = ref<EvaluationConfigInfo | null>(null)

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  description: '',
  baseSystemPrompt: '',
  tagPromptsSectionHeader: '### 标签相关要求',
  subjectiveSectionHeader: '### 主观题评估要求',
  tagPromptSeparator: '\n\n',
  sectionSeparator: '\n\n---\n\n',
  finalInstruction: '请根据以上要求对回答进行全面评估。'
})

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 生命周期钩子
onMounted(async () => {
  await fetchData()
  await fetchTags()
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const response = await getAllActiveEvaluationConfigs()
    if (response && response.success) {
      configsList.value = response.configs || []
    }
  } catch (error) {
    console.error('获取评测提示词组装配置失败:', error)
    ElMessage.error('获取评测提示词组装配置失败')
  } finally {
    loading.value = false
  }
}

// 获取标签
const fetchTags = async () => {
  try {
    const response = await getAllTags()
    availableTags.value = response || []
  } catch (error) {
    console.error('获取标签列表失败:', error)
  }
}

// 打开创建对话框
const openEvaluationConfigCreateDialog = () => {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

// 处理编辑
const handleEdit = (row: EvaluationConfigInfo) => {
  isEditing.value = true
  resetForm()

  form.id = row.id
  form.name = row.name
  form.description = row.description
  form.baseSystemPrompt = row.baseSystemPrompt
  form.tagPromptsSectionHeader = row.tagPromptsSectionHeader
  form.subjectiveSectionHeader = row.subjectiveSectionHeader
  form.tagPromptSeparator = row.tagPromptSeparator
  form.sectionSeparator = row.sectionSeparator
  form.finalInstruction = row.finalInstruction

  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: EvaluationConfigInfo, status: boolean) => {
  // 这部分需要后端接口支持，暂时仅作为示例
  ElMessage.warning('此功能需要后端API支持，暂未实现')
}

// 处理预览
const handlePreview = async (row: EvaluationConfigInfo) => {
  currentConfig.value = row
  previewOptions.tagIds = []
  previewOptions.includeSubjective = true
  previewVisible.value = true

  // 生成预览内容
  await generatePreview()
}

// 生成预览内容
const generatePreview = async () => {
  if (!currentConfig.value) return

  try {
    loading.value = true

    let preview = currentConfig.value.baseSystemPrompt + '\n\n'

    // 获取标签提示词
    if (previewOptions.tagIds.length > 0) {
      preview += currentConfig.value.tagPromptsSectionHeader + '\n\n'

      const tagPrompts: string[] = []
      for (const tagId of previewOptions.tagIds) {
        try {
          const tagPromptsResponse = await getActiveEvaluationTagPromptsByTagId(tagId)
          if (tagPromptsResponse && tagPromptsResponse.length > 0) {
            const tag = availableTags.value.find(t => t.id === tagId)
            const tagName = tag ? tag.tagName : '未知标签'

            tagPrompts.push(`【${tagName}】: ${tagPromptsResponse[0].promptTemplate}`)
          }
        } catch (error) {
          console.error(`获取标签 ${tagId} 的提示词失败:`, error)
        }
      }

      if (tagPrompts.length > 0) {
        preview += tagPrompts.join(currentConfig.value.tagPromptSeparator)
        preview += currentConfig.value.sectionSeparator
      }
    }

    // 获取主观题提示词
    if (previewOptions.includeSubjective) {
      try {
        const subjectivePromptsResponse = await getActiveEvaluationSubjectivePrompts()
        if (subjectivePromptsResponse && subjectivePromptsResponse.length > 0) {
          preview += currentConfig.value.subjectiveSectionHeader + '\n\n'
          preview += subjectivePromptsResponse[0].promptTemplate
          preview += currentConfig.value.sectionSeparator
        }
      } catch (error) {
        console.error('获取主观题提示词失败:', error)
      }
    }

    // 添加最终指令
    preview += currentConfig.value.finalInstruction

    previewContent.value = preview
  } catch (error) {
    console.error('生成预览失败:', error)
    ElMessage.error('生成预览失败')
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  form.id = 0
  form.name = ''
  form.description = ''
  form.baseSystemPrompt = ''
  form.tagPromptsSectionHeader = '### 标签相关要求'
  form.subjectiveSectionHeader = '### 主观题评估要求'
  form.tagPromptSeparator = '\n\n'
  form.sectionSeparator = '\n\n---\n\n'
  form.finalInstruction = '请根据以上要求对回答进行全面评估。'

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

        const userId = userStore.currentUser?.id
        if (!userId) {
          ElMessage.error('用户未登录')
          return
        }

        if (isEditing.value) {
          // 需要更新API
          ElMessage.warning('更新功能需要后端API支持，暂未实现')
        } else {
          // 创建配置
          const response = await createEvaluationConfig(form, userId)
          if (response && response.success) {
            ElMessage.success('创建成功')
            dialogVisible.value = false
            await fetchData()
          } else {
            ElMessage.error('创建失败')
          }
        }
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
  openEvaluationConfigCreateDialog
})
</script>

<style scoped>
.evaluation-assembly-config {
  padding: 10px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.preview-content {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  max-height: 500px;
  overflow-y: auto;
  font-family: 'Courier New', Courier, monospace;
}

.preview-options {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}
</style>
