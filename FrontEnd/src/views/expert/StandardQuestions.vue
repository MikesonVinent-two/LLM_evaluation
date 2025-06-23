<template>
  <div class="standard-questions">
    <el-card class="questions-card">
      <template #header>
        <div class="card-header">
          <h2>标准问题</h2>
        </div>
      </template>
      <div class="questions-content">
        <p>正在施工中...</p>
        <p>此页面将显示标准化后的问题列表，供专家查看和回答。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加标准问题相关逻辑
</script>

<style scoped>
.standard-questions {
  padding: 20px;
}

.questions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.questions-content {
  padding: 20px 0;
}
</style>
