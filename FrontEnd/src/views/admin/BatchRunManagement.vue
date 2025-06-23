<template>
  <div class="batch-run-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>批次运行管理</h2>
        </div>
      </template>
      <div class="management-content">
        <p>正在施工中...</p>
        <p>此页面将用于管理批次运行，包括创建批次、执行批次运行等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加批次运行管理相关逻辑
</script>

<style scoped>
.batch-run-management {
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
