/**
 * 系统角色定义
 */
export const ROLES = {
  ADMIN: 'ADMIN',           // 管理员
  CURATOR: 'CURATOR',       // 数据管理员
  EXPERT: 'EXPERT',         // 专家
  ANNOTATOR: 'ANNOTATOR',   // 标注员
  REFEREE: 'REFEREE',       // 评审员
  CROWDSOURCE_USER: 'CROWDSOURCE_USER' // 众包用户
}

/**
 * 工作台类型定义
 */
export const WORKSPACE_TYPES = {
  DATA: 'DATA',
  STANDARDIZATION: 'STANDARDIZATION',
  PROMPT: 'PROMPT',
  EVALUATION: 'EVALUATION',
  ASSESSMENT: 'ASSESSMENT', // 新增评测工作台类型
  GENERATION: 'GENERATION',
  SYSTEM: 'SYSTEM',
  CROWDSOURCE: 'CROWDSOURCE',
  EXPERT: 'EXPERT',
  DATASET: 'DATASET',
  RUNTIME: 'RUNTIME'
} as const

/**
 * 工作台配置类型
 */
interface WorkspaceConfig {
  id: string;
  name: string;
  type: string;
  path: string;
  roles: string[];
  description: string;
  icon: string;
  requiresEvaluator?: boolean;
  parentId?: string;
}

/**
 * 工作台配置列表
 * @type {WorkspaceConfig[]}
 */
export const WORKSPACES = [
  // 数据管理工作台
  {
    id: 'original-questions',
    name: '原始问题管理',
    type: WORKSPACE_TYPES.DATA,
    path: '/data/original-questions',
    roles: [ROLES.CURATOR, ROLES.ADMIN],
    description: '管理系统中的原始问题数据',
    icon: 'Document',
    requiresEvaluator: false
  },

  // 标准化工作台
  {
    id: 'question-standardization',
    name: '问题标准化',
    type: WORKSPACE_TYPES.STANDARDIZATION,
    path: '/standardization/question-standardization',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '将原始问题标准化',
    icon: 'Edit'
  },
  {
    id: 'question-history',
    name: '问题版本历史',
    type: WORKSPACE_TYPES.STANDARDIZATION,
    path: '/standardization/question-history',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '查看和管理问题的版本历史',
    icon: 'Timer'
  },
  {
    id: 'standard-answers',
    name: '标准回答管理',
    type: WORKSPACE_TYPES.STANDARDIZATION,
    path: '/standardization/standard-answers',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理标准回答',
    icon: 'ChatLineRound'
  },

  // 提示词工作台
  {
    id: 'answer-type-prompt',
    name: '回答阶段-题型提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/answer-type-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理回答阶段的题型提示词',
    icon: 'Document'
  },
  {
    id: 'answer-tag-prompt',
    name: '回答阶段-标签提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/answer-tag-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理回答阶段的标签提示词',
    icon: 'PriceTag'
  },
  {
    id: 'answer-assembly-prompt',
    name: '回答阶段-组装提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/answer-assembly-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理回答阶段的组装提示词',
    icon: 'Connection'
  },
  {
    id: 'evaluation-subjective-prompt',
    name: '评测阶段-简答题提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/evaluation-subjective-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理评测阶段的简答题提示词',
    icon: 'EditPen'
  },
  {
    id: 'evaluation-tag-prompt',
    name: '评测阶段-标签提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/evaluation-tag-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理评测阶段的标签提示词',
    icon: 'PriceTag'
  },
  {
    id: 'evaluation-assembly-prompt',
    name: '评测阶段-组装提示词',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/prompt/evaluation-assembly-prompt',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN],
    description: '管理评测阶段的组装提示词',
    icon: 'Connection'
  },
  {
    id: 'evaluation-criteria-management',
    name: '评测标准管理',
    type: WORKSPACE_TYPES.PROMPT,
    path: '/system/evaluation-criteria',
    roles: [ROLES.ADMIN],
    description: '管理评测标准',
    icon: 'List'
  },

  // 评测工作台
  // {
  //   id: 'assessment-workbench',
  //   name: '评测工作台',
  //   type: WORKSPACE_TYPES.ASSESSMENT,
  //   path: '/assessment/workbench',
  //   roles: [ROLES.ADMIN],
  //   description: '评测工作台',
  //   requiresEvaluator: true,
  //   icon: 'DataAnalysis'
  // },
  {
    id: 'evaluations',
    name: '批次评测管理',
    type: WORKSPACE_TYPES.ASSESSMENT,
    path: '/assessment/evaluations',
    roles: [ROLES.ADMIN, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE, ROLES.CROWDSOURCE_USER, ROLES.CURATOR],
    description: '管理批次评测',
    icon: 'Management',
    requiresEvaluator: true
  },
  {
    id: 'objective-evaluation',
    name: '客观题评测',
    type: WORKSPACE_TYPES.ASSESSMENT,
    path: '/assessment/objective-evaluation/:batchId',
    roles: [ROLES.ADMIN, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE],
    description: '客观题机器评测',
    icon: 'Check',
    requiresEvaluator: true,
    parentId: 'evaluations'
  },
  {
    id: 'subjective-llm-evaluation',
    name: '主观题大模型评测',
    type: WORKSPACE_TYPES.ASSESSMENT,
    path: '/assessment/subjective-llm-evaluation/:batchId',
    roles: [ROLES.ADMIN, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE],
    description: '主观题大模型评测',
    icon: 'Cpu',
    requiresEvaluator: true,
    parentId: 'evaluations'
  },
  {
    id: 'subjective-human-evaluation',
    name: '主观题人工评测',
    type: WORKSPACE_TYPES.ASSESSMENT,
    path: '/assessment/subjective-human-evaluation/:batchId',
    roles: [ROLES.ADMIN, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE],
    description: '主观题人工评测',
    icon: 'UserFilled',
    requiresEvaluator: true,
    parentId: 'evaluations'
  },
  {
    id: 'scoring',
    name: '评分管理',
    type: WORKSPACE_TYPES.ASSESSMENT,
    path: '/assessment/scoring',
    roles: [ROLES.ADMIN, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE, ROLES.CROWDSOURCE_USER, ROLES.CURATOR],
    description: '管理评分结果',
    icon: 'StarFilled',
    requiresEvaluator: true
  },

  // 评审员工作台
  {
    id: 'expert-answer-review',
    name: '专家回答评分',
    type: WORKSPACE_TYPES.EVALUATION,
    path: '/referee/expert-answer-review',
    roles: [ROLES.REFEREE, ROLES.ADMIN],
    description: '评审专家回答并评分',
    icon: 'StarFilled'
  },
  {
    id: 'crowdsourced-answer-review',
    name: '众包回答审核',
    type: WORKSPACE_TYPES.EVALUATION,
    path: '/referee/crowdsourced-answer-review',
    roles: [ROLES.REFEREE, ROLES.ADMIN],
    description: '审核众包回答并提供反馈',
    icon: 'Check'
  },

  // 众包工作台
  {
    id: 'crowdsource-dashboard',
    name: '众包仪表盘',
    type: WORKSPACE_TYPES.CROWDSOURCE,
    path: '/crowdsource/crowdsource-dashboard',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN, ROLES.CROWDSOURCE_USER, ROLES.REFEREE],
    description: '众包数据统计和分析',
    icon: 'PieChart'
  },
  {
    id: 'crowdsource-questions',
    name: '标准问题回答',
    type: WORKSPACE_TYPES.CROWDSOURCE,
    path: '/crowdsource/standard-questions',
    roles: [ROLES.ANNOTATOR, ROLES.ADMIN, ROLES.CROWDSOURCE_USER, ROLES.REFEREE],
    description: '回答标准化问题',
    icon: 'QuestionFilled'
  },

  // 专家工作台
  {
    id: 'expert-dashboard',
    name: '专家仪表盘',
    type: WORKSPACE_TYPES.EXPERT,
    path: '/expert/expert-dashboard',
    roles: [ROLES.EXPERT, ROLES.ADMIN, ROLES.REFEREE],
    description: '专家回答统计和分析',
    icon: 'PieChart'
  },
  {
    id: 'expert-questions',
    name: '专家问题回答',
    type: WORKSPACE_TYPES.EXPERT,
    path: '/expert/expert-questions',
    roles: [ROLES.EXPERT, ROLES.ADMIN],
    description: '回答专业问题',
    icon: 'QuestionFilled'
  },
  {
    id: 'expert-history',
    name: '历史回答记录',
    type: WORKSPACE_TYPES.EXPERT,
    path: '/expert/expert-history',
    roles: [ROLES.EXPERT, ROLES.ADMIN],
    description: '查看历史回答记录',
    icon: 'Timer'
  },

  // 系统管理工作台
  {
    id: 'user-management',
    name: '用户管理',
    type: WORKSPACE_TYPES.SYSTEM,
    path: '/system/users',
    roles: [ROLES.ADMIN],
    description: '管理系统用户',
    icon: 'User'
  },
  {
    id: 'model-management',
    name: '模型管理',
    type: WORKSPACE_TYPES.SYSTEM,
    path: '/system/models',
    roles: [ROLES.ADMIN],
    description: '管理AI模型',
    icon: 'CPU'
  },
  {
    id: 'evaluator-management',
    name: '评测员管理',
    type: WORKSPACE_TYPES.SYSTEM,
    path: '/system/evaluators',
    roles: [ROLES.ADMIN],
    description: '管理人类和AI评测员',
    icon: 'UserFilled'
  },


  // 数据集工作台
  {
    id: 'dataset-management',
    name: '数据集列表',
    type: WORKSPACE_TYPES.DATASET,
    path: '/dataset/list',
    roles: [ROLES.ADMIN, ROLES.CURATOR],
    description: '管理数据集版本',
    icon: 'Collection'
  },
  {
    id: 'create-dataset',
    name: '创建数据集',
    type: WORKSPACE_TYPES.DATASET,
    path: '/dataset/create',
    roles: [ROLES.ADMIN, ROLES.CURATOR],
    description: '创建新的数据集',
    icon: 'FolderAdd'
  },
  {
    id: 'edit-dataset',
    name: '编辑数据集',
    type: WORKSPACE_TYPES.DATASET,
    path: '/dataset/edit/:id',
    roles: [ROLES.ADMIN, ROLES.CURATOR],
    description: '编辑数据集',
    icon: 'Edit'
  },

  // 运行工作台
  // {
  //   id: 'batch-monitor',
  //   name: '批次实时监控',
  //   type: WORKSPACE_TYPES.RUNTIME,
  //   path: '/admin/batch-monitor',
  //   roles: [ROLES.ADMIN, ROLES.ANNOTATOR],
  //   description: '使用WebSocket实时监控批次状态',
  //   icon: 'Monitor'
  // },
  {
    id: 'answer-generation-batches',
    name: '回答生成批次',
    type: WORKSPACE_TYPES.RUNTIME,
    path: '/runtime/answer-generation-batches',
    roles: [ROLES.ADMIN, ROLES.ANNOTATOR],
    description: '管理和监控回答生成批次',
    icon: 'List'
  },
  {
    id: 'answer-batch-dashboard',
    name: '回答批次仪表盘',
    type: WORKSPACE_TYPES.RUNTIME,
    path: '/runtime/answer-batch-dashboard',
    roles: [ROLES.ADMIN, ROLES.ANNOTATOR],
    description: '回答批次详细分析和统计',
    icon: 'PieChart'
  },
  {
    id: 'create-answer-batch',
    name: '创建回答批次',
    type: WORKSPACE_TYPES.RUNTIME,
    path: '/runtime/create-answer-batch',
    roles: [ROLES.ADMIN, ROLES.ANNOTATOR],
    description: '创建新的回答生成批次',
    icon: 'Plus'
  },
  // 系统工具
  {
    id: 'websocket-demo',
    name: 'WebSocket演示',
    type: WORKSPACE_TYPES.SYSTEM,
    path: '/websocket-demo',
    roles: [ROLES.ADMIN, ROLES.CURATOR, ROLES.EXPERT, ROLES.ANNOTATOR, ROLES.REFEREE, ROLES.CROWDSOURCE_USER],
    description: 'WebSocket连接演示和测试',
    icon: 'Connection'
  },
]

/**
 * 按工作台类型分组的工作台配置
 * @type {Object.<string, WorkspaceConfig[]>}
 */
export const WORKSPACES_BY_TYPE = WORKSPACES.reduce((acc, workspace) => {
  if (!acc[workspace.type]) {
    acc[workspace.type] = []
  }
  acc[workspace.type].push(workspace)
  return acc
}, {} as Record<string, WorkspaceConfig[]>)

/**
 * 按角色分组的工作台配置
 * @type {Object.<string, WorkspaceConfig[]>}
 */
export const WORKSPACES_BY_ROLE = WORKSPACES.reduce((acc, workspace) => {
  workspace.roles.forEach(role => {
    if (!acc[role]) {
      acc[role] = []
    }
    acc[role].push(workspace)
  })
  return acc
}, {} as Record<string, WorkspaceConfig[]>)

/**
 * 检查用户是否有权限访问工作台
 * @param {string} workspaceId - 工作台ID
 * @param {string} userRole - 用户角色
 * @param {boolean} isEvaluator - 用户是否为评测员
 * @returns {boolean} - 是否有权限访问
 */
export const canAccessWorkspace = (workspaceId: string, userRole: string, isEvaluator = false): boolean => {
  const workspace = WORKSPACES.find(w => w.id === workspaceId)
  if (!workspace) return false

  const hasRole = workspace.roles.includes(userRole)
  const meetsEvaluatorRequirement = workspace.requiresEvaluator ? isEvaluator : true

  return hasRole && meetsEvaluatorRequirement
}

/**
 * 获取用户可访问的所有工作台
 * @param {string} userRole - 用户角色
 * @param {boolean} isEvaluator - 用户是否为评测员
 * @returns {WorkspaceConfig[]} - 可访问的工作台列表
 */
export const getAccessibleWorkspaces = (userRole: string, isEvaluator = false): WorkspaceConfig[] => {
  return WORKSPACES.filter(workspace => {
    const hasRole = workspace.roles.includes(userRole)
    const meetsEvaluatorRequirement = workspace.requiresEvaluator ? isEvaluator : true
    return hasRole && meetsEvaluatorRequirement
  })
}

/**
 * 工作台类型名称映射
 */
export const workspaceTypeNames = {
  [WORKSPACE_TYPES.DATA]: '数据管理',
  [WORKSPACE_TYPES.STANDARDIZATION]: '标准化工作台',
  [WORKSPACE_TYPES.PROMPT]: 'Prompt工作台',
  [WORKSPACE_TYPES.EVALUATION]: '评审工作台',
  [WORKSPACE_TYPES.ASSESSMENT]: '评测工作台', // 新增评测工作台名称
  [WORKSPACE_TYPES.GENERATION]: '生成工作台',
  [WORKSPACE_TYPES.SYSTEM]: '系统管理',
  [WORKSPACE_TYPES.CROWDSOURCE]: '众包工作台',
  [WORKSPACE_TYPES.EXPERT]: '专家工作台',
  [WORKSPACE_TYPES.DATASET]: '数据集工作台',
  [WORKSPACE_TYPES.RUNTIME]: '运行工作台'
}

/**
 * 工作台类型图标名称映射
 * 注意：这里只定义图标名称，不导入具体组件
 * 组件需要在使用处导入
 */
export const workspaceTypeIconNames = {
  [WORKSPACE_TYPES.DATA]: 'Collection',
  [WORKSPACE_TYPES.STANDARDIZATION]: 'EditPen',
  [WORKSPACE_TYPES.PROMPT]: 'Document',
  [WORKSPACE_TYPES.EVALUATION]: 'StarFilled',
  [WORKSPACE_TYPES.ASSESSMENT]: 'DataAnalysis',
  [WORKSPACE_TYPES.GENERATION]: 'MagicStick',
  [WORKSPACE_TYPES.SYSTEM]: 'Setting',
  [WORKSPACE_TYPES.CROWDSOURCE]: 'Briefcase',
  [WORKSPACE_TYPES.EXPERT]: 'UserFilled',
  [WORKSPACE_TYPES.DATASET]: 'Files',
  [WORKSPACE_TYPES.RUNTIME]: 'Connection'
}

/**
 * 工作台类型顺序定义（用于控制侧边栏显示顺序）
 * 数字越小，显示越靠前
 */
export const workspaceTypeOrder = {
  [WORKSPACE_TYPES.SYSTEM]: 1,        // 系统管理最靠前
  [WORKSPACE_TYPES.DATA]: 2,          // 数据管理
  [WORKSPACE_TYPES.DATASET]: 3,       // 数据集工作台
  [WORKSPACE_TYPES.STANDARDIZATION]: 4, // 标准化工作台
  [WORKSPACE_TYPES.PROMPT]: 5,        // Prompt工作台
  [WORKSPACE_TYPES.EXPERT]: 6,        // 专家工作台
  [WORKSPACE_TYPES.EVALUATION]: 7,    // 评审工作台
  [WORKSPACE_TYPES.ASSESSMENT]: 10,    // 评测工作台
  [WORKSPACE_TYPES.CROWDSOURCE]:8 ,   // 众包工作台
  [WORKSPACE_TYPES.RUNTIME]: 9,      // 运行工作台
  [WORKSPACE_TYPES.GENERATION]: 11    // 生成工作台
}

export default {
  ROLES,
  WORKSPACE_TYPES,
  WORKSPACES,
  WORKSPACES_BY_TYPE,
  WORKSPACES_BY_ROLE,
  canAccessWorkspace,
  getAccessibleWorkspaces,
  workspaceTypeNames,
  workspaceTypeIconNames,
  workspaceTypeOrder
}
