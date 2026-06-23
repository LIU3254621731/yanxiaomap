#!/usr/bin/env python3
"""
项目初始化脚本
用于在新建项目时，通过交互式问答生成初始项目配置和任务分配表。
使用方法：python init_project.py
"""

import json
import os
import shutil
from datetime import datetime
from pathlib import Path


def print_banner():
    """打印欢迎信息"""
    print("""
    ╔══════════════════════════════════════════════╗
    ║        多智能体协作开发框架 - 项目初始化       ║
    ║         Multi-Agent Framework Setup           ║
    ╚══════════════════════════════════════════════╝
    """)


def get_input(prompt: str, default: str = "") -> str:
    """获取用户输入，支持默认值"""
    if default:
        value = input(f"{prompt} [{default}]: ").strip()
        return value if value else default
    else:
        value = input(f"{prompt}: ").strip()
        return value if value else ""


def select_option(prompt: str, options: list) -> str:
    """让用户从选项列表中选择"""
    print(f"\n{prompt}")
    for i, option in enumerate(options, 1):
        print(f"  {i}. {option}")

    while True:
        try:
            choice = int(input(f"请选择 (1-{len(options)}): "))
            if 1 <= choice <= len(options):
                return options[choice - 1]
            else:
                print(f"请输入 1-{len(options)} 之间的数字")
        except ValueError:
            print("请输入有效的数字")


def select_multiple(prompt: str, options: list) -> list:
    """让用户从选项列表中选择多个"""
    selected = []
    print(f"\n{prompt} (输入序号选择，用逗号分隔，如: 1,3,5)")
    for i, option in enumerate(options, 1):
        print(f"  {i}. {option}")

    try:
        choice = input("请选择: ").strip()
        indices = [int(c.strip()) for c in choice.split(",") if c.strip()]
        for idx in indices:
            if 1 <= idx <= len(options):
                selected.append(options[idx - 1])
    except ValueError:
        pass

    return selected


def init_project():
    """主初始化流程"""
    print_banner()

    # === 第1步：项目基本信息 ===
    print("=" * 50)
    print("【第1步】项目基本信息")
    print("=" * 50)

    project_name = get_input("请输入项目名称", "my_project")
    project_desc = get_input("请输入项目描述", "一个基于多智能体协作开发的项目")

    # === 第2步：技术栈选择 ===
    print("\n" + "=" * 50)
    print("【第2步】技术栈配置")
    print("=" * 50)

    # 前端框架
    frontend_framework = select_option(
        "请选择前端框架:",
        ["Vue 3 + TypeScript", "React 18 + TypeScript", "Angular 16 + TypeScript"]
    )

    # 后端框架
    backend_framework = select_option(
        "请选择后端框架:",
        ["Spring Boot 3.x (Java)", "Express.js (Node.js)", "FastAPI (Python)", "Go Gin"]
    )

    # 数据库
    database = select_option(
        "请选择数据库:",
        ["MySQL 8.0", "PostgreSQL 15", "MongoDB 7.0", "SQLite"]
    )

    # ORM 框架
    orm_options = {
        "Spring Boot 3.x (Java)": ["MyBatis-Plus", "Spring Data JPA"],
        "Express.js (Node.js)": ["Prisma", "TypeORM", "Sequelize"],
        "FastAPI (Python)": ["SQLAlchemy", "Django ORM"],
        "Go Gin": ["GORM", "SQLx"]
    }
    orm = select_option("请选择 ORM 框架:", orm_options.get(backend_framework, ["默认"]))

    # === 第3步：功能模块 ===
    print("\n" + "=" * 50)
    print("【第3步】核心功能模块")
    print("=" * 50)

    print("请简要描述项目的核心功能（每行一个功能，输入空行结束）：")
    features = []
    while True:
        feature = input(f"  功能{len(features) + 1}: ").strip()
        if not feature:
            break
        features.append(feature)

    if not features:
        features = ["用户认证与权限管理", "核心数据管理", "数据展示与查询"]

    # === 第4步：额外服务 ===
    print("\n" + "=" * 50)
    print("【第4步】额外服务/集成")
    print("=" * 50)

    extra_services = select_multiple(
        "需要集成的额外服务（可多选，留空跳过）:",
        ["Redis 缓存", "Elasticsearch 搜索", "RabbitMQ/Kafka 消息队列", "MinIO/S3 对象存储", "Swagger API 文档", "Prometheus + Grafana 监控"]
    )

    # === 生成配置 ===
    print("\n" + "=" * 50)
    print("正在生成项目配置...")
    print("=" * 50)

    # 创建项目目录
    project_dir = Path.cwd() / project_name
    os.makedirs(project_dir, exist_ok=True)

    # 复制 agent 框架模板
    agent_src = Path(__file__).parent.parent
    agent_dst = project_dir / ".agent"
    if agent_dst.exists():
        print(f"  ⚠️  {agent_dst} 已存在，跳过复制")
    else:
        shutil.copytree(agent_src, agent_dst)
        print(f"  ✅ 已复制 Agent 框架到 {agent_dst}")

    # 生成结构化需求文档
    now = datetime.now().isoformat()
    tech_map = {
        "Vue 3 + TypeScript": {"framework": "Vue 3", "language": "TypeScript", "ui_library": "Element Plus", "build_tool": "Vite"},
        "React 18 + TypeScript": {"framework": "React 18", "language": "TypeScript", "ui_library": "Ant Design", "build_tool": "Vite"},
        "Angular 16 + TypeScript": {"framework": "Angular 16", "language": "TypeScript", "ui_library": "Angular Material", "build_tool": "Webpack"},
    }
    backend_map = {
        "Spring Boot 3.x (Java)": {"framework": "Spring Boot 3.x", "language": "Java"},
        "Express.js (Node.js)": {"framework": "Express.js", "language": "Node.js"},
        "FastAPI (Python)": {"framework": "FastAPI", "language": "Python"},
        "Go Gin": {"framework": "Gin", "language": "Go"},
    }

    requirements = {
        "project": {
            "name": project_name,
            "description": project_desc,
            "version": "1.0.0",
            "created_at": now,
            "features": features,
            "extra_services": extra_services
        },
        "tech_stack": {
            "frontend": {
                "framework": tech_map.get(frontend_framework, {}).get("framework", frontend_framework),
                "language": tech_map.get(frontend_framework, {}).get("language", "TypeScript"),
                "ui_library": tech_map.get(frontend_framework, {}).get("ui_library", "Element Plus"),
                "build_tool": tech_map.get(frontend_framework, {}).get("build_tool", "Vite")
            },
            "backend": {
                "framework": backend_map.get(backend_framework, {}).get("framework", backend_framework),
                "language": backend_map.get(backend_framework, {}).get("language", "Java"),
                "orm": orm,
                "database": database,
                "cache": "Redis" if "Redis 缓存" in extra_services else "None"
            },
            "deployment": {
                "containerization": "Docker",
                "ci_cd": "GitHub Actions"
            }
        }
    }

    req_file = project_dir / "structured_requirements.json"
    with open(req_file, "w", encoding="utf-8") as f:
        json.dump(requirements, f, ensure_ascii=False, indent=2)
    print(f"  ✅ 已生成结构化需求文档: {req_file}")

    # 生成任务分配表
    task_assignments = {
        "project": project_name,
        "version": "1.0.0",
        "created_at": now,
        "tasks": [
            {
                "task_id": "T001",
                "name": "数据库设计与初始化",
                "module": "数据层",
                "agent": "database",
                "priority": "high",
                "status": "pending",
                "dependencies": [],
                "description": f"设计 {database} 数据库表结构，创建迁移脚本"
            },
            {
                "task_id": "T002",
                "name": "后端核心 API 开发",
                "module": "服务层",
                "agent": "backend",
                "priority": "high",
                "status": "pending",
                "dependencies": ["T001"],
                "description": f"使用 {backend_framework} 实现业务逻辑和 RESTful API"
            },
            {
                "task_id": "T003",
                "name": "前端用户界面开发",
                "module": "表现层",
                "agent": "frontend",
                "priority": "high",
                "status": "pending",
                "dependencies": [],
                "description": f"使用 {frontend_framework} 实现前端用户界面"
            },
            {
                "task_id": "T004",
                "name": "安全合规实现",
                "module": "安全层",
                "agent": "security",
                "priority": "medium",
                "status": "pending",
                "dependencies": ["T002"],
                "description": "实现认证授权、输入验证、漏洞防护"
            },
            {
                "task_id": "T005",
                "name": "容器化与 CI/CD",
                "module": "部署层",
                "agent": "deployment_test",
                "priority": "medium",
                "status": "pending",
                "dependencies": [],
                "description": "编写 Docker 和 CI/CD 配置"
            },
            {
                "task_id": "T006",
                "name": "集成测试与部署",
                "module": "测试层",
                "agent": "deployment_test",
                "priority": "medium",
                "status": "pending",
                "dependencies": ["T002", "T003", "T004"],
                "description": "编写集成测试，执行部署验证"
            },
            {
                "task_id": "T007",
                "name": "项目集成与协调",
                "module": "项目管理",
                "agent": "integration",
                "priority": "high",
                "status": "pending",
                "dependencies": [],
                "description": "持续监控进度，协调各 Agent 协作"
            }
        ]
    }

    task_file = project_dir / "task_assignments.json"
    with open(task_file, "w", encoding="utf-8") as f:
        json.dump(task_assignments, f, ensure_ascii=False, indent=2)
    print(f"  ✅ 已生成任务分配表: {task_file}")

    # 生成项目状态看板
    status_content = f"""# 项目状态看板 - {project_name}

## 项目概述
- **项目名称**: {project_name}
- **项目描述**: {project_desc}
- **启动日期**: {now.split("T")[0]}
- **当前阶段**: 需求分析
- **整体进度**: 0%

## 技术栈
- **前端**: {frontend_framework}
- **后端**: {backend_framework}
- **数据库**: {database}
- **ORM**: {orm}
- **额外服务**: {", ".join(extra_services) if extra_services else "无"}

## 任务进度面板
| 任务ID | 任务名称 | 负责人 | 优先级 | 状态 | 进度 |
|--------|---------|--------|--------|------|------|
| T001 | 数据库设计与初始化 | database | P1 | ⏳ 待开始 | 0% |
| T002 | 后端核心 API 开发 | backend | P1 | ⏳ 待开始 | 0% |
| T003 | 前端用户界面开发 | frontend | P1 | ⏳ 待开始 | 0% |
| T004 | 安全合规实现 | security | P2 | ⏳ 待开始 | 0% |
| T005 | 容器化与 CI/CD | deployment_test | P2 | ⏳ 待开始 | 0% |
| T006 | 集成测试与部署 | deployment_test | P2 | ⏳ 待开始 | 0% |
| T007 | 项目集成与协调 | integration | P1 | ⏳ 待开始 | 0% |

## 核心功能
"""
    for i, feature in enumerate(features, 1):
        status_content += f"  {i}. {feature}\n"

    status_file = project_dir / "project_status.md"
    with open(status_file, "w", encoding="utf-8") as f:
        f.write(status_content)
    print(f"  ✅ 已生成项目状态看板: {status_file}")

    print(f"\n{'=' * 50}")
    print("🎉 项目初始化完成！")
    print(f"{'=' * 50}")
    print(f"\n项目目录: {project_dir}")
    print(f"Agent 框架: {agent_dst}")
    print(f"\n下一步操作:")
    print(f"  1. 查看 {req_file.name} - 确认结构化需求")
    print(f"  2. 查看 {task_file.name} - 确认任务分配")
    print(f"  3. 根据任务分配表，使用 02_agents/ 下的提示词模板启动各 Agent")
    print(f"  4. 使用 project_status.md 跟踪项目进度")


if __name__ == "__main__":
    try:
        init_project()
    except KeyboardInterrupt:
        print("\n\n❌ 用户取消初始化")
    except Exception as e:
        print(f"\n❌ 初始化失败: {e}")
