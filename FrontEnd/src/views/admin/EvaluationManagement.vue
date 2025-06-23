<template>
  <div class="evaluation-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>评测管理</h2>
        </div>
      </template>
      <div class="management-content">
        <p>正在施工中...</p>
        <p>此页面将用于管理评测任务，包括执行评测、查看评测进度和结果等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加评测管理相关逻辑
</script>

<style scoped>
.evaluation-management {
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
</style>
