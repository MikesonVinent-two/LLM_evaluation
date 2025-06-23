declare module '@/api/tags' {
  /**
   * 标签推荐请求参数接口
   */
  export interface RecommendTagsRequest {
    text: string
    questionType?: string
    existingTags?: string[]
  }

  /**
   * 标签推荐响应接口
   */
  export interface RecommendTagsResponse {
    tags: string[]
    confidence: number[]
  }

  /**
   * 获取标签推荐
   * @param params 推荐参数
   * @returns 推荐的标签列表
   */
  export function recommendTags(params: RecommendTagsRequest): Promise<RecommendTagsResponse>

  /**
   * 获取所有可用标签
   */
  export function getAllTags(): Promise<{ tags: string[] }>
}
