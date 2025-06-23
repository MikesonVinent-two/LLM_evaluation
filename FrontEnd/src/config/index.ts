// 应用配置
export const appConfig = {
  title: import.meta.env.VITE_APP_TITLE || '前端项目',
  env: import.meta.env.VITE_APP_ENV || 'development',
  // API配置
  api: {
    // baseUrl: 'https://m1.apifoxmock.com/m1/6367019-6063223-default',
    baseUrl: 'http://localhost:8080',
    contentType: 'application/json',
    timeout: 100000, // 普通请求超时时间：10秒
    // LLM API特殊配置
    llm: {
      chatTimeout: 600000, // LLM聊天请求超时时间：60秒
    },
  },
  storage: {
    tokenKey: 'token',
    userKey: 'user',
    apiConfigKey: 'apiConfig',
  },
  // 其他全局配置
};

// API路径配置
export const apiUrls = {
  auth: {
    login: '/api/users/login',
    register: '/api/users/register',
    logout: '/api/users/logout',
  },
  user: {
    profile: '/api/users',
    search: '/api/users/search',
    delete: '/api/users',
    update: '/api/users',
    deactivate: '/api/users/deactivate',
  },
  rawData: {
    createQuestion: '/api/raw-data/questions-dto',
    createAnswer: '/api/raw-data/answers',
    createQuestionWithAnswers: '/api/raw-data/questions-with-answers',
    searchQuestions: '/api/raw-data/questions/search',
    getQuestionsByStatus: '/api/raw-data/questions/by-status',
    deleteQuestion: '/api/raw-data/questions',
    deleteAnswer: '/api/raw-data/answers',
    getQuestions: '/api/raw-data/questions', // 获取所有原始问题（分页）
  },
  standardData: {
    createQuestion: '/api/standard-questions',
    updateQuestion: '/api/standard-questions',
    deleteQuestion: '/api/standard-questions', // 删除标准问题
    createAnswer: '/api/standard/standard-answers',
    updateAnswer: '/api/standard/standard-answers',
    deleteAnswer: '/api/standard/standard-answers',
    getQuestionHistory: '/api/standard-questions',
    getQuestionVersionTree: '/api/standard-questions',
    getQuestions: '/api/standard-questions',
    getLatestQuestions: '/api/standard-questions/latest',
    searchQuestions: '/api/standard-questions/search',
    addTags: '/api/standard-questions/tags/add',
    removeTags: '/api/standard-questions/tags/remove',
    replaceTags: '/api/standard-questions/tags/replace',
    updateTags: '/api/standard-questions/tags',
    getOriginalData: '/api/standard-questions', // 获取标准问题对应的原始数据
    getQuestionsWithoutAnswers: '/api/standard-questions/without-standard-answers', // 获取所有没有标准回答的标准问题
    getAnswerHistory: '/api/standard/standard-answers',
    getAnswerVersionTree: '/api/standard/standard-answers',
    compareAnswerVersions: '/api/standard/standard-answers',
    rollbackAnswer: '/api/standard/standard-answers',
    rollbackQuestion: '/api/standard-questions/version', // 回退标准问题版本
  },
  prompts: {
    tags: '/api/prompts/tags', // 答案标签提示词相关API
    updateTag: '/api/prompts/tags', // 更新答案标签提示词API
    getTagDetail: '/api/prompts/tags', // 获取答案标签提示词详情API
    getAllTags: '/api/prompts/tags', // 获取所有答案标签提示词API
    getActiveByTagId: '/api/prompts/tags/active/tag', // 获取特定标签的激活状态答案标签提示词API
    deleteTag: '/api/prompts/tags', // 删除答案标签提示词API
    questionTypes: '/api/prompts/question-types', // 答题类型提示词相关API
    updateQuestionType: '/api/prompts/question-types', // 更新答题类型提示词API
    getQuestionTypeDetail: '/api/prompts/question-types', // 获取答题类型提示词详情API
    getAllQuestionTypes: '/api/prompts/question-types', // 获取所有答题类型提示词API
    getActiveByQuestionType: '/api/prompts/question-types/active/type', // 获取特定题型的激活状态提示词API
    deleteQuestionType: '/api/prompts/question-types', // 删除答题类型提示词API
    getSupportedQuestionTypes: '/api/prompts/question-types/supported-types', // 获取支持的题型枚举值API
    evaluationTags: '/api/prompts/evaluation/tags', // 评测标签提示词相关API
    getActiveEvaluationTagsByTagId: '/api/prompts/evaluation/tags/active/tag', // 获取指定标签的所有激活状态评测提示词API
    deleteEvaluationTag: '/api/prompts/evaluation/tags', // 删除评测标签提示词API
    evaluationSubjective: '/api/prompts/evaluation/subjective', // 评测主观题提示词相关API
    updateEvaluationSubjective: '/api/prompts/evaluation/subjective', // 更新评测主观题提示词API
    getEvaluationSubjectiveDetail: '/api/prompts/evaluation/subjective', // 获取评测主观题提示词详情API
    getAllEvaluationSubjective: '/api/prompts/evaluation/subjective', // 获取所有评测主观题提示词API
    getActiveEvaluationSubjective: '/api/prompts/evaluation/subjective/active', // 获取所有激活状态的评测主观题提示词API
    deleteEvaluationSubjective: '/api/ prompts/evaluation/subjective', // 删除评测主观题提示词API（软删除）
  },
  promptAssembly: {
    answerConfigs: '/api/prompt-assembly/answer-configs', // 回答提示词组装配置相关API
    getAnswerConfig: '/api/prompt-assembly/answer-configs', // 获取单个回答提示词组装配置API
    getActiveAnswerConfigs: '/api/prompt-assembly/answer-configs/page', // 获取所有活跃回答提示词组装配置API
    getUserAnswerConfigs: '/api/prompt-assembly/answer-configs/user', // 获取用户创建的回答提示词组装配置API
    evaluationConfigs: '/api/prompt-assembly/evaluation-configs', // 评测提示词组装配置相关API
    getUserEvaluationConfigs: '/api/prompt-assembly/evaluation-configs/user', // 获取用户创建的评测提示词组装配置API
  },
  answerGeneration: {
    batches: '/api/answer-generation/batches',
    startBatch: '/api/answer-generation/batches',
    pauseBatch: '/api/answer-generation/batches',
    resumeBatch: '/api/answer-generation/batches',
    getBatchStatus: '/api/answer-generation/batches',
    testConnectivity: '/api/answer-generation/batches',
    testModelConnectivity: '/api/answer-generation/models',
    getUserBatches: '/api/answer-generation/batches/user'
  },
  llmModels: {
    register: '/api/llm-models/register', // 注册模型API
    getModels: '/api/llm-models', // 获取已注册模型API
    base: '/api/llm-models', // 模型基础路径
  },
  evaluators: {
    create: '/api/evaluators', // 创建评测者API
    getAll: '/api/evaluators', // 获取所有评测者API
    getAllPage: '/api/evaluators/page', // 分页获取评测者API
    getById: '/api/evaluators', // 获取评测者详情API
    update: '/api/evaluators', // 更新评测者API (POST方法)
    delete: '/api/evaluators', // 删除评测者API
    getByType: '/api/evaluators/by-type', // 按类型获取评测者API
    register: '/api/evaluators/register', // 当前登录用户注册成为评测者API
    getAllAi: '/api/evaluators/ai', // 获取所有AI评测者API
    getAllHuman: '/api/evaluators/human', // 获取所有人类评测者API
    getByUserId: '/api/evaluators/user', // 根据用户ID获取评测者API
    testAiConnectivity: '/api/evaluators/ai/connectivity-test', // 测试AI评测员连通性API
  },
  crowdsourced: {
    base: '/api/crowdsourced-answers',  // 基础路径
    create: '/api/crowdsourced-answers',  // POST 创建
    getList: '/api/crowdsourced-answers',  // GET 获取列表
    all: '/api/crowdsourced-answers',  // GET 获取所有众包回答
    pending: '/api/crowdsourced-answers/pending',  // GET 获取所有未审核的众包回答
    update: '/api/crowdsourced-answers',  // PUT /{answerId}
    delete: '/api/crowdsourced-answers',  // DELETE /{answerId}
    review: '/api/crowdsourced-answers',  // PUT /{answerId}/review
    byQuestion: '/api/crowdsourced-answers/by-question', // GET /by-question/{questionId}
    byUser: '/api/crowdsourced-answers/by-user', // GET /by-user/{userId}
    byStatus: '/api/crowdsourced-answers/by-status', // GET /by-status/{status}
    reviewedBy: '/api/crowdsourced-answers/reviewed-by', // GET /reviewed-by/{reviewedByUserId} 获取用户审核过的众包回答
  },
  expert: {
    base: '/api/expert-candidate-answers',  // 基础路径
    create: '/api/expert-candidate-answers',  // POST 创建
    getList: '/api/expert-candidate-answers',  // GET 获取列表
    update: '/api/expert-candidate-answers',  // PUT /{answerId}
    delete: '/api/expert-candidate-answers',  // DELETE /{answerId}
    byQuestion: '/api/expert-candidate-answers/by-question', // GET /by-question/{questionId}
    byUser: '/api/expert-candidate-answers/by-user', // GET /by-user/{userId}
    quality: '/api/expert-candidate-answers', // PUT /{answerId}/quality
    unrated: '/api/expert-candidate-answers/unrated', // GET 获取未评分专家回答
  },
  dataset: {
    base: '/api/datasets',  // 基础路径
    versions: '/api/datasets/versions',  // 版本相关
    createVersion: '/api/datasets/versions',  // POST 创建版本
    getVersions: '/api/datasets/versions',  // GET 获取版本列表
    getVersion: '/api/datasets/versions',  // GET /{versionId} 获取特定版本
    updateVersion: '/api/datasets/versions',  // PUT /{versionId} 更新版本
    deleteVersion: '/api/datasets/versions',  // DELETE /{versionId} 删除版本
    cloneVersion: '/api/datasets/versions',  // POST /{versionId}/clone 克隆版本
    getVersionQuestions: '/api/datasets/versions',  // GET /{versionId}/questions/pageable 获取版本问题列表
    getStandardQuestionsByDataset: '/api/standard-questions/by-dataset',  // GET /{datasetId} 获取数据集相关的标准问题
  },
  evaluations: {
    base: '/api/evaluations',  // 评测基础路径
    batchObjectiveQuestions: '/api/evaluations/batch',  // 评测一个批次中的所有客观题
    batchSubjective: '/api/evaluations/batch/subjective',  // 批量评测主观题
    humanEvaluation: '/api/evaluations/human',  // 评测员评测某个回答
    humanPending: '/api/evaluations/human/pending',  // 获取待人工评测的回答列表
    humanCompleted: '/api/evaluations/human/completed',  // 获取已完成人工评测的回答列表
    subjectiveResults: '/api/evaluations/subjective/results',  // 获取主观题大模型评测详细结果
    subjectiveResultsAllEvaluators: '/api/evaluations/subjective/results/all-evaluators',  // 获取主观题所有评测员的评测详细结果
    objectiveResults: '/api/evaluations/objective/results',  // 获取批次中客观题的详细评测结果
    batchUnevaluated: '/api/evaluations/batch',  // 返回评测员评测任务
    runResults: '/api/evaluations/runs',  // 获取评测运行的统计结果
    batchComprehensiveScores: '/api/evaluations/batch',  // 批次综合评分展示
    batchSubjectiveProgress: '/api/evaluations/batch/subjective',  // 主观题评测进度
    criteria: '/api/evaluations/criteria',  // 评测标准接口
  },
  evaluationCriteria: {
    all: '/api/evaluations/criteria/all',  // 获取所有评测标准
    detail: '/api/evaluations/criteria',   // 获取评测标准详情
    create: '/api/evaluations/criteria',   // 创建评测标准
    update: '/api/evaluations/criteria',   // 更新评测标准
    delete: '/api/evaluations/criteria',   // 删除评测标准
  },
  modelScores: {
    base: '/api/model-batch-scores',  // 模型评分基础路径
    calculateBatchModelScore: '/api/model-batch-scores/batches',  // 计算模型在批次中的评分
    calculateAllModelsInBatch: '/api/model-batch-scores/batches',  // 计算批次中所有模型的评分
    detailedScores: '/api/llm-models/batch',  // 获取模型在批次中针对特定问题的详细评分
    modelRankings: '/api/llm-models/batch',  // 获取批次内模型排名
    performanceByQuestionType: '/api/llm-models/batch',  // 获取问题类型维度的模型表现
    batchRankings: '/api/model-detailed-scores/batch',  // 模型排名接口
  },
  tags: {
    recommend: '/api/tags/recommend',
    all: '/api/tags'
  },
  // 其他API路径
};

// 默认API配置
export const defaultApiConfig = {
  apiUrl: 'https://api.openai.com/v1',
  apiKey: '',
  apiType: 'openai_compatible',
}

// 系统角色配置
export const systemRoles = {
  default: '你是一个专业的助手，请用中文回答问题。',
  developer: '你是一个专业的开发者助手，擅长编程相关问题。',
  translator: '你是一个专业的翻译助手，精通多国语言互译。',
}

// 模型配置
export const modelConfig = {
  defaultModel: 'gpt-4',
  defaultTemperature: 0.7,
  defaultMaxTokens: 2000,
}

export default {
  appConfig,
  apiUrls,
  defaultApiConfig,
  systemRoles,
  modelConfig,
};
