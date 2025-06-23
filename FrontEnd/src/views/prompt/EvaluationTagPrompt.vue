<template>
  <div class="evaluation-tag-prompt">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>评测阶段 - 标签提示词管理</h3>
          <el-button type="primary" @click="handleCreate">创建提示词</el-button>
        </div>
      </template>

      <!-- 搜索和过滤区域 -->
      <div class="search-filter-container" style="margin-bottom: 20px; display: flex; gap: 10px; align-items: center;">
        <el-input
          v-model="searchQuery"
          placeholder="搜索提示词..."
          prefix-icon="Search"
          clearable
          style="width: 300px;"
        />

        <el-button
          type="primary"
          @click="showMissingTagsDialog"
          icon="Plus"
        >
          查看未创建提示词的标签
        </el-button>
      </div>

      <el-table
        :data="filteredPrompts"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="name" label="提示词名称" />
        <el-table-column prop="tag.tagName" label="关联标签" />
        <el-table-column label="优先级" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.promptPriority as number"
              :format="(percentage: number) => `${percentage}%`"
            />
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
        <el-table-column label="创建者" width="120">
          <template #default="{ row }">
            {{ row.createdByUser?.username || '未知' }}
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
        :title="isEditing ? '编辑标签提示词' : '创建标签提示词'"
        v-model="dialogVisible"
        width="70%"
      >
        <el-form :model="form" label-width="120px" ref="formRef">
          <el-form-item label="提示词名称" prop="name" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
            <el-input v-model="form.name" placeholder="请输入提示词名称" />
          </el-form-item>

          <el-form-item label="关联标签" prop="tagId" :rules="[{ required: true, message: '请选择关联标签', trigger: 'change' }]">
            <el-select v-model="form.tagId" placeholder="请选择关联标签" filterable>
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.tagName + (tag.hasPrompt ? '' : ' (未创建提示词)')"
                :value="tag.id"
              >
                <span>{{ tag.tagName }}</span>
                <el-tag v-if="!tag.hasPrompt" size="small" type="warning" style="margin-left: 5px;">未创建提示词</el-tag>
              </el-option>
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

      <!-- 未创建提示词的标签对话框 -->
      <el-dialog
        title="未创建提示词的标签"
        v-model="missingTagsDialogVisible"
        width="50%"
      >
        <el-table
          :data="missingTags"
          style="width: 100%"
          border
          v-loading="loading"
        >
          <el-table-column prop="id" label="标签ID" width="100" />
          <el-table-column prop="tagName" label="标签名称" />
          <el-table-column prop="tagType" label="标签类型" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="createPromptForTag(row)"
              >
                创建提示词
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="missingTagsDialogVisible = false">关闭</el-button>
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
import { Search, Plus } from '@element-plus/icons-vue'

// 导入相关API
import {
  createEvaluationTagPrompt,
  deleteEvaluationTagPrompt,
  getAllEvaluationTagPrompts,
  getActiveEvaluationTagPromptsByTagId,
  getEvaluationTagPromptById,
  type TagInfo
} from '@/api/evaluationTagPrompt'
import { getAllTags } from '@/api/tags'
import { useUserStore } from '@/stores/user'

// 状态定义
const loading = ref(false)
const prompts = ref<any[]>([])
const tags = ref<any[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const searchQuery = ref('')
const userStore = useUserStore()
const missingTagsDialogVisible = ref(false)
const missingTags = ref<any[]>([])

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  tagId: 0,
  promptTemplate: '',
  description: '',
  isActive: true,
  promptPriority: 50,
  version: '1.0',
  userId: 0
})

// 过滤提示词
const filteredPrompts = computed(() => {
  if (!searchQuery.value) return prompts.value

  const query = searchQuery.value.toLowerCase()
  return prompts.value.filter(prompt =>
    prompt.name.toLowerCase().includes(query) ||
    (prompt.tag?.tagName && prompt.tag.tagName.toLowerCase().includes(query)) ||
    prompt.description.toLowerCase().includes(query) ||
    prompt.promptTemplate.toLowerCase().includes(query) ||
    (prompt.createdByUser?.username && prompt.createdByUser.username.toLowerCase().includes(query))
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
    // 获取所有标签提示词
    const promptsResponse = await getAllEvaluationTagPrompts()
    prompts.value = promptsResponse || []

    // 获取所有标签
    const tagsResponse = await getAllTags()
    console.log('标签API返回数据:', tagsResponse)

    // 处理标签数据
    if (Array.isArray(tagsResponse)) {
      // API直接返回了标签对象数组
      tags.value = tagsResponse.map(tag => ({
        id: tag.id || 0,
        tagName: tag.tagName || '未命名标签',
        hasPrompt: tag.hasEvaluationPrompt || false // 是否已有评测提示词
      }))
    } else {
      // 如果API返回格式不符合预期，创建一些测试标签数据
      console.error('标签API返回格式不符合预期:', tagsResponse)
      tags.value = [
        { id: 1, tagName: '内科', hasPrompt: true },
        { id: 2, tagName: '外科', hasPrompt: true },
        { id: 3, tagName: '心脏病', hasPrompt: false }
      ]
    }

    // 标记哪些标签已经有提示词
    if (prompts.value && prompts.value.length > 0) {
      const tagIdsWithPrompts = new Set(prompts.value.map(prompt => prompt.tag?.id))
      tags.value.forEach(tag => {
        tag.hasPrompt = tagIdsWithPrompts.has(tag.id)
      })
    }

    console.log('处理后的标签数据:', tags.value)
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取标签提示词数据失败')
    // 创建一些测试标签数据
    tags.value = [
      { id: 1, tagName: '内科', hasPrompt: true },
      { id: 2, tagName: '外科', hasPrompt: true },
      { id: 3, tagName: '心脏病', hasPrompt: false }
    ]
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
    tagId: row.tag.id,
    promptTemplate: row.promptTemplate,
    description: row.description,
    isActive: row.isActive,
    promptPriority: row.promptPriority,
    version: row.version,
    userId: userStore.currentUser?.id || 0
  })
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any, status: boolean) => {
  ElMessage.info('此功能尚未实现')
  // 由于API中没有提供更新评测标签提示词状态的方法，这里只做提示
}

// 删除提示词
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该提示词吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteEvaluationTagPrompt(row.id, { userId: userStore.currentUser?.id || 0 })
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
  form.tagId = 0
  form.promptTemplate = ''
  form.description = ''
  form.isActive = true
  form.promptPriority = 50
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

        // 确保tagId不为undefined
        if (!form.tagId) {
          ElMessage.error('请选择关联标签')
          return
        }

        // 确保tagId是数字类型
        form.tagId = Number(form.tagId)

        if (isEditing.value) {
          ElMessage.info('更新功能尚未实现')
          // 由于API中没有提供更新评测标签提示词的方法，这里只做提示
        } else {
          await createEvaluationTagPrompt(form)
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

// 计算未创建提示词的标签
const computeMissingTags = () => {
  // 如果标签数据尚未加载，返回空数组
  if (!tags.value || tags.value.length === 0) return []

  // 获取所有未创建提示词的标签
  return tags.value.filter(tag => !tag.hasPrompt)
}

// 显示未创建提示词的标签对话框
const showMissingTagsDialog = () => {
  missingTags.value = computeMissingTags()
  missingTagsDialogVisible.value = true
}

// 创建提示词为未创建提示词的标签
const createPromptForTag = (tag: any) => {
  // 关闭对话框
  missingTagsDialogVisible.value = false

  // 打开创建提示词对话框
  isEditing.value = false
  resetForm()

  // 预填标签ID
  form.tagId = tag.id

  // 打开对话框
  dialogVisible.value = true
}
</script>

<style scoped>
.evaluation-tag-prompt {
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

.search-filter-container {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
