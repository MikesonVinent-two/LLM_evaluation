# 批次评分展示详细设计方案

## 1. 评分体系详解

### 1.1 评分类型分类

#### 客观题评分（机器自动评分）
1. **单选题评分**
   - 评分方式：直接比对选项ID
   - 分数：正确=100分，错误=0分
   - 存储位置：`evaluations.normalized_score`

2. **多选题评分**
   - 评分方式：计算选中选项与正确选项的重叠度
   - 分数计算：`(正确选中数 - 错误选中数) / 总正确选项数 * 100`
   - 存储位置：`evaluations.normalized_score`

3. **简单事实题评分**
   - 评分方式：多指标加权平均
   - 包含指标：
     - 资格指标（关键词匹配）
     - 文本相似度
     - ROUGE分数
     - BLEU分数  
     - BERT语义相似度
   - 存储位置：`evaluations.normalized_score` + `evaluation_details`表中各指标详细分数

#### 主观题评分
1. **AI模型评分**
   - 评分方式：使用指定的AI评测员基于criteria进行评分
   - 评分标准：可选择多个criteria（如专业性、完整性、逻辑性等）
   - 分数范围：0-100分
   - 存储位置：
     - 总分：`evaluations.overall_score`
     - 各criteria分数：`evaluation_details.score`

2. **人工评分**
   - 评分方式：人类评测员基于选择的criteria进行评分
   - 评分标准：评测员可自选criteria
   - 分数范围：0-100分
   - 存储位置：
     - 总分：`evaluations.overall_score`
     - 各criteria分数：`evaluation_details.score`
     - 评语：`evaluations.comments` + `evaluation_details.comments`

### 1.2 评分数据存储结构

```sql
-- 主评测表
evaluations:
  - overall_score: 总体评分（主观题）
  - normalized_score: 标准化分数（客观题）
  - evaluation_results: JSON格式的详细结果
  - comments: 总体评语

-- 评测详情表  
evaluation_details:
  - criterion_name: 评测标准名称
  - score: 该标准的得分
  - comments: 该标准的评语

-- 批次统计表
model_batch_scores:
  - score_type: 评分类型（OVERALL, OBJECTIVE, SUBJECTIVE等）
  - average_score: 平均分
  - total_answers: 总回答数
  - scored_answers: 已评分回答数
```

## 2. 完整接口文档

### 2.0 重要说明

#### 模型筛选行为
- **所有接口的 `modelIds` 参数都是可选的**
- **如果不指定 `modelIds` 参数，接口将返回批次中所有模型的结果**
- **如果指定了 `modelIds` 参数，接口只返回指定模型的结果**
- **这个行为适用于所有包含 `modelIds` 参数的接口**

#### 分页行为
- **所有接口都支持分页，`page` 和 `size` 参数都是可选的**
- **默认 `page=0`（第一页），`size=20`（每页20条）**
- **返回结果中包含完整的分页信息**

### 2.1 主接口：批次综合评分

#### 接口信息
- **路径**：`GET /api/evaluations/batch/{batchId}/comprehensive-scores`
- **描述**：获取批次的综合评分展示数据，包含所有评分类型的统计信息

#### 请求参数
```json
{
  "路径参数": {
    "batchId": {
      "类型": "Long",
      "必需": true,
      "描述": "批次ID"
    }
  },
  "查询参数": {
    "modelIds": {
      "类型": "List<Long>",
      "必需": false,
      "描述": "模型ID列表，用于筛选特定模型。如果不指定，则返回批次中所有模型的结果",
      "示例": "1,2,3"
    },
    "page": {
      "类型": "int",
      "必需": false,
      "默认值": 0,
      "描述": "页码（从0开始）"
    },
    "size": {
      "类型": "int", 
      "必需": false,
      "默认值": 20,
      "描述": "每页大小"
    }
  }
}
```

#### 请求示例
```bash
GET /api/evaluations/batch/123/comprehensive-scores?modelIds=1,2,3&page=0&size=10
```

#### 返回体结构
```json
{
  "success": true,
  "batchInfo": {
    "id": 123,
    "name": "医学问答评测批次",
    "description": "针对医学领域的综合评测",
    "creation_time": "2024-01-15T10:30:00",
    "dataset_name": "医学问答数据集",
    "dataset_version": "v2.1"
  },
  "models": [
    {
      "id": 1,
      "name": "GPT-4",
      "provider": "OpenAI",
      "version": "gpt-4-0613"
    },
    {
      "id": 2,
      "name": "Claude-3",
      "provider": "Anthropic", 
      "version": "claude-3-opus"
    }
  ],
  "modelScores": [
    {
      "rank": 1,
      "modelInfo": {
        "id": 1,
        "name": "GPT-4",
        "provider": "OpenAI",
        "version": "gpt-4-0613"
      },
      "overallScore": 87.5,
      "objectiveScores": {
        "total_answers": 150,
        "average_score": 89.2,
        "max_score": 100.0,
        "min_score": 65.0,
        "single_choice_count": 50,
        "multiple_choice_count": 30,
        "simple_fact_count": 70,
        "single_choice_avg": 92.0,
        "multiple_choice_avg": 88.5,
        "simple_fact_avg": 87.8
      },
      "subjectiveAiScores": {
        "total_answers": 80,
        "average_score": 85.3,
        "max_score": 96.0,
        "min_score": 72.0,
        "evaluator_count": 2,
        "criteriaScores": [
          {
            "criterion_name": "专业性",
            "average_score": 88.5,
            "count": 80
          },
          {
            "criterion_name": "完整性", 
            "average_score": 82.1,
            "count": 80
          },
          {
            "criterion_name": "逻辑性",
            "average_score": 85.7,
            "count": 80
          }
        ]
      },
      "subjectiveHumanScores": {
        "total_answers": 40,
        "average_score": 88.7,
        "max_score": 98.0,
        "min_score": 75.0,
        "evaluator_count": 3,
        "criteriaScores": [
          {
            "criterion_name": "专业性",
            "average_score": 90.2,
            "count": 40
          },
          {
            "criterion_name": "实用性",
            "average_score": 87.3,
            "count": 40
          }
        ]
      },
      "detailStats": {
        "total_evaluations": 270,
        "total_answers": 270,
        "total_evaluators": 5,
        "success_count": 265,
        "failed_count": 5
      }
    }
  ],
  "overview": {
    "total_models": 3,
    "total_answers": 810,
    "total_evaluations": 1620,
    "total_evaluators": 8,
    "single_choice_count": 150,
    "multiple_choice_count": 90,
    "simple_fact_count": 210,
    "subjective_count": 360,
    "ai_evaluation_count": 1080,
    "human_evaluation_count": 540
  },
  "pagination": {
    "currentPage": 0,
    "pageSize": 10,
    "totalItems": 3,
    "totalPages": 1
  }
}
```

### 2.2 客观题详细结果接口

#### 接口信息
- **路径**：`GET /api/evaluations/objective/results`
- **描述**：获取客观题的详细评测结果

#### 请求参数
```json
{
  "查询参数": {
    "batchId": {
      "类型": "Long",
      "必需": true,
      "描述": "批次ID"
    },
    "modelIds": {
      "类型": "List<Long>",
      "必需": false,
      "描述": "模型ID列表，用于筛选特定模型。如果不指定，则返回批次中所有模型的结果"
    },
    "page": {
      "类型": "int",
      "必需": false,
      "默认值": 0
    },
    "size": {
      "类型": "int",
      "必需": false,
      "默认值": 20
    }
  }
}
```

#### 请求示例
```bash
GET /api/evaluations/objective/results?batchId=123&modelIds=1,2&page=0&size=20
```

#### 返回体结构
```json
{
  "success": true,
  "results": [
    {
      "questionId": 1,
      "questionText": "以下哪种药物用于治疗高血压？",
      "questionType": "SINGLE_CHOICE",
      "standardAnswer": "A",
      "modelAnswers": [
        {
          "modelId": 1,
          "modelName": "GPT-4",
          "answerText": "A",
          "isCorrect": true,
          "score": 100.0,
          "evaluationTime": "2024-01-15T11:00:00"
        }
      ]
    }
  ],
  "statistics": {
    "totalQuestions": 150,
    "averageScore": 89.2,
    "accuracyRate": 0.892,
    "questionTypeStats": {
      "SINGLE_CHOICE": {
        "count": 50,
        "averageScore": 92.0,
        "accuracyRate": 0.92
      },
      "MULTIPLE_CHOICE": {
        "count": 30,
        "averageScore": 88.5,
        "accuracyRate": 0.885
      },
      "SIMPLE_FACT": {
        "count": 70,
        "averageScore": 87.8,
        "accuracyRate": 0.878
      }
    }
  },
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8
  }
}
```

### 2.3 主观题详细结果接口

#### 接口信息
- **路径**：`GET /api/evaluations/subjective/results`
- **描述**：获取主观题的详细评测结果

#### 请求参数
```json
{
  "查询参数": {
    "batchId": {
      "类型": "Long",
      "必需": true,
      "描述": "批次ID"
    },
    "modelIds": {
      "类型": "List<Long>",
      "必需": false,
      "描述": "模型ID列表，用于筛选特定模型。如果不指定，则返回批次中所有模型的结果"
    },
    "evaluatorId": {
      "类型": "Long",
      "必需": false,
      "描述": "评测者ID，不指定则返回所有评测者结果"
    },
    "page": {
      "类型": "int",
      "必需": false,
      "默认值": 0
    },
    "size": {
      "类型": "int",
      "必需": false,
      "默认值": 20
    }
  }
}
```

#### 请求示例
```bash
GET /api/evaluations/subjective/results?batchId=123&evaluatorId=5&page=0&size=20
```

#### 返回体结构
```json
{
  "success": true,
  "results": [
    {
      "questionId": 201,
      "questionText": "请解释糖尿病的发病机制",
      "questionType": "SUBJECTIVE",
      "referenceAnswer": "糖尿病是由于胰岛素分泌不足或作用缺陷导致的代谢性疾病...",
      "modelAnswers": [
        {
          "modelId": 1,
          "modelName": "GPT-4",
          "answerText": "糖尿病的发病机制主要包括...",
          "evaluations": [
            {
              "evaluatorId": 5,
              "evaluatorName": "DeepSeek-Chat",
              "evaluatorType": "AI_MODEL",
              "overallScore": 88.5,
              "comments": "回答专业准确，逻辑清晰，但可以增加更多临床实例",
              "criteriaScores": [
                {
                  "criterionName": "专业性",
                  "score": 92.0,
                  "comments": "医学术语使用准确"
                },
                {
                  "criterionName": "完整性",
                  "score": 85.0,
                  "comments": "涵盖了主要发病机制"
                },
                {
                  "criterionName": "逻辑性",
                  "score": 88.5,
                  "comments": "逻辑结构清晰"
                }
              ],
              "evaluationTime": "2024-01-15T12:00:00"
            }
          ]
        }
      ]
    }
  ],
  "statistics": {
    "totalQuestions": 80,
    "averageScore": 85.3,
    "evaluatorStats": [
      {
        "evaluatorId": 5,
        "evaluatorName": "DeepSeek-Chat",
        "evaluatorType": "AI_MODEL",
        "evaluatedCount": 80,
        "averageScore": 85.3
      }
    ],
    "criteriaStats": [
      {
        "criterionName": "专业性",
        "averageScore": 88.5,
        "evaluatedCount": 80
      },
      {
        "criterionName": "完整性",
        "averageScore": 82.1,
        "evaluatedCount": 80
      }
    ]
  },
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalItems": 80,
    "totalPages": 4
  }
}
```

### 2.4 模型排名接口

#### 接口信息
- **路径**：`GET /api/model-detailed-scores/batch/{batchId}/rankings`
- **描述**：获取批次中模型的排名信息

#### 请求参数
```json
{
  "路径参数": {
    "batchId": {
      "类型": "Long",
      "必需": true,
      "描述": "批次ID"
    }
  },
  "查询参数": {
    "scoreType": {
      "类型": "String",
      "必需": false,
      "默认值": "OVERALL",
      "可选值": ["OVERALL", "OBJECTIVE", "SUBJECTIVE"],
      "描述": "排名依据的评分类型"
    }
  }
}
```

#### 请求示例
```bash
GET /api/model-detailed-scores/batch/123/rankings?scoreType=OVERALL
```

#### 返回体结构
```json
{
  "success": true,
  "batchId": 123,
  "scoreType": "OVERALL",
  "rankings": [
    {
      "rank": 1,
      "model_id": 1,
      "model_name": "GPT-4",
      "provider": "OpenAI",
      "version": "gpt-4-0613",
      "average_score": 87.5,
      "total_answers": 270,
      "scored_answers": 270,
      "max_score": 98.0,
      "min_score": 65.0,
      "calculated_at": "2024-01-15T15:30:00"
    },
    {
      "rank": 2,
      "model_id": 2,
      "model_name": "Claude-3",
      "provider": "Anthropic",
      "version": "claude-3-opus",
      "average_score": 85.2,
      "total_answers": 270,
      "scored_answers": 270,
      "max_score": 96.0,
      "min_score": 62.0,
      "calculated_at": "2024-01-15T15:30:00"
    }
  ],
  "totalModels": 3
}
```

### 2.5 评测进度接口

#### 接口信息
- **路径**：`GET /api/evaluations/batch/subjective/{batchId}/progress`
- **描述**：获取批次主观题评测的进度信息

#### 请求参数
```json
{
  "路径参数": {
    "batchId": {
      "类型": "Long",
      "必需": true,
      "描述": "批次ID"
    }
  },
  "查询参数": {
    "evaluatorId": {
      "类型": "Long",
      "必需": true,
      "描述": "评测者ID"
    }
  }
}
```

#### 请求示例
```bash
GET /api/evaluations/batch/subjective/123/progress?evaluatorId=5
```

#### 返回体结构
```json
{
  "success": true,
  "batchId": 123,
  "evaluatorId": 5,
  "progress": {
    "status": "IN_PROGRESS",
    "progressPercentage": 75.5,
    "completedAnswers": 60,
    "totalAnswers": 80,
    "failedAnswers": 2,
    "startTime": "2024-01-15T10:00:00",
    "estimatedEndTime": "2024-01-15T16:30:00",
    "lastActivityTime": "2024-01-15T14:20:00"
  }
}
```

### 2.6 评测标准接口

#### 接口信息
- **路径**：`GET /api/evaluations/criteria`
- **描述**：获取评测标准列表

#### 请求参数
```json
{
  "查询参数": {
    "questionType": {
      "类型": "String",
      "必需": true,
      "可选值": ["SINGLE_CHOICE", "MULTIPLE_CHOICE", "SIMPLE_FACT", "SUBJECTIVE"],
      "描述": "问题类型"
    },
    "page": {
      "类型": "int",
      "必需": false,
      "默认值": 0
    },
    "size": {
      "类型": "int",
      "必需": false,
      "默认值": 20
    }
  }
}
```

#### 请求示例
```bash
GET /api/evaluations/criteria?questionType=SUBJECTIVE&page=0&size=10
```

#### 返回体结构
```json
{
  "success": true,
  "criteria": [
    {
      "id": 1,
      "name": "专业性",
      "description": "回答在专业领域的准确性和规范性",
      "dataType": "SCORE",
      "scoreRange": "0-100",
      "weight": 1.5,
      "isRequired": true,
      "orderIndex": 1
    },
    {
      "id": 2,
      "name": "完整性",
      "description": "回答内容的完整程度",
      "dataType": "SCORE", 
      "scoreRange": "0-100",
      "weight": 1.0,
      "isRequired": true,
      "orderIndex": 2
    }
  ],
  "pagination": {
    "currentPage": 0,
    "pageSize": 10,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

## 3. 前端使用指南

### 3.0 接口使用要点

#### 模型筛选逻辑
```javascript
// 获取所有模型的结果（推荐的默认行为）
const allModelsData = await this.$api.get(`/evaluations/batch/${batchId}/comprehensive-scores`)

// 获取特定模型的结果
const specificModelsData = await this.$api.get(`/evaluations/batch/${batchId}/comprehensive-scores`, {
  params: {
    modelIds: '1,2,3'  // 只返回ID为1,2,3的模型结果
  }
})

// 动态筛选：根据用户选择决定是否传递modelIds
const params = {}
if (this.selectedModelIds.length > 0) {
  params.modelIds = this.selectedModelIds.join(',')
}
// 如果selectedModelIds为空，则不传递modelIds参数，获取所有模型
const data = await this.$api.get(`/evaluations/batch/${batchId}/comprehensive-scores`, { params })
```

#### 分页处理
```javascript
// 基本分页
const pagedData = await this.$api.get(`/evaluations/batch/${batchId}/comprehensive-scores`, {
  params: {
    page: 0,    // 第一页
    size: 10    // 每页10条
  }
})

// 处理分页信息
const { currentPage, pageSize, totalItems, totalPages } = pagedData.pagination
```

### 3.1 页面加载流程

```javascript
// 1. 页面初始化
async mounted() {
  this.batchId = this.$route.params.batchId
  await this.loadBatchData()
}

// 2. 加载批次综合数据
async loadBatchData() {
  try {
    // 主要数据加载
    const response = await this.$api.get(`/evaluations/batch/${this.batchId}/comprehensive-scores`, {
      params: {
        modelIds: this.selectedModelIds.join(','),
        page: this.currentPage,
        size: this.pageSize
      }
    })
    
    this.batchData = response.data
    this.processScoreData()
    
  } catch (error) {
    this.$message.error('加载批次数据失败')
  }
}

// 3. 处理评分数据
processScoreData() {
  // 计算评分分布
  this.scoreDistribution = this.calculateScoreDistribution()
  
  // 生成图表数据
  this.chartData = this.generateChartData()
  
  // 处理排名数据
  this.rankingData = this.batchData.modelScores
}
```

### 3.2 评分数据展示

#### 3.2.1 综合评分卡片
```vue
<template>
  <el-card class="score-overview-card">
    <div class="score-header">
      <h3>{{ modelInfo.name }}</h3>
      <el-tag :type="getScoreType(overallScore)">
        排名 #{{ rank }}
      </el-tag>
    </div>
    
    <!-- 综合评分 -->
    <div class="overall-score">
      <el-progress 
        type="circle" 
        :percentage="overallScore" 
        :width="120"
        :color="getScoreColor(overallScore)"
      >
        <span class="score-text">{{ overallScore.toFixed(1) }}</span>
      </el-progress>
    </div>
    
    <!-- 分类评分 -->
    <div class="category-scores">
      <div class="score-item">
        <span class="label">客观题</span>
        <span class="value">{{ objectiveScores.average_score?.toFixed(1) || 'N/A' }}</span>
      </div>
      <div class="score-item">
        <span class="label">主观题(AI)</span>
        <span class="value">{{ subjectiveAiScores.average_score?.toFixed(1) || 'N/A' }}</span>
      </div>
      <div class="score-item">
        <span class="label">主观题(人工)</span>
        <span class="value">{{ subjectiveHumanScores.average_score?.toFixed(1) || 'N/A' }}</span>
      </div>
    </div>
  </el-card>
</template>
```

#### 3.2.2 详细评分表格
```vue
<template>
  <el-table :data="detailedScores" class="detailed-scores-table">
    <el-table-column prop="questionType" label="题型" width="120">
      <template #default="{ row }">
        <el-tag :type="getQuestionTypeTag(row.questionType)">
          {{ getQuestionTypeName(row.questionType) }}
        </el-tag>
      </template>
    </el-table-column>
    
    <el-table-column prop="totalAnswers" label="回答数" width="100" />
    
    <el-table-column prop="averageScore" label="平均分" width="100">
      <template #default="{ row }">
        <span :class="getScoreClass(row.averageScore)">
          {{ row.averageScore?.toFixed(1) || 'N/A' }}
        </span>
      </template>
    </el-table-column>
    
    <el-table-column prop="maxScore" label="最高分" width="100" />
    <el-table-column prop="minScore" label="最低分" width="100" />
    
    <el-table-column label="评测标准" min-width="200">
      <template #default="{ row }">
        <div v-if="row.criteriaScores && row.criteriaScores.length > 0">
          <el-tag 
            v-for="criteria in row.criteriaScores" 
            :key="criteria.criterion_name"
            size="small"
            class="criteria-tag"
          >
            {{ criteria.criterion_name }}: {{ criteria.average_score.toFixed(1) }}
          </el-tag>
        </div>
        <span v-else class="no-criteria">无详细标准</span>
      </template>
    </el-table-column>
    
    <el-table-column label="操作" width="150">
      <template #default="{ row }">
        <el-button size="small" @click="viewDetails(row)">查看详情</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
```

### 3.3 图表展示

#### 3.3.1 评分分布图
```javascript
// 评分分布柱状图配置
getScoreDistributionOption() {
  return {
    title: {
      text: '评分分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: ['0-60', '60-70', '70-80', '80-90', '90-100']
    },
    yAxis: {
      type: 'value',
      name: '模型数量'
    },
    series: [
      {
        name: '客观题',
        type: 'bar',
        data: this.objectiveDistribution,
        itemStyle: { color: '#5470c6' }
      },
      {
        name: '主观题(AI)',
        type: 'bar', 
        data: this.subjectiveAiDistribution,
        itemStyle: { color: '#91cc75' }
      },
      {
        name: '主观题(人工)',
        type: 'bar',
        data: this.subjectiveHumanDistribution,
        itemStyle: { color: '#fac858' }
      }
    ]
  }
}
```

#### 3.3.2 雷达图对比
```javascript
// 多维度雷达图配置
getRadarChartOption() {
  return {
    title: {
      text: '评测维度对比',
      left: 'center'
    },
    tooltip: {},
    radar: {
      indicator: [
        { name: '专业性', max: 100 },
        { name: '完整性', max: 100 },
        { name: '逻辑性', max: 100 },
        { name: '实用性', max: 100 },
        { name: '创新性', max: 100 }
      ]
    },
    series: [
      {
        name: '评测维度',
        type: 'radar',
        data: this.selectedModels.map(model => ({
          value: this.getModelCriteriaScores(model.id),
          name: model.name,
          itemStyle: { color: this.getModelColor(model.id) }
        }))
      }
    ]
  }
}
```

### 3.4 交互功能实现

#### 3.4.1 模型筛选
```javascript
// 模型筛选功能
async filterModels() {
  this.loading = true
  try {
    const response = await this.$api.get(`/evaluations/batch/${this.batchId}/comprehensive-scores`, {
      params: {
        modelIds: this.selectedModelIds.join(','),
        page: 0,
        size: this.pageSize
      }
    })
    
    this.batchData = response.data
    this.updateCharts()
    
  } catch (error) {
    this.$message.error('筛选失败')
  } finally {
    this.loading = false
  }
}
```

#### 3.4.2 详情查看
```javascript
// 查看详细评分
async viewModelDetails(modelId) {
  try {
    // 获取客观题详情
    const objectiveResponse = await this.$api.get('/evaluations/objective/results', {
      params: {
        batchId: this.batchId,
        modelIds: [modelId],
        page: 0,
        size: 100
      }
    })
    
    // 获取主观题详情
    const subjectiveResponse = await this.$api.get('/evaluations/subjective/results', {
      params: {
        batchId: this.batchId,
        modelIds: [modelId],
        page: 0,
        size: 100
      }
    })
    
    // 显示详情弹窗
    this.showDetailDialog = true
    this.detailData = {
      objective: objectiveResponse.data,
      subjective: subjectiveResponse.data
    }
    
  } catch (error) {
    this.$message.error('获取详情失败')
  }
}
```

#### 3.4.3 模型对比
```javascript
// 模型对比功能
compareModels(modelIds) {
  if (modelIds.length < 2) {
    this.$message.warning('请至少选择2个模型进行对比')
    return
  }
  
  // 跳转到对比页面
  this.$router.push({
    name: 'ModelComparison',
    query: {
      batchId: this.batchId,
      modelIds: modelIds.join(',')
    }
  })
}
```

### 3.5 工具函数

```javascript
// 评分颜色映射
getScoreColor(score) {
  if (score >= 90) return '#67C23A'      // 绿色
  if (score >= 80) return '#E6A23C'      // 橙色  
  if (score >= 70) return '#F56C6C'      // 红色
  return '#909399'                       // 灰色
}

// 问题类型名称映射
getQuestionTypeName(type) {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题', 
    'SIMPLE_FACT': '简单事实题',
    'SUBJECTIVE': '主观题'
  }
  return typeMap[type] || type
}

// 评测者类型映射
getEvaluatorTypeName(type) {
  const typeMap = {
    'AI_MODEL': 'AI评测',
    'HUMAN': '人工评测'
  }
  return typeMap[type] || type
}

// 计算评分分布
calculateScoreDistribution() {
  const distribution = {
    '0-60': 0,
    '60-70': 0, 
    '70-80': 0,
    '80-90': 0,
    '90-100': 0
  }
  
  this.batchData.modelScores.forEach(model => {
    const score = model.overallScore
    if (score < 60) distribution['0-60']++
    else if (score < 70) distribution['60-70']++
    else if (score < 80) distribution['70-80']++
    else if (score < 90) distribution['80-90']++
    else distribution['90-100']++
  })
  
  return distribution
}
```

## 4. 使用场景示例

### 4.1 场景1：查看批次总体情况
```javascript
// 1. 加载批次综合数据（不指定modelIds，获取所有模型）
const batchData = await this.$api.get(`/evaluations/batch/123/comprehensive-scores`)

// 2. 展示概览信息
this.displayOverview(batchData.overview)

// 3. 展示模型排名
this.displayModelRankings(batchData.modelScores)
```

### 4.2 场景2：对比特定模型
```javascript
// 1. 筛选特定模型（指定modelIds，只获取指定模型）
const selectedModels = [1, 2, 3]
const comparisonData = await this.$api.get(`/evaluations/batch/123/comprehensive-scores`, {
  params: { modelIds: selectedModels.join(',') }
})

// 2. 生成对比图表
this.generateComparisonCharts(comparisonData.modelScores)
```

### 4.3 场景3：查看评测进度
```javascript
// 1. 获取进度信息
const progress = await this.$api.get(`/evaluations/batch/subjective/123/progress`, {
  params: { evaluatorId: 5 }
})

// 2. 显示进度条
this.updateProgressBar(progress.progressPercentage)

// 3. 定时刷新进度
setInterval(() => {
  this.refreshProgress()
}, 5000)
```

## 5. 错误处理

### 5.1 常见错误码
```javascript
const ERROR_CODES = {
  404: '批次不存在',
  400: '参数错误',
  500: '服务器内部错误',
  403: '权限不足'
}

// 统一错误处理
handleApiError(error) {
  const message = ERROR_CODES[error.status] || '未知错误'
  this.$message.error(message)
  
  // 记录错误日志
  console.error('API Error:', error)
}
```

### 5.2 数据验证
```javascript
// 验证批次数据完整性
validateBatchData(data) {
  if (!data.batchInfo) {
    throw new Error('批次信息缺失')
  }
  
  if (!data.modelScores || data.modelScores.length === 0) {
    throw new Error('模型评分数据缺失')
  }
  
  return true
}
```

这个详细的设计方案提供了完整的接口文档、数据结构说明和前端实现指南，前端开发者可以根据这个文档直接进行开发。 