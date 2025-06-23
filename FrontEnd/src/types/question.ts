/**
 * 问题来源接口
 */
export interface QuestionSource {
  /**
   * 问题来源URL
   */
  sourceUrl: string;

  /**
   * 来源网站名称
   */
  sourceSite: string;

  /**
   * 问题标题
   */
  title: string;

  /**
   * 问题详细内容
   */
  content: string;

  /**
   * 其他元数据（JSON字符串）
   */
  otherMetadata: string;

  /**
   * 问题标签
   */
  tags: string[];
}

/**
 * 用户角色枚举
 */
export enum UserRole {
  ADMIN = 'ADMIN',           // 管理员
  CURATOR = 'CURATOR',       // 策展人
  EXPERT = 'EXPERT',         // 专家
  ANNOTATOR = 'ANNOTATOR',   // 标注员
  REFEREE = 'REFEREE',       // 审核员
  CROWDSOURCE_USER = 'CROWDSOURCE_USER'  // 众包用户
}

/**
 * 用户信息接口
 */
export interface User {
  /**
   * 用户ID
   */
  id: number;

  /**
   * 用户名
   */
  username: string;

  /**
   * 密码（加密后）
   */
  password: string;

  /**
   * 姓名
   */
  name: string;

  /**
   * 联系信息
   */
  contactInfo: string;

  /**
   * 用户角色
   */
  role: UserRole;

  /**
   * 创建时间
   */
  createdAt: string;

  /**
   * 更新时间
   */
  updatedAt: string;
}

/**
 * 标签类型接口
 */
export interface Tag {
  /**
   * 标签ID
   */
  id: number;

  /**
   * 标签名称
   */
  tagName: string;

  /**
   * 标签类型
   */
  tagType?: string;

  /**
   * 标签描述
   */
  description?: string;

  /**
   * 创建时间
   */
  createdAt: string;

  /**
   * 创建者
   */
  createdByUser?: User;
}

/**
 * 问题标签关联接口
 */
export interface QuestionTag {
  /**
   * 关联ID
   */
  id: number;

  /**
   * 标签信息
   */
  tag: Tag;

  /**
   * 创建时间
   */
  createdAt: string;
}

/**
 * 问题详细信息接口
 */
export interface Question {
  /**
   * 问题ID
   */
  id: number;

  /**
   * 问题来源URL
   */
  sourceUrl: string;

  /**
   * 来源网站名称
   */
  sourceSite: string;

  /**
   * 问题标题
   */
  title: string;

  /**
   * 问题详细内容
   */
  content: string;

  /**
   * 爬取时间
   */
  crawlTime: string;

  /**
   * 标签字符串（JSON格式）
   */
  tags: string;

  /**
   * 其他元数据（JSON字符串）
   */
  otherMetadata: string;

  /**
   * 问题标签关联列表
   */
  questionTags: QuestionTag[];
}

/**
 * 问题来源示例
 * {
 *   "sourceUrl": "https://example.com/question/123222111",
 *   "sourceSite": "示例网站",
 *   "title": "问题标题",
 *   "content": "问题详细内容",
 *   "otherMetadata": "{\"originalId\": \"123\", \"author\": \"张三\"}",
 *   "tags": ["糖尿病", "并发症", "嘻嘻"]
 * }
 */

/**
 * 问题详细信息示例
 * {
 *    "id": 3,
 *    "sourceUrl": "https://example.com/question/123222111",
 *    "sourceSite": "示例网站",
 *    "title": "问题标题",
 *    "content": "问题详细内容",
 *    "crawlTime": "2025-05-30T12:49:41.9074696",
 *    "tags": "[\"糖尿病\",\"并发症\",\"嘻嘻\"]",
 *    "otherMetadata": "{\"originalId\": \"123\", \"author\": \"张三\"}",
 *    "questionTags": [
 *        {
 *            "id": 7,
 *            "tag": {
 *                "id": 5,
 *                "tagName": "糖尿病",
 *                "tagType": "疾病",
 *                "description": "糖尿病相关问题",
 *                "createdAt": "2025-05-28T22:25:30",
 *                "createdByUser": {
 *                    "id": 1,
 *                    "username": "admin",
 *                    "password": "$2a$10$rJELrG6LpF1WC1UPD8kZPeYwZR0dDiXXJ5qpkWP0JKZWzKFg3ydJi",
 *                    "name": "系统管理员",
 *                    "contactInfo": "admin@example.com",
 *                    "role": "ADMIN",
 *                    "createdAt": "2025-05-28T22:25:30",
 *                    "updatedAt": "2025-05-28T22:25:30"
 *                }
 *            },
 *            "createdAt": "2025-05-30T12:49:41.9099864"
 *        },
 *        {
 *            "id": 8,
 *            "tag": {
 *                "id": 9,
 *                "tagName": "并发症",
 *                "createdAt": "2025-05-30T12:38:38"
 *            },
 *            "createdAt": "2025-05-30T12:49:41.910986"
 *        },
 *        {
 *            "id": 9,
 *            "tag": {
 *                "id": 10,
 *                "tagName": "嘻嘻",
 *                "createdAt": "2025-05-30T12:49:41.9119852"
 *            },
 *            "createdAt": "2025-05-30T12:49:41.9129849"
 *        }
 *    ]
 * }
 */
