import { getAllTags, recommendTags } from '@/api/tags';
import { ElMessage } from 'element-plus';

// 用于缓存标签数据，避免频繁请求
let tagsCache: string[] = [];
let lastFetchTime = 0;
const CACHE_DURATION = 5 * 60 * 1000; // 缓存有效期：5分钟

/**
 * 获取所有标签
 * @param forceRefresh 是否强制刷新缓存
 * @returns 标签列表
 */
export const fetchAllTags = async (forceRefresh = false): Promise<string[]> => {
  const now = Date.now();

  // 如果缓存有效且不强制刷新，则使用缓存
  if (!forceRefresh && tagsCache.length > 0 && (now - lastFetchTime) < CACHE_DURATION) {
    return tagsCache;
  }

  try {
    const response = await getAllTags();
    tagsCache = response.tags || [];
    lastFetchTime = now;
    return tagsCache;
  } catch (error) {
    console.error('获取标签失败:', error);
    // 如果有缓存，返回缓存数据
    if (tagsCache.length > 0) {
      ElMessage.warning('获取最新标签失败，将使用缓存数据');
      return tagsCache;
    }

    // 提供一些默认标签作为备用
    const defaultTags = ['编程', '算法', '数据结构', '网络', '数据库'];
    ElMessage.warning('获取标签列表失败，将使用默认标签');
    return defaultTags;
  }
};

/**
 * 根据文本内容生成标签建议
 * @param text 文本内容
 * @returns 建议的标签列表
 */
export const generateTagSuggestions = (text: string): string[] => {
  // TODO: 实现基于文本内容的标签推荐算法
  // 这里可以调用后端API或使用前端简单算法提取关键词
  return [];
};

/**
 * 根据文本内容获取标签推荐
 * @param text 文本内容
 * @param existingTags 已存在的标签
 * @param questionType 问题类型（可选）
 * @returns 推荐的标签列表
 */
export const getTagRecommendations = async (
  text: string,
  existingTags: string[] = [],
  questionType?: string
): Promise<string[]> => {
  try {
    const response = await recommendTags({
      text,
      existingTags,
      questionType
    });

    return response.tags || [];
  } catch (error) {
    console.error('获取标签推荐失败:', error);
    return [];
  }
};

export default {
  fetchAllTags,
  generateTagSuggestions,
  getTagRecommendations
};
