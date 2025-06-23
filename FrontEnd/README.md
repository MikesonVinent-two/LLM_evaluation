# 数据库课程设计 - 前端项目

## 📋 项目简介

这是一个基于 Vue 3 + TypeScript + Element Plus 的现代化前端应用，用于数据库课程设计的大模型评测系统。系统支持多角色用户管理、实时数据监控、批次处理等功能。

## 🛠 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.0+
- **构建工具**: Vite 5.0+
- **UI组件库**: Element Plus 2.4+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **HTTP客户端**: Axios 1.6+
- **WebSocket**: @stomp/stompjs 7.0+
- **图表**: ECharts 5.4+
- **样式**: CSS3 + Element Plus主题

## 📋 环境要求

### 必需环境
- **Node.js**: >= 18.0.0 (推荐 18.17.0 或更高版本)
- **npm**: >= 9.0.0 (或 yarn >= 1.22.0)
- **现代浏览器**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

### 推荐开发工具
- **IDE**: VS Code + Volar 插件
- **浏览器**: Chrome DevTools
- **Git**: >= 2.30.0

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/MikesonVinent-two/DB-H-PJFrontEnd.git
cd DB-H-PJFrontEnd
```

### 2. 安装依赖
```bash
# 使用 npm
npm install

# 或使用 yarn
yarn install

# 或使用 pnpm (推荐)
pnpm install
```

### 3. 环境配置
复制环境配置文件并根据需要修改：
```bash
cp .env.example .env.local
```

编辑 `.env.local` 文件：
```env
# 应用配置
VITE_APP_TITLE=大模型评测系统
VITE_APP_ENV=development

# API配置
VITE_API_BASE_URL=http://localhost:8080

# WebSocket配置
VITE_WS_URL=ws://localhost:8080/api/ws
```

### 4. 启动开发服务器
```bash
npm run dev
```

访问 http://localhost:5173 查看应用

## 📦 可用脚本

```bash
# 开发环境启动
npm run dev

# 生产环境构建
npm run build

# 预览生产构建
npm run preview

# 类型检查
npm run type-check

# 代码格式化
npm run format

# 代码检查
npm run lint

# 修复代码问题
npm run lint:fix

# 清理依赖
npm run clean
```

## 🏗 项目结构

```
FrontEnd/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API接口定义
│   │   ├── index.ts       # API基础配置
│   │   ├── user.ts        # 用户相关API
│   │   ├── evaluations.ts # 评测相关API
│   │   └── ...
│   ├── components/        # 公共组件
│   │   ├── WebSocketStatus.vue
│   │   ├── BatchStatusMonitor.vue
│   │   └── ...
│   ├── config/           # 配置文件
│   │   ├── index.ts      # 应用配置
│   │   └── workspaceRoles.ts
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # 状态管理
│   │   ├── websocket.ts  # WebSocket状态
│   │   └── ...
│   ├── services/         # 服务层
│   │   ├── websocket.ts  # WebSocket服务
│   │   └── ...
│   ├── types/            # 类型定义
│   │   ├── websocketTypes.ts
│   │   └── ...
│   ├── views/            # 页面组件
│   │   ├── admin/        # 管理员页面
│   │   ├── assessment/   # 评测页面
│   │   ├── curator/      # 策展人页面
│   │   └── ...
│   ├── App.vue           # 根组件
│   └── main.ts           # 应用入口
├── .env.example          # 环境变量示例
├── package.json          # 项目配置
├── tsconfig.json         # TypeScript配置
├── vite.config.ts        # Vite配置
└── README.md             # 项目文档
```

## 🔧 功能模块

### 用户管理
- 多角色登录 (管理员、策展人、专家、标注员等)
- 用户注册和权限管理
- 个人资料管理

### 数据管理
- 原始问题管理
- 标准化问题处理
- 批量数据导入
- 数据集版本管理

### 评测系统
- 客观题自动评测
- 主观题人工评测
- 批次评测管理
- 实时进度监控

### 实时监控
- WebSocket实时通信
- 批次状态监控
- 进度追踪
- 错误日志记录

### 评分展示
- 模型排名展示
- 详细评分分析
- 图表可视化
- 数据导出

## 🌐 API 接口

### 基础配置
- 基础URL: `http://localhost:8080`
- 请求超时: 100秒 (LLM请求: 600秒)
- 认证方式: Token认证

### 主要接口模块
- `/api/users/*` - 用户管理
- `/api/evaluations/*` - 评测相关
- `/api/answer-generation/*` - 答案生成
- `/api/llm-models/*` - 模型管理
- `/api/datasets/*` - 数据集管理

## 🔌 WebSocket 功能

### 连接配置
- WebSocket URL: `ws://localhost:8080/api/ws`
- 协议: STOMP over WebSocket
- 自动重连: 支持 (最多5次)
- 心跳检测: 4秒间隔

### 消息类型
- `STATUS_CHANGE` - 状态变更
- `PROGRESS_UPDATE` - 进度更新
- `QUESTION_COMPLETED` - 问题完成
- `TASK_COMPLETED` - 任务完成
- `ERROR` - 错误消息

### 测试页面
访问 `/websocket-test` 可以测试WebSocket功能

## 🎨 主题定制

### Element Plus 主题
项目使用 Element Plus 默认主题，可以通过以下方式自定义：

```scss
// src/styles/element-variables.scss
@forward 'element-plus/theme-chalk/src/common/var.scss' with (
  $colors: (
    'primary': (
      'base': #409eff,
    ),
  ),
);
```

### 自定义样式
全局样式文件位于 `src/styles/` 目录下

## 🔍 开发调试

### 开发工具
1. **Vue DevTools**: 浏览器扩展，用于调试Vue组件
2. **Network面板**: 监控API请求
3. **Console面板**: 查看日志输出
4. **WebSocket面板**: 监控WebSocket连接

### 常用调试技巧
```typescript
// 在组件中添加调试日志
console.log('组件数据:', toRaw(data))

// 监听响应式数据变化
watch(() => someData.value, (newVal) => {
  console.log('数据变化:', newVal)
}, { deep: true })
```

## 📱 响应式设计

项目支持多种屏幕尺寸：
- **桌面端**: >= 1200px
- **平板端**: 768px - 1199px  
- **移动端**: < 768px

## 🚀 部署指南

### 生产环境构建
```bash
npm run build
```

### 部署到静态服务器
```bash
# 构建完成后，dist目录包含所有静态文件
# 可以部署到 Nginx、Apache 或 CDN

# Nginx 配置示例
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend-server:8080;
    }
}
```

### Docker 部署
```dockerfile
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🐛 常见问题

### 1. 安装依赖失败
```bash
# 清理缓存后重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 2. 开发服务器启动失败
- 检查端口是否被占用
- 确认Node.js版本是否符合要求
- 检查环境变量配置

### 3. WebSocket连接失败
- 确认后端服务是否启动
- 检查WebSocket URL配置
- 查看浏览器控制台错误信息

### 4. API请求失败
- 检查后端服务状态
- 确认API基础URL配置
- 检查网络连接和防火墙设置

## 📞 技术支持

如果遇到问题，请按以下步骤排查：

1. 查看浏览器控制台错误信息
2. 检查网络请求状态
3. 确认环境配置是否正确
4. 查看项目文档和代码注释

## 📄 许可证

本项目仅用于教育目的，请勿用于商业用途。

## 🔄 更新日志

### v1.0.0 (2024-01-XX)
- 初始版本发布
- 基础功能实现
- WebSocket实时通信
- 多角色权限管理

---

**开发团队**: 数据库课程设计小组  
**最后更新**: 2024年1月


