# Agent协作机制与通信协议

## 概述
本协议定义了多Agent协作开发框架中各个Agent之间的交互规则和通信机制，确保并行开发过程中的信息同步、接口一致性和问题及时解决。

## Agent角色定义

### 1. 前端Agent (Frontend Agent)
- **职责**：负责用户界面开发、交互逻辑、前端状态管理
- **通信需求**：需要从后端Agent获取API定义，从数据库Agent获取数据模型，向集成协调器报告进度

### 2. 后端Agent (Backend Agent)
- **职责**：负责业务逻辑、API实现、服务端架构
- **通信需求**：需要从数据库Agent获取数据模型，向前端Agent提供API定义，向集成协调器报告进度

### 3. 数据库Agent (Database Agent)
- **职责**：负责数据库设计、迁移脚本、数据模型定义
- **通信需求**：需要向所有Agent提供数据模型定义，接收数据访问需求

### 4. 测试Agent (Test Agent)
- **职责**：负责测试用例编写、自动化测试、质量验证
- **通信需求**：需要从所有Agent获取代码变更信息，向集成协调器报告测试结果

### 5. 部署Agent (Deployment Agent)
- **职责**：负责容器化、环境配置、CI/CD流水线
- **通信需求**：需要从所有Agent获取构建产物，向集成协调器报告部署状态

### 6. 集成协调器 (Integration Coordinator)
- **核心协调者**：负责监控所有Agent进度，协调接口对齐，解决依赖冲突
- **通信需求**：与所有Agent双向通信，维护项目状态看板

## 通信机制

### 通信方式
提供两种通信方式，可根据项目规模选择：

#### 方式一：文件系统通信（推荐用于小型项目）
- **原理**：通过共享目录下的JSON文件进行消息传递
- **目录结构**：
  ```
  communication/
  ├── inbox/                    # 接收消息目录
  │   ├── frontend/
  │   ├── backend/
  │   └── integration/
  ├── outbox/                   # 发送消息目录
  │   ├── frontend/
  │   ├── backend/
  │   └── integration/
  └── archives/                 # 消息归档
  ```
- **工作流程**：
  1. Agent将消息写入目标Agent的inbox目录
  2. 目标Agent定期扫描自己的inbox目录处理消息
  3. 处理后将消息移动到archives目录

#### 方式二：HTTP API通信（推荐用于分布式团队）
- **原理**：每个Agent运行一个HTTP服务，提供消息接收端点
- **端点示例**：
  - `POST /api/messages/receive` - 接收消息
  - `GET /api/messages/pending` - 获取待处理消息
  - `POST /api/messages/ack` - 确认消息处理
- **服务发现**：通过注册中心或配置文件获取其他Agent的地址

### 消息格式

#### 基础消息结构
```json
{
  "message_id": "uuid-v4",
  "timestamp": "2026-04-17T10:30:00Z",
  "sender": "frontend",
  "receiver": "backend",
  "message_type": "request|response|notification|error",
  "payload_type": "api_definition|data_model|progress_report|...",
  "payload": {},
  "correlation_id": "uuid-v4",  // 用于关联请求和响应
  "priority": "low|medium|high|critical"
}
```

#### 消息类型详解

##### 1. API定义请求 (API Definition Request)
**场景**：前端Agent需要后端API接口定义
```json
{
  "message_id": "msg_001",
  "timestamp": "2026-04-17T10:30:00Z",
  "sender": "frontend",
  "receiver": "backend",
  "message_type": "request",
  "payload_type": "api_definition_request",
  "payload": {
    "module": "user_authentication",
    "required_endpoints": ["register", "login", "logout"],
    "response_format": "openapi"  // 或 "json_schema"
  },
  "correlation_id": "req_001"
}
```

##### 2. API定义响应 (API Definition Response)
```json
{
  "message_id": "msg_002",
  "timestamp": "2026-04-17T10:35:00Z",
  "sender": "backend",
  "receiver": "frontend",
  "message_type": "response",
  "payload_type": "api_definition",
  "payload": {
    "module": "user_authentication",
    "openapi_spec": {
      "openapi": "3.0.0",
      "paths": {
        "/api/auth/register": {
          "post": {
            "summary": "用户注册",
            "requestBody": {
              "content": {
                "application/json": {
                  "schema": {
                    "type": "object",
                    "properties": {
                      "email": {"type": "string"},
                      "password": {"type": "string"}
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  },
  "correlation_id": "req_001"
}
```

##### 3. 数据模型通知 (Data Model Notification)
**场景**：数据库Agent发布数据模型变更
```json
{
  "message_id": "msg_003",
  "timestamp": "2026-04-17T09:00:00Z",
  "sender": "database",
  "receiver": "all",  // 广播给所有Agent
  "message_type": "notification",
  "payload_type": "data_model_update",
  "payload": {
    "version": "1.1.0",
    "changes": [
      {
        "table": "users",
        "operation": "add_column",
        "column": "phone_number",
        "type": "varchar(20)",
        "nullable": true
      }
    ],
    "full_schema": {
      "users": {
        "columns": [
          {"name": "id", "type": "serial", "primary_key": true},
          {"name": "email", "type": "varchar(255)", "unique": true}
        ]
      }
    }
  }
}
```

##### 4. 进度报告 (Progress Report)
**场景**：Agent定期向集成协调器报告进度
```json
{
  "message_id": "msg_004",
  "timestamp": "2026-04-17T11:00:00Z",
  "sender": "frontend",
  "receiver": "integration",
  "message_type": "notification",
  "payload_type": "progress_report",
  "payload": {
    "task_id": "T001",
    "status": "in_progress|completed|blocked",
    "progress_percentage": 75,
    "current_activity": "正在实现表单验证逻辑",
    "estimated_completion": "2026-04-17T18:00:00Z",
    "blockers": [
      {
        "description": "需要后端API错误码定义",
        "blocking_agent": "backend",
        "priority": "high"
      }
    ],
    "deliverables_completed": [
      "src/components/auth/LoginForm.tsx",
      "src/components/auth/RegisterForm.tsx"
    ]
  }
}
```

##### 5. 集成检查请求 (Integration Check Request)
**场景**：集成协调器检查接口一致性
```json
{
  "message_id": "msg_005",
  "timestamp": "2026-04-17T12:00:00Z",
  "sender": "integration",
  "receiver": "frontend",
  "message_type": "request",
  "payload_type": "integration_check",
  "payload": {
    "check_type": "api_compatibility",
    "api_endpoint": "/api/auth/login",
    "expected_request": {
      "email": "string",
      "password": "string"
    },
    "expected_response": {
      "token": "string",
      "user": {
        "id": "number",
        "email": "string"
      }
    }
  }
}
```

## 状态同步机制

### 项目状态看板
集成协调器维护全局项目状态看板，所有Agent可以查询：

```json
{
  "project_id": "project_001",
  "overall_status": "healthy|warning|critical",
  "agents": {
    "frontend": {
      "status": "online",
      "last_heartbeat": "2026-04-17T11:05:00Z",
      "current_task": "T001",
      "progress": 75,
      "health": "good"
    },
    "backend": {
      "status": "online",
      "last_heartbeat": "2026-04-17T11:04:30Z",
      "current_task": "T002",
      "progress": 60,
      "health": "good"
    }
  },
  "tasks": {
    "T001": {
      "status": "in_progress",
      "assigned_to": "frontend",
      "started_at": "2026-04-17T09:00:00Z",
      "estimated_completion": "2026-04-17T18:00:00Z",
      "dependencies_met": true
    }
  },
  "integration_issues": [
    {
      "id": "issue_001",
      "description": "前端期望的登录响应字段与后端实现不一致",
      "severity": "high",
      "affected_agents": ["frontend", "backend"],
      "status": "open",
      "created_at": "2026-04-17T10:30:00Z"
    }
  ]
}
```

### 心跳机制
每个Agent每隔30秒向集成协调器发送心跳消息：
```json
{
  "message_type": "heartbeat",
  "agent_id": "frontend",
  "timestamp": "2026-04-17T11:05:30Z",
  "status": "healthy",
  "resource_usage": {
    "cpu": 45.2,
    "memory_mb": 512
  }
}
```

## 冲突解决流程

### 1. 冲突检测
- **接口不一致**：集成协调器定期对比前后端API定义
- **数据模型冲突**：数据库Agent检测到不兼容的迁移请求
- **依赖死锁**：任务分配器检测到循环依赖

### 2. 冲突上报
检测到冲突的Agent向集成协调器发送冲突报告：
```json
{
  "message_type": "conflict_report",
  "conflict_type": "api_mismatch",
  "description": "前端期望/users/:id返回user对象，后端返回userProfile对象",
  "evidence": {
    "frontend_expectation": {
      "endpoint": "GET /api/users/:id",
      "response_type": "User"
    },
    "backend_implementation": {
      "endpoint": "GET /api/users/:id",
      "response_type": "UserProfile"
    }
  },
  "suggested_resolution": [
    "修改后端返回User对象",
    "修改前端期待UserProfile对象",
    "创建适配层转换"
  ]
}
```

### 3. 冲突解决会议
集成协调器召集相关Agent进行虚拟会议：
1. 分享冲突详情
2. 讨论解决方案
3. 投票决定最佳方案
4. 更新相关文档和接口定义

### 4. 解决方案执行
选定方案后，集成协调器向相关Agent发送解决方案指令：
```json
{
  "message_type": "conflict_resolution",
  "conflict_id": "conflict_001",
  "selected_solution": "修改后端返回User对象",
  "actions": [
    {
      "agent": "backend",
      "action": "修改UserController的getUser方法",
      "deadline": "2026-04-17T15:00:00Z"
    },
    {
      "agent": "frontend",
      "action": "更新TypeScript类型定义",
      "deadline": "2026-04-17T15:30:00Z"
    }
  ]
}
```

## 错误处理

### 错误消息格式
```json
{
  "message_type": "error",
  "error_code": "API_VALIDATION_ERROR",
  "error_message": "请求参数验证失败",
  "details": {
    "field": "email",
    "issue": "必须为有效的邮箱格式",
    "received_value": "invalid-email"
  },
  "suggested_fix": "提供有效的邮箱地址，如user@example.com",
  "retryable": true,
  "reporting_agent": "backend"
}
```

### 错误处理策略
1. **可重试错误**：自动重试最多3次，每次间隔指数退避
2. **不可重试错误**：立即上报集成协调器，等待人工干预
3. **级联错误**：错误传播到依赖任务，暂停相关开发

## 安全考虑

### 消息安全
1. **消息签名**：使用HMAC对重要消息进行签名验证
2. **敏感数据加密**：密码、密钥等敏感字段在传输前加密
3. **访问控制**：基于Agent角色的消息访问权限

### 防篡改机制
1. **消息序列号**：防止消息重放攻击
2. **时间戳验证**：拒绝过时消息（超过5分钟）
3. **来源验证**：验证发送者身份

## 实施建议

### 小型团队（2-5人）
- 使用文件系统通信方式
- 简化消息类型，只保留核心类型
- 人工处理冲突解决

### 中型团队（5-15人）
- 使用HTTP API通信方式
- 实现自动化集成检查
- 半自动冲突解决流程

### 大型团队（15人以上）
- 实现完整的消息队列系统（如RabbitMQ、Kafka）
- 自动化冲突检测和解决
- 实时项目状态仪表板

## 附录：消息类型参考表

| 消息类型 | 发送者 | 接收者 | 目的 | 频率 |
|---------|--------|--------|------|------|
| api_definition_request | 前端 | 后端 | 获取API定义 | 按需 |
| api_definition_response | 后端 | 前端 | 提供API定义 | 按需 |
| data_model_notification | 数据库 | 所有 | 发布数据模型 | 变更时 |
| progress_report | 所有 | 集成 | 报告进度 | 每小时 |
| heartbeat | 所有 | 集成 | 健康检查 | 每30秒 |
| conflict_report | 任何 | 集成 | 报告冲突 | 发生时 |
| integration_check | 集成 | 任何 | 检查集成 | 每天 |
| error | 任何 | 相关方 | 报告错误 | 发生时 |