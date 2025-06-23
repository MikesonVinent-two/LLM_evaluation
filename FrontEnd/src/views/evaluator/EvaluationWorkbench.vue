<template>
  <div class="evaluation-workbench">
    <!-- 左侧导航菜单 -->
    <el-aside width="200px" class="workbench-aside">
      <el-menu
        :default-active="activeMenu"
        class="workbench-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/evaluation/batch-evaluation">
          <el-icon><DataAnalysis /></el-icon>
          <span>批次评测</span>
        </el-menu-item>
        <el-menu-item index="/evaluation/evaluations">
          <el-icon><Management /></el-icon>
          <span>评测管理</span>
        </el-menu-item>
        <el-menu-item index="/evaluation/scoring">
          <el-icon><StarFilled /></el-icon>
          <span>评分管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主要内容区域 -->
    <el-main class="workbench-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis,
  Management,
  StarFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 当前激活的菜单项
const activeMenu = computed(() => route.path)

// 处理菜单选择
const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.evaluation-workbench {
  display: flex;
  height: 100%;
  background-color: #f5f7fa;
}

.workbench-aside {
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  height: 100%;
}

.workbench-menu {
  border-right: none;
}

.workbench-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
