<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { User, Lock, Message, Star, Setting, Collection, EditPen, Check } from '@element-plus/icons-vue'
import type { FormInstance, FormItemRule } from 'element-plus'
import { UserRole } from '@/api/user'
import type { RegisterData } from '@/api/user'

// 定义emit事件
const emit = defineEmits(['register-success', 'navigate-to-login'])

const userStore = useUserStore()

// 表单引用
const registerFormRef = ref<FormInstance>()

// 注册表单数据
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  contactInfo: '',
  role: UserRole.CROWDSOURCE_USER,
})

// 验证密码是否一致
const validateConfirmPassword = (
  _rule: FormItemRule,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度应在4-50个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应在6-100个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  contactInfo: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 状态
const isLoading = ref(false)
const error = ref<string | null>(null)

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    // 表单验证
    await registerFormRef.value.validate()

    isLoading.value = true
    error.value = null

    // 构建注册数据
    const registerData: RegisterData = {
      username: registerForm.value.username,
      password: registerForm.value.password,
      contactInfo: registerForm.value.contactInfo,
      role: registerForm.value.role,
    }

    try {
      // 调用API直接注册
      await userStore.register(registerData)
      // 注册成功，通知父组件
      emit('register-success')
    } catch (err) {
      if (err instanceof Error) {
        error.value = err.message
      } else {
        error.value = '注册失败，请重试'
      }
    }
  } catch (err: Error | unknown) {
    if (err instanceof Error) {
      error.value = err.message
    } else {
      error.value = '表单验证失败'
    }
  } finally {
    isLoading.value = false
  }
}

// 跳转到登录页面
const goToLogin = () => {
  emit('navigate-to-login')
}
</script>

<template>
  <el-card class="register-card" shadow="never">
    <el-form
      ref="registerFormRef"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
      class="register-form-component"
      @submit.prevent="handleRegister"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="registerForm.username"
          placeholder="请输入用户名(4-50个字符)"
          :prefix-icon="User"
          :disabled="isLoading"
      />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="registerForm.password"
        type="password"
          placeholder="请输入密码(6-100个字符)"
          show-password
          :prefix-icon="Lock"
          :disabled="isLoading"
      />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
        type="password"
        placeholder="请再次输入密码"
          show-password
          :prefix-icon="Lock"
          :disabled="isLoading"
      />
      </el-form-item>

      <el-form-item label="联系方式" prop="contactInfo">
        <el-input
          v-model="registerForm.contactInfo"
          placeholder="请输入邮箱或其他联系方式"
          :prefix-icon="Message"
          :disabled="isLoading"
        />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-select
          v-model="registerForm.role"
          placeholder="请选择角色"
          class="role-select"
          :disabled="isLoading"
        >
          <el-option label="众包用户" value="CROWDSOURCE_USER">
            <div class="option-content">
              <el-icon><User /></el-icon>
              <span>众包用户</span>
            </div>
          </el-option>
          <el-option label="专家" value="EXPERT">
            <div class="option-content">
              <el-icon><Star /></el-icon>
              <span>专家</span>
            </div>
          </el-option>
          <el-option label="策展人" value="CURATOR">
            <div class="option-content">
              <el-icon><Collection /></el-icon>
              <span>策展人</span>
            </div>
          </el-option>
          <el-option label="标注员" value="ANNOTATOR">
            <div class="option-content">
              <el-icon><EditPen /></el-icon>
              <span>标注员</span>
            </div>
          </el-option>
          <el-option label="审核员" value="REFEREE">
            <div class="option-content">
              <el-icon><Check /></el-icon>
              <span>审核员</span>
            </div>
          </el-option>
          <el-option label="管理员" value="ADMIN">
            <div class="option-content">
              <el-icon><Setting /></el-icon>
              <span>管理员</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <div class="form-footer">
        <el-button
          type="primary"
          native-type="submit"
          :loading="isLoading"
          class="submit-button"
        >
          {{ isLoading ? '注册中...' : '注册' }}
        </el-button>

        <div class="additional-links">
          <el-link type="primary" underline="never" @click="goToLogin">
            已有账号？立即登录
          </el-link>
    </div>
  </div>
    </el-form>

    <!-- 错误提示 -->
    <el-alert v-if="error" :title="error" type="error" show-icon closable class="error-alert" />
  </el-card>
</template>

<style scoped>
.register-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.register-form-component {
  padding: 24px 0 0;
}

:deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #a3a6ad inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset !important;
}

.role-select {
  width: 100%;
}

:deep(.el-select-dropdown__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-select-dropdown__item .el-icon) {
  margin-right: 4px;
  font-size: 16px;
}

.form-footer {
  margin-top: 32px;
}

.submit-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #1890ff 0%, #1d39c4 100%);
  border: none;
  margin-bottom: 16px;
}

.submit-button:hover {
  opacity: 0.9;
}

.additional-links {
  text-align: center;
}

:deep(.el-link) {
  font-size: 14px;
}

.error-alert {
  margin-top: 16px;
}

.option-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
