# 智能体协作协议

## 概述

本协议定义了多 Agent 协作开发框架中各个 Agent 之间的交互规则和通信机制，确保并行开发过程中的信息同步、接口一致性和问题及时解决。

## Agent 角色定义

### 1. 前端 Agent（Frontend Agent）
- **职责**：负责用户界面开发、交互逻辑、前端状态管理
- **通信需求**：需要从后端 Agent 获取 API 定义，从数据库 Agent 获取数据模型，向集成协调 Agent 报告进度

### 2. 后端 Agent（Backend Agent）
- **职责**：负责业务逻辑、API 实现、服务端架构
- **通信需求**：需要从数据库 Agent 获取数据模型，向前端 Agent 提供 API 定义，向集成协调 Agent 报告进度

### 3. 数据库 Agent（Database Agent）
- **职责**：负责数据库设计、迁移脚本、数据模型定义
- **通信需求**：需要向所有 Agent 提供数据模型定义，接收数据访问需求

### 4. 集成协调 Agent（Integration Coordinator）
- **核心协调者**：负责监控所有 Agent 进度，协调接口对齐，解决依赖冲突
- **通信需求**：与所有 Agent 双向通信，维护项目状态看板

### 5. 安全 Agent（Security Agent）
- **职责**：负责安全加固、身份认证、合规检查
- **通信需求**：需要从所有 Agent 获取代码和安全需求信息

### 6. 部署测试 Agent（Deployment & Test Agent）
- **职责**：负责容器化部署、CI/CD 流水线、测试自动化
- **通信需求**：需要从所有 Agent 获取构建产物和测试需求

## 通信机制

### 方式一：文件系统通信（推荐用于小型项目）

通过共享目录下的 Markdown/JSON 文件进行消息传递：

```
project_root/
├── .agent/                      # Agent 框架目录
├── communication/               # 通信目录（自动创建）
│   ├── project_status.md        # 主状态文件（所有 Agent 读写）
│   ├── messages/                # 消息记录
│   │   ├── frontend/            # 前端消息
│   │   ├── backend/             # 后端消息
│   │   └── integration/         # 集成协调消息
│   └── archives/                # 消息归档
```

### 方式二：HTTP API 通信（推荐用于分布式团队）

每个 Agent 运行一个 HTTP 服务，提供消息接收端点：
- `POST /api/messages/receive` - 接收消息
- `GET /api/messages/pending` - 获取待处理消息
- `POST /api/messages/ack` - 确认消息处理

## 消息格式

### 基础消息结构

````json
{
  "message_id": "uuid-v4",
  "timestamp": "2026-04-17T10:30:00Z",
  "sender": "frontend",
  "receiver": "backend",
  "message_type": "request|response|notification|error",
  "payload_type": "api_definition|data_model|progress_report|...",
  "payload": {},
  "correlation_id": "uuid-v4",
  "priority": "low|medium|high|critical"
}
````

### 消息类型详解

#### 1. API 定义请求（前端 → 后端）

````json
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
    "response_format": "openapi"
  },
  "correlation_id": "req_001",
  "priority": "high"
}
````

#### 2. API 定义响应（后端 → 前端）

````json
{
  "message_id": "msg_002",
  "timestamp": "2026-04-17T10:35:00Z",
  "sender": "backend",
  "receiver": "frontend",
  "message_type": "response",
  "payload_type": "api_definition",
  "payload": {
    "module": "user_authentication",
    "endpoints": [
      {
        "method": "POST",
        "path": "/api/auth/register",
        "request": { "email": "string", "password": "string" },
        "response": { "success": "boolean", "message": "string" }
      }
    ]
  },
  "correlation_id": "req_001"
}
````

#### 3. 数据模型通知（数据库 → 所有 Agent）

````json
{
  "message_id": "msg_003",
  "timestamp": "2026-04-17T09:00:00Z",
  "sender": "database",
  "receiver": "all",
  "message_type": "notification",
  "payload_type": "data_model_update",
  "payload": {
    "version": "1.1.0",
    "changes": [
      { "table": "users", "operation": "add_column", "column": "phone_number", "type": "varchar(20)" }
    ],
    "full_schema": { "users": { "columns": [...] } }
  }
}
````

#### 4. 进度报告（所有 Agent → 集成协调）

````json
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
    "blockers": [
      { "description": "需要后端 API 错误码定义", "blocking_agent": "backend", "priority": "high" }
    ],
    "deliverables_completed": ["src/components/LoginForm.tsx"]
  }
}
````

## 状态同步机制

### 项目状态看板

集成协调 Agent 维护全局项目状态看板（`project_status.md`），所有 Agent 可以查询和更新：

````markdown
## 项目状态摘要
- **整体进度**: 65%
- **当前阶段**: 并行开发阶段
- **关键风险**: 2 个未解决

## 任务状态
| 任务ID | 任务名称 | 负责人 | 状态 | 进度 | 阻塞 |
|--------|---------|--------|------|------|------|
| T001 | 数据库设计 | database | ✅ 已完成 | 100% | 无 |
| T002 | 后端 API | backend | 🔄 进行中 | 60% | 无 |

## 通信记录
### [2026-04-17 10:00] | frontend Agent
**类型**: 问题
**内容**: 需要确认登录接口的响应格式
**状态**: ⏳ 等待 backend Agent 回复
````

### 心跳机制
每个 Agent 定期向集成协调 Agent 报告状态：
- **开发阶段**：每 2 小时或重大进展时
- **集成阶段**：每 30 分钟
- **问题发生时**：立即报告

## 冲突解决流程

### 冲突类型
1. **接口冲突**：前后端对接口理解不一致
2. **数据冲突**：数据模型或格式不一致
3. **技术冲突**：技术方案选择分歧
4. **依赖冲突**：任务依赖关系导致阻塞

### 解决流程

```
发现冲突 → 记录到状态看板 → 分析影响范围 → 讨论解决方案
    ↑                                              │
    └────────────── 验证解决 ← 执行方案 ← 决策 ────┘
```

### 解决原则
1. **数据驱动**：基于事实决策，而非个人偏好
2. **用户导向**：以用户需求和业务价值为最终标准
3. **技术可行**：考虑技术实现难度和成本
4. **长期维护**：考虑方案的长期可维护性

## 事件驱动通信

### 发布/订阅模式

在文件系统通信和 HTTP API 通信的基础上，引入事件驱动（Event-Driven）的发布/订阅模式，实现 Agent 之间的解耦通信：

```
                    ┌─────────────────┐
                    │   Event Bus     │
                    │  (状态看板)      │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
   ┌──────────┐      ┌──────────┐        ┌──────────┐
   │ frontend │      │ backend  │        │ database │
   │ Agent    │      │ Agent    │        │ Agent    │
   └──────────┘      └──────────┘        └──────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ integration     │
                    │ Agent (协调者)   │
                    └─────────────────┘
```

### 事件类型

| 事件类型 | 说明 | 发布者 | 订阅者 |
|---------|------|--------|--------|
| `api.defined` | API 接口已定义 | 后端 Agent | 前端 Agent、测试 Agent |
| `api.changed` | API 接口发生变更 | 后端 Agent | 前端 Agent、集成协调 Agent |
| `data.model_updated` | 数据模型已更新 | 数据库 Agent | 所有 Agent |
| `data.schema_ready` | 数据库 Schema 就绪 | 数据库 Agent | 后端 Agent |
| `task.started` | 任务开始执行 | Agent 自身 | 集成协调 Agent |
| `task.progress` | 任务进度更新 | Agent 自身 | 集成协调 Agent |
| `task.completed` | 任务完成 | Agent 自身 | 集成协调 Agent、依赖方 |
| `task.blocked` | 任务被阻塞 | Agent 自身 | 集成协调 Agent |
| `conflict.detected` | 检测到冲突 | 任意 Agent | 集成协调 Agent |
| `conflict.resolved` | 冲突已解决 | 集成协调 Agent | 相关 Agent |
| `review.required` | 需要代码审核 | Agent 自身 | 集成协调 Agent |
| `review.approved` | 审核通过 | 集成协调 Agent | Agent 自身 |
| `lifecycle.*` | 生命周期事件 | Agent 自身 | 集成协调 Agent |

### 事件消息格式

```json
{
  "event_id": "evt_001",
  "event_type": "api.changed",
  "timestamp": "2026-05-05T10:00:00Z",
  "publisher": "backend",
  "publisher_state": "running",
  "payload": {
    "summary": "登录接口响应格式变更",
    "details": {}
  },
  "priority": "high",
  "ttl_minutes": 60
}
```

### 事件路由规则

集成协调 Agent 根据事件类型自动路由到相关订阅者：

| 事件类型 | 路由策略 | 目标 |
|---------|---------|------|
| `api.*` | 广播 | 前端 Agent、测试 Agent |
| `data.*` | 广播 | 所有 Agent |
| `task.*` | 定向 | 集成协调 Agent |
| `conflict.*` | 定向 | 集成协调 Agent |
| `review.*` | 定向 | 集成协调 Agent |
| `lifecycle.*` | 广播 | 集成协调 Agent + 依赖方 |

## 生命周期事件集成

### 生命周期状态同步

Agent 的生命周期状态通过协作协议的心跳机制同步到项目状态看板：

```json
{
  "message_id": "msg_hb_001",
  "timestamp": "2026-05-05T14:00:00Z",
  "sender": "frontend",
  "receiver": "integration",
  "message_type": "notification",
  "payload_type": "heartbeat",
  "payload": {
    "agent_id": "frontend_001",
    "lifecycle_state": "running",
    "task_id": "T003",
    "progress_percentage": 65,
    "current_activity": "正在实现用户列表组件",
    "blockers": []
  },
  "priority": "low"
}
```

### 状态转换通知

当 Agent 发生生命周期状态转换时，自动发布 `lifecycle.*` 事件：

```
Agent 状态变更
    │
    ▼
记录状态变更到本地历史
    │
    ▼
发布 lifecycle 事件到事件总线
    │
    ▼
集成协调 Agent 更新状态看板
    │
    ▼
通知依赖该 Agent 的其他 Agent（如有必要）
```

详细的生命周期状态定义和转换规则请参考 [agent_lifecycle.md](file:///c:/Users/32546/Desktop/yanxiaomap/my_agent/01_framework/agent_lifecycle.md)。

## 通信纪律

### 进度报告格式

```
### [时间] | [Agent 名称]
**类型**: 进度报告
**任务**: [任务ID]
**进度**: [百分比]
**已完成**: [具体完成的工作]
**当前工作**: [正在进行的工作]
**问题/阻塞**: [遇到的问题]
**预计完成**: [预计完成时间]
```

### 问题上报格式

```
### [时间] | [Agent 名称]
**类型**: 问题/决策需求
**任务**: [任务ID]
**优先级**: [高/中/低]
**问题描述**: [清晰描述问题]
**影响范围**: [影响哪些任务或功能]
**建议方案**: [提出 1-3 个解决方案]
**需求决策**: [需要决策的具体事项]
```

### 响应时效

| 优先级 | 响应时间 | 适用场景 |
|--------|---------|---------|
| 紧急 | 2 小时内 | 阻塞性接口问题、安全漏洞 |
| 高 | 4 小时内 | 接口变更、数据模型变更 |
| 中 | 8 小时内 | 一般技术问题、进度确认 |
| 低 | 24 小时内 | 文档完善、非功能性需求 |
