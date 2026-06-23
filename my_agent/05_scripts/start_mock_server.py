#!/usr/bin/env python3
"""
Mock Server 自动化启动脚本
根据 OpenAPI 3.0 规范文件自动启动 Mock HTTP 服务，为前端 Agent 提供模拟接口。
使用方法：python start_mock_server.py [api_spec.yaml] [选项]

依赖：pip install pyyaml
"""

import argparse
import http.server
import json
import os
import random
import re
import string
import sys
import time
import traceback
import uuid
from datetime import datetime, timezone
from urllib.parse import urlparse, parse_qs

try:
    import yaml
except ImportError:
    print("错误: 需要 PyYAML 库来解析 OpenAPI 规范文件")
    print("请运行: pip install pyyaml")
    sys.exit(1)


MOCK_SERVER_VERSION = "1.0.0"
DEFAULT_PORT = 3000
DEFAULT_DELAY_MS = 0
DEFAULT_ERROR_RATE = 0.0


def generate_mock_value(schema, depth=0):
    """根据 JSON Schema 生成模拟数据"""
    if depth > 5:
        return None

    if schema is None:
        return None

    schema_type = schema.get("type", "string")
    example = schema.get("example")
    if example is not None:
        return example

    if schema_type == "string":
        enum_values = schema.get("enum")
        if enum_values:
            return random.choice(enum_values)
        fmt = schema.get("format", "")
        if fmt == "date-time":
            return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        if fmt == "date":
            return datetime.now(timezone.utc).strftime("%Y-%m-%d")
        if fmt == "email":
            return "user@example.com"
        if fmt == "password":
            return "password123"
        if fmt == "uri":
            return "https://example.com/resource"
        if fmt == "uuid":
            return str(uuid.uuid4())
        return "string_value"

    if schema_type == "integer":
        minimum = schema.get("minimum", 0)
        maximum = schema.get("maximum", 10000)
        return random.randint(minimum, maximum)

    if schema_type == "number":
        return round(random.uniform(0, 1000), 2)

    if schema_type == "boolean":
        return True

    if schema_type == "array":
        items_schema = schema.get("items", {})
        count = random.randint(1, 3)
        return [generate_mock_value(items_schema, depth + 1) for _ in range(count)]

    if schema_type == "object":
        result = {}
        properties = schema.get("properties", {})
        for prop_name, prop_schema in properties.items():
            result[prop_name] = generate_mock_value(prop_schema, depth + 1)
        return result

    return None


def resolve_ref(ref_path, components):
    """解析 $ref 引用"""
    if not ref_path or not ref_path.startswith("#/"):
        return None

    parts = ref_path.split("/")[1:]
    current = components
    for part in parts:
        if isinstance(current, dict) and part in current:
            current = current[part]
        else:
            return None
    return current


def resolve_schema(schema, components, visited=None):
    """递归解析 schema 中的所有 $ref"""
    if visited is None:
        visited = set()

    if not isinstance(schema, dict):
        return schema

    schema_id = str(id(schema))
    if schema_id in visited:
        return schema
    visited.add(schema_id)

    if "$ref" in schema:
        resolved = resolve_ref(schema["$ref"], components)
        if resolved:
            return resolve_schema(resolved, components, visited)
        return schema

    result = {}
    for key, value in schema.items():
        if key == "properties" and isinstance(value, dict):
            result[key] = {}
            for prop_name, prop_schema in value.items():
                result[key][prop_name] = resolve_schema(prop_schema, components, visited)
        elif key == "items" and isinstance(value, dict):
            result[key] = resolve_schema(value, components, visited)
        elif isinstance(value, dict):
            result[key] = resolve_schema(value, components, visited)
        elif isinstance(value, list):
            result[key] = [
                resolve_schema(item, components, visited) if isinstance(item, dict) else item
                for item in value
            ]
        else:
            result[key] = value

    return result


def build_mock_response(path_item, method, components):
    """构建 Mock 响应"""
    method_info = path_item.get(method, {})
    responses = method_info.get("responses", {})
    success_response = responses.get("200") or responses.get("201")

    if not success_response:
        return {"success": True, "message": "操作成功"}

    content = success_response.get("content", {})
    json_content = content.get("application/json", {})
    schema = json_content.get("schema", {})

    resolved_schema = resolve_schema(schema, components)
    mock_data = generate_mock_value(resolved_schema)

    status_code = 200
    if "201" in responses:
        status_code = 201

    return mock_data, status_code


def extract_path_params(path_pattern):
    """从路径模式中提取参数名"""
    return re.findall(r"\{(\w+)\}", path_pattern)


def match_path(request_path, path_pattern):
    """匹配请求路径与 OpenAPI 路径模式"""
    pattern = re.sub(r"\{(\w+)\}", r"(?P<\1>[^/]+)", path_pattern)
    pattern = f"^{pattern}$"
    match = re.match(pattern, request_path)
    return match


class MockRequestHandler(http.server.BaseHTTPRequestHandler):
    """Mock 请求处理器"""

    mock_routes = {}
    components = {}
    delay_ms = 0
    error_rate = 0.0
    request_log = []
    spec_title = "Mock Server"

    def log_message(self, format, *args):
        msg = format % args
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_entry = f"[{timestamp}] {msg}"
        self.request_log.append(log_entry)
        print(f"  {log_entry}")

    def _send_mock_response(self, status_code=200, content_type="application/json"):
        self.send_response(status_code)
        self.send_header("Content-Type", content_type)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
        self.send_header("X-Mock-Server-Version", MOCK_SERVER_VERSION)
        self.send_header("X-Mock-Server-Time", datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"))
        self.end_headers()

    def _handle_request(self, method):
        parsed = urlparse(self.path)
        request_path = parsed.path.rstrip("/") or "/"

        if self.delay_ms > 0:
            time.sleep(self.delay_ms / 1000.0)

        matched_route = None
        path_params = {}

        for route_pattern, route_config in self.mock_routes.items():
            match = match_path(request_path, route_pattern)
            if match:
                matched_route = route_pattern
                path_params = match.groupdict()
                break

        if matched_route is None:
            self._send_mock_response(404)
            response = {"success": False, "message": f"未找到匹配的路由: {method} {request_path}"}
            self.wfile.write(json.dumps(response, ensure_ascii=False, indent=2).encode("utf-8"))
            return

        route_config = self.mock_routes[matched_route]
        method_lower = method.lower()

        if method_lower not in route_config:
            allowed = [m.upper() for m in ["get", "post", "put", "delete", "patch"] if m in route_config]
            self._send_mock_response(405)
            self.send_header("Allow", ", ".join(allowed))
            response = {
                "success": False,
                "message": f"不支持的方法: {method}，支持的: {', '.join(allowed)}"
            }
            self.wfile.write(json.dumps(response, ensure_ascii=False, indent=2).encode("utf-8"))
            return

        if self.error_rate > 0 and random.random() < self.error_rate:
            self._send_mock_response(500)
            response = {
                "success": False,
                "message": "模拟服务器内部错误",
                "code": 500,
                "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
            }
            self.wfile.write(json.dumps(response, ensure_ascii=False, indent=2).encode("utf-8"))
            return

        try:
            mock_data, status_code = build_mock_response(route_config, method_lower, self.components)
            self._send_mock_response(status_code)
            self.wfile.write(json.dumps(mock_data, ensure_ascii=False, indent=2).encode("utf-8"))
        except Exception as e:
            self._send_mock_response(500)
            error_response = {
                "success": False,
                "message": f"Mock 数据生成失败: {str(e)}",
                "code": 500
            }
            self.wfile.write(json.dumps(error_response, ensure_ascii=False, indent=2).encode("utf-8"))

    def do_GET(self):
        self._handle_request("get")

    def do_POST(self):
        self._handle_request("post")

    def do_PUT(self):
        self._handle_request("put")

    def do_DELETE(self):
        self._handle_request("delete")

    def do_PATCH(self):
        self._handle_request("patch")

    def do_OPTIONS(self):
        self._send_mock_response(200, "text/plain")
        self.wfile.write(b"OK")


def load_openapi_spec(spec_path):
    """加载并解析 OpenAPI 规范文件"""
    print(f"  加载规范文件: {spec_path}")

    with open(spec_path, "r", encoding="utf-8") as f:
        spec = yaml.safe_load(f)

    if not spec:
        print("  错误: 规范文件为空")
        sys.exit(1)

    if "openapi" not in spec:
        print("  警告: 文件可能不是 OpenAPI 3.0 格式")

    return spec


def build_routes(spec):
    """从 OpenAPI 规范构建路由表"""
    routes = {}
    components = spec.get("components", {})
    paths = spec.get("paths", {})

    for path, path_item in paths.items():
        if not isinstance(path_item, dict):
            continue

        routes[path] = {}
        for method in ["get", "post", "put", "delete", "patch"]:
            if method in path_item:
                routes[path][method] = path_item

    return routes, components


def print_routes(routes):
    """打印路由摘要"""
    print(f"\n  {'=' * 60}")
    print(f"  已注册端点:")
    print(f"  {'=' * 60}")

    for path, path_item in sorted(routes.items()):
        methods = [m.upper() for m in ["get", "post", "put", "delete", "patch"] if m in path_item]
        tags = set()
        for method in ["get", "post", "put", "delete", "patch"]:
            if method in path_item:
                method_tags = path_item[method].get("tags", [])
                tags.update(method_tags)

        tag_str = f" [{', '.join(tags)}]" if tags else ""
        methods_str = ", ".join(methods)
        print(f"    {methods_str:20s} {path:40s}{tag_str}")

    total_endpoints = sum(len(path_item) for path_item in routes.values())
    print(f"\n  总计: {len(routes)} 个路径, {total_endpoints} 个端点")


def validate_spec(spec):
    """验证 OpenAPI 规范的基本完整性"""
    issues = []

    if "info" not in spec:
        issues.append("缺少 info 字段")

    if "paths" not in spec or not spec["paths"]:
        issues.append("缺少 paths 定义")

    if "openapi" not in spec:
        issues.append("缺少 openapi 版本声明")

    return issues


def create_parser():
    """创建命令行参数解析器"""
    parser = argparse.ArgumentParser(
        description="Mock Server - 根据 OpenAPI 规范自动启动模拟 API 服务",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  python start_mock_server.py api_spec.yaml
  python start_mock_server.py api_spec.yaml --port 8080 --delay 200
  python start_mock_server.py api_spec.yaml --error-rate 0.1 --verbose
  python start_mock_server.py --help
        """
    )
    parser.add_argument(
        "spec_file",
        nargs="?",
        default="api_spec_template.yaml",
        help="OpenAPI 3.0 规范文件路径 (默认: api_spec_template.yaml)"
    )
    parser.add_argument(
        "--port", "-p",
        type=int,
        default=DEFAULT_PORT,
        help=f"监听端口 (默认: {DEFAULT_PORT})"
    )
    parser.add_argument(
        "--host",
        default="127.0.0.1",
        help="绑定主机地址 (默认: 127.0.0.1)"
    )
    parser.add_argument(
        "--delay",
        type=int,
        default=DEFAULT_DELAY_MS,
        help="模拟响应延迟，单位毫秒 (默认: 0)"
    )
    parser.add_argument(
        "--error-rate",
        type=float,
        default=DEFAULT_ERROR_RATE,
        help="模拟错误率，0.0-1.0 (默认: 0.0)"
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="显示详细信息"
    )
    parser.add_argument(
        "--log-file",
        help="将请求日志写入文件"
    )
    return parser


def main():
    parser = create_parser()
    args = parser.parse_args()

    print(f"\n{'=' * 60}")
    print(f"  Mock Server v{MOCK_SERVER_VERSION}")
    print(f"{'=' * 60}")

    spec_path = args.spec_file
    if not os.path.exists(spec_path):
        print(f"\n  错误: 规范文件不存在: {spec_path}")
        print(f"  提示: 请指定正确的 OpenAPI 规范文件路径")
        print(f"  示例: python start_mock_server.py /path/to/api_spec.yaml\n")
        sys.exit(1)

    spec = load_openapi_spec(spec_path)

    spec_title = spec.get("info", {}).get("title", "Mock Server")
    spec_version = spec.get("info", {}).get("version", "未知")

    print(f"\n  API 规范: {spec_title}")
    print(f"  版本: {spec_version}")

    issues = validate_spec(spec)
    if issues:
        print(f"\n  规范检查发现以下问题:")
        for issue in issues:
            print(f"    ⚠️  {issue}")

    routes, components = build_routes(spec)

    if not routes:
        print("\n  错误: 规范中未定义任何 API 路径")
        sys.exit(1)

    MockRequestHandler.mock_routes = routes
    MockRequestHandler.components = components
    MockRequestHandler.delay_ms = max(0, args.delay)
    MockRequestHandler.error_rate = max(0.0, min(1.0, args.error_rate))
    MockRequestHandler.spec_title = spec_title

    if args.verbose:
        print_routes(routes)

    server = http.server.HTTPServer((args.host, args.port), MockRequestHandler)

    print(f"\n{'=' * 60}")
    print(f"  Mock Server 已启动")
    print(f"  {'=' * 60}")
    print(f"  地址: http://{args.host}:{args.port}")
    print(f"  延迟: {args.delay}ms")
    print(f"  错误率: {args.error_rate * 100:.1f}%")
    print(f"  路径数: {len(routes)}")
    print(f"  {'=' * 60}")
    print(f"  按 Ctrl+C 停止服务\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print(f"\n\n  正在停止 Mock Server...")
        server.shutdown()

        if args.log_file and MockRequestHandler.request_log:
            try:
                with open(args.log_file, "w", encoding="utf-8") as f:
                    for entry in MockRequestHandler.request_log:
                        f.write(entry + "\n")
                print(f"  请求日志已保存: {args.log_file}")
            except IOError as e:
                print(f"  警告: 无法写入日志文件: {e}")

        request_count = len(MockRequestHandler.request_log)
        print(f"  服务已停止，共处理 {request_count} 个请求")
        print(f"\n{'=' * 60}\n")


if __name__ == "__main__":
    main()
