# LLM 评估系统

一个完整的大语言模型（LLM）评估系统，包含前端界面和后端API服务。系统集成了现代化的分布式架构技术栈，支持多角色用户管理、实时数据监控、批次处理等功能。

## 📋 项目概述

本项目是一个基于Spring Boot + Vue 3的医疗问答系统，采用前后端分离架构，支持问题标准化、答案生成、评估体系等核心功能。系统具备完整的用户权限管理、WebSocket实时通信、Redis缓存优化等特性。

## 🏗 项目结构

```
LLM_evaluation/
├── .gitignore              # 综合的忽略规则文件
├── README.md               # 项目总体说明文档  
├── BackEnd/                # 后端服务 (Spring Boot)
│   ├── src/main/java/      # Java源代码
│   ├── src/main/resources/ # 配置文件和资源
│   ├── mysql/              # 数据库脚本
│   ├── build.gradle        # Gradle构建配置
│   ├── start-app.bat       # Windows启动脚本
│   └── 项目使用说明.md     # 后端详细文档
└── FrontEnd/               # 前端界面 (Vue.js)
    ├── src/                # Vue源代码
    ├── public/             # 静态资源
    ├── package.json        # npm依赖配置
    ├── vite.config.ts      # Vite构建配置
    └── README.md           # 前端详细文档
```

## 🛠 技术栈

### 后端技术
- **框架**: Spring Boot 3.x
- **语言**: Java 21
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **通信**: WebSocket (STOMP)
- **构建工具**: Gradle 8.x
- **其他**: Spring Security, JPA, Jackson

### 前端技术
- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.0+
- **构建工具**: Vite 5.0+
- **UI组件库**: Element Plus 2.4+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **HTTP客户端**: Axios 1.6+
- **WebSocket**: @stomp/stompjs 7.0+
- **图表**: ECharts 5.4+

## 📋 环境要求

### 必需环境
- **Java**: JDK 21 或以上版本
- **Node.js**: >= 18.0.0 (推荐 18.17.0 或更高版本)
- **MySQL**: 8.0 或以上版本
- **Redis**: 6.0 或以上版本
- **操作系统**: Windows 10/11, macOS, 或 Linux

### 推荐配置
- **内存**: 8GB 或以上
- **硬盘空间**: 至少1GB可用空间
- **浏览器**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

### 推荐开发工具
- **IDE**: IntelliJ IDEA / VS Code + Volar 插件
- **数据库工具**: MySQL Workbench / Navicat
- **API测试**: Postman / Apifox
- **Git**: >= 2.30.0

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone git@github.com:MikesonVinent-two/LLM_evaluation.git
cd LLM_evaluation
```

### 2. 环境准备

#### Java 21 安装与配置
1. 下载并安装 JDK 21
2. 配置环境变量 `JAVA_HOME`
3. 验证安装：
   ```bash
   java -version
   javac -version
   ```

#### Node.js 安装
1. 安装 Node.js >= 18.0.0
2. 验证安装：
   ```bash
   node -v
   npm -v
   ```

#### MySQL 8.0 安装与配置
1. 安装 MySQL 8.0 数据库服务
2. 启动 MySQL 服务
3. 创建数据库：
   ```sql
   CREATE DATABASE demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

#### Redis 安装与配置
1. 安装 Redis 服务
2. 启动 Redis 服务（默认端口6379）

### 3. 后端配置与启动

#### 数据库配置
**⚠️ 重要**: 修改 `BackEnd/src/main/resources/application.yml` 中的数据库密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&createDatabaseIfNotExist=true
    username: root
    password: 您的MySQL密码  # 请修改为您实际的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 初始化数据库
```bash
cd BackEnd

# 执行建表脚本
mysql -u root -p demo < mysql/create_tables.sql

# 插入示例数据
mysql -u root -p demo < mysql/insert_sample_data.sql
```

#### 验证数据库
```sql
USE demo;
SELECT COUNT(*) FROM USERS;        -- 应该有3条用户记录
SELECT COUNT(*) FROM TAGS;         -- 应该有20条标签记录
SELECT COUNT(*) FROM RAW_QUESTIONS; -- 应该有41条原始问题记录
```

#### 启动后端服务
```bash
cd BackEnd

# Windows环境（推荐）
start-app.bat

# 或者手动运行
gradlew.bat bootRun
```

#### 验证后端启动
- 访问 http://localhost:8080/api 验证服务启动
- 检查WebSocket连接：ws://localhost:8080/api/ws

### 4. 前端配置与启动

#### 安装依赖
```bash
cd FrontEnd

# 使用 npm
npm install

# 或使用 yarn
yarn install

# 或使用 pnpm (推荐)
pnpm install
```

#### 环境配置
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

#### 启动前端服务
```bash
cd FrontEnd
npm run dev
```

访问 http://localhost:5173 查看应用

## 👥 系统用户

系统预置了三个测试用户：

| 用户名 | 密码   | 角色            | 说明           |
|--------|--------|-----------------|----------------|
| admin  | 123456 | ADMIN           | 系统管理员     |
| expert | 123456 | EXPERT          | 专家用户       |
| user   | 123456 | CROWDSOURCE_USER| 普通用户       |

**注意**: 
- 所有用户的初始密码均为：`123456`
- 密码在数据库中经过BCrypt加密存储
- 用户具有不同的权限级别，可以体验不同的功能

## 📦 可用脚本

### 后端脚本
```bash
cd BackEnd

# 启动服务
start-app.bat        # Windows推荐方式
gradlew.bat bootRun  # 或手动启动

# 构建项目
gradlew.bat build

# 清理构建
gradlew.bat clean
```

### 前端脚本
```bash
cd FrontEnd

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
```

## 🔧 核心功能模块

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
- **基础URL**: `http://localhost:8080/api`
- **WebSocket**: `ws://localhost:8080/api/ws`
- **认证方式**: Token认证
- **请求超时**: 100秒 (LLM请求: 600秒)

### 主要接口模块
- `/api/users/*` - 用户管理
- `/api/evaluations/*` - 评测相关
- `/api/answer-generation/*` - 答案生成
- `/api/llm-models/*` - 模型管理
- `/api/datasets/*` - 数据集管理

## 🔌 WebSocket 功能

### 连接配置
- **WebSocket URL**: `ws://localhost:8080/api/ws`
- **协议**: STOMP over WebSocket
- **自动重连**: 支持 (最多5次)
- **心跳检测**: 4秒间隔

### 消息类型
- `STATUS_CHANGE` - 状态变更
- `PROGRESS_UPDATE` - 进度更新
- `QUESTION_COMPLETED` - 问题完成
- `TASK_COMPLETED` - 任务完成
- `ERROR` - 错误消息

## 🐛 故障排除

### 常见问题及解决方案

#### 1. 后端问题

**中文乱码问题**
- 确保使用 `start-app.bat` 启动
- 检查IDE和终端的编码设置为UTF-8

**数据库连接失败**
- 确认MySQL服务已启动
- 检查 `application.yml` 中的数据库密码是否与您的MySQL密码一致
- 确认数据库 `demo` 已创建

**Redis连接失败**
- 确认Redis服务已启动
- 检查端口6379是否被占用

**端口占用问题**
- 默认端口8080，如被占用可修改 `application.yml`
- 使用 `netstat -ano | findstr :8080` 检查端口占用

#### 2. 前端问题

**安装依赖失败**
```bash
# 清理缓存后重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

**开发服务器启动失败**
- 检查端口是否被占用
- 确认Node.js版本是否符合要求
- 检查环境变量配置

**WebSocket连接失败**
- 确认后端服务是否启动
- 检查WebSocket URL配置
- 查看浏览器控制台错误信息

**API请求失败**
- 检查后端服务状态
- 确认API基础URL配置
- 检查网络连接和防火墙设置

## 🚀 部署指南

### 开发环境
1. 按照快速开始部分配置环境
2. 分别启动后端和前端服务
3. 通过 http://localhost:5173 访问应用

### 生产环境
1. **后端部署**:
   ```bash
   cd BackEnd
   gradlew.bat build
   java -jar build/libs/demo-*.jar
   ```

2. **前端部署**:
   ```bash
   cd FrontEnd
   npm run build
   # 将 dist 目录部署到 web 服务器
   ```

## 📞 技术支持

如果遇到问题，请按以下步骤排查：

1. 查看控制台错误信息和日志输出
2. 检查网络请求状态
3. 确认环境配置是否正确
4. 参考详细文档：
   - [后端详细文档](./BackEnd/项目使用说明.md)
   - [前端详细文档](./FrontEnd/README.md)

## 📄 开发规范

### 日志查看
- **后端日志**: `BackEnd/logs/application.log`
- **前端日志**: 浏览器控制台
- **日志级别**: DEBUG（开发环境）

### 配置文件
- **后端配置**: `BackEnd/src/main/resources/application.yml`
- **前端配置**: `FrontEnd/.env.local`

## 🔄 更新日志

### v1.0.0 (2024-01-XX)
- 初始版本发布
- 前后端项目合并到统一仓库
- 基础功能实现
- WebSocket实时通信
- 多角色权限管理

---

**开发团队**: 嗯，只有我一个人 
**项目仓库**: https://github.com/MikesonVinent-two/LLM_evaluation  
**最后更新**: 2025年6月 


## 写在最后
这门课是吴毅坚的，这个老师人还是蛮好的，作业也少，就是PJ布置的太晚了，期末前一个月才布置，比较的赶，还是比较累的，不过给分还是蛮好的，认真一点，应该就有挺好的分数