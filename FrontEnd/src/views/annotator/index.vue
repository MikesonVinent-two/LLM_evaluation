<template>
  <div class="annotator-workspace">
    <el-container>
      <el-main class="workspace-content">
        <div class="workspace-header">
          <div class="header-title">
            {{ currentPageTitle }}
          </div>
          <div class="header-user">
            <el-dropdown trigger="click">
              <span class="el-dropdown-link">
                {{ userInfo.name }}<i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人资料</el-dropdown-item>
                  <el-dropdown-item>修改密码</el-dropdown-item>
                  <el-dropdown-item>消息通知</el-dropdown-item>
                  <el-dropdown-item divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <router-view></router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 用户信息
const userInfo = ref({
  id: 1,
  name: '标注员',
  role: 'ANNOTATOR'
})

// 当前页面标题
const currentPageTitle = computed(() => {
  // 根据路由路径获取当前页面标题
  const pathMap = {
    '/annotator/dashboard': '数据统计',
    '/annotator/tasks': '任务管理',

    '/annotator/prompt-management': '提示词管理',
    '/annotator/prompt-assembly': '提示词组装',
    '/annotator/prompt-testing': '提示词测试',

    '/annotator/question-standardization': '问题标准化',
    '/annotator/question-history': '版本历史',
    '/annotator/question-batch': '批量处理',

    '/annotator/standard-answers': '回答管理',
    '/annotator/answer-review': '回答评审',
    '/annotator/answer-history': '回答版本',

    '/annotator/crowdsource-tasks': '众包任务',
    '/annotator/crowdsource-review': '众包审核',
    '/annotator/crowdsource-stats': '众包统计',

    '/annotator/config': '配置管理',
    '/annotator/tag-management': '标签管理',
    '/annotator/logs': '操作日志',
  }

  return pathMap[route.path] || '标注工作台'
})
</script>

<style scoped>
.annotator-workspace {
  height: 100vh;
  width: 100%;
}

.workspace-header {
  background-color: #fff;
  color: #333;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.header-user {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409eff;
  display: flex;
  align-items: center;
}

.workspace-content {
  padding: 0;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
  overflow-y: auto;
}
</style>
