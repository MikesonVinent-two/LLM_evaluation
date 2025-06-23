<script setup lang="ts">
import { useRouter } from 'vue-router'
import RegisterForm from '@/components/RegisterForm.vue'

const router = useRouter()

const handleRegisterSuccess = () => {
  router.push('/login') // 注册成功后跳转到登录页
}

const navigateToLogin = () => {
  router.push('/login') // 跳转到登录页面
}
</script>

<template>
  <div class="register-container">
    <div class="register-content">
      <div class="register-header">
        <img src="@/assets/logo.svg" alt="Logo" class="logo" />
        <h2>创建新账号</h2>
        <p class="subtitle">加入我们，开始智能对话之旅</p>
      </div>
      <RegisterForm
        @register-success="handleRegisterSuccess"
        @navigate-to-login="navigateToLogin"
      />
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  padding: 40px 20px;
  box-sizing: border-box;
}

.register-content {
  width: 100%;
  max-width: 500px;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.register-header h2 {
  margin: 0 0 8px;
  color: #1f1f1f;
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 16px;
}

@media (max-width: 480px) {
  .register-content {
    max-width: 100%;
  }

  .register-header h2 {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }
}
</style>
