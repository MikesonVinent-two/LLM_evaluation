<template>
  <div class="prompt-assembly">
    <el-card class="assembly-card">
      <template #header>
        <div class="card-header">
          <h2>提示词组装</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshData">刷新数据</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="assembly-tabs">
        <el-tab-pane label="回答提示词组装配置" name="answerConfig">
          <!-- 回答提示词组装配置列表 -->
          <div class="tool-bar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索配置..."
              prefix-icon="el-icon-search"
              clearable
              style="width: 250px; margin-right: 15px;"
            />

            <el-button
              type="primary"
              icon="el-icon-plus"
              @click="openAnswerConfigCreateDialog"
            >
              创建回答提示词组装配置
            </el-button>
          </div>

          <el-table
            :data="filteredAnswerConfigs"
            style="width: 100%"
            border
            v-loading="loading.answer"
          >
            <el-table-column prop="name" label="配置名称" />
            <el-table-column prop="createdByUser.name" label="创建者" width="120" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
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
                  @click="handleEditAnswerConfig(row)"
                >
                  编辑
                </el-button>
                <el-button
                  type="success"
                  size="small"
                  v-if="!row.isActive"
                  @click="toggleAnswerConfigStatus(row, true)"
                >
                  启用
                </el-button>
                <el-button
                  type="info"
                  size="small"
                  v-if="row.isActive"
                  @click="toggleAnswerConfigStatus(row, false)"
                >
                  禁用
                </el-button>
                <el-button
                  type="warning"
                  size="small"
                  @click="handlePreviewAnswerConfig(row)"
                >
                  预览
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="评测提示词组装配置" name="evaluationConfig">
          <!-- 评测提示词组装配置列表 -->
          <div class="tool-bar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索配置..."
              prefix-icon="el-icon-search"
              clearable
              style="width: 250px; margin-right: 15px;"
            />

            <el-button
              type="primary"
              icon="el-icon-plus"
              @click="openEvaluationConfigCreateDialog"
            >
              创建评测提示词组装配置
            </el-button>
          </div>

          <el-table
            :data="filteredEvaluationConfigs"
            style="width: 100%"
            border
            v-loading="loading.evaluation"
          >
            <el-table-column prop="name" label="配置名称" />
            <el-table-column prop="createdByUser.name" label="创建者" width="120" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
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
                  @click="handleEditEvaluationConfig(row)"
                >
                  编辑
                </el-button>
                <el-button
                  type="success"
                  size="small"
                  v-if="!row.isActive"
                  @click="toggleEvaluationConfigStatus(row, true)"
                >
                  启用
                </el-button>
                <el-button
                  type="info"
                  size="small"
                  v-if="row.isActive"
                  @click="toggleEvaluationConfigStatus(row, false)"
                >
                  禁用
                </el-button>
                <el-button
                  type="warning"
                  size="small"
                  @click="handlePreviewEvaluationConfig(row)"
                >
                  预览
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <!-- 分页器 -->
      <div class="pagination">
        <el-pagination
          layout="total, prev, pager, next"
          :total="getPaginationTotal()"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 回答提示词组装配置编辑对话框 -->
    <el-dialog
      :title="isEditing.answer ? '编辑回答提示词组装配置' : '创建回答提示词组装配置'"
      v-model="dialogVisible.answer"
      width="80%"
    >
      <el-form :model="answerConfigForm" label-width="150px" ref="answerConfigFormRef">
        <el-form-item label="配置名称" prop="name" :rules="[{ required: true, message: '请输入配置名称', trigger: 'blur' }]">
          <el-input v-model="answerConfigForm.name" placeholder="请输入配置名称" />
        </el-form-item>

        <el-form-item label="配置描述" prop="description">
          <el-input
            v-model="answerConfigForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
          />
        </el-form-item>

        <el-form-item label="基础系统提示词" prop="baseSystemPrompt" :rules="[{ required: true, message: '请输入基础系统提示词', trigger: 'blur' }]">
          <el-input
            v-model="answerConfigForm.baseSystemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入基础系统提示词"
          />
        </el-form-item>

        <el-form-item label="标签提示词部分标题" prop="tagPromptsSectionHeader">
          <el-input v-model="answerConfigForm.tagPromptsSectionHeader" placeholder="请输入标签提示词部分标题" />
        </el-form-item>

        <el-form-item label="题型提示词部分标题" prop="questionTypePromptsSectionHeader">
          <el-input v-model="answerConfigForm.questionTypePromptsSectionHeader" placeholder="请输入题型提示词部分标题" />
        </el-form-item>

        <el-form-item label="标签提示词分隔符" prop="tagPromptSeparator">
          <el-input v-model="answerConfigForm.tagPromptSeparator" placeholder="请输入标签提示词分隔符" />
        </el-form-item>

        <el-form-item label="各部分分隔符" prop="sectionSeparator">
          <el-input v-model="answerConfigForm.sectionSeparator" placeholder="请输入各部分分隔符" />
        </el-form-item>

        <el-form-item label="最终指令" prop="finalInstruction">
          <el-input
            v-model="answerConfigForm.finalInstruction"
            type="textarea"
            :rows="3"
            placeholder="请输入最终指令"
          />
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="answerConfigForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible.answer = false">取消</el-button>
          <el-button type="primary" @click="submitAnswerConfigForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评测提示词组装配置编辑对话框 -->
    <el-dialog
      :title="isEditing.evaluation ? '编辑评测提示词组装配置' : '创建评测提示词组装配置'"
      v-model="dialogVisible.evaluation"
      width="80%"
    >
      <el-form :model="evaluationConfigForm" label-width="150px" ref="evaluationConfigFormRef">
        <el-form-item label="配置名称" prop="name" :rules="[{ required: true, message: '请输入配置名称', trigger: 'blur' }]">
          <el-input v-model="evaluationConfigForm.name" placeholder="请输入配置名称" />
        </el-form-item>

        <el-form-item label="配置描述" prop="description">
          <el-input
            v-model="evaluationConfigForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
          />
        </el-form-item>

        <el-form-item label="基础系统提示词" prop="baseSystemPrompt" :rules="[{ required: true, message: '请输入基础系统提示词', trigger: 'blur' }]">
          <el-input
            v-model="evaluationConfigForm.baseSystemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入基础系统提示词"
          />
        </el-form-item>

        <el-form-item label="标签提示词部分标题" prop="tagPromptsSectionHeader">
          <el-input v-model="evaluationConfigForm.tagPromptsSectionHeader" placeholder="请输入标签提示词部分标题" />
        </el-form-item>

        <el-form-item label="主观题部分标题" prop="subjectiveSectionHeader">
          <el-input v-model="evaluationConfigForm.subjectiveSectionHeader" placeholder="请输入主观题部分标题" />
        </el-form-item>

        <el-form-item label="标签提示词分隔符" prop="tagPromptSeparator">
          <el-input v-model="evaluationConfigForm.tagPromptSeparator" placeholder="请输入标签提示词分隔符" />
        </el-form-item>

        <el-form-item label="各部分分隔符" prop="sectionSeparator">
          <el-input v-model="evaluationConfigForm.sectionSeparator" placeholder="请输入各部分分隔符" />
        </el-form-item>

        <el-form-item label="最终指令" prop="finalInstruction">
          <el-input
            v-model="evaluationConfigForm.finalInstruction"
            type="textarea"
            :rows="3"
            placeholder="请输入最终指令"
          />
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="evaluationConfigForm.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible.evaluation = false">取消</el-button>
          <el-button type="primary" @click="submitEvaluationConfigForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 提示词组装预览对话框 -->
    <el-dialog
      title="提示词组装预览"
      v-model="previewDialogVisible"
      width="80%"
    >
      <div class="preview-content">
        <pre>{{ previewContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'

// 导入相关API
import {
  createAnswerConfig,
  updateAnswerConfig,
  getActiveAnswerConfigs,
  getUserAnswerConfigs,
  previewAnswerConfig
} from '@/api/promptAssembly'

import {
  createEvaluationConfig,
  updateEvaluationConfig,
  getUserEvaluationConfigs,
  previewEvaluationConfig
} from '@/api/evaluationPromptAssembly'

// 状态定义
const activeTab = ref('answerConfig')
const searchQuery = ref('')
const loading = reactive({
  answer: false,
  evaluation: false
})
const answerConfigs = ref<any[]>([])
const evaluationConfigs = ref<any[]>([])
const dialogVisible = reactive({
  answer: false,
  evaluation: false
})
const isEditing = reactive({
  answer: false,
  evaluation: false
})
const previewDialogVisible = ref(false)
const previewContent = ref('')
const answerConfigFormRef = ref<FormInstance>()
const evaluationConfigFormRef = ref<FormInstance>()

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)

// 表单数据
const answerConfigForm = reactive({
  id: 0,
  name: '',
  description: '',
  baseSystemPrompt: '',
  tagPromptsSectionHeader: '### 标签相关提示',
  questionTypePromptsSectionHeader: '### 题型相关提示',
  tagPromptSeparator: '\n\n',
  sectionSeparator: '\n\n---\n\n',
  finalInstruction: '请根据以上指导生成高质量的回答。',
  isActive: true,
  userId: 1 // 应该从用户状态获取
})

const evaluationConfigForm = reactive({
  id: 0,
  name: '',
  description: '',
  baseSystemPrompt: '',
  tagPromptsSectionHeader: '### 标签评测标准',
  subjectiveSectionHeader: '### 主观题评测标准',
  tagPromptSeparator: '\n\n',
  sectionSeparator: '\n\n---\n\n',
  finalInstruction: '请根据以上标准对回答进行公正评价。',
  isActive: true,
  userId: 1 // 应该从用户状态获取
})

// 过滤配置
const filteredAnswerConfigs = computed(() => {
  if (!searchQuery.value) return answerConfigs.value

  const query = searchQuery.value.toLowerCase()
  return answerConfigs.value.filter(config =>
    config.name.toLowerCase().includes(query) ||
    config.description.toLowerCase().includes(query)
  )
})

const filteredEvaluationConfigs = computed(() => {
  if (!searchQuery.value) return evaluationConfigs.value

  const query = searchQuery.value.toLowerCase()
  return evaluationConfigs.value.filter(config =>
    config.name.toLowerCase().includes(query) ||
    config.description.toLowerCase().includes(query)
  )
})

// 生命周期钩子
onMounted(async () => {
  await refreshData()
})

// 刷新数据
const refreshData = async () => {
  await Promise.all([
    fetchAnswerConfigs(),
    fetchEvaluationConfigs()
  ])
}

// 日期格式化工具
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 获取分页总数
const getPaginationTotal = () => {
  if (activeTab.value === 'answerConfig') {
    return answerConfigs.value.length
  } else {
    return evaluationConfigs.value.length
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
}

// 获取回答提示词组装配置数据
const fetchAnswerConfigs = async () => {
  loading.answer = true
  try {
    // 获取用户的回答提示词组装配置
    const configsResponse = await getUserAnswerConfigs(answerConfigForm.userId)
    answerConfigs.value = configsResponse.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取回答提示词组装配置数据失败')
  } finally {
    loading.answer = false
  }
}

// 获取评测提示词组装配置数据
const fetchEvaluationConfigs = async () => {
  loading.evaluation = true
  try {
    // 获取用户的评测提示词组装配置
    const configsResponse = await getUserEvaluationConfigs(evaluationConfigForm.userId)
    evaluationConfigs.value = configsResponse.data || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取评测提示词组装配置数据失败')
  } finally {
    loading.evaluation = false
  }
}

// 回答提示词组装配置相关方法
const handleEditAnswerConfig = (row: any) => {
  isEditing.answer = true
  answerConfigForm.id = row.id
  answerConfigForm.name = row.name
  answerConfigForm.description = row.description
  answerConfigForm.baseSystemPrompt = row.baseSystemPrompt
  answerConfigForm.tagPromptsSectionHeader = row.tagPromptsSectionHeader
  answerConfigForm.questionTypePromptsSectionHeader = row.questionTypePromptsSectionHeader
  answerConfigForm.tagPromptSeparator = row.tagPromptSeparator
  answerConfigForm.sectionSeparator = row.sectionSeparator
  answerConfigForm.finalInstruction = row.finalInstruction
  answerConfigForm.isActive = row.isActive
  dialogVisible.answer = true
}

const toggleAnswerConfigStatus = async (row: any, status: boolean) => {
  try {
    await updateAnswerConfig(row.id, {
      ...row,
      userId: answerConfigForm.userId,
      isActive: status
    })
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchAnswerConfigs()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handlePreviewAnswerConfig = async (row: any) => {
  try {
    // 调用预览API，假设这里传入一个示例问题ID进行预览
    const response = await previewAnswerConfig(row.id, 1)
    previewContent.value = response.data.previewText || '预览内容为空'
    previewDialogVisible.value = true
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败')
  }
}

const submitAnswerConfigForm = async () => {
  if (!answerConfigFormRef.value) return

  await answerConfigFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.answer) {
          await updateAnswerConfig(answerConfigForm.id, answerConfigForm)
          ElMessage.success('更新成功')
        } else {
          await createAnswerConfig(answerConfigForm)
          ElMessage.success('创建成功')
        }
        dialogVisible.answer = false
        await fetchAnswerConfigs()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

const openAnswerConfigCreateDialog = () => {
  isEditing.answer = false
  answerConfigForm.id = 0
  answerConfigForm.name = ''
  answerConfigForm.description = ''
  answerConfigForm.baseSystemPrompt = ''
  answerConfigForm.tagPromptsSectionHeader = '### 标签相关提示'
  answerConfigForm.questionTypePromptsSectionHeader = '### 题型相关提示'
  answerConfigForm.tagPromptSeparator = '\n\n'
  answerConfigForm.sectionSeparator = '\n\n---\n\n'
  answerConfigForm.finalInstruction = '请根据以上指导生成高质量的回答。'
  answerConfigForm.isActive = true
  dialogVisible.answer = true
}

// 评测提示词组装配置相关方法
const handleEditEvaluationConfig = (row: any) => {
  isEditing.evaluation = true
  evaluationConfigForm.id = row.id
  evaluationConfigForm.name = row.name
  evaluationConfigForm.description = row.description
  evaluationConfigForm.baseSystemPrompt = row.baseSystemPrompt
  evaluationConfigForm.tagPromptsSectionHeader = row.tagPromptsSectionHeader
  evaluationConfigForm.subjectiveSectionHeader = row.subjectiveSectionHeader
  evaluationConfigForm.tagPromptSeparator = row.tagPromptSeparator
  evaluationConfigForm.sectionSeparator = row.sectionSeparator
  evaluationConfigForm.finalInstruction = row.finalInstruction
  evaluationConfigForm.isActive = row.isActive
  dialogVisible.evaluation = true
}

const toggleEvaluationConfigStatus = async (row: any, status: boolean) => {
  try {
    await updateEvaluationConfig(row.id, {
      ...row,
      userId: evaluationConfigForm.userId,
      isActive: status
    })
    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
    await fetchEvaluationConfigs()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handlePreviewEvaluationConfig = async (row: any) => {
  try {
    // 调用预览API，假设这里传入一个示例问题ID和回答ID进行预览
    const response = await previewEvaluationConfig(row.id, 1, 1)
    previewContent.value = response.data.previewText || '预览内容为空'
    previewDialogVisible.value = true
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败')
  }
}

const submitEvaluationConfigForm = async () => {
  if (!evaluationConfigFormRef.value) return

  await evaluationConfigFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.evaluation) {
          await updateEvaluationConfig(evaluationConfigForm.id, evaluationConfigForm)
          ElMessage.success('更新成功')
        } else {
          await createEvaluationConfig(evaluationConfigForm)
          ElMessage.success('创建成功')
        }
        dialogVisible.evaluation = false
        await fetchEvaluationConfigs()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

const openEvaluationConfigCreateDialog = () => {
  isEditing.evaluation = false
  evaluationConfigForm.id = 0
  evaluationConfigForm.name = ''
  evaluationConfigForm.description = ''
  evaluationConfigForm.baseSystemPrompt = ''
  evaluationConfigForm.tagPromptsSectionHeader = '### 标签评测标准'
  evaluationConfigForm.subjectiveSectionHeader = '### 主观题评测标准'
  evaluationConfigForm.tagPromptSeparator = '\n\n'
  evaluationConfigForm.sectionSeparator = '\n\n---\n\n'
  evaluationConfigForm.finalInstruction = '请根据以上标准对回答进行公正评价。'
  evaluationConfigForm.isActive = true
  dialogVisible.evaluation = true
}
</script>

<style scoped>
.prompt-assembly {
  padding: 20px;
}

.assembly-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.assembly-tabs {
  margin-bottom: 20px;
}

.tool-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.preview-content {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  max-height: 500px;
  overflow-y: auto;
  font-family: monospace;
}
</style>
