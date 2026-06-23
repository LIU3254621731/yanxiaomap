# Agent 生命周期管理

## 概述

Agent 生命周期管理定义了每个 Agent 从创建到销毁的完整状态流转路径，确保多 Agent 协作开发过程中每个角色的状态可追踪、可管理、可恢复。

## 生命周期状态定义

### 状态总览

```
                    ┌─────────────────────────────────────────────┐
                    │              waiting_for_dependency         │
                    │              waiting_for_review             │
                    │              waiting_for_response           │
                    └─────────────────────────────────────────────┘
                                          │
                    ┌──────────────────────┘
                    ▼
┌─────────┐   ┌──────────┐   ┌────────┐   ┌───────────┐   ┌──────────┐
│ created │──▶│initialized│──▶│running │──▶│ completed │──▶│ archived │
└─────────┘   └──────────┘   └────────┘   └───────────┘   └──────────┘
                    │               │  │                        ▲
                    │               │  │                        │
                    │               ▼  │                        │
                    │          ┌───────┴──┐                    │
                    │          │  paused  │────────────────────┘
                    │          └──────────┘
                    │               │
                    │               ▼
                    │          ┌────────┐
                    └─────────▶│ failed │
                               └────────┘
                    ┌──────────┐
                    │ blocked  │
                    └──────────┘
```

### 核心状态

| 状态 | 标识 | 说明 | 可触发者 |
|------|------|------|---------|
| 已创建 | `created` | Agent 模板已实例化，尚未分配任务 | 系统/集成协调 Agent |
| 已初始化 | `initialized` | 任务已分配，环境已就绪，准备开始工作 | Agent 自身 |
| 运行中 | `running` | 正在执行开发任务 | Agent 自身 |
| 已暂停 | `paused` | 因外部依赖或阻塞暂停工作 | Agent 自身/集成协调 Agent |
| 已完成 | `completed` | 任务开发完成，交付物已提交 | Agent 自身 |
| 已归档 | `archived` | 交付物已验证，进入归档 | 集成协调 Agent |

### 异常状态

| 状态 | 标识 | 说明 | 可触发者 |
|------|------|------|---------|
| 失败 | `failed` | 任务执行失败，需要人工介入 | Agent 自身/系统 |
| 阻塞 | `blocked` | 被外部依赖阻塞，无法继续 | Agent 自身 |

### 等待状态（暂停的子类型）

| 状态 | 标识 | 说明 | 可触发者 |
|------|------|------|---------|
| 等待依赖 | `waiting_for_dependency` | 等待前置任务完成 | Agent 自身 |
| 等待审核 | `waiting_for_review` | 等待代码审核或设计审核 | Agent 自身 |
| 等待响应 | `waiting_for_response` | 等待其他 Agent 回复问题 | Agent 自身 |

## 状态转换规则

### 合法转换矩阵

| 当前状态 \\ 目标状态 | created | initialized | running | paused | completed | archived | failed | blocked |
|---------------------|---------|-------------|---------|--------|-----------|----------|--------|---------|
| **created** | - | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **initialized** | ❌ | - | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **running** | ❌ | ❌ | - | ✅ | ✅ | ❌ | ✅ | ✅ |
| **paused** | ❌ | ❌ | ✅ | - | ❌ | ✅ | ❌ | ❌ |
| **completed** | ❌ | ❌ | ❌ | ❌ | - | ✅ | ❌ | ❌ |
| **archived** | ❌ | ❌ | ❌ | ❌ | ❌ | - | ❌ | ❌ |
| **failed** | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | - | ❌ |
| **blocked** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | - |

### 转换条件

| 转换 | 前置条件 | 触发事件 |
|------|---------|---------|
| created → initialized | 任务已分配、环境变量已配置 | `agent.initialized` |
| initialized → running | 所有依赖状态为 completed | `agent.started` |
| initialized → failed | 环境检查失败、配置错误 | `agent.failed` |
| initialized → blocked | 检测到缺失依赖 | `agent.blocked` |
| running → paused | 收到暂停指令或外部依赖未就绪 | `agent.paused` |
| running → completed | 所有交付物已提交并通过自检 | `agent.completed` |
| running → failed | 不可恢复的错误发生 | `agent.failed` |
| running → blocked | 运行时发现关键依赖缺失 | `agent.blocked` |
| paused → running | 阻塞解除或收到恢复指令 | `agent.resumed` |
| paused → archived | 项目终止，当前工作已保存 | `agent.archived` |
| completed → archived | 集成协调 Agent 验证通过 | `agent.archived` |
| failed → initialized | 问题已修复，重新初始化 | `agent.reinitialized` |

## 生命周期事件

### 事件格式

每个状态转换都会触发对应的事件，事件按照发布/订阅模式广播给所有订阅者：

```json
{
  "event_id": "evt_001",
  "event_type": "agent.completed",
  "timestamp": "2026-05-05T14:30:00Z",
  "source": "backend",
  "payload": {
    "agent_id": "backend_001",
    "previous_state": "running",
    "current_state": "completed",
    "transition_reason": "所有 API 接口已实现并通过自测",
    "deliverables": ["backend/src/controllers/*.java", "backend/src/services/*.java"],
    "metrics": {
      "total_files": 15,
      "total_lines": 3200,
      "test_coverage": 85.5
    }
  }
}
```

### 事件类型定义

| 事件类型 | 说明 | 发布者 | 订阅者 |
|---------|------|--------|--------|
| `agent.created` | Agent 已创建 | 系统 | 集成协调 Agent |
| `agent.initialized` | Agent 初始化完成 | Agent 自身 | 集成协调 Agent |
| `agent.started` | Agent 开始工作 | Agent 自身 | 所有 Agent |
| `agent.paused` | Agent 暂停工作 | Agent 自身 | 集成协调 Agent、依赖方 |
| `agent.resumed` | Agent 恢复工作 | Agent 自身 | 集成协调 Agent、依赖方 |
| `agent.completed` | Agent 完成任务 | Agent 自身 | 所有 Agent |
| `agent.failed` | Agent 执行失败 | Agent 自身 | 集成协调 Agent |
| `agent.blocked` | Agent 被阻塞 | Agent 自身 | 集成协调 Agent、阻塞方 |
| `agent.archived` | Agent 已归档 | 集成协调 Agent | 系统 |

### 事件优先级

| 事件类型 | 优先级 | 处理时限 |
|---------|--------|---------|
| `agent.failed` | critical | 立即处理 |
| `agent.blocked` | high | 15 分钟内 |
| `agent.completed` | high | 30 分钟内 |
| `agent.paused` | medium | 1 小时内 |
| `agent.started` | medium | 1 小时内 |
| `agent.resumed` | medium | 2 小时内 |
| `agent.created` | low | 4 小时内 |
| `agent.initialized` | low | 4 小时内 |
| `agent.archived` | low | 8 小时内 |

## 生命周期钩子

每个状态转换可以绑定自定义钩子函数，在转换前后自动执行特定逻辑：

### 钩子定义格式

```json
{
  "hook_id": "hook_001",
  "name": "初始化数据库连接",
  "trigger_state": "initialized",
  "hook_type": "post_transition",
  "action": "environment_check",
  "timeout_seconds": 30,
  "on_failure": "block|warn|ignore"
}
```

### 预定义钩子

| 钩子名称 | 触发状态 | 执行时机 | 说明 |
|---------|---------|---------|------|
| 环境检查 | `initialized` | 后置 | 检查开发环境是否就绪 |
| 依赖检查 | `initialized` | 后置 | 检查前置依赖是否完成 |
| 交付物验证 | `completed` | 前置 | 验证交付物完整性 |
| 接口对齐检查 | `completed` | 后置 | 检查接口定义是否一致 |
| 资源清理 | `archived` | 前置 | 清理临时文件和资源 |

## 与协作协议的集成

### 生命周期消息类型

在协作协议的消息格式中，新增 `lifecycle_event` 消息类型：

```json
{
  "message_id": "msg_lc_001",
  "timestamp": "2026-05-05T14:30:00Z",
  "sender": "backend",
  "receiver": "all",
  "message_type": "notification",
  "payload_type": "lifecycle_event",
  "payload": {
    "event_type": "agent.completed",
    "agent_id": "backend_001",
    "previous_state": "running",
    "current_state": "completed",
    "summary": "后端 API 开发完成，所有接口已实现"
  },
  "correlation_id": null,
  "priority": "high"
}
```

### 生命周期状态查询

集成协调 Agent 可以通过查询接口获取任意 Agent 的完整生命周期历史：

```json
{
  "agent_id": "backend_001",
  "current_state": "completed",
  "state_history": [
    { "state": "created", "timestamp": "2026-05-05T09:00:00Z", "duration_minutes": 5 },
    { "state": "initialized", "timestamp": "2026-05-05T09:05:00Z", "duration_minutes": 10 },
    { "state": "running", "timestamp": "2026-05-05T09:15:00Z", "duration_minutes": 315 },
    { "state": "completed", "timestamp": "2026-05-05T14:30:00Z", "duration_minutes": null }
  ],
  "total_active_minutes": 330,
  "blocked_count": 2,
  "blocked_minutes": 45
}
```

## 生命周期仪表盘

### Agent 健康评分

基于生命周期数据计算每个 Agent 的健康评分（0-100）：

| 指标 | 权重 | 计算方式 |
|------|------|---------|
| 阻塞频率 | 30% | 阻塞次数 / 总运行小时数，越低分越高 |
| 状态稳定性 | 25% | 异常状态（failed/blocked）占比越低分越高 |
| 交付准时率 | 25% | 实际完成时间与预计完成时间的偏差 |
| 响应及时性 | 20% | 对协作消息的平均响应时间 |

### 评分等级

| 分数范围 | 等级 | 说明 |
|---------|------|------|
| 90-100 | 🟢 健康 | 状态稳定，进展顺利 |
| 70-89 | 🟡 亚健康 | 偶有阻塞，整体可控 |
| 50-69 | 🟠 警示 | 频繁阻塞，需要关注 |
| 0-49 | 🔴 危险 | 严重受阻，需要人工介入 |

## 使用流程

### 1. 生命周期配置

在项目初始化时，集成协调 Agent 为每个 Agent 创建生命周期记录：

```json
{
  "agent_id": "frontend_001",
  "agent_role": "frontend",
  "created_at": "2026-05-05T09:00:00Z",
  "config": {
    "max_blocked_minutes": 120,
    "auto_archive_days": 7,
    "heartbeat_interval_minutes": 30,
    "hooks_enabled": true
  }
}
```

### 2. 状态更新流程

```
Agent 完成任务
    │
    ▼
Agent 执行 pre_completed 钩子（交付物验证）
    │
    ├── 验证通过 ──▶ 状态更新为 completed
    │                   │
    │                   ▼
    │               发布 agent.completed 事件
    │                   │
    │                   ▼
    │               触发 post_completed 钩子（接口对齐检查）
    │
    └── 验证失败 ──▶ 保持 running 状态
                        │
                        ▼
                    记录失败原因，继续工作
```

### 3. 异常处理

当 Agent 进入 failed 或 blocked 状态时：

1. Agent 发布异常事件（`agent.failed` / `agent.blocked`）
2. 集成协调 Agent 记录异常并评估影响范围
3. 自动通知受影响的依赖 Agent
4. 集成协调 Agent 决定：重试 / 重新分配 / 人工介入
5. 问题解决后，Agent 重新进入 initialized 状态

### 4. 归档条件

Agent 进入 archived 状态的先决条件：

- [ ] 交付物已通过集成协调 Agent 验证
- [ ] 所有接口定义已对齐确认
- [ ] 相关文档已完善
- [ ] 依赖该 Agent 的其他 Agent 已完成或已适配
- [ ] 集成测试通过
