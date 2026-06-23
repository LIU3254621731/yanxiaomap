#!/usr/bin/env python3
"""
集成检查脚本
用于检查项目中各模块的集成状态，包括接口一致性、文件完整性、依赖性等。
使用方法：python check_integration.py /path/to/project
"""

import json
import os
import re
import sys
from pathlib import Path


class IntegrationChecker:
    """集成状态检查器"""

    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.results = {
            "passed": [],
            "warnings": [],
            "errors": []
        }

    def check(self):
        """运行所有检查"""
        print(f"\n{'=' * 60}")
        print(f"  集成状态检查报告")
        print(f"  项目路径: {self.project_root}")
        print(f"{'=' * 60}\n")

        self._check_backend_config()
        self._check_frontend_config()
        self._check_docker_config()
        self._check_database_schema()
        self._check_api_consistency()
        self._check_project_files()
        self._check_agent_lifecycle()

        self._print_report()
        return len(self.results["errors"]) == 0

    def _check_backend_config(self):
        """检查后端配置"""
        print("[1/6] 检查后端配置...")

        backend_dir = self.project_root / "backend"
        if not backend_dir.exists():
            self.results["warnings"].append("未找到 backend 目录")
            return

        # 检查 application.yml 或 application.properties
        config_files = list(backend_dir.rglob("application*.yml")) + \
                       list(backend_dir.rglob("application*.yaml")) + \
                       list(backend_dir.rglob("application*.properties"))
        if config_files:
            self.results["passed"].append(f"后端配置文件存在: {config_files[0].name}")
        else:
            self.results["warnings"].append("未找到后端配置文件")

        # 检查 pom.xml 或 build.gradle
        if list(backend_dir.rglob("pom.xml")):
            self.results["passed"].append("Maven 构建配置已就绪")
        elif list(backend_dir.rglob("build.gradle")):
            self.results["passed"].append("Gradle 构建配置已就绪")
        else:
            self.results["warnings"].append("未找到构建配置文件 (pom.xml / build.gradle)")

    def _check_frontend_config(self):
        """检查前端配置"""
        print("[2/6] 检查前端配置...")

        frontend_dir = self.project_root / "frontend"
        if not frontend_dir.exists():
            self.results["warnings"].append("未找到 frontend 目录")
            return

        # 检查 package.json
        if (frontend_dir / "package.json").exists():
            self.results["passed"].append("前端 package.json 已就绪")
        else:
            self.results["errors"].append("缺少 package.json")

        # 检查 vite.config.ts 或 vue.config.js
        if list(frontend_dir.rglob("vite.config.*")):
            self.results["passed"].append("Vite 构建配置已就绪")
        elif list(frontend_dir.rglob("vue.config.*")):
            self.results["passed"].append("Vue CLI 配置已就绪")
        else:
            self.results["warnings"].append("未找到构建配置文件 (vite.config.*)")

        # 检查 src 目录
        src_dir = frontend_dir / "src"
        if src_dir.exists() and list(src_dir.iterdir()):
            self.results["passed"].append("前端源代码目录已就绪")
        else:
            self.results["warnings"].append("前端 src 目录为空或不存在")

    def _check_docker_config(self):
        """检查 Docker 配置"""
        print("[3/6] 检查 Docker 配置...")

        docker_files = list(self.project_root.rglob("Dockerfile"))
        if docker_files:
            for df in docker_files:
                self.results["passed"].append(f"Dockerfile 已就绪: {df.relative_to(self.project_root)}")
        else:
            self.results["warnings"].append("未找到 Dockerfile")

        if list(self.project_root.rglob("docker-compose*")):
            self.results["passed"].append("Docker Compose 配置已就绪")
        else:
            self.results["warnings"].append("未找到 docker-compose 配置")

    def _check_database_schema(self):
        """检查数据库 Schema"""
        print("[4/6] 检查数据库 Schema...")

        schema_files = list(self.project_root.rglob("schema.sql")) + \
                       list(self.project_root.rglob("init.sql"))
        if schema_files:
            self.results["passed"].append(f"数据库 Schema 已就绪: {schema_files[0].name}")

            # 检查基本字段约定
            with open(schema_files[0], "r", encoding="utf-8") as f:
                content = f.read().upper()
                if "DELETED_AT" in content:
                    self.results["passed"].append("Schema 包含软删除字段 (deleted_at)")
                if "CREATED_AT" in content:
                    self.results["passed"].append("Schema 包含创建时间字段 (created_at)")
                if "UPDATED_AT" in content:
                    self.results["passed"].append("Schema 包含更新时间字段 (updated_at)")
        else:
            self.results["warnings"].append("未找到数据库 Schema 文件")

    def _check_api_consistency(self):
        """检查 API 一致性（前后端接口定义）"""
        print("[5/6] 检查 API 一致性...")

        # 查找 API 定义文件
        api_files = list(self.project_root.rglob("api_spec*.yaml")) + \
                    list(self.project_root.rglob("api_spec*.yml")) + \
                    list(self.project_root.rglob("api*.ts")) + \
                    list(self.project_root.rglob("*Api*.ts")) + \
                    list(self.project_root.rglob("*Controller*.java"))

        backend_api_endpoints = set()
        frontend_api_endpoints = set()

        # 检查后端 Controller 中的 API 端点
        for controller in list(self.project_root.rglob("*Controller*.java")):
            with open(controller, "r", encoding="utf-8") as f:
                content = f.read()
                matches = re.findall(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping)\("([^"]+)"\)', content)
                for method, path in matches:
                    backend_api_endpoints.add(f"{method}:{path}")

        # 检查前端 API 文件中的调用
        for api_file in list(self.project_root.rglob("*Api*.ts")) + list(self.project_root.rglob("api*.ts")):
            try:
                with open(api_file, "r", encoding="utf-8") as f:
                    content = f.read()
                    matches = re.findall(r'`([^`]*/api/[^`]+)`', content)
                    for path in matches:
                        frontend_api_endpoints.add(path)
            except (UnicodeDecodeError, IOError):
                pass

        if not backend_api_endpoints and not frontend_api_endpoints:
            self.results["warnings"].append("未找到 API 端点定义（Controller 或 API 文件）")
            return

        # 对比一致性
        if backend_api_endpoints:
            self.results["passed"].append(f"后端定义了 {len(backend_api_endpoints)} 个 API 端点")
        if frontend_api_endpoints:
            self.results["passed"].append(f"前端使用了 {len(frontend_api_endpoints)} 个 API 端点")

        # 简化检查：路径关键词一致性
        if backend_api_endpoints and frontend_api_endpoints:
            self.results["passed"].append("前后端 API 定义均已存在，建议手动逐条核对一致性")

    def _check_project_files(self):
        """检查项目文件完整性"""
        print("[6/6] 检查项目文件完整性...")

        # 检查 README
        if list(self.project_root.rglob("README*")):
            self.results["passed"].append("项目 README 已就绪")
        else:
            self.results["warnings"].append("未找到 README 文件")

        # 检查 .gitignore
        if (self.project_root / ".gitignore").exists():
            self.results["passed"].append(".gitignore 已就绪")
        else:
            self.results["warnings"].append("未找到 .gitignore 文件")

        # 检查 .env 示例
        if list(self.project_root.rglob(".env*")):
            self.results["passed"].append("环境变量配置已就绪")
        else:
            self.results["warnings"].append("未找到环境变量配置 (.env)")

        # 检查测试文件
        test_dirs = []
        for test_pattern in ["**/test/**", "**/tests/**", "**/__test__/**", "**/*.test.*", "**/*.spec.*"]:
            if list(self.project_root.rglob(test_pattern)):
                test_dirs.append(test_pattern)
        if test_dirs:
            self.results["passed"].append("测试文件已就绪")
        else:
            self.results["warnings"].append("未找到测试文件")

    def _check_agent_lifecycle(self):
        """检查 Agent 生命周期状态"""
        print("[7/7] 检查 Agent 生命周期状态...")

        lifecycle_valid_states = {
            "created", "initialized", "running", "paused",
            "completed", "archived", "failed", "blocked",
            "waiting_for_dependency", "waiting_for_review", "waiting_for_response"
        }

        lifecycle_dir = self.project_root / ".agent" / "lifecycle"
        if not lifecycle_dir.exists():
            self.results["warnings"].append("未找到 Agent 生命周期目录 (.agent/lifecycle/)")
            return

        state_files = list(lifecycle_dir.glob("*.json"))
        if not state_files:
            self.results["warnings"].append("生命周期目录中无状态文件，Agent 可能尚未初始化")
            return

        agent_count = 0
        valid_count = 0
        invalid_states = []
        completed_count = 0
        blocked_count = 0
        failed_count = 0

        for state_file in state_files:
            agent_count += 1
            try:
                with open(state_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                agent_state = data.get("lifecycle_state", data.get("current_state", ""))
                if agent_state in lifecycle_valid_states:
                    valid_count += 1
                    if agent_state == "completed":
                        completed_count += 1
                    elif agent_state == "blocked":
                        blocked_count += 1
                    elif agent_state == "failed":
                        failed_count += 1
                else:
                    invalid_states.append(f"{state_file.stem}: {agent_state}")
            except (json.JSONDecodeError, IOError) as e:
                self.results["warnings"].append(f"无法读取生命周期文件 {state_file.name}: {e}")

        if agent_count == 0:
            self.results["warnings"].append("生命周期目录中无 Agent 状态文件")
            return

        if invalid_states:
            for inv_state in invalid_states:
                self.results["errors"].append(f"Agent 状态无效: {inv_state}")
        else:
            self.results["passed"].append(f"所有 {agent_count} 个 Agent 状态有效")
            self.results["passed"].append(f"  已完成: {completed_count}, 阻塞: {blocked_count}, 失败: {failed_count}")

        if blocked_count > 0:
            self.results["warnings"].append(f"{blocked_count} 个 Agent 处于阻塞状态，需要关注")
        if failed_count > 0:
            self.results["errors"].append(f"{failed_count} 个 Agent 处于失败状态，需要立即处理")

    def _print_report(self):
        """打印检查报告"""
        total = len(self.results["passed"]) + len(self.results["warnings"]) + len(self.results["errors"])

        print(f"\n{'=' * 60}")
        print(f"  检查完成")
        print(f"{'=' * 60}")
        print(f"  总计: {total} 项检查")
        print(f"  ✅ 通过: {len(self.results['passed'])}")
        print(f"  ⚠️  警告: {len(self.results['warnings'])}")
        print(f"  ❌ 错误: {len(self.results['errors'])}")
        print(f"{'=' * 60}\n")

        if self.results["passed"]:
            print("✅ 通过的检查:")
            for item in self.results["passed"]:
                print(f"    ✓ {item}")

        if self.results["warnings"]:
            print(f"\n⚠️  警告 (建议处理):")
            for item in self.results["warnings"]:
                print(f"    ○ {item}")

        if self.results["errors"]:
            print(f"\n❌ 错误 (必须处理):")
            for item in self.results["errors"]:
                print(f"    ✗ {item}")

        overall = "✅ 通过" if not self.results["errors"] else "❌ 需要修复"
        print(f"\n{'=' * 60}")
        print(f"  集成状态: {overall}")
        print(f"{'=' * 60}\n")


def print_usage():
    """打印使用说明"""
    print("""
集成检查工具 - 检查项目模块集成状态

用法:
    python check_integration.py [项目路径]

参数:
    项目路径   要检查的项目根目录路径（默认为当前目录）

示例:
    python check_integration.py
    python check_integration.py /path/to/my_project
    """)


def main():
    if len(sys.argv) > 1 and sys.argv[1] in ("-h", "--help"):
        print_usage()
        return

    project_path = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()

    if not os.path.exists(project_path):
        print(f"错误: 路径 '{project_path}' 不存在")
        sys.exit(1)

    checker = IntegrationChecker(project_path)
    success = checker.check()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
