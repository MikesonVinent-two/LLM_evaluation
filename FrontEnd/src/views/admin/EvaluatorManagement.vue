<template>
  <div class="evaluator-management-container">
    <el-card class="evaluator-management-card">
      <template #header>
        <div class="card-header">
          <h2>评测员管理</h2>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="showCreateDialog">
              添加评测员
            </el-button>
            <el-radio-group v-model="currentType" @change="handleTypeChange" size="small">
              <el-radio-button label="ALL">全部</el-radio-button>
              <el-radio-button label="HUMAN">人类评测员</el-radio-button>
              <el-radio-button label="AI_MODEL">AI评测员</el-radio-button>
            </el-radio-group>
            <el-tooltip content="刷新列表" placement="top">
              <el-button :icon="Refresh" circle @click="fetchEvaluators" :loading="isLoading"></el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 数据加载中 -->
      <div v-if="isLoading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 数据为空 -->
      <div v-else-if="evaluators.length === 0" class="empty-container">
        <el-empty description="暂无评测员数据" />
        <el-button type="primary" @click="showCreateDialog">添加评测员</el-button>
      </div>

      <!-- 评测员列表 -->
      <div v-else>
        <el-table :data="evaluators" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="评测员名称" min-width="150" />
          <el-table-column prop="evaluatorType" label="类型" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.evaluatorType === 'HUMAN' ? 'success' : 'primary'">
                {{ scope.row.evaluatorType === 'HUMAN' ? '人类评测员' : 'AI评测员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联信息" min-width="180">
            <template #default="scope">
              <div v-if="scope.row.evaluatorType === 'HUMAN'">
                用户ID: {{ scope.row.user?.id || '未知' }}
              </div>
              <div v-else>
                模型ID: {{ scope.row.llmModel?.id || '未知' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdByUser.username" label="创建者" min-width="120" />
          <el-table-column prop="createdAt" label="创建时间" min-width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="primary"
                  :icon="Edit"
                  @click="showEditDialog(scope.row)"
                  :loading="isEditing === scope.row.id">
                  编辑
                </el-button>
                <el-popconfirm
                  :title="`确认删除评测员 ${scope.row.name}?`"
                  width="220"
                  confirm-button-text="确认"
                  cancel-button-text="取消"
                  @confirm="deleteEvaluatorItem(scope.row.id)">
                  <template #reference>
                    <el-button
                      size="small"
                      type="danger"
                      :icon="Delete"
                      :loading="isDeleting === scope.row.id">
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 创建评测员对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="isEditMode ? '编辑评测员' : '创建评测员'"
      width="500px"
      :close-on-click-modal="false">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px">
        <el-form-item label="评测员名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入评测员名称" />
        </el-form-item>

        <el-form-item label="评测员类型" prop="evaluatorType">
          <el-radio-group v-model="form.evaluatorType" @change="handleFormTypeChange">
            <el-radio :label="EvaluatorType.HUMAN">人类评测员</el-radio>
            <el-radio :label="EvaluatorType.AI_MODEL">AI评测员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.evaluatorType === EvaluatorType.HUMAN" label="关联用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="请输入用户名或姓名搜索"
            filterable
            remote
            :remote-method="handleUserSearch"
            :loading="isLoadingUsers"
            clearable>
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.name ? `${user.username}(${user.name})` : user.username"
              :value="user.id">
              <div class="user-option">
                <span>{{ user.username }}</span>
                <span v-if="user.name" class="user-name">({{ user.name }})</span>
                <el-tag size="small" class="user-role">{{ user.role }}</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.evaluatorType === EvaluatorType.AI_MODEL" label="关联模型" prop="modelId">
          <el-select
            v-model="form.modelId"
            placeholder="请选择模型"
            filterable
            :loading="isLoadingModels">
            <el-option
              v-for="model in modelOptions"
              :key="model.id"
              :label="`${model.name}(${model.provider})`"
              :value="model.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="isSubmitting">
            {{ isEditMode ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import {
  getEvaluatorsWithPagination,
  getEvaluatorsByType,
  createEvaluator,
  updateEvaluator,
  deleteEvaluator,
  type EvaluatorInfo,
  type GetEvaluatorsResponse,
  EvaluatorType
} from '@/api/evaluator'
import { useUserStore } from '@/stores/user'
import { getRegisteredLlmModels, type ModelInfo } from '@/api/llmModel'
import { searchUsers, type UserInfo, type UserSearchParams } from '@/api/user'
import type { FormInstance, FormRules } from 'element-plus'

// 评测员列表
const evaluators = ref<EvaluatorInfo[]>([])
// 加载状态
const isLoading = ref(false)
const isEditing = ref<number | null>(null)
const isDeleting = ref<number | null>(null)
const isSubmitting = ref(false)

// 用户store
const userStore = useUserStore()

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选相关
const currentType = ref('ALL')

// 对话框相关
const createDialogVisible = ref(false)
const isEditMode = ref(false)
const currentEditId = ref<number | null>(null)
const formRef = ref<FormInstance | null>(null)

// 用户和模型选项
const userOptions = ref<UserInfo[]>([])
const modelOptions = ref<ModelInfo[]>([])
const isLoadingUsers = ref(false)
const isLoadingModels = ref(false)

// 用户搜索相关
const userSearchTimeout = ref<number | null>(null)

// 表单数据
const form = reactive({
  name: '',
  evaluatorType: EvaluatorType.HUMAN,
  userId: undefined as number | undefined,
  modelId: undefined as number | undefined
})

// 表单验证规则 - 使用计算属性实现动态验证
const rules = computed<FormRules>(() => ({
  name: [
    { required: true, message: '请输入评测员名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  evaluatorType: [
    { required: true, message: '请选择评测员类型', trigger: 'change' }
  ],
  userId: form.evaluatorType === EvaluatorType.HUMAN ? [
    { required: true, message: '请选择关联用户', trigger: 'change' }
  ] : [],
  modelId: form.evaluatorType === EvaluatorType.AI_MODEL ? [
    { required: true, message: '请选择关联模型', trigger: 'change' }
  ] : []
}))

// 获取评测员列表
const fetchEvaluators = async () => {
  try {
    isLoading.value = true
    let response: GetEvaluatorsResponse

    if (currentType.value === 'ALL') {
      response = await getEvaluatorsWithPagination({
        pageNumber: currentPage.value,
        pageSize: pageSize.value
      })
    } else {
      response = await getEvaluatorsByType(currentType.value as EvaluatorType, {
        pageNumber: currentPage.value,
        pageSize: pageSize.value
      })
    }

    evaluators.value = response.evaluators
    total.value = response.total
  } catch (error) {
    console.error('获取评测员列表失败:', error)
    ElMessage.error('获取评测员列表失败')
  } finally {
    isLoading.value = false
  }
}

// 处理分页大小变化
const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchEvaluators()
}

// 处理当前页变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchEvaluators()
}

// 处理类型变化
const handleTypeChange = () => {
  currentPage.value = 1 // 重置到第一页
  fetchEvaluators()
}

// 显示创建对话框
const showCreateDialog = () => {
  resetForm()
  isEditMode.value = false
  currentEditId.value = null
  createDialogVisible.value = true
  // 预加载模型列表
  fetchModels()
}

// 显示编辑对话框
const showEditDialog = (row: EvaluatorInfo) => {
  resetForm()
  isEditMode.value = true
  currentEditId.value = row.id

  // 填充表单数据
  form.name = row.name
  form.evaluatorType = row.evaluatorType as EvaluatorType

  if (row.evaluatorType === EvaluatorType.HUMAN && row.user) {
    form.userId = row.user.id
    // 添加用户到选项中
    userOptions.value = [{
      id: row.user.id,
      username: '用户ID: ' + row.user.id,
      name: '',
      contactInfo: row.user.contactInfo || '',
      createdAt: row.user.createdAt || new Date().toISOString(),
      updatedAt: row.user.updatedAt || new Date().toISOString()
    }]
  } else if (row.evaluatorType === EvaluatorType.AI_MODEL && row.llmModel) {
    form.modelId = row.llmModel.id
    // 预加载模型列表
    fetchModels()
  }

  createDialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }

  form.name = ''
  form.evaluatorType = EvaluatorType.HUMAN
  form.userId = undefined
  form.modelId = undefined
}

// 处理表单类型变化
const handleFormTypeChange = () => {
  // 清除不相关字段的值和验证错误
  if (form.evaluatorType === EvaluatorType.HUMAN) {
    form.modelId = undefined
    // 清除modelId字段的验证错误
    if (formRef.value) {
      formRef.value.clearValidate('modelId')
    }
  } else {
    form.userId = undefined
    // 清除userId字段的验证错误
    if (formRef.value) {
      formRef.value.clearValidate('userId')
    }
  }
}

// 搜索用户
const handleUserSearch = async (query: string) => {
  if (query.length < 2) {
    userOptions.value = []
    return
  }

  // 清除之前的定时器
  if (userSearchTimeout.value) {
    clearTimeout(userSearchTimeout.value)
  }

  // 设置新的定时器，防抖处理
  userSearchTimeout.value = window.setTimeout(async () => {
    isLoadingUsers.value = true
    try {
      const params: UserSearchParams = {
        keyword: query,
        page: 0,
        size: 10
      }
      const response = await searchUsers(params)
      userOptions.value = response.users.map(user => ({
        id: user.id,
        username: user.username,
        name: user.name,
        role: user.role,
        contactInfo: user.contactInfo || '',
        createdAt: user.createdAt || new Date().toISOString(),
        updatedAt: user.updatedAt || new Date().toISOString()
      }))
    } catch (error) {
      console.error('搜索用户失败:', error)
      ElMessage.error('搜索用户失败')
      userOptions.value = []
    } finally {
      isLoadingUsers.value = false
    }
  }, 300)
}

// 获取模型列表
const fetchModels = async () => {
  try {
    isLoadingModels.value = true
    const response = await getRegisteredLlmModels()
    if (response.success) {
      modelOptions.value = response.models
    } else {
      ElMessage.error('获取模型列表失败')
    }
  } catch (error) {
    console.error('获取模型列表失败:', error)
    ElMessage.error('获取模型列表失败')
  } finally {
    isLoadingModels.value = false
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  try {
    // 验证表单
    await formRef.value.validate()

    // 额外的业务逻辑验证
    if (form.evaluatorType === EvaluatorType.HUMAN && !form.userId) {
      ElMessage.error('请选择关联用户')
      return
    }

    if (form.evaluatorType === EvaluatorType.AI_MODEL && !form.modelId) {
      ElMessage.error('请选择关联模型')
      return
    }

    isSubmitting.value = true

    if (isEditMode.value) {
      // 编辑模式
      if (!currentEditId.value) {
        ElMessage.error('未找到要编辑的评测员ID')
        return
      }

      await updateEvaluator(currentEditId.value, {
        name: form.name
      })

      ElMessage.success('更新评测员成功')
    } else {
      // 创建模式
      const currentUser = userStore.currentUser
      if (!currentUser || !currentUser.id) {
        ElMessage.error('未获取到当前用户信息')
        return
      }

      const data: any = {
        name: form.name,
        evaluatorType: form.evaluatorType,
        createdByUser: {
          id: currentUser.id
        }
      }

      if (form.evaluatorType === EvaluatorType.HUMAN) {
        data.user = {
          id: form.userId
        }
      } else {
        data.llmModel = {
          id: form.modelId
        }
      }

      await createEvaluator(data)

      ElMessage.success('创建评测员成功')
    }

    // 关闭对话框并刷新列表
    createDialogVisible.value = false
    fetchEvaluators()
  } catch (error: any) {
    console.error('提交表单失败:', error)

    // 如果是表单验证错误，显示具体的验证信息
    if (error && typeof error === 'object' && error.userId) {
      ElMessage.error('请选择关联用户')
    } else if (error && typeof error === 'object' && error.modelId) {
      ElMessage.error('请选择关联模型')
    } else if (error?.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('提交失败，请检查表单')
    }
  } finally {
    isSubmitting.value = false
  }
}

// 删除评测员
const deleteEvaluatorItem = async (id: number) => {
  try {
    isDeleting.value = id
    const result = await deleteEvaluator(id)

    if (result.success) {
      ElMessage.success(result.message || '删除评测员成功')
      fetchEvaluators()
    } else {
      ElMessage.error(result.message || '删除评测员失败')
    }
  } catch (error) {
    console.error('删除评测员失败:', error)
    ElMessage.error('删除评测员失败')
  } finally {
    isDeleting.value = null
  }
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '未知时间'

  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    return dateString
  }
}

// 生命周期钩子
onMounted(() => {
  fetchEvaluators()
})
</script>

<style scoped>
.evaluator-management-container {
  padding: 20px;
}

.evaluator-management-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.empty-container .el-button {
  margin-top: 20px;
}

.loading-container {
  padding: 20px 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  color: #666;
}

.user-role {
  margin-left: auto;
  font-size: 12px;
}
</style>
