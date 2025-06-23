<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const emit = defineEmits(['login-success', 'navigate-to-register'])

const userStore = useUserStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录表单数据
const loginForm = ref({
  username: '',
  password: '',
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度应在4-50个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应在6-100个字符之间', trigger: 'blur' },
  ],
}

// 状态
const isLoading = ref(false)
const error = ref<string | null>(null)

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    // 表单验证
    await loginFormRef.value.validate()

    isLoading.value = true
    error.value = null

    // 调用登录接口
    const success = await userStore.loginUser({
      username: loginForm.value.username,
      password: loginForm.value.password,
    })

    if (success && userStore.currentUser) {
      // 显式地将用户信息写入内存和localStorage
      const userInfo = userStore.currentUser

      // 确保用户数据保存在'user'键中，与路由守卫检查一致
      window.userInfo = userInfo  // 写入全局内存
      localStorage.setItem('user', JSON.stringify(userInfo))  // 主要存储位置

      // 兼容其他地方的读取
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo))

      // 打印日志确认写入成功
      console.log('✅ 用户信息已写入：', {
        memory: window.userInfo,
        localStorage_user: JSON.parse(localStorage.getItem('user') || '{}'),
        localStorage_userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
        sessionStorage: JSON.parse(sessionStorage.getItem('userInfo') || '{}')
      })

      // 同步到所有组件：以下四个事件触发确保不同组件能及时更新状态

      // 1. 触发登录事件，确保其他组件能够响应
      window.dispatchEvent(new CustomEvent('user-login', {
        detail: userInfo
      }))

      // 2. 手动触发存储事件，确保监听storage的组件能够监听到
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'user',
        oldValue: null,
        newValue: JSON.stringify(userInfo)
      }))

      // 3. 触发身份验证状态变化事件
      window.dispatchEvent(new CustomEvent('auth-state-change'))

      // 4. 设置一个短时间延迟，确保所有组件有足够时间更新状态
      setTimeout(() => {
        // 登录成功，通知父组件
        emit('login-success')

        // 再次触发状态更新事件，确保所有组件都能响应
        window.dispatchEvent(new CustomEvent('auth-state-change'))
      }, 100)
    }
  } catch (err: unknown) {
    const errorMessage = err instanceof Error ? err.message : '登录失败，请重试'
    error.value = errorMessage
  } finally {
    isLoading.value = false
  }
}

// 在组件卸载时清理内存
onUnmounted(() => {
  // 仅在登出时才清理，避免影响其他组件
  if (!userStore.isLoggedIn) {
    window.userInfo = undefined
  }
})

const goToRegister = () => {
  emit('navigate-to-register')
}
</script>

<template>
  <el-card class="login-card" shadow="never">
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      class="login-form-component"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="用户名" prop="username" class="form-item">
        <el-input
          v-model="loginForm.username"
        placeholder="请输入用户名"
          :prefix-icon="User"
          :disabled="isLoading"
          class="form-input"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password" class="form-item">
        <el-input
          v-model="loginForm.password"
        type="password"
        placeholder="请输入密码"
          show-password
          :prefix-icon="Lock"
          :disabled="isLoading"
          class="form-input"
      />
      </el-form-item>

      <div class="form-footer">
        <el-button
          type="primary"
          native-type="submit"
          :loading="isLoading"
          class="submit-button"
        >
          {{ isLoading ? '登录中...' : '登录' }}
        </el-button>

        <div class="additional-links">
          <el-link type="primary" underline="never" @click="goToRegister">
            还没有账号？立即注册
          </el-link>
    </div>
  </div>
    </el-form>

    <!-- 错误提示 -->
    <el-alert v-if="error" :title="error" type="error" show-icon closable class="error-alert" />
  </el-card>
</template>

<style scoped>
.login-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 30px 20px;
  width: 100%;
}

.login-form-component {
  padding: 20px 30px;
  max-width: 600px;
  margin: 0 auto;
}

.form-item {
  margin-bottom: 25px;
}

:deep(.el-form-item__label) {
  padding-bottom: 10px;
  font-weight: 500;
  font-size: 17px;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  padding: 0 15px;
  height: 52px;
}

:deep(.el-input__inner) {
  font-size: 16px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #a3a6ad inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset !important;
}

.form-footer {
  margin-top: 45px;
}

.submit-button {
  width: 100%;
  height: 52px;
  font-size: 18px;
  font-weight: 500;
  background: linear-gradient(135deg, #1890ff 0%, #1d39c4 100%);
  border: none;
  margin-bottom: 30px;
  letter-spacing: 2px;
  border-radius: 8px;
}

.submit-button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(29, 57, 196, 0.2);
}

.additional-links {
  text-align: center;
  margin-bottom: 15px;
}

:deep(.el-link) {
  font-size: 16px;
}

.error-alert {
  margin-top: 25px;
}
</style>
