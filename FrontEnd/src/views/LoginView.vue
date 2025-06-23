<script setup lang="ts">
import { useRouter } from 'vue-router'
import LoginForm from '@/components/LoginForm.vue' // 导入新的表单组件

const router = useRouter()

const handleLoginSuccess = () => {
  console.log('登录成功，正在跳转到首页...')
  // 使用replace而不是push，防止用户按回退按钮时返回登录页
  router.replace('/')

  // 如果路由跳转失败，可以考虑直接刷新页面
  setTimeout(() => {
    if (window.location.pathname.includes('/login')) {
      console.log('检测到仍在登录页，强制跳转到首页')
      window.location.href = '/'
    }
  }, 500)
}

const navigateToRegister = () => {
  router.push('/register') // 跳转到注册页面
}
</script>

<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-header">
        <img src="@/assets/logo.svg" alt="Logo" class="logo" />
        <h2>欢迎使用LLM评测系统</h2>
        <p class="subtitle">登录您的账号以开始对话</p>
      </div>
      <LoginForm @login-success="handleLoginSuccess" @navigate-to-register="navigateToRegister" />
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh; /* 使用100vh而不是减去导航栏高度，因为main-content已经有padding-top */
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  padding: 80px 20px 40px;
  box-sizing: border-box;
}

.login-content {
  width: 100%;
  max-width: 800px; /* 将最大宽度从600px增加到800px */
  padding: 40px 50px; /* 增加内边距 */
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(8px);
}

.login-header {
  text-align: center;
  margin-bottom: 36px; /* 增加下边距 */
}

.logo {
  width: 80px; /* 稍微增大logo */
  height: 80px;
  margin-bottom: 24px;
}

.login-header h2 {
  margin: 0 0 16px;
  color: #1f1f1f;
  font-size: 32px; /* 增大字体 */
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 18px; /* 增大字体 */
}

/* LoginForm.vue 将处理卡片样式，这里可以移除或调整 login-card 相关样式 */

@media (max-width: 900px) {
  .login-content {
    max-width: 85%;
    padding: 35px 45px;
}
}

@media (max-width: 768px) {
  .login-content {
    max-width: 90%;
    padding: 30px 35px;
}

  .login-header h2 {
    font-size: 28px;
}

  .subtitle {
  font-size: 16px;
  }
}

@media (max-width: 480px) {
  .login-content {
    max-width: 100%;
    padding: 20px 15px;
  }

  .login-header h2 {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }

  .logo {
    width: 64px;
    height: 64px;
    margin-bottom: 16px;
  }
}
</style>
