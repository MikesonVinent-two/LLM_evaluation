 import { getUserById, UserInfo } from '@/api/user'

/**
 * 获取用户信息示例
 * @param userId 用户ID
 */
export const getUserProfileExample = async (userId: number | string) => {
  try {
    const userInfo = await getUserById(userId)

    console.log('获取用户信息成功:')
    console.log(`用户ID: ${userInfo.id}`)
    console.log(`用户名: ${userInfo.username}`)
    console.log(`姓名: ${userInfo.name || '未设置'}`)
    console.log(`角色: ${userInfo.role}`)
    console.log(`联系方式: ${userInfo.contactInfo}`)
    console.log(`创建时间: ${formatDateTime(userInfo.createdAt)}`)
    console.log(`更新时间: ${formatDateTime(userInfo.updatedAt)}`)

    return userInfo
  } catch (error) {
    console.error('获取用户信息失败:', error)
    throw error
  }
}

/**
 * 格式化日期时间
 * @param dateTimeString ISO格式的日期时间字符串
 * @returns 格式化后的日期时间字符串
 */
const formatDateTime = (dateTimeString: string): string => {
  try {
    const date = new Date(dateTimeString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  } catch (error) {
    return dateTimeString // 如果解析失败，返回原字符串
  }
}

/**
 * 使用示例
 */
export const useProfileExample = () => {
  // 获取ID为1的用户信息
  getUserProfileExample(1)
    .then(userInfo => {
      // 这里可以根据用户信息进行其他操作
      console.log('用户信息获取完成')
    })
    .catch(error => {
      console.error('示例执行失败:', error)
    })
}
