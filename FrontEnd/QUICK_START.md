# 🚀 快速开始指南

## 📋 环境要求

- **Node.js**: >= 18.0.0
- **npm**: >= 9.0.0
- **现代浏览器**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

## ⚡ 一键安装

### Windows 用户
```bash
# 双击运行安装脚本
setup.bat
```

### Linux/macOS 用户
```bash
# 给脚本执行权限并运行
chmod +x setup.sh
./setup.sh
```

## 📝 手动安装

### 1. 安装依赖
```bash
npm install
```

### 2. 创建环境配置
创建 `.env.local` 文件：
```env
VITE_APP_TITLE=大模型评测系统
VITE_APP_ENV=development
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/api/ws
```

### 3. 启动开发服务器
```bash
npm run dev
```

## 🌐 访问应用

- **主页**: http://localhost:3000
- **WebSocket测试**: http://localhost:3000/websocket-test
- **批次监控**: http://localhost:3000/admin/batch-monitor

## 🔧 常用命令

```bash
# 开发
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run preview      # 预览构建结果

# 代码质量
npm run lint         # 代码检查
npm run format       # 代码格式化
npm run type-check   # 类型检查
```

## 🐛 常见问题

### 端口被占用
如果 3000 端口被占用，Vite 会自动选择下一个可用端口。

### 依赖安装失败
```bash
# 清理缓存重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### API 连接失败
检查后端服务是否在 `http://localhost:8080` 运行。

## 📚 更多信息

详细文档请查看 [README.md](./README.md) 