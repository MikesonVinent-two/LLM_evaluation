/**
 * 题目类型枚举
 */
export enum QuestionType {
  SINGLE_CHOICE = 'SINGLE_CHOICE', // 单选题
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE', // 多选题
  TRUE_FALSE = 'TRUE_FALSE', // 判断题
  SIMPLE_FACT = 'SIMPLE_FACT', // 简单题/填空题
  SUBJECTIVE = 'SUBJECTIVE', // 主观题/论述题
}

/**
 * 选项接口
 */
export interface Option {
  id: string;
  text: string;
}

/**
 * 答案基础接口
 */
export interface BaseAnswer {
  userId: number;
  questionType: QuestionType;
  answerText: string;
  commitMessage: string;
}

/**
 * 客观题答案接口
 */
export interface ObjectiveAnswer extends BaseAnswer {
  questionType: QuestionType.SINGLE_CHOICE | QuestionType.MULTIPLE_CHOICE | QuestionType.TRUE_FALSE;
  options: string; // JSON字符串，包含选项数组
  correctIds: string; // JSON字符串，包含正确选项ID数组
}

/**
 * 简单题/填空题答案接口
 */
export interface SimpleFactAnswer extends BaseAnswer {
  questionType: QuestionType.SIMPLE_FACT;
  alternativeAnswers: string; // JSON字符串，包含可接受的替代答案数组
}

/**
 * 主观题/论述题答案接口
 */
export interface SubjectiveAnswer extends BaseAnswer {
  questionType: QuestionType.SUBJECTIVE;
  scoringGuidance: string; // 评分指导
}

/**
 * 所有答案类型的联合类型
 */
export type Answer = ObjectiveAnswer | SimpleFactAnswer | SubjectiveAnswer;

/**
 * 客观题答案示例
 * {
 *   "userId": 1,
 *   "questionType": "SINGLE_CHOICE",
 *   "answerText": "选项A是正确的",
 *   "options": "[{\"id\":\"A\",\"text\":\"选项A\"},{\"id\":\"B\",\"text\":\"选项B\"}]",
 *   "correctIds": "[\"A\"]",
 *   "commitMessage": "更新单选题标准答案"
 * }
 */

/**
 * 简单题答案示例
 * {
 *   "userId": 1,
 *   "questionType": "SIMPLE_FACT",
 *   "answerText": "36.3-37.2°C",
 *   "alternativeAnswers": "[\"36.3-37.2度\", \"36.3到37.2摄氏度\"]",
 *   "commitMessage": "更新简单题标准答案"
 * }
 */

/**
 * 主观题答案示例
 * {
 *   "userId": 1,
 *   "questionType": "SUBJECTIVE",
 *   "answerText": "高血压的主要症状包括：\n1. 头痛，特别是后脑部\n2. 头晕和眩晕\n3. 耳鸣\n4. 心悸\n5. 疲劳\n6. 视物模糊\n7. 失眠",
 *   "scoringGuidance": "评分要点：\n1. 症状的完整性（3分）\n2. 症状的准确性（4分）\n3. 补充说明的合理性（3分）",
 *   "commitMessage": "更新主观题标准答案"
 * }
 */
