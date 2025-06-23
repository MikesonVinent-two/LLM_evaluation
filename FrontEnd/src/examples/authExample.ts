import { login, register, logout, UserRole, UserInfo, LogoutData } from '@/api/user'

/**
 * 登录示例
 */
export const loginExample = async () => {
  try {
    const loginData = {
      username: 'testuser',
      password: '123456'
    }

    const userInfo = await login(loginData)

    console.log('登录成功，用户信息:', userInfo)

    // 将用户信息存储到本地存储
    localStorage.setItem('user', JSON.stringify(userInfo))

    return userInfo
  } catch (error) {
    console.error('登录失败:', error)
    throw error
  }
}

/**
 * 注册示例
 */
export const registerExample = async () => {
  try {
    const registerData = {
      username: 'newuser',
      password: '123456',
      contactInfo: '13812345678',
      role: UserRole.CROWDSOURCE_USER
    }

    const userInfo = await register(registerData)

    console.log('注册成功，用户信息:', userInfo)

    // 注册成功后自动登录
    localStorage.setItem('user', JSON.stringify(userInfo))

    return userInfo
  } catch (error) {
    console.error('注册失败:', error)
    throw error
  }
}

/**
 * 处理用户信息示例
 */
export const handleUserInfo = (userInfo: UserInfo) => {
  // 根据用户角色执行不同操作
  switch (userInfo.role) {
    case UserRole.ADMIN:
      console.log('管理员用户，显示管理面板')
      break
    case UserRole.EXPERT:
      console.log('专家用户，显示专家工作台')
      break
    case UserRole.CROWDSOURCE_USER:
      console.log('众包用户，显示众包任务列表')
      break
    case UserRole.CURATOR:
      console.log('策展人用户，显示内容管理界面')
      break
    case UserRole.ANNOTATOR:
      console.log('标注员用户，显示标注任务')
      break
    case UserRole.REFEREE:
      console.log('审核员用户，显示审核任务')
      break
    default:
      console.log('未知角色，显示默认界面')
  }

  // 显示用户基本信息
  console.log(`用户名: ${userInfo.username}`)
  console.log(`联系方式: ${userInfo.contactInfo}`)
  console.log(`用户ID: ${userInfo.id}`)

  // 如果有姓名，显示姓名
  if (userInfo.name) {
    console.log(`姓名: ${userInfo.name}`)
  }
}

/**
 * 登出示例
 */
export const logoutExample = async () => {
  try {
    // 从本地存储获取用户信息
    const userJson = localStorage.getItem('user')
    if (!userJson) {
      throw new Error('未登录')
    }

    const user = JSON.parse(userJson) as UserInfo

    // 构建登出请求数据
    const logoutData: LogoutData = {
      id: user.id,
      username: user.username
    }

    // 调用登出接口
    const response = await logout(logoutData)

    console.log('登出成功:', response.message)

    // 清除本地存储中的用户信息
    localStorage.removeItem('user')

    return response
  } catch (error) {
    console.error('登出失败:', error)
    throw error
  }
}
