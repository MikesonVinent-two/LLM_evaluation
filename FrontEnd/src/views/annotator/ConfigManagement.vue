<template>
  <div class="config-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>配置管理</h2>
        </div>
      </template>
      <div class="management-content">
        <p>正在施工中...</p>
        <p>此页面将用于管理组装配置，包括创建、编辑、删除配置等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加配置管理相关逻辑
</script>

<style scoped>
.config-management {
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
