<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  padding: 20px;
}

.content {
  text-align: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(8px);
}

h1 {
  font-size: 6rem;
  margin: 0;
  color: var(--primary-color);
  line-height: 1;
}

h2 {
  font-size: 2rem;
  margin: 20px 0;
  color: var(--text-color);
}

p {
  font-size: 1.2rem;
  color: var(--light-text);
  margin-bottom: 30px;
}

.el-button {
  padding: 12px 24px;
  font-size: 1.1rem;
}
</style>
