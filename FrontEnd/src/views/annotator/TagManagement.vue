<template>
  <div class="tag-management">
    <el-card class="tag-card">
      <template #header>
        <div class="card-header">
          <h2>标签管理</h2>
          <div class="header-actions">
            <el-button type="primary" @click="openCreateTagDialog">创建标签</el-button>
            <el-button type="info" @click="refreshTags">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="search-filter">
        <el-input
          v-model="searchQuery"
          placeholder="搜索标签..."
          prefix-icon="el-icon-search"
          clearable
          style="width: 300px; margin-right: 15px;"
        />

        <el-select v-model="filterCategory" placeholder="分类筛选" clearable style="width: 200px;">
          <el-option
            v-for="category in categories"
            :key="category"
            :label="category"
            :value="category"
          />
        </el-select>
      </div>

      <el-table
        :data="filteredTags"
        style="width: 100%"
        border
        v-loading="loading"
      >
        <el-table-column prop="name" label="标签名称" />
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="使用量">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, (row.usageCount / maxUsage) * 100)"
              :format="() => row.usageCount"
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
        <el-table-column label="操作" width="280">
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
              :disabled="row.usageCount > 0"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalTags"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      :title="isEditing ? '编辑标签' : '创建标签'"
      v-model="dialogVisible"
      width="50%"
    >
      <el-form :model="form" label-width="100px" ref="formRef">
        <el-form-item label="标签名称" prop="name" :rules="[{ required: true, message: '请输入标签名称', trigger: 'blur' }]">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>

        <el-form-item label="分类" prop="category" :rules="[{ required: true, message: '请选择分类', trigger: 'change' }]">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option
              v-for="category in categories"
              :key="category"
              :label="category"
              :value="category"
            />
            <el-input
              v-if="addingNewCategory"
              v-model="newCategory"
              placeholder="请输入新分类名称"
              @keyup.enter="addCategory"
              style="width: 100%;"
            >
              <template #append>
                <el-button @click="addCategory">添加</el-button>
              </template>
            </el-input>
          </el-select>
          <div class="add-category-btn">
            <el-button type="text" @click="addingNewCategory = true">+ 添加新分类</el-button>
          </div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入标签描述"
          />
        </el-form-item>

        <el-form-item label="状态" prop="isActive">
          <el-switch
            v-model="form.isActive"
            active-text="启用"
            inactive-text="禁用"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'

// 导入相关API
// import { createTag, updateTag, deleteTag, getAllTags, getTagCategories } from '@/api/tags'

// 状态定义
const loading = ref(false)
const tags = ref<any[]>([])
const categories = ref<string[]>(['领域知识', '技术', '工具', '方法论', '行业', '概念', '其他'])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const searchQuery = ref('')
const filterCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalTags = ref(0)
const addingNewCategory = ref(false)
const newCategory = ref('')

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  category: '',
  description: '',
  isActive: true,
})

// 计算属性
const filteredTags = computed(() => {
  let result = tags.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(tag =>
      tag.name.toLowerCase().includes(query) ||
      tag.description.toLowerCase().includes(query) ||
      tag.category.toLowerCase().includes(query)
    )
  }

  if (filterCategory.value) {
    result = result.filter(tag => tag.category === filterCategory.value)
  }

  // 分页处理
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return result.slice(start, end)
})

const maxUsage = computed(() => {
  if (tags.value.length === 0) return 1
  return Math.max(...tags.value.map(tag => tag.usageCount))
})

// 生命周期钩子
onMounted(() => {
  fetchTags()
})

// 获取标签数据
const fetchTags = async () => {
  loading.value = true
  try {
    // 在实际应用中应该调用API
    // const response = await getAllTags()
    // tags.value = response.data || []

    // 模拟数据
    tags.value = [
      { id: 1, name: 'JavaScript', category: '技术', description: '一种脚本编程语言', isActive: true, usageCount: 245 },
      { id: 2, name: 'React', category: '技术', description: '用于构建用户界面的JavaScript库', isActive: true, usageCount: 183 },
      { id: 3, name: 'Python', category: '技术', description: '一种解释型高级编程语言', isActive: true, usageCount: 320 },
      { id: 4, name: 'DevOps', category: '方法论', description: '开发与运维的结合', isActive: true, usageCount: 98 },
      { id: 5, name: 'AI', category: '领域知识', description: '人工智能', isActive: true, usageCount: 276 },
      { id: 6, name: 'Docker', category: '工具', description: '容器化平台', isActive: true, usageCount: 145 },
      { id: 7, name: 'Kubernetes', category: '工具', description: '容器编排系统', isActive: false, usageCount: 72 },
      { id: 8, name: '金融科技', category: '行业', description: '金融与科技的结合', isActive: true, usageCount: 56 },
      { id: 9, name: '健康科技', category: '行业', description: '健康领域的科技应用', isActive: true, usageCount: 42 },
      { id: 10, name: '区块链', category: '技术', description: '分布式账本技术', isActive: false, usageCount: 89 },
      { id: 11, name: 'REST API', category: '概念', description: '表述性状态传递API', isActive: true, usageCount: 134 },
      { id: 12, name: 'GraphQL', category: '技术', description: '查询语言和运行时', isActive: true, usageCount: 67 },
      { id: 13, name: 'IoT', category: '领域知识', description: '物联网', isActive: true, usageCount: 98 },
      { id: 14, name: 'Vue.js', category: '技术', description: '渐进式JavaScript框架', isActive: true, usageCount: 167 },
      { id: 15, name: 'Node.js', category: '技术', description: 'JavaScript运行环境', isActive: true, usageCount: 201 },
    ]

    totalTags.value = tags.value.length

    // 获取分类
    // const categoriesResponse = await getTagCategories()
    // categories.value = categoriesResponse.data || []
  } catch (error) {
    console.error('获取标签失败:', error)
    ElMessage.error('获取标签失败')
  } finally {
    loading.value = false
  }
}

// 刷新标签数据
const refreshTags = () => {
  fetchTags()
}

// 打开创建标签对话框
const openCreateTagDialog = () => {
  isEditing.value = false
  form.id = 0
  form.name = ''
  form.category = ''
  form.description = ''
  form.isActive = true
  dialogVisible.value = true
}

// 编辑标签
const handleEdit = (row: any) => {
  isEditing.value = true
  form.id = row.id
  form.name = row.name
  form.category = row.category
  form.description = row.description
  form.isActive = row.isActive
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any, status: boolean) => {
  try {
    // 在实际应用中应该调用API
    // await updateTag(row.id, {
    //   ...row,
    //   isActive: status
    // })

    // 模拟更新
    const tag = tags.value.find(t => t.id === row.id)
    if (tag) {
      tag.isActive = status
    }

    ElMessage.success(`${status ? '启用' : '禁用'}成功`)
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除标签
const handleDelete = (row: any) => {
  if (row.usageCount > 0) {
    ElMessage.warning('该标签正在使用中，无法删除')
    return
  }

  ElMessageBox.confirm(
    '确定要删除该标签吗？此操作不可逆。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 在实际应用中应该调用API
      // await deleteTag(row.id)

      // 模拟删除
      tags.value = tags.value.filter(t => t.id !== row.id)
      totalTags.value = tags.value.length

      ElMessage.success('删除成功')
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 添加新分类
const addCategory = () => {
  if (!newCategory.value) {
    ElMessage.warning('请输入分类名称')
    return
  }

  if (categories.value.includes(newCategory.value)) {
    ElMessage.warning('该分类已存在')
    return
  }

  categories.value.push(newCategory.value)
  form.category = newCategory.value
  newCategory.value = ''
  addingNewCategory.value = false
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.value) {
          // 在实际应用中应该调用API
          // await updateTag(form.id, form)

          // 模拟更新
          const index = tags.value.findIndex(t => t.id === form.id)
          if (index !== -1) {
            tags.value[index] = { ...tags.value[index], ...form }
          }

          ElMessage.success('更新成功')
        } else {
          // 在实际应用中应该调用API
          // const response = await createTag(form)
          // const newTag = response.data

          // 模拟创建
          const newTag = {
            id: Math.max(...tags.value.map(t => t.id)) + 1,
            ...form,
            usageCount: 0
          }
          tags.value.push(newTag)
          totalTags.value = tags.value.length

          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
}

// 处理每页显示数量变化
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}
</script>

<style scoped>
.tag-management {
  padding: 20px;
}

.tag-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-filter {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.add-category-btn {
  margin-top: 5px;
  text-align: right;
}
</style>
