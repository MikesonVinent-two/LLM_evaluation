<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import type { UserInfo, LoginResponse } from '@/api/user'
import { UserRole } from '@/api/user'

const props = defineProps<{
  userId?: string | number
}>()

const userStore = useUserStore()
const userInfo = ref<UserInfo | LoginResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// 类型守卫函数
const isUserInfo = (user: UserInfo | LoginResponse | null): user is UserInfo => {
  return user !== null && 'createdAt' in user
}
// 如果提供了userId，就加载该用户的信息，否则使用当前登录用户
const fetchData = async () => {
  loading.value = true
  error.value = null

  try {
    if (props.userId) {
      userInfo.value = await userStore.getUserInfoById(props.userId)
    } else if (userStore.currentUser) {
      userInfo.value = userStore.currentUser
    } else {
      // 尝试获取当前登录用户
      await userStore.fetchUserInfo()
      userInfo.value = userStore.currentUser
    }
  } catch (err) {
    error.value = '获取用户信息失败'
    console.error('获取用户信息失败', err)
  } finally {
    loading.value = false
  }
}

// 格式化角色名称
const roleName = computed(() => {
  if (!userInfo.value) return '未知角色'

  switch (userInfo.value.role) {
    case UserRole.ADMIN:
      return '管理员'
    case UserRole.CURATOR:
      return '策展人'
    case UserRole.EXPERT:
      return '专家'
    case UserRole.ANNOTATOR:
      return '标注员'
    case UserRole.REFEREE:
      return '审核员'
    case UserRole.CROWDSOURCE_USER:
      return '众包用户'
    default:
      return userInfo.value.role || '未知角色'
  }
})

// 格式化创建时间
const formattedCreatedAt = computed(() => {
  if (!userInfo.value) return '未知'
  // 使用类型守卫判断是否有 createdAt 属性
  if (isUserInfo(userInfo.value)) {
    return new Date(userInfo.value.createdAt).toLocaleString()
  }
  return '未知'
})

// 计算用户邮箱
const userEmail = computed(() => {
  if (!userInfo.value) return '未设置'
  // 使用类型守卫判断是否有 email 属性
  if (isUserInfo(userInfo.value) && userInfo.value.email) {
    return userInfo.value.email
  }
  // 如果是登录响应，使用联系方式代替
  if (userInfo.value.contactInfo) {
    return userInfo.value.contactInfo
  }
  return '未设置'
})

onMounted(fetchData)
</script>

<template>
  <el-card class="user-profile-card" shadow="hover" v-loading="loading">
    <template #header>
      <div class="card-header">
        <h3>用户信息</h3>
      </div>
    </template>

    <div v-if="error" class="error-message">
      <el-alert :title="error" type="error" show-icon />
    </div>

    <div v-else-if="userInfo" class="user-info">
      <div class="user-avatar">
        <el-avatar :size="80">
          {{ userInfo.username?.charAt(0).toUpperCase() }}
        </el-avatar>
      </div>

      <div class="user-details">
        <div class="info-item">
          <span class="label">用户名：</span>
          <span class="value">{{ userInfo.username }}</span>
        </div>

        <div class="info-item">
          <span class="label">邮箱：</span>
          <span class="value">{{ userEmail }}</span>
        </div>

        <div class="info-item">
          <span class="label">角色：</span>
          <span class="value">{{ roleName }}</span>
        </div>

        <div class="info-item">
          <span class="label">注册时间：</span>
          <span class="value">{{ formattedCreatedAt }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      未找到用户信息
    </div>
  </el-card>
</template>

<style scoped>
.user-profile-card {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-avatar {
  margin-bottom: 20px;
}

.user-details {
  width: 100%;
}

.info-item {
  margin-bottom: 12px;
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  width: 100px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.value {
  flex: 1;
  color: var(--el-text-color-primary);
}

.error-message {
  margin: 20px 0;
}

.empty-state {
  text-align: center;
  padding: 30px 0;
  color: var(--el-text-color-secondary);
  font-style: italic;
}

@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
  }

  .label {
    width: 100%;
    margin-bottom: 4px;
  }
}
</style>
