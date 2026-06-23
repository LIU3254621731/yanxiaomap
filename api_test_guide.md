# 前后端接口联调测试指南

## 概述
本文档提供前后端接口联调的详细测试步骤、验证方法和问题排查指南。

## 测试前提条件

### 环境要求
1. **后端服务运行** - 通过以下任一方式启动：
   - Docker方案：`docker-compose up --build -d`（需先安装Docker Desktop）
   - Java 11本地方案：安装JDK 11，运行 `.\mvnw.cmd spring-boot:run`
2. **前端开发环境** - Vue 3项目已启动：`npm run dev`
3. **网络连通** - 前端能访问后端服务（默认：http://localhost:8080）

### 服务验证
启动后验证服务是否正常运行：
```bash
# 验证后端服务
curl http://localhost:8080/actuator/health
# 预期响应: {"status":"UP"}

# 验证数据库连接（通过API）
curl http://localhost:8080/api/map/config
# 预期响应: {"success":true,"message":"获取高德地图配置成功","data":{...}}
```

## 核心API测试步骤

### 1. 高德地图配置接口
**接口**: `GET /api/map/config`

**测试目的**: 验证后端服务基本功能和高德地图密钥配置

**请求**:
```bash
curl -X GET "http://localhost:8080/api/map/config"
```

**预期响应**:
```json
{
  "success": true,
  "message": "获取高德地图配置成功",
  "data": {
    "apiKey": "your-amap-api-key",
    "securityKey": "your-security-key",
    "version": "2.0",
    "plugin": "AMap.Geocoder,AMap.AutoComplete,AMap.PlaceSearch",
    "appName": "研校地图-demo"
  }
}
```

**验证点**:
- ✅ HTTP状态码: 200
- ✅ 响应格式符合`BaseResponse`规范
- ✅ `success`字段为`true`
- ✅ `data`中包含`apiKey`字段

### 2. 院校地图数据接口
**接口**: `GET /api/map/schools`

**测试目的**: 验证地图点位数据查询和筛选功能

**测试用例**:

**用例1**: 基本查询（无参数）
```bash
curl -X GET "http://localhost:8080/api/map/schools"
```

**用例2**: 省份筛选
```bash
curl -X GET "http://localhost:8080/api/map/schools?province=北京市"
```

**用例3**: 院校层次筛选
```bash
curl -X GET "http://localhost:8080/api/map/schools?level=985"
```

**用例4**: 组合筛选
```bash
curl -X GET "http://localhost:8080/api/map/schools?province=北京市&level=985&type=综合"
```

**预期响应**:
```json
{
  "success": true,
  "message": "获取院校点位数据成功",
  "data": [
    {
      "id": 1,
      "code": "10001",
      "name": "清华大学",
      "province": "北京市",
      "city": "北京市",
      "longitude": 116.32644,
      "latitude": 40.00301,
      "level": "985",
      "type": "综合",
      "belong": "教育部",
      "status": 1
    }
  ]
}
```

**验证点**:
- ✅ 响应包含`data`数组
- ✅ 数组中的每个对象包含必需字段：`id`, `name`, `longitude`, `latitude`, `level`
- ✅ 筛选条件生效（如指定省份时只返回该省份院校）

### 3. 院校详情接口
**接口**: `GET /api/schools/{schoolId}`

**测试目的**: 验证院校详细信息查询

**请求**:
```bash
curl -X GET "http://localhost:8080/api/schools/1"
```

**预期响应**:
```json
{
  "success": true,
  "message": "查询成功",
  "data": {
    "school": {
      "id": 1,
      "code": "10001",
      "name": "清华大学",
      "enrollmentUnit": "清华大学研究生院",
      "level": "985",
      "type": "综合",
      "belong": "教育部",
      "province": "北京市",
      "city": "北京市",
      "address": "北京市海淀区清华园1号",
      "longitude": 116.32644,
      "latitude": 40.00301,
      "introduction": "清华大学简介...",
      "website": "https://www.tsinghua.edu.cn",
      "establishedYear": 1911,
      "status": 1
    }
  }
}
```

### 4. 多校对比接口
**接口**: `POST /api/compare`

**测试目的**: 验证多院校、多专业对比功能

**请求**:
```bash
curl -X POST "http://localhost:8080/api/compare" \
  -H "Content-Type: application/json" \
  -d '{
    "schoolIds": [1, 2, 3],
    "majorIds": [101, 102],
    "year": 2024
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "对比分析完成",
  "data": {
    "schools": [...],
    "majors": [...],
    "admissionData": [...],
    "statistics": {...}
  }
}
```

## 前端测试方法

### 1. 使用Vue组件测试
在前端代码中，确保API调用配置正确：

```typescript
// 在mapApi.ts中测试
import { mapApi } from '@/api/mapApi'

async function testApi() {
  try {
    // 测试配置接口
    const config = await mapApi.getAMapConfig()
    console.log('地图配置:', config)
    
    // 测试院校数据接口
    const schools = await mapApi.getMapSchools({ province: '北京市' })
    console.log('院校数据:', schools)
    
    // 测试详情接口
    const detail = await mapApi.getSchoolDetail(1)
    console.log('院校详情:', detail)
  } catch (error) {
    console.error('API测试失败:', error)
  }
}
```

### 2. 切换模拟数据模式
前端已实现模拟数据回退机制，测试时确保使用真实API：

```typescript
// 在request.ts中，确保useMock为false
const useMock = false; // 设置为false使用真实API

// 或者在.env.development中配置
VITE_USE_MOCK_API=false
```

### 3. 浏览器开发者工具检查
1. 打开浏览器开发者工具（F12）
2. 切换到Network（网络）标签页
3. 执行前端操作触发API调用
4. 检查请求和响应：
   - 请求URL是否正确
   - 请求方法（GET/POST）
   - 请求参数
   - 响应状态码
   - 响应数据格式

## 常见问题排查

### 问题1: 502 Bad Gateway
**症状**: 前端收到502错误
**可能原因**: 
- 后端服务未启动
- 端口冲突（8080端口被占用）
- 网络代理配置错误

**解决方案**:
1. 检查后端服务状态：
   ```bash
   # 检查端口占用
   netstat -ano | findstr :8080
   
   # 如果端口被占用，停止占用进程或修改服务端口
   # 在application.yml中修改server.port
   ```
2. 重启后端服务
3. 检查前端代理配置

### 问题2: CORS跨域错误
**症状**: 浏览器控制台显示CORS策略错误
**解决方案**:
1. 确认后端SecurityConfig允许跨域
2. 检查CORS配置中的`allowedOrigins`包含前端地址
3. 在前端请求中添加CORS头（开发服务器通常已配置代理）

### 问题3: 响应格式不匹配
**症状**: 前端无法解析响应数据
**解决方案**:
1. 检查后端响应格式是否符合`BaseResponse`规范
2. 验证TypeScript类型定义与后端实体匹配
3. 使用Swagger文档验证API响应格式

### 问题4: 数据库连接错误
**症状**: 后端启动失败，数据库连接异常
**解决方案**:
1. 检查MySQL服务是否运行
2. 验证数据库连接配置（用户名、密码、数据库名）
3. 确认数据库表结构已创建

## 自动化测试脚本

### HTTP测试脚本 (test_api.sh)
```bash
#!/bin/bash
# 后端API测试脚本

BASE_URL="http://localhost:8080"

echo "=== 开始API测试 ==="

# 测试1: 健康检查
echo "测试1: 健康检查"
curl -s "$BASE_URL/actuator/health" | jq .
echo ""

# 测试2: 地图配置
echo "测试2: 地图配置接口"
curl -s "$BASE_URL/api/map/config" | jq .
echo ""

# 测试3: 院校数据
echo "测试3: 院校地图数据"
curl -s "$BASE_URL/api/map/schools?province=北京市&level=985" | jq '.data | length'
echo "返回院校数量: 见上方"
echo ""

# 测试4: 院校详情
echo "测试4: 院校详情"
curl -s "$BASE_URL/api/schools/1" | jq '.data.school.name'
echo "院校名称: 见上方"
echo ""

echo "=== API测试完成 ==="
```

### Windows批处理脚本 (test_api.bat)
```batch
@echo off
echo === 开始API测试 ===
echo.

echo 测试1: 健康检查
curl -s http://localhost:8080/actuator/health
echo.

echo 测试2: 地图配置接口
curl -s http://localhost:8080/api/map/config
echo.

echo 测试3: 院校地图数据
curl -s "http://localhost:8080/api/map/schools?province=北京市&level=985"
echo.

echo 测试4: 院校详情
curl -s http://localhost:8080/api/schools/1
echo.

echo === API测试完成 ===
pause
```

## 联调验收标准

### 必须通过的项目
1. ✅ 所有核心API返回HTTP 200状态码
2. ✅ 响应格式符合`BaseResponse`规范
3. ✅ 数据字段与TypeScript类型定义匹配
4. ✅ 筛选条件正确生效
5. ✅ 错误处理机制正常（如查询不存在的ID返回适当错误）

### 推荐测试项目
1. ✅ 边界测试：空参数、无效参数、边界值
2. ✅ 性能测试：大量数据查询响应时间
3. ✅ 并发测试：多个接口同时调用
4. ✅ 安全测试：敏感数据不暴露

## 联系与支持

遇到问题时，请参考以下资源：
1. **API文档**: [frontend_api_integration.md](frontend_api_integration.md)
2. **项目状态**: [project_status.md](project_status.md)
3. **后端代码**: backend/src/main/java/com/yanxiaomap/
4. **前端代码**: frontend/src/

**责任人**:
- 后端问题: backend Agent
- 前端问题: frontend Agent  
- 数据问题: database Agent
- 协调问题: integration Agent

---

*最后更新: 2026-04-18*
*文档版本: v1.0*