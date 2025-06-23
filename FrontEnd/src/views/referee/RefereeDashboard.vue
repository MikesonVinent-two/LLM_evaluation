<template>
  <div class="referee-dashboard">
    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <h2>审核员工作台</h2>
        </div>
      </template>
      <div class="dashboard-content">
        <p>正在施工中...</p>
        <p>这里将显示审核员的工作概览和审核任务指标。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加审核员仪表盘相关逻辑
</script>

<style scoped>
.referee-dashboard {
  padding: 20px;
}

.dashboard-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-content {
  padding: 20px 0;
}
</style>
