# 智能问答系统后端

这是一个基于Spring Boot的智能问答系统后端项目，专门用于管理和评估大语言模型(LLM)的问答能力。系统支持多角色协作，提供从问题采集、标准化、答案生成到质量评估的完整工作流程，是一个专业的AI模型评测平台。

## 技术栈

### 核心框架
- Java 21
- Spring Boot 3.2.0
- Spring Security 6.2.0
- Spring WebSocket (STOMP协议)
- Spring Retry
- Spring Scheduling

### 数据存储
- MySQL 8.0+ (主数据库)
- Redis 7.0+ (缓存和会话存储)
- HikariCP 连接池

### 开发工具
- Gradle 8.0+
- Lombok 1.18.32
- JUnit 5
- FindBugs JSR305

### 外部集成
- OpenAI API (GPT模型)
- 支持多种LLM API接入
- WebSocket实时通信
- RESTful API设计

### 其他特性
- JDBC原生数据访问（无JPA/Hibernate）
- 软删除机制
- 变更日志追踪
- 异步任务处理
- 批量数据处理
- 实时进度推送

## 系统架构

### 核心模块
1. **用户管理模块** - 多角色权限控制
2. **数据采集模块** - 原始问答数据收集
3. **问题标准化模块** - 问题分类和标准化处理
4. **答案管理模块** - 标准答案和候选答案管理
5. **LLM集成模块** - 大语言模型API集成
6. **评估引擎** - 自动化答案质量评估
7. **实时通信模块** - WebSocket消息推送
8. **变更追踪模块** - 完整的审计日志

### 数据流程
```
原始数据采集 → 问题标准化 → 答案生成 → 质量评估 → 结果分析
     ↓              ↓           ↓          ↓         ↓
  众包标注    →   专家审核  →  LLM调用  →  多维评估  →  统计报告
```

## 主要功能

### 1. 用户管理系统
- **多角色支持**：
  - `ADMIN`：系统管理员，拥有最高权限
  - `CURATOR`：策展人，负责问题管理和标准化
  - `EXPERT`：专家，提供专业答案和评估
  - `ANNOTATOR`：标注员，负责数据标注
  - `REFEREE`：评审员，负责答案质量评估
  - `CROWDSOURCE_USER`：众包用户，参与答案收集
- **认证授权**：基于Spring Security的无状态认证
- **用户行为审计**：完整的操作日志记录

### 2. 问题管理系统
- **原始数据采集**：
  - 支持多源数据采集（网页爬虫、API接入）
  - 自动去重和数据清洗
  - 元数据提取和标签化
- **标准问题体系**：
  - 问题分类：`SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`SIMPLE_FACT`、`SUBJECTIVE`
  - 难度分级：`EASY`、`MEDIUM`、`HARD`
  - 标签管理和推荐系统
- **版本控制**：
  - 问题变更历史追踪
  - 版本回滚支持
  - 协作编辑功能

### 3. 答案管理系统
- **标准答案体系**：
  - 客观题答案：选项和正确答案管理
  - 简单题答案：事实型答案和同义词变体
  - 主观题答案：评估标准和参考答案
- **专家候选答案**：
  - 多专家答案收集
  - 答案质量评分
  - 专家反馈系统
- **众包答案收集**：
  - 任务批次管理
  - 质量控制机制
  - 审核工作流

### 4. LLM集成与答案生成
- **模型管理**：
  - 支持多种LLM模型注册
  - 模型参数配置
  - API密钥管理
- **批量答案生成**：
  - 异步批处理
  - 进度实时监控
  - 失败重试机制
- **提示词工程**：
  - 标签提示词管理
  - 题型提示词配置
  - 提示词组装配置

### 5. 评估系统
- **多维度评估标准**：
  - 准确性评估
  - 完整性评估
  - 相关性评估
  - 可读性评估
- **自动评估流程**：
  - 规则引擎评估
  - LLM辅助评估
  - 批量评估处理
- **人工评审集成**：
  - 评审任务分配
  - 评审标准统一
  - 评审结果汇总
- **统计分析**：
  - 模型性能对比
  - 评估结果可视化
  - 趋势分析报告

### 6. 实时通信系统
- **WebSocket支持**：
  - STOMP协议消息传输
  - 实时进度推送
  - 状态变更通知
- **消息类型**：
  - 任务进度更新
  - 状态变更通知
  - 错误消息推送
  - 系统通知

### 7. 数据集管理
- **版本控制**：数据集版本管理和克隆
- **问题映射**：数据集与问题的关联管理
- **批量操作**：数据集批量导入导出

### 8. 变更追踪系统
- **完整审计日志**：
  - 实体变更记录
  - 变更原因追踪
  - 变更影响分析
- **版本控制**：
  - 版本号管理
  - 版本比较功能
  - 版本回滚支持

## 数据库设计

项目使用MySQL数据库，采用JDBC原生访问，主要包含以下模块：

### 1. 用户和权限管理
- `USERS`：用户信息表，支持多角色
- 软删除支持，完整的时间戳记录

### 2. 原始数据采集
- `RAW_QUESTIONS`：原始问题表，支持JSON元数据
- `RAW_ANSWERS`：原始答案表，关联问题
- `RAW_QUESTION_TAGS`：原始问题标签关联

### 3. 变更日志系统
- `CHANGE_LOG`：变更日志主表，类似Git提交
- `CHANGE_LOG_DETAILS`：变更详情表，记录字段级变更

### 4. 标准问题体系
- `STANDARD_QUESTIONS`：标准问题表，支持版本链
- `STANDARD_QUESTION_TAGS`：问题标签关联
- `TAGS`：标签定义表

### 5. 标准答案体系
- `STANDARD_OBJECTIVE_ANSWERS`：客观题答案表
- `STANDARD_SIMPLE_ANSWERS`：简单题答案表
- `STANDARD_SUBJECTIVE_ANSWERS`：主观题答案表

### 6. LLM模型管理
- `LLM_MODELS`：模型信息表
- `MODEL_ANSWER_RUNS`：模型答案运行表
- `LLM_ANSWERS`：模型生成答案表

### 7. 评估系统
- `EVALUATION_CRITERIA`：评估标准表
- `EVALUATORS`：评估器表
- `EVALUATION_RUNS`：评估运行表
- `EVALUATIONS`：评估结果表
- `EVALUATION_DETAILS`：评估详情表

### 8. 提示词管理
- `ANSWER_TAG_PROMPTS`：答案标签提示词
- `ANSWER_QUESTION_TYPE_PROMPTS`：答案题型提示词
- `EVALUATION_TAG_PROMPTS`：评估标签提示词
- `EVALUATION_SUBJECTIVE_PROMPTS`：评估主观提示词

## API文档

### 主要API端点

#### 用户管理
- `POST /api/users/register` - 用户注册
- `POST /api/users/login` - 用户登录
- `GET /api/users` - 获取用户列表
- `PUT /api/users/{id}` - 更新用户信息

#### 问题管理
- `GET /api/standard/standard-questions` - 获取标准问题列表
- `POST /api/standard/standard-questions` - 创建标准问题
- `PUT /api/standard/standard-questions/{id}` - 更新标准问题
- `DELETE /api/standard/standard-questions/{id}` - 删除标准问题

#### 答案生成
- `POST /api/answer-generation/batches` - 创建答案生成批次
- `POST /api/answer-generation/batches/{id}/start` - 启动批次
- `POST /api/answer-generation/batches/{id}/pause` - 暂停批次
- `GET /api/answer-generation/batches/{id}/status` - 获取批次状态

#### 评估管理
- `POST /api/evaluations/runs` - 创建评估运行
- `POST /api/evaluations/runs/{id}/start` - 启动评估
- `GET /api/evaluations/runs/{id}/results` - 获取评估结果

#### LLM模型管理
- `POST /api/llm-models` - 注册LLM模型
- `GET /api/llm-models` - 获取模型列表
- `PUT /api/llm-models/{id}` - 更新模型配置

#### WebSocket端点
- `/api/ws` - WebSocket连接端点
- `/topic/batch/{batchId}` - 批次消息订阅
- `/topic/run/{runId}` - 运行消息订阅
- `/topic/global` - 全局消息订阅

## 快速开始

### 环境要求
- JDK 21
- MySQL 8.0+
- Redis 7.0+
- Gradle 8.0+
- 至少4GB内存
- 10GB可用磁盘空间

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/MikesonVinent-two/DB-H-PJBackEnd.git
cd DB-H-PJBackEnd
```

2. **配置数据库**
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE qa_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行建表脚本
mysql -u root -p qa_system < create_tables.sql

# 插入示例数据（可选）
mysql -u root -p qa_system < insert_sample_data.sql
```

3. **配置应用**
```bash
# 修改配置文件
vim src/main/resources/application.yml
```

主要配置项：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qa_system?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&createDatabaseIfNotExist=true
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

# LLM配置
llm:
  default-api-url: https://api.openai.com/v1/chat/completions
  default-model: gpt-3.5-turbo

# AI服务配置
ai:
  service:
    api-key: your-openai-api-key
```

4. **构建项目**
```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

5. **运行项目**
```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun

# 或使用批处理文件
start-app.bat
```

6. **验证安装**
- 访问 `http://localhost:8080/api` 查看API状态
- 访问 `http://localhost:8080/api/websocket-demo.html` 测试WebSocket功能

## 项目结构

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── config/                    # 配置类
│   │   │   ├── SecurityConfig.java    # 安全配置
│   │   │   ├── WebSocketConfig.java   # WebSocket配置
│   │   │   ├── RedisConfig.java       # Redis配置
│   │   │   ├── JdbcConfig.java        # JDBC配置
│   │   │   └── LlmConfig.java         # LLM配置
│   │   ├── controller/                # REST控制器
│   │   │   ├── UserController.java    # 用户管理
│   │   │   ├── StandardQuestionController.java # 问题管理
│   │   │   ├── AnswerGenerationController.java # 答案生成
│   │   │   ├── EvaluationController.java # 评估管理
│   │   │   ├── LLMModelController.java # 模型管理
│   │   │   └── WebSocketController.java # WebSocket控制器
│   │   ├── entity/jdbc/               # 实体类（JDBC）
│   │   │   ├── User.java              # 用户实体
│   │   │   ├── StandardQuestion.java  # 标准问题实体
│   │   │   ├── LlmAnswer.java         # LLM答案实体
│   │   │   └── ...                    # 其他实体
│   │   ├── repository/jdbc/           # 数据访问层（JDBC）
│   │   │   ├── UserRepository.java    # 用户数据访问
│   │   │   ├── StandardQuestionRepository.java # 问题数据访问
│   │   │   └── ...                    # 其他Repository
│   │   ├── service/                   # 业务逻辑层
│   │   │   ├── UserService.java       # 用户服务
│   │   │   ├── StandardQuestionService.java # 问题服务
│   │   │   ├── AnswerGenerationService.java # 答案生成服务
│   │   │   ├── EvaluationService.java # 评估服务
│   │   │   └── WebSocketService.java  # WebSocket服务
│   │   ├── dto/                       # 数据传输对象
│   │   ├── util/                      # 工具类
│   │   ├── exception/                 # 异常处理
│   │   ├── task/                      # 异步任务
│   │   ├── manager/                   # 管理器类
│   │   └── enums/                     # 枚举类
│   └── resources/
│       ├── static/                    # 静态资源
│       │   ├── js/websocket-client.js # WebSocket客户端
│       │   └── websocket-demo.html    # WebSocket演示页面
│       ├── db/                        # 数据库脚本
│       └── application.yml            # 配置文件
└── test/                             # 测试代码
```

## 开发指南

### 编码规范
- **Java规范**：
  - 类名使用UpperCamelCase
  - 方法名使用lowerCamelCase
  - 常量使用UPPER_SNAKE_CASE
  - 包名使用小写字母
- **Lombok使用**：
  - 实体类避免使用@Data，手动编写getter/setter
  - 使用@Slf4j进行日志记录
  - 使用@Builder构建复杂对象
- **数据库访问**：
  - 使用JDBC原生访问，避免ORM
  - SQL语句使用常量定义
  - 支持分页和排序
- **异常处理**：
  - 使用@ControllerAdvice全局异常处理
  - 自定义业务异常类
  - 详细的错误信息返回

### 测试规范
- **单元测试**：
  - 测试覆盖率要求>80%
  - 使用@SpringBootTest集成测试
  - 使用@MockBean模拟依赖
- **API测试**：
  - 使用@WebMvcTest测试控制器
  - 测试正常流程和异常情况
  - 验证返回数据格式

### 提交规范
- **语义化提交**：
  - `feat`: 新功能
  - `fix`: 修复bug
  - `docs`: 文档更新
  - `style`: 代码格式
  - `refactor`: 重构
  - `test`: 测试
  - `chore`: 构建过程或辅助工具的变动
- **提交要求**：
  - 每个提交专注于一个功能或修复
  - 提交信息清晰描述变更内容
  - 通过所有自动化测试

## 部署指南

### 开发环境
```bash
# 启动MySQL和Redis
docker-compose up -d mysql redis

# 运行应用
./gradlew bootRun
```

### 测试环境
```bash
# 构建Docker镜像
docker build -t qa-system:latest .

# 运行容器
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/qa_system \
  -e SPRING_REDIS_HOST=redis \
  qa-system:latest
```

### 生产环境
- **服务器要求**：8核CPU，16GB内存，100GB SSD
- **数据库配置**：MySQL主从复制，定期备份
- **缓存配置**：Redis集群，持久化配置
- **监控告警**：集成Prometheus和Grafana
- **日志管理**：ELK Stack日志收集

## 性能优化

### 数据库优化
- 合理使用索引
- 分页查询优化
- 连接池配置调优
- 慢查询监控

### 缓存策略
- Redis缓存热点数据
- 查询结果缓存
- 会话状态缓存

### 异步处理
- 大批量任务异步执行
- WebSocket消息异步推送
- 定时任务调度优化

## 常见问题

### 1. 数据库连接问题
```bash
# 检查数据库状态
systemctl status mysql

# 验证连接
mysql -u root -p -h localhost

# 检查配置
grep -r "datasource" src/main/resources/
```

### 2. Redis连接问题
```bash
# 检查Redis状态
redis-cli ping

# 查看Redis配置
redis-cli config get "*"
```

### 3. WebSocket连接问题
```bash
# 检查端口占用
netstat -tlnp | grep 8080

# 查看WebSocket日志
tail -f logs/application.log | grep WebSocket
```

### 4. LLM API调用问题
- 检查API密钥配置
- 验证网络连接
- 查看API调用日志
- 检查请求频率限制

### 5. 内存不足问题
```bash
# 调整JVM参数
export JAVA_OPTS="-Xmx4g -Xms2g"

# 监控内存使用
jstat -gc [pid]
```
