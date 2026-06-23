# 多智能体协作开发框架 - 快速入门指南

## 框架概述

本框架是一套标准化的 **多智能体（Multi-Agent）协作开发体系**，能够将复杂的软件需求拆解为多个独立子任务，通过多个专用 AI Agent 并行开发，最终集成为一个完整的项目。

### 核心思想

- **分而治之**：将复杂需求拆解为前端、后端、数据库等独立子任务
- **并行开发**：多个 Agent 同时工作，大幅缩短开发周期
- **接口驱动**：先定义接口契约，再各自实现，降低集成风险
- **模板化**：标准化的提示词模板和工作流程，确保一致性

## 目录结构

```
my_agent/
├── 00_QUICK_START.md              # 本文件 - 快速入门
├── 01_framework/                  # 框架核心文档
│   ├── multi_agent_framework.md   # 多智能体协作框架（架构+流程）
│   ├── collaboration_protocol.md  # 智能体协作协议（通信+状态同步+事件驱动）
│   ├── integration_mechanism.md   # 集成与耦合机制（接口对齐+测试策略）
│   └── agent_lifecycle.md         # Agent 生命周期管理（状态机+事件）
├── 02_agents/                     # Agent 提示词模板
│   ├── _template_.md              # Agent 通用模板
│   ├── frontend_agent.md          # 前端 Agent
│   ├── backend_agent.md           # 后端 Agent
│   ├── database_agent.md          # 数据库 Agent
│   ├── integration_agent.md       # 集成协调 Agent
│   ├── security_agent.md          # 安全 Agent
│   └── deployment_test_agent.md   # 部署测试 Agent
├── 03_workflows/                  # 工作流程指南
│   ├── requirement_breakdown.md   # 需求拆解流程
│   ├── parallel_development.md    # 并行开发指南
│   ├── task_management.md         # 任务管理模板
│   └── project_status_template.md # 项目状态看板模板
├── 04_templates/                  # 结构化模板
│   ├── structured_requirements.json  # 结构化需求模板
│   ├── task_assignments.json         # 任务分配表模板
│   └── api_spec_template.yaml        # API 接口规范模板
└── 05_scripts/                    # 辅助工具脚本
    ├── init_project.py            # 项目初始化脚本
    ├── check_integration.py       # 集成检查脚本（含生命周期校验）
    └── start_mock_server.py       # Mock Server 自动启动脚本
```

## 三步快速上手

### 第一步：初始化项目

```bash
# 将本框架复制到新项目目录
cp -r my_agent /path/to/your/new/project/.agent

# 运行初始化脚本（交互式）
python .agent/05_scripts/init_project.py
```

初始化脚本会引导你输入：
- 项目名称和技术栈
- 核心功能需求
- 自动生成结构化需求文档和任务分配表

### 第二步：启动 Agent 并行开发

根据任务分配表，为每个子任务使用对应的 Agent 提示词模板：

1. **数据库 Agent** → `02_agents/database_agent.md`
2. **后端 Agent** → `02_agents/backend_agent.md`
3. **前端 Agent** → `02_agents/frontend_agent.md`
4. **集成协调 Agent** → `02_agents/integration_agent.md`
5. **安全 Agent** → `02_agents/security_agent.md`
6. **部署测试 Agent** → `02_agents/deployment_test_agent.md`

每个 Agent 的工作方式：
1. 打开对应的提示词模板
2. 将模板中的 `[变量]` 替换为实际项目信息
3. 将填充后的提示词发送给 AI 助手开始开发
4. 按照模板中的协作规则定期报告进度

### 第三步：集成与交付

1. **集成协调 Agent** 持续监控各 Agent 进度（参考 `agent_lifecycle.md` 管理状态）
2. 使用 `project_status_template.md` 跟踪状态（含 Agent 健康状态面板）
3. 使用 `check_integration.py` 检查接口一致性和 Agent 生命周期状态
4. 各 Agent 完成后启动 **部署测试 Agent** 进行集成测试
5. 使用 `start_mock_server.py` 根据 API 规范自动启动 Mock 服务：

```bash
# 根据 API 规范启动 Mock Server
python .agent/05_scripts/start_mock_server.py api_spec.yaml --port 3000

# 模拟延迟和错误场景
python .agent/05_scripts/start_mock_server.py api_spec.yaml --delay 200 --error-rate 0.1
```

## 使用场景

| 项目类型 | 适用性 | 说明 |
|---------|--------|------|
| Web 全栈应用 | ⭐⭐⭐⭐⭐ | 前后端分离架构的最佳匹配 |
| 移动端应用 | ⭐⭐⭐⭐ | 需补充移动端 Agent |
| 微服务系统 | ⭐⭐⭐⭐⭐ | 每个微服务可分配独立 Agent |
| 数据处理平台 | ⭐⭐⭐⭐ | 可补充数据处理 Agent |
| AI/ML 项目 | ⭐⭐⭐ | 需补充算法/数据科学 Agent |

## 核心原则

1. **接口先行**：先定义接口规范，再各自实现
2. **Mock 数据驱动**：依赖未就绪时使用 Mock 数据
3. **持续协调**：通过共享状态文件持续同步信息
4. **文档即代码**：所有接口定义、数据模型都有文档记录
5. **渐进集成**：分阶段集成，每个阶段都有验证

## 获取帮助

- 阅读 `01_framework/multi_agent_framework.md` 了解完整架构
- 阅读 `01_framework/agent_lifecycle.md` 了解 Agent 生命周期管理
- 阅读 `01_framework/collaboration_protocol.md` 了解通信协议和事件驱动模式
- 阅读 `03_workflows/requirement_breakdown.md` 学习需求拆解
- 阅读 `03_workflows/parallel_development.md` 了解并行开发策略
- 使用 `05_scripts/start_mock_server.py` 快速启动 Mock API 服务
