<template>
  <div class="profile-view">
    <div class="container">
      <div class="profile-header">
        <h1>个人信息</h1>
        <p class="subtitle">查看和编辑您的个人资料</p>
      </div>

      <div class="profile-container">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="6" animated />
        </div>

        <div v-else-if="error" class="error-container">
          <el-result
            icon="error"
            title="获取个人信息失败"
            :sub-title="error"
          >
            <template #extra>
              <el-button type="primary" @click="fetchUserProfile">重试</el-button>
            </template>
          </el-result>
        </div>

        <div v-else-if="userProfile" class="profile-content">
          <el-card class="profile-card">
            <template #header>
              <div class="card-header">
                <h3>基本信息</h3>
                <div class="action-buttons">
                  <el-button type="primary" @click="handleEdit">编辑资料</el-button>
                  <el-button type="warning" @click="handleDeactivate">注销账户</el-button>
                </div>
              </div>
            </template>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">
                {{ userProfile.username }}
              </el-descriptions-item>
              <el-descriptions-item label="姓名">
                {{ userProfile.name || '暂未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="getRoleTagType(userProfile.role)">
                  {{ getRoleDisplayName(userProfile.role) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="联系方式">
                {{ userProfile.contactInfo }}
              </el-descriptions-item>
              <el-descriptions-item label="账号创建时间">
                {{ formatDate(userProfile.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="最近更新时间">
                {{ formatDate(userProfile.updatedAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card v-if="isEvaluator" class="profile-card mt-20">
            <template #header>
              <div class="card-header">
                <h3>评测员信息</h3>
              </div>
            </template>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="评测员ID">
                {{ evaluatorId }}
              </el-descriptions-item>
              <el-descriptions-item label="评测员状态">
                <el-tag type="success">已激活</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>

        <div v-else class="empty-container">
          <el-empty description="未找到个人信息" />
        </div>
      </div>
    </div>

    <!-- 编辑个人信息对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑个人信息"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="top"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contactInfo">
          <el-input v-model="form.contactInfo" placeholder="请输入联系方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 注销账户确认对话框 -->
    <el-dialog
      v-model="deactivateDialogVisible"
      title="注销账户确认"
      width="500px"
    >
      <div class="deactivate-content">
        <el-alert
          title="注销账户将导致以下结果："
          type="warning"
          :closable="false"
          show-icon
        />
        <ul class="deactivate-list">
          <li>您将无法再使用此账户登录系统</li>
          <li>您的个人数据将被保留，但账户状态将变为"已注销"</li>
          <li>如需恢复账户，请联系系统管理员</li>
        </ul>
        <el-divider />
        <p class="confirm-text">请输入您的密码以确认注销操作：</p>
        <el-input
          v-model="deactivateForm.password"
          type="password"
          placeholder="请输入密码"
          show-password
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deactivateDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDeactivate" :loading="deactivating">
            确认注销
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserById, updateUserInfo, deactivateUser } from '@/api/user'
import type { UserInfo, UserRole } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()
const userProfile = ref<UserInfo | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// 编辑相关状态
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  name: '',
  contactInfo: ''
})

// 注销账户相关状态
const deactivateDialogVisible = ref(false)
const deactivating = ref(false)
const deactivateForm = reactive({
  password: ''
})

// 表单规则
const rules = {
  contactInfo: [
    { required: true, message: '请输入联系方式', trigger: 'blur' }
  ]
}

// 评测员信息
const isEvaluator = computed(() => userStore.currentUser?.isEvaluator || false)
const evaluatorId = computed(() => userStore.currentUser?.evaluatorId || '-')

// 获取用户资料
const fetchUserProfile = async () => {
  if (!userStore.currentUser) {
    error.value = '未登录'
    return
  }

  try {
    loading.value = true
    error.value = null

    const userId = userStore.currentUser.id
    const result = await getUserById(userId)
    userProfile.value = result
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取个人信息失败'
    console.error('获取个人信息失败:', err)
  } finally {
    loading.value = false
  }
}

// 处理编辑按钮点击
const handleEdit = () => {
  if (!userProfile.value) return

  form.name = userProfile.value.name || ''
  form.contactInfo = userProfile.value.contactInfo || ''

  dialogVisible.value = true
}

// 处理注销账户按钮点击
const handleDeactivate = () => {
  deactivateForm.password = ''
  deactivateDialogVisible.value = true
}

// 确认注销账户
const confirmDeactivate = async () => {
  if (!userProfile.value || !deactivateForm.password) {
    ElMessage.warning('请输入密码以确认注销操作')
    return
  }

  try {
    deactivating.value = true

    // 二次确认
    await ElMessageBox.confirm(
      '此操作将永久注销您的账户，是否继续？',
      '警告',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用注销API
    await deactivateUser(userProfile.value.id)

    ElMessage.success('账户已成功注销')
    deactivateDialogVisible.value = false

    // 退出登录并跳转到登录页
    userStore.logout()
    router.push('/login')
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err instanceof Error ? err.message : '注销账户失败')
      console.error('注销账户失败:', err)
    }
  } finally {
    deactivating.value = false
  }
}

// 提交表单
const submitForm = async () => {
  if (!userProfile.value) return

  try {
    submitting.value = true

    const updatedData = {
      name: form.name,
      contactInfo: form.contactInfo
    }

    const result = await updateUserInfo(userProfile.value.id, updatedData)

    // 更新本地数据
    userProfile.value = result

    ElMessage.success('个人信息更新成功')
    dialogVisible.value = false
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '更新个人信息失败')
    console.error('更新个人信息失败:', err)
  } finally {
    submitting.value = false
  }
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
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取角色显示名称
const getRoleDisplayName = (role: UserRole) => {
  const roleMap: Record<string, string> = {
    'ADMIN': '管理员',
    'CURATOR': '策展人',
    'EXPERT': '专家',
    'ANNOTATOR': '标注员',
    'REFEREE': '审核员',
    'CROWDSOURCE_USER': '众包用户'
  }
  return roleMap[role] || '未知角色'
}

// 获取角色标签类型
const getRoleTagType = (role: UserRole) => {
  const tagMap: Record<string, string> = {
    'ADMIN': 'danger',
    'CURATOR': 'warning',
    'EXPERT': 'success',
    'ANNOTATOR': 'info',
    'REFEREE': 'warning',
    'CROWDSOURCE_USER': ''
  }
  return tagMap[role] || ''
}

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserProfile()
})
</script>

<style scoped>
.profile-view {
  padding: 80px 0 30px;
  min-height: calc(100vh - var(--navbar-height));
  background-color: var(--background-color);
}

.container {
  max-width: var(--container-width);
  margin: 0 auto;
  padding: 0 var(--container-padding);
}

.profile-header {
  text-align: center;
  margin-bottom: 40px;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 16px;
  color: var(--primary-color);
}

.subtitle {
  font-size: 1.2rem;
  color: var(--light-text);
}

.profile-container {
  background-color: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  min-height: 600px;
  padding: 30px;
}

.profile-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.mt-20 {
  margin-top: 20px;
}

.deactivate-content {
  padding: 10px 0;
}

.deactivate-list {
  margin: 15px 0;
  padding-left: 20px;
  line-height: 1.8;
}

.confirm-text {
  margin: 15px 0;
  font-weight: bold;
}
</style>
