# 需求拆解流程指南

## 目标
将用户提供的自然语言需求系统化地拆解为独立、可并行开发的子任务，涵盖前端、后端、数据库、测试、部署等各个层面。

## 输入
- 原始需求描述（自然语言文本）
- 可选：技术栈偏好、现有系统约束、性能要求等附加信息

## 输出
1. 结构化需求文档（JSON格式）
2. 任务分配表（JSON格式）
3. 各子任务的详细描述和验收标准

## 拆解步骤

### 步骤1：需求澄清与范围界定
- 识别核心功能 vs 附加功能
- 确定用户角色和权限
- 明确系统边界和外部依赖

### 步骤2：功能模块划分
将需求分解为独立的功能模块，每个模块应满足：
- 高内聚：模块内部功能紧密相关
- 低耦合：模块间依赖最小化
- 可测试：模块可独立验证

**常见功能模块类型**：
- 用户认证与授权
- 数据管理（CRUD操作）
- 业务逻辑处理
- 报表与统计分析
- 通知与消息推送
- 文件上传与处理
- 第三方服务集成

### 步骤3：技术栈映射
为每个功能模块分配技术组件：

| 模块         | 前端组件          | 后端服务          | 数据库表          | 外部依赖          |
|--------------|-------------------|-------------------|-------------------|-------------------|
| 用户认证     | 登录/注册表单     | Auth API          | users表           | OAuth提供商       |
| 数据管理     | 数据表格/表单     | CRUD API          | 业务表            | 无                |
| 文件上传     | 文件选择器        | 上传处理服务      | files表           | 云存储(S3等)      |

### 步骤4：接口定义
识别模块间的接口需求：
- **API接口**：REST端点、GraphQL查询、WebSocket事件
- **数据格式**：请求/响应结构、错误码规范
- **事件流**：系统事件、消息队列主题

### 步骤5：非功能性需求分解
- **性能**：响应时间、并发用户数、数据量预估
- **安全**：认证机制、数据加密、访问控制
- **可维护性**：日志记录、监控指标、文档要求
- **可扩展性**：水平扩展策略、微服务划分

### 步骤6：任务生成
为每个技术组件创建开发任务：

| 任务类型     | 描述                                     | 负责Agent      | 依赖任务        |
|--------------|------------------------------------------|----------------|-----------------|
| 前端任务     | 实现用户界面和交互逻辑                   | 前端Agent      | API接口定义     |
| 后端任务     | 实现业务逻辑和API端点                    | 后端Agent      | 数据模型定义    |
| 数据库任务   | 设计表结构、索引、迁移脚本               | 数据库Agent    | 无              |
| 测试任务     | 编写单元测试、集成测试                   | 测试Agent      | 对应模块完成    |
| 部署任务     | 配置部署环境、CI/CD流水线                | 部署Agent      | 所有代码完成    |

## 结构化需求文档模板

```json
{
  "project_name": "项目名称",
  "version": "1.0.0",
  "description": "项目简要描述",
  "functional_requirements": [
    {
      "id": "FR001",
      "module": "用户认证",
      "description": "用户可以通过邮箱和密码注册、登录",
      "acceptance_criteria": [
        "注册表单包含邮箱、密码、确认密码字段",
        "密码需满足复杂度要求",
        "登录后跳转到仪表盘页面"
      ],
      "frontend_components": ["LoginForm", "RegisterForm", "AuthContext"],
      "backend_services": ["/api/auth/register", "/api/auth/login", "/api/auth/logout"],
      "database_tables": ["users"],
      "dependencies": []
    }
  ],
  "non_functional_requirements": {
    "performance": {
      "response_time": "API响应时间 < 500ms",
      "concurrent_users": "支持1000并发用户"
    },
    "security": {
      "authentication": "JWT token认证",
      "data_encryption": "敏感数据加密存储"
    }
  },
  "technical_stack": {
    "frontend": "React 18 + TypeScript + Tailwind CSS",
    "backend": "Node.js + Express + TypeScript",
    "database": "PostgreSQL 15",
    "deployment": "Docker + AWS ECS"
  },
  "interfaces": [
    {
      "name": "用户认证API",
      "type": "REST",
      "endpoints": [
        {
          "method": "POST",
          "path": "/api/auth/register",
          "request": {
            "email": "string",
            "password": "string"
          },
          "response": {
            "success": "boolean",
            "message": "string",
            "user_id": "number"
          }
        }
      ]
    }
  ]
}
```

## 任务分配表模板

```json
{
  "project_id": "project_001",
  "tasks": [
    {
      "task_id": "T001",
      "title": "前端用户认证界面",
      "description": "开发登录和注册页面，包括表单验证和错误处理",
      "agent_type": "frontend",
      "priority": "high",
      "estimated_effort": "2 days",
      "dependencies": [],
      "deliverables": [
        "src/components/auth/LoginForm.tsx",
        "src/components/auth/RegisterForm.tsx",
        "src/contexts/AuthContext.tsx"
      ],
      "acceptance_criteria": [
        "表单验证正确显示错误信息",
        "成功登录后跳转到仪表盘",
        "移动端响应式设计"
      ]
    },
    {
      "task_id": "T002",
      "title": "后端用户认证API",
      "description": "实现用户注册、登录、注销的API端点",
      "agent_type": "backend",
      "priority": "high",
      "estimated_effort": "3 days",
      "dependencies": ["T003"], // 依赖数据库设计
      "deliverables": [
        "src/routes/auth.ts",
        "src/controllers/authController.ts",
        "src/middlewares/authMiddleware.ts"
      ],
      "acceptance_criteria": [
        "密码加密存储",
        "JWT token生成和验证",
        "输入验证和错误处理"
      ]
    },
    {
      "task_id": "T003",
      "title": "用户数据表设计",
      "description": "设计users表结构，包含必要字段和索引",
      "agent_type": "database",
      "priority": "high",
      "estimated_effort": "1 day",
      "dependencies": [],
      "deliverables": [
        "migrations/001_create_users_table.sql",
        "models/User.ts"
      ],
      "acceptance_criteria": [
        "包含email、password_hash、created_at等字段",
        "email字段唯一索引",
        "支持软删除"
      ]
    }
  ],
  "timeline": {
    "start_date": "2026-04-17",
    "milestones": [
      {
        "date": "2026-04-20",
        "description": "完成所有核心模块开发"
      }
    ]
  }
}
```

## 拆解示例：考研数据管理系统

### 原始需求
"开发一个考研数据管理系统，能够导入Excel格式的考研分数线数据，提供多维度查询和可视化图表展示，支持用户注册和数据导出功能。"

### 拆解结果

#### 功能模块
1. **用户管理模块**：注册、登录、权限控制
2. **数据导入模块**：Excel文件解析、数据清洗、数据库存储
3. **数据查询模块**：多条件筛选、分页、排序
4. **可视化模块**：图表展示（柱状图、折线图、地图）
5. **数据导出模块**：Excel/PDF导出功能

#### 技术栈映射
- **前端**：React + Ant Design + ECharts
- **后端**：Node.js + Express + TypeORM
- **数据库**：PostgreSQL + Redis（缓存）
- **文件处理**：xlsx库、multer中间件

#### 任务分配（简略）
- 前端Agent：用户界面、图表组件、文件上传组件
- 后端Agent：用户认证API、数据导入API、查询API、导出API
- 数据库Agent：设计考研数据表、用户表、导入日志表
- 测试Agent：编写各模块单元测试和集成测试
- 部署Agent：配置Docker化部署、Nginx反向代理

## 最佳实践

1. **保持任务粒度适中**：每个任务应在1-3天内完成，便于进度跟踪。
2. **明确依赖关系**：准确标识任务间的先后顺序，避免阻塞。
3. **定义清晰验收标准**：每个任务应有可验证的完成标准。
4. **预留集成缓冲**：在时间线中预留20%时间用于集成和问题修复。
5. **持续沟通**：通过通信总线定期同步进度和接口变更。

## 下一步
1. 使用结构化需求文档生成任务分配表。
2. 将任务分配给对应Agent。
3. 启动并行开发流程。