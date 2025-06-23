#!/bin/bash

# 数据库课程设计前端项目快速安装脚本
# 适用于 Linux/macOS 系统

echo "🚀 开始安装数据库课程设计前端项目..."

# 检查 Node.js 版本
echo "📋 检查环境要求..."
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装，请先安装 Node.js 18.0.0 或更高版本"
    echo "   下载地址: https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2)
REQUIRED_VERSION="18.0.0"

if [ "$(printf '%s\n' "$REQUIRED_VERSION" "$NODE_VERSION" | sort -V | head -n1)" != "$REQUIRED_VERSION" ]; then
    echo "❌ Node.js 版本过低，当前版本: $NODE_VERSION，要求版本: >= $REQUIRED_VERSION"
    exit 1
fi

echo "✅ Node.js 版本检查通过: $NODE_VERSION"

# 检查 npm 版本
if ! command -v npm &> /dev/null; then
    echo "❌ npm 未安装"
    exit 1
fi

NPM_VERSION=$(npm -v)
echo "✅ npm 版本: $NPM_VERSION"

# 安装依赖
echo "📦 安装项目依赖..."
npm install

if [ $? -ne 0 ]; then
    echo "❌ 依赖安装失败，尝试清理缓存后重新安装..."
    npm cache clean --force
    rm -rf node_modules package-lock.json
    npm install
    
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败，请检查网络连接或手动安装"
        exit 1
    fi
fi

echo "✅ 依赖安装完成"

# 创建环境配置文件
echo "⚙️  配置环境变量..."
if [ ! -f ".env.local" ]; then
    cat > .env.local << EOF
# 应用配置
VITE_APP_TITLE=大模型评测系统
VITE_APP_ENV=development

# API配置
VITE_API_BASE_URL=http://localhost:8080

# WebSocket配置
VITE_WS_URL=ws://localhost:8080/api/ws

# 开发配置
VITE_DEV_PORT=3000
VITE_DEV_HOST=localhost

# 调试配置
VITE_DEBUG=false
VITE_LOG_LEVEL=info
EOF
    echo "✅ 环境配置文件已创建: .env.local"
else
    echo "✅ 环境配置文件已存在: .env.local"
fi

# 类型检查
echo "🔍 执行类型检查..."
npm run type-check

if [ $? -ne 0 ]; then
    echo "⚠️  类型检查发现问题，但不影响运行"
fi

echo ""
echo "🎉 安装完成！"
echo ""
echo "📋 可用命令:"
echo "  npm run dev      - 启动开发服务器"
echo "  npm run build    - 构建生产版本"
echo "  npm run preview  - 预览生产构建"
echo "  npm run lint     - 代码检查"
echo "  npm run format   - 代码格式化"
echo ""
echo "🌐 访问地址:"
echo "  开发环境: http://localhost:3000"
echo "  WebSocket测试: http://localhost:3000/websocket-test"
echo ""
echo "📚 更多信息请查看 README.md"
echo ""

# 询问是否立即启动开发服务器
read -p "是否立即启动开发服务器? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🚀 启动开发服务器..."
    npm run dev
fi 