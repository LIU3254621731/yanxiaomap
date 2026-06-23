# 任务管理模板

## 概述

任务管理是多 Agent 协作的核心环节。本文档提供了一套标准化的任务定义、跟踪和更新机制，确保所有 Agent 对任务状态有统一的认知。

## 任务定义格式

每个任务在任务分配表中使用以下格式定义：

```json
{
  "task_id": "T001",
  "name": "任务名称",
  "module": "所属功能模块",
  "agent": "负责 Agent 类型",
  "priority": "high|medium|low",
  "status": "pending|in_progress|completed|blocked",
  "dependencies": ["T000"],
  "estimated_hours": 8,
  "description": "任务详细描述，包括业务背景和具体需求",
  "input": [
    "输入信息1: 说明",
    "输入信息2: 说明"
  ],
  "output": [
    "输出1: 说明",
    "输出2: 说明"
  ],
  "acceptance_criteria": [
    "验收标准1",
    "验收标准2"
  ],
  "notes": "备注信息"
}
```

## 任务状态定义

| 状态 | 图标 | 说明 | 条件 |
|------|------|------|------|
| 待分配 | 📋 | 任务已定义但未分配 Agent | 需求分析完成 |
| 待开始 | ⏳ | 已分配 Agent，等待开始 | 前置依赖满足 |
| 进行中 | 🔄 | Agent 正在执行 | 任务已领取 |
| 已完成 | ✅ | 任务完成并验收 | 验收标准全部满足 |
| 阻塞 | 🚫 | 任务被阻塞无法继续 | 存在未解决的依赖或问题 |

## 任务优先级定义

| 优先级 | 标签 | 响应时间 | 说明 |
|--------|------|---------|------|
| 最高 | 🔴 P0 | 立即 | 关键路径任务，阻塞其他任务 |
| 高 | 🟠 P1 | 2 小时内 | 核心功能，重要但不紧急 |
| 中 | 🟡 P2 | 8 小时内 | 非核心功能，可以延迟 |
| 低 | 🟢 P3 | 24 小时内 | 优化/附加功能，不影响核心流程 |

## Agent 能力矩阵

| 能力 | 前端 Agent | 后端 Agent | 数据库 Agent | 安全 Agent | 部署测试 Agent |
|------|-----------|-----------|-------------|-----------|---------------|
| 用户界面开发 | ⭐⭐⭐⭐⭐ | - | - | - | - |
| 交互逻辑 | ⭐⭐⭐⭐⭐ | - | - | - | - |
| 状态管理 | ⭐⭐⭐⭐ | - | - | - | - |
| REST API 开发 | - | ⭐⭐⭐⭐⭐ | - | - | - |
| 业务逻辑实现 | - | ⭐⭐⭐⭐⭐ | - | - | - |
| 数据库设计 | - | ⭐⭐ | ⭐⭐⭐⭐⭐ | - | - |
| 数据访问层 | - | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | - | - |
| ORM 映射 | - | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | - | - |
| 身份认证 | - | ⭐⭐⭐ | - | ⭐⭐⭐⭐⭐ | - |
| 安全审计 | - | - | - | ⭐⭐⭐⭐⭐ | - |
| 容器化部署 | - | ⭐⭐ | - | - | ⭐⭐⭐⭐⭐ |
| CI/CD 配置 | - | ⭐ | - | - | ⭐⭐⭐⭐⭐ |
| 自动化测试 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 集成协调 | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ |

## 任务分配表模板

```json
{
  "project": "项目名称",
  "version": "1.0.0",
  "created_at": "2026-04-17T10:00:00Z",
  "updated_at": "2026-04-17T10:00:00Z",
  "tasks": [
    {
      "task_id": "T001",
      "name": "数据库设计与初始化",
      "module": "数据层",
      "agent": "database",
      "priority": "high",
      "status": "pending",
      "dependencies": [],
      "estimated_hours": 8,
      "description": "根据需求设计数据库表结构，创建迁移脚本，准备初始数据",
      "input": ["需求文档中的实体关系描述"],
      "output": ["建表脚本", "实体类定义", "初始数据脚本", "数据字典"],
      "acceptance_criteria": [
        "表结构覆盖所有业务实体",
        "迁移脚本可重复执行",
        "初始数据满足基本功能演示"
      ]
    },
    {
      "task_id": "T002",
      "name": "后端核心 API 开发",
      "module": "服务层",
      "agent": "backend",
      "priority": "high",
      "status": "pending",
      "dependencies": ["T001"],
      "estimated_hours": 16,
      "description": "实现业务逻辑和 RESTful API 接口",
      "input": ["数据库模型定义", "API 接口规范"],
      "output": ["Controller 层代码", "Service 层代码", "API 文档"],
      "acceptance_criteria": [
        "所有接口通过 Postman 测试",
        "统一响应格式",
        "全局异常处理"
      ]
    },
    {
      "task_id": "T003",
      "name": "前端用户界面开发",
      "module": "表现层",
      "agent": "frontend",
      "priority": "high",
      "status": "pending",
      "dependencies": ["T001"],
      "estimated_hours": 16,
      "description": "实现前端用户界面和交互逻辑",
      "input": ["UI 设计稿", "API 接口规范"],
      "output": ["Vue/React 组件代码", "状态管理", "类型定义"],
      "acceptance_criteria": [
        "UI 与设计稿一致",
        "响应式布局适配移动端",
        "Mock 数据可正常渲染"
      ]
    },
    {
      "task_id": "T004",
      "name": "安全合规实现",
      "module": "安全层",
      "agent": "security",
      "priority": "medium",
      "status": "pending",
      "dependencies": ["T002"],
      "estimated_hours": 8,
      "description": "实现认证授权、输入验证和漏洞防护",
      "input": ["后端代码", "安全需求文档"],
      "output": ["安全配置", "认证过滤器", "安全测试报告"],
      "acceptance_criteria": [
        "JWT 认证正常",
        "接口权限控制生效",
        "OWASP Top 10 主要风险已防护"
      ]
    },
    {
      "task_id": "T005",
      "name": "集成测试与部署",
      "module": "部署层",
      "agent": "deployment_test",
      "priority": "medium",
      "status": "pending",
      "dependencies": ["T002", "T003", "T004"],
      "estimated_hours": 8,
      "description": "编写集成测试，配置部署环境",
      "input": ["完整项目代码", "部署要求"],
      "output": ["Docker Compose 配置", "CI/CD 配置", "测试报告", "部署文档"],
      "acceptance_criteria": [
        "Docker Compose 一键部署",
        "核心业务流程 E2E 测试通过",
        "部署文档完整可操作"
      ]
    },
    {
      "task_id": "T006",
      "name": "项目集成与协调",
      "module": "项目管理",
      "agent": "integration",
      "priority": "high",
      "status": "pending",
      "dependencies": ["T001", "T002", "T003", "T004", "T005"],
      "estimated_hours": 0,
      "description": "持续监控进度，协调接口对齐，解决冲突",
      "input": ["所有任务的状态报告"],
      "output": ["项目状态报告", "集成计划", "问题清单"],
      "acceptance_criteria": [
        "接口一致性 100%",
        "所有冲突及时解决",
        "项目状态透明可视"
      ]
    }
  ]
}
```
