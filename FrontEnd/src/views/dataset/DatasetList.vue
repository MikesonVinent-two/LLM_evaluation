<template>
  <div class="dataset-list-container">
    <!-- 标题卡片 -->
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>数据集列表</h2>
          <div class="header-actions">
            <el-button type="primary" @click="goToCreateDataset" :icon="Plus">
              创建数据集
            </el-button>
            <el-tooltip content="刷新列表" placement="top">
              <el-button :icon="Refresh" circle @click="fetchDatasetVersions" :loading="isLoading" />
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 加载中状态 -->
      <div v-if="isLoading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="!datasetVersions.length" class="empty-container">
        <el-empty description="暂无数据集版本" />
        <el-button type="primary" @click="goToCreateDataset">创建数据集</el-button>
      </div>

      <!-- 数据集版本列表 -->
      <div v-else class="dataset-versions-container">
        <el-table :data="datasetVersions" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="versionNumber" label="版本号" width="120" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="questionCount" label="问题数量" width="100" align="center" />
          <el-table-column prop="creationTime" label="创建时间" width="160">
            <template #default="scope">
              {{ formatDate(scope.row.creationTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdByUserName" label="创建者" width="120" />
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  @click="viewVersionQuestions(scope.row.id)"
                  :loading="isViewingQuestions === scope.row.id"
                >
                  查看问题
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  plain
                  @click="openEditDialog(scope.row)"
                  :loading="isEditing === scope.row.id"
                >
                  编辑信息
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  plain
                  @click="goToEditDataset(scope.row.id)"
                >
                  编辑问题
                </el-button>
                <el-button
                  size="small"
                  type="info"
                  plain
                  @click="cloneVersion(scope.row)"
                  :loading="isCloning === scope.row.id"
                >
                  克隆
                </el-button>
                <el-popconfirm
                  :title="`确定要删除数据集版本 ${scope.row.versionNumber} 吗?`"
                  @confirm="deleteVersion(scope.row.id)"
                >
                  <template #reference>
                    <el-button
                      size="small"
                      type="danger"
                      plain
                      :loading="isDeleting === scope.row.id"
                    >
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 编辑数据集版本对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑数据集版本"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="版本号" prop="versionNumber">
          <el-input v-model="form.versionNumber" placeholder="请输入版本号，如 1.0.0" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入数据集描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="isSubmitting">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 克隆数据集版本对话框 -->
    <el-dialog
      v-model="cloneDialogVisible"
      title="克隆数据集版本"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="cloneFormRef"
        :model="cloneForm"
        :rules="cloneRules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="源版本">
          <span>{{ cloneSourceVersion?.versionNumber }} - {{ cloneSourceVersion?.name }}</span>
        </el-form-item>
        <el-form-item label="新版本号" prop="newVersionNumber">
          <el-input v-model="cloneForm.newVersionNumber" placeholder="请输入新版本号，如 1.0.1" />
        </el-form-item>
        <el-form-item label="新名称" prop="newName">
          <el-input v-model="cloneForm.newName" placeholder="请输入新数据集名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="cloneForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入数据集描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cloneDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCloneForm" :loading="isCloneSubmitting">
            克隆
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看数据集问题对话框 -->
    <el-dialog
      v-model="questionsDialogVisible"
      :title="`数据集问题列表 - ${currentDatasetVersion?.name || ''}`"
      width="900px"
      destroy-on-close
      @closed="handleQuestionsDialogClosed"
    >
      <div v-if="isLoadingQuestions" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="!datasetQuestions.length" class="empty-container">
        <el-empty description="此数据集暂无问题" />
        <el-button type="primary" @click="currentDatasetVersion?.id ? goToEditDataset(currentDatasetVersion.id) : ElMessage.error('数据集ID不存在')">添加问题</el-button>
      </div>
      <div v-else>
        <div class="dialog-actions">
          <el-button type="primary" @click="currentDatasetVersion?.id ? goToEditDataset(currentDatasetVersion.id) : ElMessage.error('数据集ID不存在')">添加问题</el-button>
        </div>
        <el-table :data="datasetQuestions" border stripe style="width: 100%; margin-top: 15px;">
          <el-table-column prop="standardQuestionId" label="问题ID" width="80" />
          <el-table-column prop="standardQuestionText" label="问题文本" min-width="300" show-overflow-tooltip />
          <el-table-column prop="orderInDataset" label="顺序" width="80" align="center" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-popconfirm
                title="确认从数据集中移除此问题?"
                @confirm="removeQuestionFromDataset(scope.row)"
              >
                <template #reference>
                  <el-button size="small" type="danger" plain>移除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页器 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="questionsPage"
            v-model:page-size="questionsPageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            :total="questionsTotalElements"
            @size-change="handleQuestionsSizeChange"
            @current-change="handleQuestionsPageChange"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getAllDatasetVersions,
  updateDatasetVersion,
  deleteDatasetVersion,
  cloneDatasetVersion,
  getDatasetVersionQuestions,
  type DatasetVersionResponse,
  type DatasetQuestionItem
} from '@/api/dataset'
import { useUserStore } from '@/stores/user'

// 路由
const router = useRouter()

// 用户信息
const userStore = useUserStore()

// 数据集版本列表
const datasetVersions = ref<DatasetVersionResponse[]>([])

// 加载状态
const isLoading = ref(false)
const isSubmitting = ref(false)
const isEditing = ref<number | null>(null)
const isDeleting = ref<number | null>(null)
const isCloning = ref<number | null>(null)
const isViewingQuestions = ref<number | null>(null)
const isCloneSubmitting = ref(false)

// 对话框控制
const dialogVisible = ref(false)
const currentEditId = ref<number | null>(null)
const formRef = ref<FormInstance | null>(null)

// 克隆对话框控制
const cloneDialogVisible = ref(false)
const cloneSourceVersion = ref<DatasetVersionResponse | null>(null)
const cloneFormRef = ref<FormInstance | null>(null)

// 问题对话框控制
const questionsDialogVisible = ref(false)
const isLoadingQuestions = ref(false)
const datasetQuestions = ref<DatasetQuestionItem[]>([])
const currentDatasetVersion = ref<DatasetVersionResponse | null>(null)
const questionsPage = ref(1)
const questionsPageSize = ref(10)
const questionsTotalElements = ref(0)

// 表单数据
const form = reactive({
  versionNumber: '',
  name: '',
  description: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入数据集描述', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ]
}

// 克隆表单数据
const cloneForm = reactive({
  newVersionNumber: '',
  newName: '',
  description: ''
})

// 克隆表单验证规则
const cloneRules = {
  newVersionNumber: [
    { required: true, message: '请输入新版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+(\.\d+)?$/, message: '版本号格式应为 x.y.z', trigger: 'blur' }
  ],
  newName: [
    { required: true, message: '请输入新数据集名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入数据集描述', trigger: 'blur' }
  ]
}

// 获取所有数据集版本
const fetchDatasetVersions = async () => {
  try {
    isLoading.value = true
    const response = await getAllDatasetVersions()
    datasetVersions.value = response
  } catch (error) {
    console.error('获取数据集版本失败:', error)
    ElMessage.error('获取数据集版本失败')
  } finally {
    isLoading.value = false
  }
}

// 前往创建数据集页面
const goToCreateDataset = () => {
  router.push('/dataset/create')
}

// 前往编辑数据集页面
const goToEditDataset = (datasetId: number) => {
  if (datasetId) {
  router.push(`/dataset/edit/${datasetId}`)
  } else {
    ElMessage.error('数据集ID不存在')
  }
}

// 打开编辑对话框
const openEditDialog = (row: DatasetVersionResponse) => {
  currentEditId.value = row.id
  form.versionNumber = row.versionNumber
  form.name = row.name
  form.description = row.description
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    isSubmitting.value = true
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    if (!currentEditId.value) {
      ElMessage.error('未找到要编辑的数据集版本ID')
      return
    }

    await updateDatasetVersion(currentEditId.value, {
      name: form.name,
      description: form.description,
      standardQuestionsToAdd: [],
      standardQuestionsToRemove: [],
      userId: currentUser.id
    })

    ElMessage.success('更新数据集版本成功')
    dialogVisible.value = false
    fetchDatasetVersions()
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('提交失败，请检查表单')
  } finally {
    isSubmitting.value = false
  }
}

// 删除数据集版本
const deleteVersion = async (id: number) => {
  try {
    isDeleting.value = id
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    await deleteDatasetVersion(id, { userId: currentUser.id })
    ElMessage.success('删除数据集版本成功')
    fetchDatasetVersions()
  } catch (error) {
    console.error('删除数据集版本失败:', error)
    ElMessage.error('删除数据集版本失败')
  } finally {
    isDeleting.value = null
  }
}

// 克隆数据集版本
const cloneVersion = (row: DatasetVersionResponse) => {
  cloneSourceVersion.value = row
  cloneForm.newVersionNumber = generateNextVersion(row.versionNumber)
  cloneForm.newName = `${row.name} - 副本`
  cloneForm.description = row.description
  cloneDialogVisible.value = true
}

// 生成下一个版本号
const generateNextVersion = (currentVersion: string): string => {
  const parts = currentVersion.split('.')
  if (parts.length === 3) {
    const patch = parseInt(parts[2]) + 1
    return `${parts[0]}.${parts[1]}.${patch}`
  } else if (parts.length === 2) {
    return `${parts[0]}.${parts[1]}.1`
  } else {
    return `${currentVersion}.1`
  }
}

// 提交克隆表单
const submitCloneForm = async () => {
  if (!cloneFormRef.value || !cloneSourceVersion.value) return

  try {
    await cloneFormRef.value.validate()

    isCloneSubmitting.value = true
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    await cloneDatasetVersion(cloneSourceVersion.value.id, {
      newVersionNumber: cloneForm.newVersionNumber,
      newName: cloneForm.newName,
      description: cloneForm.description,
      userId: currentUser.id
    })

    ElMessage.success('克隆数据集版本成功')
    cloneDialogVisible.value = false
    fetchDatasetVersions()
  } catch (error) {
    console.error('克隆数据集版本失败:', error)
    ElMessage.error('克隆数据集版本失败')
  } finally {
    isCloneSubmitting.value = false
  }
}

// 查看版本问题
const viewVersionQuestions = async (versionId: number) => {
  try {
    isViewingQuestions.value = versionId
    const version = datasetVersions.value.find(v => v.id === versionId)
    if (!version) {
      throw new Error('未找到数据集版本')
    }

    currentDatasetVersion.value = version
    questionsDialogVisible.value = true
    await fetchDatasetQuestions(versionId)
  } catch (error) {
    console.error('加载数据集问题失败:', error)
    ElMessage.error('加载数据集问题失败')
  } finally {
    isViewingQuestions.value = null
  }
}

// 获取数据集问题
const fetchDatasetQuestions = async (versionId: number) => {
  try {
    isLoadingQuestions.value = true
    const response = await getDatasetVersionQuestions(versionId, {
      page: (questionsPage.value - 1).toString(), // 后端分页从0开始
      size: questionsPageSize.value.toString()
    })

    datasetQuestions.value = response.content
    questionsTotalElements.value = response.totalElements
  } catch (error) {
    console.error('获取数据集问题失败:', error)
    ElMessage.error('获取数据集问题失败')
  } finally {
    isLoadingQuestions.value = false
  }
}

// 处理问题分页大小变化
const handleQuestionsSizeChange = (val: number) => {
  questionsPageSize.value = val
  if (currentDatasetVersion.value) {
    fetchDatasetQuestions(currentDatasetVersion.value.id)
  }
}

// 处理问题页码变化
const handleQuestionsPageChange = (val: number) => {
  questionsPage.value = val
  if (currentDatasetVersion.value) {
    fetchDatasetQuestions(currentDatasetVersion.value.id)
  }
}

// 从数据集中移除问题
const removeQuestionFromDataset = async (question: DatasetQuestionItem) => {
  if (!currentDatasetVersion.value) return

  try {
    const currentUser = userStore.currentUser

    if (!currentUser || !currentUser.id) {
      ElMessage.error('未获取到当前用户信息')
      return
    }

    await updateDatasetVersion(currentDatasetVersion.value.id, {
      name: currentDatasetVersion.value.name,
      description: currentDatasetVersion.value.description,
      standardQuestionsToAdd: [],
      standardQuestionsToRemove: [question.standardQuestionId],
      userId: currentUser.id
    })

    ElMessage.success('成功从数据集中移除问题')

    // 刷新数据集问题列表和数据集版本列表
    if (currentDatasetVersion.value) {
      fetchDatasetQuestions(currentDatasetVersion.value.id)
    }
    fetchDatasetVersions()
  } catch (error) {
    console.error('从数据集中移除问题失败:', error)
    ElMessage.error('从数据集中移除问题失败')
  }
}

// 处理问题对话框关闭
const handleQuestionsDialogClosed = () => {
  // 清理状态
  datasetQuestions.value = []
  currentDatasetVersion.value = null
  questionsPage.value = 1
  questionsPageSize.value = 10
  questionsTotalElements.value = 0
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 生命周期钩子
onMounted(() => {
  fetchDatasetVersions()
})
</script>

<style scoped>
.dataset-list-container {
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

.dataset-versions-container {
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.search-container {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 15px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
}
</style>
