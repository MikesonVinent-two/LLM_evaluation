import { getRawQuestions } from '@/api/rawData'

/**
 * 获取所有原始问题示例
 */
export const fetchRawQuestionsExample = async () => {
  try {
    // 获取第一页，每页10条记录，按id降序排序
    const response = await getRawQuestions({
      page: 0,
      size: 10,
      sort: 'id,desc'
    })

    console.log('原始问题总数:', response.totalElements)
    console.log('总页数:', response.totalPages)
    console.log('当前页问题列表:', response.content)

    // 处理问题数据
    response.content.forEach(question => {
      console.log(`问题ID: ${question.id}`)
      console.log(`标题: ${question.title}`)
      console.log(`内容: ${question.content}`)
      console.log(`来源: ${question.sourceSite}`)
      console.log(`标签: ${question.tags.join(', ')}`)
      console.log(`是否已标准化: ${question.standardized ? '是' : '否'}`)
      console.log('-------------------')
    })

    return response
  } catch (error) {
    console.error('获取原始问题失败:', error)
    throw error
  }
}

/**
 * 分页获取所有原始问题示例
 */
export const paginatedRawQuestionsExample = async () => {
  // 初始页码和每页大小
  let currentPage = 0
  const pageSize = 20
  let hasMorePages = true

  try {
    while (hasMorePages) {
      const response = await getRawQuestions({
        page: currentPage,
        size: pageSize,
        sort: 'crawlTime,desc' // 按爬取时间降序排序
      })

      console.log(`第 ${currentPage + 1} 页数据:`)
      response.content.forEach(question => {
        console.log(`- ${question.title} (ID: ${question.id})`)
      })

      // 检查是否还有更多页
      hasMorePages = !response.last
      currentPage++

      // 可以在这里添加延时，避免请求过于频繁
      if (hasMorePages) {
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
    }

    console.log('所有页面已获取完毕')
  } catch (error) {
    console.error('分页获取原始问题失败:', error)
    throw error
  }
}
