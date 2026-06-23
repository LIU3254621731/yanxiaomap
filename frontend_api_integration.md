# 前端-后端API对接文档

## 概述
本文档详细说明前端（T003）与后端（T002）之间的API对接规范、调用方式和常见问题解决方案。

## 当前状态
- **后端服务端口**: 8080
- **API基础路径**: `/api`
- **前端环境配置**: `VITE_API_BASE_URL=/api`
- **主要问题**: 前端遇到502网关错误，表明后端服务可能未启动或无法访问

## 核心API接口

### 1. 地图配置接口
**用途**: 动态获取高德地图API密钥，避免密钥暴露在前端代码中

**后端接口**:
```
GET /api/map/config
```

**响应格式**:
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

**前端调用方式**:
```typescript
import { mapApi } from '@/api/mapApi'

const config = await mapApi.getAMapConfig()
```

**注意事项**:
1. API密钥从环境变量读取，需要配置`AMAP_API_KEY`环境变量
2. 如果后端返回空密钥，前端会使用开发环境默认密钥

### 2. 院校地图数据接口
**用途**: 获取地图上显示的院校点位数据，支持多维度筛选

**后端接口**:
```
GET /api/map/schools
```

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| province | string | 否 | 省份筛选，如'北京市'、'上海市' |
| city | string | 否 | 城市筛选，如'北京市'、'南京市' |
| level | string | 否 | 院校层次筛选，如'985'、'211'、'双一流'、'双非' |
| type | string | 否 | 院校类型筛选，如'综合'、'理工'、'师范' |
| belong | string | 否 | 隶属单位筛选，如'教育部'、'省属' |
| minLng | double | 否 | 经度范围最小值 |
| maxLng | double | 否 | 经度范围最大值 |
| minLat | double | 否 | 纬度范围最小值 |
| maxLat | double | 否 | 纬度范围最大值 |

**响应格式**:
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

**前端调用方式**:
```typescript
import { mapApi } from '@/api/mapApi'

const params = {
  province: '北京市',
  level: '985'
}
const schools = await mapApi.getMapSchools(params)
```

### 3. 院校详情接口
**用途**: 获取院校详细信息，包括专业、招生数据等

**后端接口**:
```
GET /api/schools/{schoolId}
```

**响应格式**:
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

### 4. 专业详情接口
**用途**: 获取专业详细信息

**后端接口**:
```
GET /api/majors/{majorId}
```

### 5. 多校对比接口
**用途**: 对比多所院校的专业、招生数据

**后端接口**:
```
POST /api/compare
```

**请求体**:
```json
{
  "schoolIds": [1, 2, 3],
  "majorIds": [101, 102],
  "year": 2024
}
```

## 前端TypeScript类型定义

已根据后端实体定义了TypeScript类型，确保类型安全：

```typescript
// School类型
interface School {
  id: number
  code: string
  name: string
  enrollmentUnit: string
  level: '985' | '211' | '双一流' | '双非' | '普通本科'
  type: string
  belong: string
  province: string
  city: string
  address: string
  longitude: number
  latitude: number
  introduction?: string
  website?: string
  establishedYear?: number
  status: 0 | 1
  createdAt: string
  updatedAt: string
}

// 地图学校标记类型
interface SchoolMarker {
  id: number
  name: string
  longitude: number
  latitude: number
  level: string
  type: string
  province: string
  city: string
}

// API响应类型
interface BaseResponse<T = any> {
  success: boolean
  message: string
  data?: T
  code?: string
}
```

## 环境配置

### 前端环境变量 (.env.development)
```
VITE_API_BASE_URL=/api
VITE_APP_TITLE=考研院校地图
VITE_APP_VERSION=1.0.0
VITE_MAP_KEY=default_dev_key_should_be_from_backend
VITE_NODE_ENV=development
```

### 后端环境变量 (.env.example)
```
AMAP_API_KEY=your-amap-api-key
AMAP_SECURITY_KEY=your-security-key
DATABASE_URL=jdbc:mysql://localhost:3306/yanxiaomap
DATABASE_USERNAME=root
DATABASE_PASSWORD=root
REDIS_HOST=localhost
REDIS_PORT=6379
```

## 调试与问题解决

### 常见问题

#### 1. 502网关错误
**症状**: 前端收到502错误响应
**可能原因**:
- 后端服务未启动
- 网络配置问题
- 代理配置错误

**解决方案**:
1. 确认后端服务已启动并运行在8080端口
   ```bash
   # 检查端口占用
   netstat -ano | findstr :8080
   ```
2. 直接访问后端接口测试
   ```
   http://localhost:8080/api/map/config
   ```
3. 检查前端代理配置是否正确

#### 2. CORS跨域错误
**症状**: 浏览器控制台显示CORS错误
**解决方案**:
1. 确认后端SecurityConfig允许跨域请求
2. 检查CORS配置中的`allowedOrigins`设置
3. 确认前端请求包含正确的Origin头

#### 3. 响应格式不匹配
**症状**: 前端无法解析响应数据
**解决方案**:
1. 确认后端返回的响应格式符合`BaseResponse`规范
2. 检查前端响应拦截器的处理逻辑
3. 使用Swagger文档验证API响应格式

### 调试步骤
1. **检查网络请求**
   - 在浏览器开发者工具中查看Network标签页
   - 检查请求URL、请求头、响应状态码
   - 查看响应内容格式

2. **后端日志检查**
   - 查看后端应用日志
   - 检查是否有异常抛出
   - 验证数据库连接是否正常

3. **独立测试API**
   ```bash
   # 使用curl测试API
   curl http://localhost:8080/api/map/config
   ```

## 数据格式一致性

### 枚举值映射
前端与后端保持一致的枚举值定义：

| 字段 | 后端值 | 前端TypeScript类型 |
|------|--------|-------------------|
| level | '985' | '985' |
| level | '211' | '211' |
| level | '双一流' | '双一流' |
| level | '普通本科' | '普通本科' |
| status | 0 | 0 (禁用) |
| status | 1 | 1 (启用) |

### 字段命名约定
- 数据库字段: 下划线命名法 (`created_at`)
- Java实体字段: 驼峰命名法 (`createdAt`)
- TypeScript字段: 驼峰命名法 (`createdAt`)

## 后续优化建议

### 短期优化
1. **启动后端服务** - 解决502错误的关键
2. **完善错误处理** - 提供更友好的错误提示
3. **添加接口缓存** - 减少重复请求

### 中期优化
1. **API版本管理** - 添加API版本前缀 (`/api/v1/`)
2. **请求限流** - 防止API滥用
3. **响应压缩** - 减少数据传输量

### 长期优化
1. **GraphQL支持** - 更灵活的数据查询
2. **WebSocket实时更新** - 实时数据推送
3. **API文档自动化** - 自动生成最新API文档

## 联系与支持

- **后端负责人**: backend Agent
- **前端负责人**: frontend Agent
- **数据库负责人**: database Agent
- **集成协调**: integration Agent

遇到问题时，请及时在`project_status.md`中报告，并@相关负责人。

---

*最后更新: 2026-04-18*
*文档版本: v1.0*