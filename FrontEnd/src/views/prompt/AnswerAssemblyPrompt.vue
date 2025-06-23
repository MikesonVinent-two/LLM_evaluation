<template>
  <div class="answer-assembly-prompt">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>回答阶段 - 组装提示词管理</h3>
          <el-button type="primary" @click="handleCreate">创建组装配置</el-button>
        </div>
      </template>

      <el-input
        v-model="searchQuery"
        placeholder="搜索配置..."
        prefix-icon="Search"
        clearable
        style="margin-bottom: 20px; width: 300px;"
      />

      <el-table
        :data="filteredConfigs"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="name" label="配置名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
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
        <el-table-column label="创建者" width="120">
          <template #default="{ row }">
            {{ row.createdByUsername }}
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
        :title="isEditing ? '编辑组装配置' : '创建组装配置'"
        v-model="dialogVisible"
        width="80%"
      >
        <el-form :model="form" label-width="180px" ref="formRef">
          <el-form-item label="配置名称" prop="name" :rules="[{ required: true, message: '请输入配置名称', trigger: 'blur' }]">
            <el-input v-model="form.name" placeholder="请输入配置名称" />
          </el-form-item>

          <el-form-item label="配置描述" prop="description" :rules="[{ required: true, message: '请输入配置描述', trigger: 'blur' }]">
            <el-input v-model="form.description" placeholder="请输入配置描述" />
          </el-form-item>

          <el-form-item label="基础系统提示词" prop="baseSystemPrompt" :rules="[{ required: true, message: '请输入基础系统提示词', trigger: 'blur' }]">
            <el-input
              v-model="form.baseSystemPrompt"
              type="textarea"
              :rows="5"
              placeholder="请输入基础系统提示词"
            />
          </el-form-item>

          <el-form-item label="标签提示词部分标题" prop="tagPromptsSectionHeader" :rules="[{ required: true, message: '请输入标签提示词部分标题', trigger: 'blur' }]">
            <el-input v-model="form.tagPromptsSectionHeader" placeholder="请输入标签提示词部分标题" />
          </el-form-item>

          <el-form-item label="题型部分标题" prop="questionTypeSectionHeader" :rules="[{ required: true, message: '请输入题型部分标题', trigger: 'blur' }]">
            <el-input v-model="form.questionTypeSectionHeader" placeholder="请输入题型部分标题" />
          </el-form-item>

          <el-form-item label="标签提示词分隔符" prop="tagPromptSeparator" :rules="[{ required: true, message: '请输入标签提示词分隔符', trigger: 'blur' }]">
            <el-input v-model="form.tagPromptSeparator" placeholder="请输入标签提示词分隔符" />
          </el-form-item>

          <el-form-item label="部分分隔符" prop="sectionSeparator" :rules="[{ required: true, message: '请输入部分分隔符', trigger: 'blur' }]">
            <el-input v-model="form.sectionSeparator" placeholder="请输入部分分隔符" />
          </el-form-item>

          <el-form-item label="最终指令" prop="finalInstruction" :rules="[{ required: true, message: '请输入最终指令', trigger: 'blur' }]">
            <el-input
              v-model="form.finalInstruction"
              type="textarea"
              :rows="5"
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
  createAnswerPromptAssemblyConfig,
  getActiveAnswerPromptAssemblyConfigs,
  getUserAnswerPromptAssemblyConfigs
} from '@/api/promptApis'
import { useUserStore } from '@/stores/user'

// 状态定义
const loading = ref(false)
const configs = ref<any[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const searchQuery = ref('')
const userStore = useUserStore()

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  description: '',
  baseSystemPrompt: '',
  tagPromptsSectionHeader: '',
  questionTypeSectionHeader: '',
  tagPromptSeparator: '',
  sectionSeparator: '',
  finalInstruction: '',
  isActive: true
})

// 过滤配置
const filteredConfigs = computed(() => {
  if (!searchQuery.value) return configs.value

  const query = searchQuery.value.toLowerCase()
  return configs.value.filter(config =>
    config.name.toLowerCase().includes(query) ||
    config.description.toLowerCase().includes(query) ||
    config.createdByUsername.toLowerCase().includes(query)
  )
})

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
    // 获取所有活跃的组装配置
    const response = await getActiveAnswerPromptAssemblyConfigs()
    configs.value = response?.configs || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取组装配置数据失败')
  } finally {
    loading.value = false
  }
}

// 创建配置
const handleCreate = () => {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑配置
const handleEdit = (row: any) => {
  isEditing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description,
    baseSystemPrompt: row.baseSystemPrompt,
    tagPromptsSectionHeader: row.tagPromptsSectionHeader,
    questionTypeSectionHeader: row.questionTypeSectionHeader,
    tagPromptSeparator: row.tagPromptSeparator,
    sectionSeparator: row.sectionSeparator,
    finalInstruction: row.finalInstruction,
    isActive: row.isActive
  })
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any, status: boolean) => {
  ElMessage.info('此功能尚未实现')
  // 由于API中没有提供更新组装配置状态的方法，这里只做提示
}

// 删除配置
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该组装配置吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    ElMessage.info('删除功能尚未实现')
    // 由于API中没有提供删除组装配置的方法，这里只做提示
  }).catch(() => {
    // 取消删除
  })
}

// 重置表单
const resetForm = () => {
  form.id = 0
  form.name = ''
  form.description = ''
  form.baseSystemPrompt = ''
  form.tagPromptsSectionHeader = ''
  form.questionTypeSectionHeader = ''
  form.tagPromptSeparator = ''
  form.sectionSeparator = ''
  form.finalInstruction = ''
  form.isActive = true
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const userId = userStore.currentUser?.id

        if (isEditing.value) {
          ElMessage.info('更新功能尚未实现')
          // 由于API中没有提供更新组装配置的方法，这里只做提示
        } else {
          await createAnswerPromptAssemblyConfig(form, userId)
          ElMessage.success('创建组装配置成功')
        }

        dialogVisible.value = false
        await fetchData()
      } catch (error) {
        console.error('提交表单失败:', error)
        ElMessage.error(isEditing.value ? '更新组装配置失败' : '创建组装配置失败')
      }
    }
  })
}
</script>

<style scoped>
.answer-assembly-prompt {
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
