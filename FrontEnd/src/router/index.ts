import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, RouteLocationNormalized } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'

// 定义路由元数据类型
interface RouteMeta {
  requiresAuth: boolean;
  title: string;
  roles: string[];
  requiresEvaluator?: boolean;
}

// 扩展 vue-router 模块，使用正确的声明合并方式
declare module 'vue-router' {
  // 扩展现有接口而不是重新定义
  interface RouteMeta {
    requiresAuth: boolean;
    title: string;
    roles: string[];
    requiresEvaluator?: boolean;
  }
}

// 路由配置
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: {
      requiresAuth: true,
      title: '首页',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      requiresAuth: false,
      title: '登录',
      roles: []
    }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: {
      requiresAuth: false,
      title: '注册',
      roles: []
    }
  },
  // 聊天和历史记录页面 - 所有用户可访问
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue'),
    meta: {
      requiresAuth: true,
      title: 'AI对话',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },

  // WebSocket演示页面
  {
    path: '/websocket-demo',
    name: 'websocket-demo',
    component: () => import('../components/WebSocketDemo.vue'),
    meta: {
      requiresAuth: true,
      title: 'WebSocket演示',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },

  // WebSocket测试页面
  {
    path: '/websocket-test',
    name: 'websocket-test',
    component: () => import('../components/WebSocketTestPage.vue'),
    meta: {
      requiresAuth: true,
      title: 'WebSocket消息测试',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },

  // 数据管理功能
  {
    path: '/data/original-questions',
    name: 'original-questions',
    component: () => import('../views/curator/QuestionManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '原始问题管理',
      roles: ['CURATOR', 'ADMIN']
    }
  },
  {
    path: '/data/batch-import',
    name: 'batch-import',
    component: () => import('../views/curator/BatchImport.vue'),
    meta: {
      requiresAuth: true,
      title: '批量数据导入',
      roles: ['CURATOR', 'ADMIN']
    }
  },
  {
    path: '/data/datasets',
    name: 'datasets',
    component: () => import('../views/admin/DatasetManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '数据集管理',
      roles: ['ADMIN']
    }
  },

  // 标准化工作台
  {
    path: '/standardization/question-standardization',
    name: 'question-standardization',
    component: () => import('../views/annotator/QuestionStandardization.vue'),
    meta: {
      requiresAuth: true,
      title: '问题标准化',
      roles: ['ANNOTATOR', 'EXPERT', 'ADMIN']
    }
  },
  {
    path: '/standardization/question-history',
    name: 'question-history',
    component: () => import('../views/annotator/QuestionHistory.vue'),
    meta: {
      requiresAuth: true,
      title: '问题版本历史',
      roles: ['ANNOTATOR', 'EXPERT', 'ADMIN']
    }
  },
  {
    path: '/standardization/standard-answers',
    name: 'standard-answers',
    component: () => import('../views/annotator/StandardAnswers.vue'),
    meta: {
      requiresAuth: true,
      title: '标准回答管理',
      roles: ['ANNOTATOR', 'EXPERT', 'ADMIN']
    }
  },

  // Prompt工作台
  {
    path: '/prompt/answer-type-prompt',
    name: 'answer-type-prompt',
    component: () => import('../views/prompt/AnswerTypePrompt.vue'),
    meta: {
      requiresAuth: true,
      title: '回答阶段-题型提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },
  {
    path: '/prompt/answer-tag-prompt',
    name: 'answer-tag-prompt',
    component: () => import('../views/prompt/AnswerTagPrompt.vue'),
    meta: {
      requiresAuth: true,
      title: '回答阶段-标签提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },
  {
    path: '/prompt/answer-assembly-prompt',
    name: 'answer-assembly-prompt',
    component: () => import('../views/prompt/AnswerAssemblyPrompt.vue'),
    meta: {
      requiresAuth: true,
      title: '回答阶段-组装提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },
  {
    path: '/prompt/evaluation-subjective-prompt',
    name: 'evaluation-subjective-prompt',
    component: () => import('../views/prompt/EvaluationSubjectivePrompt.vue'),
    meta: {
      requiresAuth: true,
      title: '评测阶段-简答题提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },
  {
    path: '/prompt/evaluation-tag-prompt',
    name: 'evaluation-tag-prompt',
    component: () => import('../views/prompt/EvaluationTagPrompt.vue'),
    meta: {
      requiresAuth: true,
      title: '评测阶段-标签提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },
  {
    path: '/prompt/evaluation-assembly-prompt',
    name: 'evaluation-assembly-prompt',
    component: () => import('../views/prompt/EvaluationPromptAssembly.vue'),
    meta: {
      requiresAuth: true,
      title: '评测阶段-组装提示词',
      roles: ['ANNOTATOR', 'ADMIN', 'EXPERT']
    }
  },

  // 评测工作台

  {
    path: '/assessment/evaluations',
    name: 'Evaluations',
    component: () => import('../views/assessment/Evaluations.vue'),
    meta: {
      requiresAuth: true,
      title: '评测管理',
      roles: ['ADMIN','EXPERT','ANNOTATOR','REFEREE','CROWDSOURCE_USER','CURATOR'],
      requiresEvaluator: true
    }
  },
  {
    path: '/assessment/objective-evaluation/:batchId',
    name: 'ObjectiveEvaluation',
    component: () => import('../views/assessment/ObjectiveEvaluationPage.vue'),
    meta: {
      requiresAuth: true,
      title: '客观题评测',
      roles: ['ADMIN','EXPERT','ANNOTATOR','REFEREE','CROWDSOURCE_USER','CURATOR'],
      requiresEvaluator: true
    }
  },
  {
    path: '/assessment/subjective-llm-evaluation/:batchId',
    name: 'SubjectiveLlmEvaluation',
    component: () => import('../views/assessment/SubjectiveLlmEvaluationPage.vue'),
    meta: {
      requiresAuth: true,
      title: '主观题大模型评测',
      roles: ['ADMIN','EXPERT','ANNOTATOR','REFEREE','CROWDSOURCE_USER','CURATOR'],
      requiresEvaluator: true
    }
  },
  {
    path: '/assessment/subjective-human-evaluation/:batchId',
    name: 'SubjectiveHumanEvaluation',
    component: () => import('../views/assessment/SubjectiveHumanEvaluationPage.vue'),
    meta: {
      requiresAuth: true,
      title: '主观题人工评测',
      roles: ['ADMIN','EXPERT','ANNOTATOR','REFEREE','CROWDSOURCE_USER','CURATOR'],
      requiresEvaluator: true
    }
  },
  {
    path: '/assessment/scoring',
    name: 'scoring',
    component: () => import('../views/assessment/Scoring.vue'),
    meta: {
      requiresAuth: true,
      title: '评分管理',
      roles: ['ADMIN','EXPERT','ANNOTATOR','REFEREE','CROWDSOURCE_USER','CURATOR'],
      requiresEvaluator: true
    }
  },


  // 评审员工作台
  {
    path: '/referee/expert-answer-review',
    name: 'expert-answer-review',
    component: () => import('../views/referee/ExpertAnswerReview.vue'),
    meta: {
      requiresAuth: true,
      title: '专家回答评分',
      roles: ['REFEREE', 'ADMIN']
    }
  },
  {
    path: '/referee/crowdsourced-answer-review',
    name: 'crowdsourced-answer-review',
    component: () => import('../views/referee/CrowdsourcedAnswerReview.vue'),
    meta: {
      requiresAuth: true,
      title: '众包回答审核',
      roles: ['REFEREE', 'ADMIN']
    }
  },

  // 众包管理
  {
    path: '/crowdsource/crowdsource-tasks',
    name: 'crowdsource-tasks',
    component: () => import('../views/crowdsource/CrowdsourceTasks.vue'),
    meta: {
      requiresAuth: true,
      title: '众包任务',
      roles: ['CROWDSOURCE_USER', 'ANNOTATOR', 'ADMIN']
    }
  },
  {
    path: '/crowdsource/crowdsource-review',
    name: 'crowdsource-review',
    component: () => import('../views/referee/CrowdsourceReview.vue'),
    meta: {
      requiresAuth: true,
      title: '众包审核',
      roles: ['REFEREE', 'ANNOTATOR', 'ADMIN']
    }
  },
  {
    path: '/crowdsource/crowdsource-stats',
    name: 'crowdsource-stats',
    component: () => import('../views/crowdsource/CrowdsourceDashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '众包统计',
      roles: ['ADMIN', 'ANNOTATOR']
    }
  },

  // 众包任务台
  {
    path: '/crowdsource/crowdsource-workbench',
    name: 'crowdsource-workbench',
    component: () => import('../views/crowdsource/CrowdsourceWorkbench.vue'),
    meta: {
      requiresAuth: true,
      title: '众包任务台',
      roles: ['ANNOTATOR', 'ADMIN', 'CROWDSOURCE_USER', 'REFEREE']
    }
  },
  {
    path: '/crowdsource/crowdsource-dashboard',
    name: 'crowdsource-dashboard',
    component: () => import('../views/crowdsource/CrowdsourceDashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '众包仪表盘',
      roles: ['ANNOTATOR', 'ADMIN', 'CROWDSOURCE_USER', 'REFEREE']
    }
  },
  {
    path: '/crowdsource/standard-questions',
    name: 'crowdsource-questions',
    component: () => import('../views/crowdsource/StandardQuestions.vue'),
    meta: {
      requiresAuth: true,
      title: '标准问题回答',
      roles: ['ANNOTATOR', 'ADMIN', 'CROWDSOURCE_USER', 'REFEREE']
    }
  },

  // 系统管理
  {
    path: '/system/users',
    name: 'user-management',
    component: () => import('../views/admin/UserManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '用户管理',
      roles: ['ADMIN']
    }
  },
  {
    path: '/system/models',
    name: 'model-management',
    component: () => import('../views/admin/ModelManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '模型管理',
      roles: ['ADMIN']
    }
  },
  {
    path: '/system/evaluators',
    name: 'evaluators',
    component: () => import('../views/admin/EvaluatorManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '评测员管理',
      roles: ['ADMIN']
    }
  },
  {
    path: '/system/evaluation-criteria',
    name: 'evaluation-criteria',
    component: () => import('../views/admin/EvaluationCriteriaManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '评测标准管理',
      roles: ['ADMIN']
    }
  },
  {
    path: '/system/config',
    name: 'config',
    component: () => import('../views/annotator/ConfigManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '配置管理',
      roles: ['ANNOTATOR', 'ADMIN']
    }
  },
  {
    path: '/system/tags',
    name: 'tags',
    component: () => import('../views/annotator/TagManagement.vue'),
    meta: {
      requiresAuth: true,
      title: '标签管理',
      roles: ['ANNOTATOR', 'ADMIN']
    }
  },


  // 用户中心
  {
    path: '/user/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: {
      requiresAuth: true,
      title: '个人信息',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },
  {
    path: '/user/settings',
    name: 'settings',
    component: () => import('../views/SettingsView.vue'),
    meta: {
      requiresAuth: true,
      title: '设置',
      roles: ['ADMIN', 'CURATOR', 'EXPERT', 'ANNOTATOR', 'REFEREE', 'CROWDSOURCE_USER']
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: {
      requiresAuth: false,
      title: '404',
      roles: []
    }
  },

  // 专家工作台路由
  {
    path: '/expert/expert-dashboard',
    name: 'expert-dashboard',
    component: () => import('../views/expert/ExpertDashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '专家仪表盘',
      roles: ['EXPERT', 'ADMIN', 'REFEREE']
    }
  },
  {
    path: '/expert/expert-questions',
    name: 'expert-questions',
    component: () => import('../views/expert/ExpertQuestions.vue'),
    meta: {
      requiresAuth: true,
      title: '专家问题回答',
      roles: ['EXPERT', 'ADMIN']
    }
  },
  {
    path: '/expert/expert-history',
    name: 'expert-history',
    component: () => import('../views/expert/ExpertHistory.vue'),
    meta: {
      requiresAuth: true,
      title: '历史回答记录',
      roles: ['EXPERT', 'ADMIN']
    }
  },

  // 数据集工作台路由
  {
    path: '/dataset/list',
    name: 'dataset-management',
    component: () => import('../views/dataset/DatasetList.vue'),
    meta: {
      requiresAuth: true,
      title: '数据集列表',
      roles: ['ADMIN', 'CURATOR']
    }
  },
  {
    path: '/dataset/create',
    name: 'create-dataset',
    component: () => import('../views/dataset/CreateDataset.vue'),
    meta: {
      requiresAuth: true,
      title: '创建数据集',
      roles: ['ADMIN', 'CURATOR']
    }
  },
  {
    path: '/dataset/edit/:id',
    name: 'edit-dataset',
    component: () => import('../views/dataset/EditDataset.vue'),
    meta: {
      requiresAuth: true,
      title: '编辑数据集',
      roles: ['ADMIN', 'CURATOR']
    }
  },

  // 运行时工作台路由
  {
    path: '/admin/batch-monitor',
    name: 'batch-monitor',
    component: () => import('../views/admin/BatchMonitorView.vue'),
    meta: {
      requiresAuth: true,
      title: '批次实时监控',
      roles: ['ADMIN', 'ANNOTATOR']
    }
  },
  {
    path: '/runtime/answer-generation-batches',
    name: 'answer-generation-batches',
    component: () => import('../views/runtime/AnswerGenerationBatchesView.vue'),
    meta: {
      requiresAuth: true,
      title: '回答生成批次',
      roles: ['ADMIN', 'ANNOTATOR']
    }
  },
  {
    path: '/runtime/answer-batch-dashboard',
    name: 'answer-batch-dashboard',
    component: () => import('../views/runtime/AnswerBatchDashboardView.vue'),
    meta: {
      requiresAuth: true,
      title: '回答批次仪表盘',
      roles: ['ADMIN', 'ANNOTATOR']
    }
  },
  {
    path: '/runtime/create-answer-batch',
    name: 'create-answer-batch',
    component: () => import('../views/runtime/CreateAnswerBatchView.vue'),
    meta: {
      requiresAuth: true,
      title: '创建回答批次',
      roles: ['ADMIN', 'ANNOTATOR']
    }
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 调试工具：记录路由变化
const logRouteChange = (to: RouteLocationNormalized, from: RouteLocationNormalized) => {
  const timestamp = new Date().toLocaleTimeString()

  console.group(`🚀 路由变化 - ${timestamp}`)
  console.log('从:', {
    name: from.name,
    path: from.path,
    params: from.params,
    query: from.query,
    meta: from.meta
  })
  console.log('到:', {
    name: to.name,
    path: to.path,
    params: to.params,
    query: to.query,
    meta: to.meta
  })
  console.groupEnd()
}

// 调试工具：记录认证状态
const logAuthStatus = (isAuthenticated: boolean, requiresAuth: boolean, path: string) => {
  console.group('🔐 认证状态检查')
  console.log({
    路径: path,
    是否已登录: isAuthenticated,
    需要认证: requiresAuth,
    用户数据存在: !!localStorage.getItem('user')
  })
  console.groupEnd()
}

// 调试工具：记录页面性能
const logPagePerformance = (to: RouteLocationNormalized) => {
  // 使用现代Performance API
  if (window.performance && performance.getEntriesByType) {
    const pageEntries = performance.getEntriesByType('navigation')
    if (pageEntries.length > 0) {
      const navigationEntry = pageEntries[0] as PerformanceNavigationTiming

      console.group('📊 页面性能 - ' + to.path)
      console.log({
        总加载时间: `${Math.round(navigationEntry.loadEventEnd - navigationEntry.startTime)}ms`,
        DNS查询: `${Math.round(navigationEntry.domainLookupEnd - navigationEntry.domainLookupStart)}ms`,
        TCP连接: `${Math.round(navigationEntry.connectEnd - navigationEntry.connectStart)}ms`,
        首字节时间: `${Math.round(navigationEntry.responseStart - navigationEntry.requestStart)}ms`
      })
      console.groupEnd()
    }
  }
}

// 用户数据接口
interface UserData {
  id: number;
  role: string;
  username: string;
  isEvaluator: boolean;
  // 添加其他用户属性
}

// 为路由守卫的next函数定义一个类型
type NavigationGuardNextCallback = (
  to?:
    | string
    | { path: string; query?: Record<string, string>; replace?: boolean; name?: string }
    | false
    | void
) => void;

// 检查用户是否需要重定向到登录页
const checkAuthRedirect = (
  requiresAuth: boolean,
  isAuthenticated: boolean,
  to: RouteLocationNormalized,
  next: NavigationGuardNextCallback
) => {
  // 如果需要认证但用户未登录，重定向到登录页
  if (requiresAuth && !isAuthenticated) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return true
  }

  // 如果用户已登录但访问登录页，重定向到首页
  if (isAuthenticated && (to.path === '/login' || to.path === '/register')) {
    next({ path: '/' })
    return true
  }

  return false
}

// 解析用户数据
const parseUserData = (userStr: string | null): UserData | null => {
  let userData: UserData | null = null;
  try {
    if (userStr) {
      userData = JSON.parse(userStr) as UserData;
    }
  } catch (e) {
    console.error('解析用户数据时出错:', e);
  }
  return userData;
}

// 检查用户角色权限
const checkRolePermission = (
  userData: UserData | null,
  to: RouteLocationNormalized,
  next: NavigationGuardNextCallback
): boolean => {
  const userRole = userData?.role;
  console.log('当前用户角色:', userRole);

  if (!userRole || !Array.isArray(to.meta.roles) || !to.meta.roles.includes(userRole)) {
    console.warn(`用户角色 ${userRole} 没有权限访问路径 ${to.path}，所需角色:`, to.meta.roles);
    next({ path: '/403' })
    return true
  }

  // 检查评测员权限
  if (to.meta.requiresEvaluator) {
    const isEvaluator = userData?.isEvaluator === true;
    if (!isEvaluator) {
      console.warn('此路径需要评测员权限');
      next({ path: '/403' })
      return true
    }
  }

  return false
}

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${String(to.meta.title)} - 标注系统` : '标注系统'

  // 记录路由变化（仅在开发环境）
  if (import.meta.env.DEV) {
    logRouteChange(to, from)
  }

  // 检查用户登录状态
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const userStr = localStorage.getItem('user')
  const isAuthenticated = !!userStr // 根据user数据判断是否已登录，不再使用token

  // 记录认证状态（仅在开发环境）
  if (import.meta.env.DEV) {
    logAuthStatus(isAuthenticated, requiresAuth, to.path)
  }

  // 检查是否需要重定向（登录/首页）
  if (checkAuthRedirect(requiresAuth, isAuthenticated, to, next)) {
    return
  }

  // 检查用户角色权限
  if (requiresAuth && isAuthenticated && to.meta.roles && Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
    const userData = parseUserData(userStr);
    if (checkRolePermission(userData, to, next)) {
      return
    }
  }

  next()
})

// 全局后置钩子
router.afterEach((to) => {
  // 记录页面性能（仅在开发环境）
  if (import.meta.env.DEV) {
    // 使用setTimeout确保性能度量在页面加载完成后执行
    setTimeout(() => logPagePerformance(to), 0)
  }
})

export default router
