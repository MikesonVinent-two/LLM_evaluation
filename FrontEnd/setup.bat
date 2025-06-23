@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo 🚀 开始安装数据库课程设计前端项目...
echo.

:: 检查 Node.js 是否安装
echo 📋 检查环境要求...
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Node.js 未安装，请先安装 Node.js 18.0.0 或更高版本
    echo    下载地址: https://nodejs.org/
    pause
    exit /b 1
)

:: 获取 Node.js 版本
for /f "tokens=1" %%i in ('node -v') do set NODE_VERSION=%%i
set NODE_VERSION=%NODE_VERSION:v=%
echo ✅ Node.js 版本检查通过: %NODE_VERSION%

:: 检查 npm 是否安装
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ npm 未安装
    pause
    exit /b 1
)

:: 获取 npm 版本
for /f "tokens=1" %%i in ('npm -v') do set NPM_VERSION=%%i
echo ✅ npm 版本: %NPM_VERSION%
echo.

:: 安装依赖
echo 📦 安装项目依赖...
call npm install

if %errorlevel% neq 0 (
    echo ❌ 依赖安装失败，尝试清理缓存后重新安装...
    call npm cache clean --force
    if exist node_modules rmdir /s /q node_modules
    if exist package-lock.json del package-lock.json
    call npm install
    
    if %errorlevel% neq 0 (
        echo ❌ 依赖安装失败，请检查网络连接或手动安装
        pause
        exit /b 1
    )
)

echo ✅ 依赖安装完成
echo.

:: 创建环境配置文件
echo ⚙️  配置环境变量...
if not exist ".env.local" (
    (
        echo # 应用配置
        echo VITE_APP_TITLE=大模型评测系统
        echo VITE_APP_ENV=development
        echo.
        echo # API配置
        echo VITE_API_BASE_URL=http://localhost:8080
        echo.
        echo # WebSocket配置
        echo VITE_WS_URL=ws://localhost:8080/api/ws
        echo.
        echo # 开发配置
        echo VITE_DEV_PORT=3000
        echo VITE_DEV_HOST=localhost
        echo.
        echo # 调试配置
        echo VITE_DEBUG=false
        echo VITE_LOG_LEVEL=info
    ) > .env.local
    echo ✅ 环境配置文件已创建: .env.local
) else (
    echo ✅ 环境配置文件已存在: .env.local
)
echo.

:: 类型检查
echo 🔍 执行类型检查...
call npm run type-check
if %errorlevel% neq 0 (
    echo ⚠️  类型检查发现问题，但不影响运行
)
echo.

echo 🎉 安装完成！
echo.
echo 📋 可用命令:
echo   npm run dev      - 启动开发服务器
echo   npm run build    - 构建生产版本
echo   npm run preview  - 预览生产构建
echo   npm run lint     - 代码检查
echo   npm run format   - 代码格式化
echo.
echo 🌐 访问地址:
echo   开发环境: http://localhost:3000
echo   WebSocket测试: http://localhost:3000/websocket-test
echo.
echo 📚 更多信息请查看 README.md
echo.

:: 询问是否立即启动开发服务器
set /p choice="是否立即启动开发服务器? (y/n): "
if /i "%choice%"=="y" (
    echo 🚀 启动开发服务器...
    call npm run dev
) else if /i "%choice%"=="yes" (
    echo 🚀 启动开发服务器...
    call npm run dev
)

pause 