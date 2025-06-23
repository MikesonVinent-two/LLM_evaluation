<template>
  <div class="user-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>用户管理</h2>
        </div>
      </template>
      <div class="management-content">
        <!-- 搜索区域 -->
        <div class="search-area">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入用户名、姓名或联系方式"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>

        <!-- 用户列表 -->
        <el-table
          v-loading="loading"
          :data="userList"
          style="width: 100%"
          border
          stripe
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="name" label="姓名" width="150">
            <template #default="scope">
              {{ scope.row.name || '未设置' }}
            </template>
          </el-table-column>
          <el-table-column prop="contactInfo" label="联系方式" width="200" />
          <el-table-column prop="role" label="角色" width="150">
            <template #default="scope">
              <el-tag :type="getRoleTagType(scope.row.role)">{{ getRoleDisplayName(scope.row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评测员状态" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.isEvaluator" type="success" size="small">
                <el-icon><UserFilled /></el-icon>
                评测员
              </el-tag>
              <el-tag v-else type="info" size="small">
                <el-icon><User /></el-icon>
                普通用户
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="200">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" @click="handleViewUser(scope.row)">查看</el-button>
                <el-button
                  v-if="!scope.row.isEvaluator"
                  size="small"
                  type="success"
                  :loading="evaluatorLoading === scope.row.id"
                  @click="handleSetAsEvaluator(scope.row)">
                  设为评测员
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="warning"
                  :loading="evaluatorLoading === scope.row.id"
                  @click="handleRemoveEvaluator(scope.row)">
                  取消评测员
                </el-button>
                <el-dropdown trigger="click" @command="(command) => handleCommand(command, scope.row)">
                  <el-button size="small" type="primary">
                    更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="deactivate" divided>注销账户</el-dropdown-item>
                      <el-dropdown-item command="delete" divided style="color: #F56C6C;">删除用户</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页器 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalElements"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑用户"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="editForm.password"
            type="password"
            placeholder="请输入新密码（留空则不修改）"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contactInfo">
          <el-input v-model="editForm.contactInfo" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" placeholder="请选择角色">
            <el-option
              v-for="(label, role) in roleOptions"
              :key="role"
              :label="label"
              :value="role"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitEditForm">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看用户详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="用户详情"
      width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ viewingUser.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ viewingUser.name || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ viewingUser.contactInfo }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="getRoleTagType(viewingUser.role)">
            {{ getRoleDisplayName(viewingUser.role) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评测员状态">
          <el-tag v-if="viewingUser.isEvaluator" type="success" size="small">
            <el-icon><UserFilled /></el-icon>
            评测员 (ID: {{ viewingUser.evaluatorId }})
          </el-tag>
          <el-tag v-else type="info" size="small">
            <el-icon><User /></el-icon>
            普通用户
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(viewingUser.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(viewingUser.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown, User, UserFilled } from '@element-plus/icons-vue'
import { searchUsers, UserRole, deleteUser, updateUser, deactivateUser } from '@/api/user'
import type { UserInfo, UpdateUserData } from '@/api/user'
import { createEvaluator, deleteEvaluator, EvaluatorType } from '@/api/evaluator'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'

// 搜索关键词
const searchKeyword = ref('')

// 用户列表数据
const userList = ref<UserInfo[]>([])
const loading = ref(false)

// 分页相关
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

// 编辑用户相关
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const submitting = ref(false)
const currentUserId = ref<number | null>(null)

// 用户store
const userStore = useUserStore()

// 评测员操作相关
const evaluatorLoading = ref<number | null>(null)

// 编辑表单数据
const editForm = reactive<UpdateUserData>({
  username: '',
  password: '',
  name: '',
  contactInfo: '',
  role: UserRole.CROWDSOURCE_USER
})

// 表单验证规则
const editFormRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度应为4-50个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  contactInfo: [
    { required: true, message: '请输入联系方式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
})

// 角色选项
const roleOptions = {
  [UserRole.ADMIN]: '管理员',
  [UserRole.CURATOR]: '策展人',
  [UserRole.EXPERT]: '专家',
  [UserRole.ANNOTATOR]: '标注员',
  [UserRole.REFEREE]: '评审员',
  [UserRole.CROWDSOURCE_USER]: '众包用户'
}

// 查看用户相关
const viewDialogVisible = ref(false)
const viewingUser = ref<UserInfo>({
  id: 0,
  username: '',
  name: '',
  contactInfo: '',
  role: UserRole.CROWDSOURCE_USER,
  createdAt: '',
  updatedAt: ''
})

// 初始化加载用户数据
onMounted(() => {
  // 确保用户store已初始化
  userStore.initializeFromStorage()

  // 调试：检查localStorage中的用户数据
  const storedUser = localStorage.getItem('user')
  console.log('localStorage中的用户数据:', storedUser)
  if (storedUser) {
    try {
      const userData = JSON.parse(storedUser)
      console.log('解析后的用户数据:', userData)
    } catch (e) {
      console.error('解析用户数据失败:', e)
    }
  }

  fetchUserList()
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const response = await searchUsers({
      page: currentPage.value,
      size: pageSize.value,
      sort: 'id,desc',
      keyword: searchKeyword.value
    })

    userList.value = response.users
    totalElements.value = response.pageInfo.totalElements
    totalPages.value = response.pageInfo.totalPages
  } catch (error) {
    console.error('获取用户列表失败', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 0
  fetchUserList()
}

// 分页大小变化处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  fetchUserList()
}

// 页码变化处理
const handleCurrentChange = (page: number) => {
  currentPage.value = page - 1 // API从0开始计数
  fetchUserList()
}

// 查看用户详情
const handleViewUser = (user: UserInfo) => {
  viewingUser.value = { ...user }
  viewDialogVisible.value = true
}

// 编辑用户
const handleEditUser = (user: UserInfo) => {
  // 设置当前编辑的用户ID
  currentUserId.value = user.id

  // 填充表单数据
  editForm.username = user.username
  editForm.password = '' // 密码不回显，留空表示不修改
  editForm.name = user.name || ''
  editForm.contactInfo = user.contactInfo
  editForm.role = user.role

  // 显示编辑对话框
  editDialogVisible.value = true
}

// 提交编辑表单
const submitEditForm = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid && currentUserId.value) {
      submitting.value = true

      try {
        await updateUser(currentUserId.value, editForm)
        ElMessage.success('用户信息更新成功')
        editDialogVisible.value = false
        // 重新加载用户列表
        fetchUserList()
      } catch (error) {
        console.error('更新用户失败', error)
        ElMessage.error('更新用户失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 删除用户
const handleDeleteUser = (user: UserInfo) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${user.username}" 吗？此操作不可恢复！`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await deleteUser(user.id)
        ElMessage({
          type: 'success',
          message: `用户 "${user.username}" 已成功删除`,
        })
        // 重新加载用户列表
        fetchUserList()
      } catch (error) {
        console.error('删除用户失败', error)
        ElMessage.error('删除用户失败')
      }
    })
    .catch(() => {
      ElMessage({
        type: 'info',
        message: '已取消删除',
      })
    })
}

// 注销账户
const handleDeactivateUser = (user: UserInfo) => {
  ElMessageBox.confirm(
    `确定要注销用户 "${user.username}" 的账户吗？注销后用户将无法登录，但数据将保留。`,
    '注销确认',
    {
      confirmButtonText: '确定注销',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await deactivateUser(user.id)
        ElMessage({
          type: 'success',
          message: `用户 "${user.username}" 的账户已成功注销`,
        })
        // 重新加载用户列表
        fetchUserList()
      } catch (error) {
        console.error('注销账户失败', error)
        ElMessage.error('注销账户失败')
      }
    })
    .catch(() => {
      ElMessage({
        type: 'info',
        message: '已取消注销操作',
      })
    })
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取角色显示名称
const getRoleDisplayName = (role: UserRole) => {
  const roleMap: Record<UserRole, string> = {
    [UserRole.ADMIN]: '管理员',
    [UserRole.CURATOR]: '策展人',
    [UserRole.EXPERT]: '专家',
    [UserRole.ANNOTATOR]: '标注员',
    [UserRole.REFEREE]: '评审员',
    [UserRole.CROWDSOURCE_USER]: '众包用户'
  }
  return roleMap[role] || role
}

// 获取角色标签类型
const getRoleTagType = (role: UserRole) => {
  const typeMap: Record<UserRole, string> = {
    [UserRole.ADMIN]: 'danger',
    [UserRole.CURATOR]: 'warning',
    [UserRole.EXPERT]: 'success',
    [UserRole.ANNOTATOR]: 'info',
    [UserRole.REFEREE]: 'warning',
    [UserRole.CROWDSOURCE_USER]: 'info'
  }
  return typeMap[role] || ''
}

// 设为评测员
const handleSetAsEvaluator = async (user: UserInfo) => {
  try {
    const result = await ElMessageBox.confirm(
      `确定要将用户 "${user.username}" 设为评测员吗？`,
      '设为评测员',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
      }
    )

    if (result === 'confirm') {
      evaluatorLoading.value = user.id

      const currentUser = userStore.getCurrentUser()
      console.log('当前用户信息:', currentUser)

      if (!currentUser) {
        ElMessage.error('未获取到当前用户信息，请重新登录')
        return
      }

      if (!currentUser.id) {
        ElMessage.error('当前用户ID为空，请重新登录')
        console.error('当前用户对象:', currentUser)
        return
      }

      // 创建评测员
      const evaluatorData = {
        name: `${user.username}的评测账号`,
        evaluatorType: EvaluatorType.HUMAN,
        user: {
          id: user.id
        },
        createdByUser: {
          id: currentUser.id
        }
      }

      console.log('创建评测员请求数据:', evaluatorData)
      await createEvaluator(evaluatorData)

      ElMessage.success(`用户 "${user.username}" 已成功设为评测员`)
      // 重新加载用户列表
      fetchUserList()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('设为评测员失败:', error)
      ElMessage.error(error?.response?.data?.message || '设为评测员失败')
    }
  } finally {
    evaluatorLoading.value = null
  }
}

// 取消评测员
const handleRemoveEvaluator = async (user: UserInfo) => {
  if (!user.evaluatorId) {
    ElMessage.error('未找到评测员信息')
    return
  }

  try {
    const result = await ElMessageBox.confirm(
      `确定要取消用户 "${user.username}" 的评测员身份吗？此操作将删除相关的评测员记录。`,
      '取消评测员',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    if (result === 'confirm') {
      evaluatorLoading.value = user.id

      // 删除评测员
      const deleteResult = await deleteEvaluator(user.evaluatorId)

      if (deleteResult.success) {
        ElMessage.success(`用户 "${user.username}" 的评测员身份已成功取消`)
        // 重新加载用户列表
        fetchUserList()
      } else {
        ElMessage.error(deleteResult.message || '取消评测员失败')
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消评测员失败:', error)
      ElMessage.error(error?.response?.data?.message || '取消评测员失败')
    }
  } finally {
    evaluatorLoading.value = null
  }
}

// 处理下拉菜单命令
const handleCommand = (command: string, user: UserInfo) => {
  switch (command) {
    case 'edit':
      handleEditUser(user)
      break
    case 'delete':
      handleDeleteUser(user)
      break
    case 'deactivate':
      handleDeactivateUser(user)
      break
    default:
      break
  }
}
</script>

<style scoped>
.user-management {
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

.management-content {
  padding: 20px 0;
}

.search-area {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
}

.search-input {
  width: 400px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
}
</style>
