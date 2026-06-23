# 前端API接口集成指南

## 概述
本文档详细说明前端Vue3应用与后端Spring Boot API的接口集成规范，供前后端联调、integration Agent协调和测试使用。

## 基础配置

### 1. API基础URL
- 开发环境：`http://localhost:8080/api`
- 生产环境：根据部署配置调整
- 前端配置位置：`src/api/request.ts` 中的 `baseURL`

### 2. 请求/响应格式
- **请求头**：自动添加 `Content-Type: application/json`
- **响应格式**：统一使用 `BaseResponse<T>` 格式
  ```typescript
  interface BaseResponse<T = any> {
    success: boolean
    message: string
    data?: T
    errors?: Record<string, string[]>
    timestamp: string
  }
  ```
- **错误处理**：HTTP状态码非2xx时，前端自动转换为错误响应

### 3. 模拟数据回退机制
当后端API不可用时，前端将使用内置的模拟数据确保功能可用：
- 地图数据：约50所院校的模拟数据
- 专业详情：根据院校ID和专业ID生成模拟数据
- 对比数据：支持最多4所院校的对比模拟
- 高德地图：使用测试密钥（仅用于开发演示）

## API接口详情

### 1. 高德地图配置接口

**用途**：获取高德地图JS API的安全配置信息

**前端API模块**：`src/api/mapApi.ts` → `getAMapConfig()`

**请求**：
- 方法：`GET`
- 路径：`/map/config`
- 参数：无

**成功响应**：
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "apiKey": "string",
    "securityKey": "string",
    "version": "2.0",
    "plugin": "string",
    "appName": "string"
  },
  "timestamp": "2026-04-18T09:50:00Z"
}
```

**模拟数据回退**：
- API失败时返回测试密钥：`f2a14d5a5748760eea937b4a756d6e81`
- 确保地图功能在开发阶段可用

### 2. 地图院校数据接口

**用途**：获取地图上显示的院校列表，支持省份和院校层次筛选

**前端API模块**：`src/api/mapApi.ts` → `getMapSchools(params)`

**请求**：
- 方法：`GET`
- 路径：`/map/schools`
- 查询参数：
  ```typescript
  interface MapSchoolsParams {
    province?: string  // 省份名称，如"北京"
    level?: string     // 院校层次：'985' | '211' | '双一流' | '双非'
    keyword?: string   // 关键词搜索（前端本地筛选）
  }
  ```

**成功响应**：
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "name": "清华大学",
      "code": "10001",
      "level": "985",
      "province": "北京",
      "city": "北京",
      "type": "综合类",
      "belong": "教育部",
      "longitude": 116.327,
      "latitude": 39.999,
      "website": "https://www.tsinghua.edu.cn",
      "logo": "https://example.com/logo.png",
      "status": "active",
      "createdAt": "2026-04-17T10:00:00Z",
      "updatedAt": "2026-04-17T10:00:00Z",
      "deletedAt": null
    }
  ],
  "timestamp": "2026-04-18T09:50:00Z"
}
```

**前端处理逻辑**：
1. 首次加载时调用API获取所有院校
2. 用户筛选时重新调用API（省份、层次）
3. 关键词搜索在前端本地执行（性能优化）
4. 边界筛选（地图视口变化）通过防抖机制调用API

### 3. 院校专业详情接口

**用途**：获取院校和专业的详细信息，包括招生数据

**前端API模块**：`src/api/schoolApi.ts` → `getSchoolMajorDetail(params)`

**请求**：
- 方法：`GET`
- 路径：`/schools/{schoolId}/majors/{majorId}/detail`
- 查询参数：
  ```typescript
  interface SchoolMajorDetailParams {
    schoolId: number
    majorId: number
  }
  ```

**成功响应**：
```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "school": { /* School对象 */ },
    "major": { /* Major对象 */ },
    "admissionData": [
      {
        "id": 1,
        "year": 2025,
        "score": 380,
        "enroll": 50,
        "apply": 200,
        "status": "active",
        "createdAt": "2026-04-17T10:00:00Z",
        "updatedAt": "2026-04-17T10:00:00Z",
        "deletedAt": null
      }
    ]
  },
  "timestamp": "2026-04-18T09:50:00Z"
}
```

**前端页面**：`src/views/DetailView.vue`
**支持功能**：
- 院校基本信息展示
- 专业详情展示
- 历年招生数据表格
- 添加到对比功能

### 4. 批量院校专业详情接口

**用途**：同时获取多个院校专业的详细信息，用于对比功能

**前端API模块**：`src/api/compareApi.ts` → `getBatchSchoolMajorDetails(items)`

**请求**：
- 方法：`POST`
- 路径：`/compare/batch`
- 请求体：
  ```typescript
  interface BatchCompareItem {
    schoolId: number
    majorId: number
  }
  
  // 请求体：BatchCompareItem[]
  ```

**成功响应**：
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "school": { /* School对象 */ },
      "major": { /* Major对象 */ },
      "admissionData": [ /* AdmissionData[] */ ]
    }
  ],
  "timestamp": "2026-04-18T09:50:00Z"
}
```

**前端页面**：`src/views/CompareView.vue`
**对比维度**：
- 院校基本信息（名称、层次、所在地）
- 专业信息（名称、代码、学位类型）
- 招生数据（分数线、招生人数、报录比）
- 支持按年份筛选对比数据

## 前端状态管理（Pinia）

### 1. 地图状态管理（useMapStore）
**位置**：`src/stores/useMapStore.ts`
**功能**：
- 管理当前显示的院校列表
- 筛选状态（省份、层次、关键词）
- 地图边界状态
- 选中的院校标记

### 2. 搜索状态管理（useSearchStore）
**位置**：`src/stores/useSearchStore.ts`
**功能**：
- 搜索历史记录
- 热门搜索关键词
- 搜索建议缓存

### 3. 对比状态管理（useCompareStore）
**位置**：`src/stores/useCompareStore.ts`
**功能**：
- 对比项列表（最多4个）
- 选中年份
- 对比表格数据生成
- 差异高亮设置

## 类型定义（TypeScript）

### 核心类型定义
**位置**：`src/types/school.ts`
```typescript
// 院校
interface School {
  id: number
  name: string
  code: string
  level: '985' | '211' | '双一流' | '双非'
  province: string
  city: string
  type: string
  belong: string  // 隶属单位
  longitude?: number
  latitude?: number
  website?: string
  logo?: string
  status: string
  createdAt: string
  updatedAt: string
  deletedAt?: string
}

// 专业
interface Major {
  id: number
  schoolId: number
  name: string
  code: string
  degree: string
  duration: number
  description?: string
  status: string
  createdAt: string
  updatedAt: string
  deletedAt?: string
}

// 招生数据
interface AdmissionData {
  id: number
  majorId: number
  year: number
  score: number
  enroll: number
  apply: number
  status: string
  createdAt: string
  updatedAt: string
  deletedAt?: string
}
```

### API响应类型
**位置**：`src/types/api.ts`
```typescript
// 基础响应
interface BaseResponse<T = any> {
  success: boolean
  message: string
  data?: T
  errors?: Record<string, string[]>
  timestamp: string
}

// 地图配置
interface AMapConfig {
  apiKey: string
  securityKey: string
  version?: string
  plugin?: string
  appName?: string
}
```

## 前端组件说明

### 1. 地图容器组件
**位置**：`src/components/map/MapContainer.vue`
**功能**：
- 初始化高德地图
- 渲染院校标记
- 处理地图事件（缩放、平移）
- 边界筛选（防抖500ms）
- 自适应标记渲染（简单/详细模式）

### 2. 院校标记组件
**位置**：`src/components/map/SchoolMarker.vue`
**功能**：
- 单个院校标记显示
- 点击显示信息窗口
- 标记样式根据院校层次变化
- 支持点击添加到对比

### 3. 信息窗口组件
**位置**：`src/components/map/MapInfoWindow.vue`
**功能**：
- 显示院校基本信息
- 提供操作按钮（查看详情、添加到对比）
- 响应式设计

## 前后端联调清单

### 环境准备
- [ ] 后端服务启动在 `http://localhost:8080`
- [ ] 前端服务启动在 `http://localhost:3000`
- [ ] CORS配置允许前端域名
- [ ] 数据库连接正常
- [ ] 高德地图密钥配置正确

### 接口测试顺序
1. **地图配置接口**：验证高德地图密钥获取
2. **院校数据接口**：验证基础数据返回
3. **筛选功能**：验证省份和层次筛选
4. **详情接口**：验证单个院校专业详情
5. **对比接口**：验证批量数据获取

### 数据验证要点
1. **类型匹配**：确保后端返回数据与前端`School`、`Major`、`AdmissionData`类型一致
2. **字段完整性**：关键字段（id、name、level、province等）必须存在
3. **空值处理**：可选字段（website、logo等）正确处理null/undefined
4. **分页支持**：大量数据时是否需要分页（当前前端未实现分页）

## 故障排查指南

### 常见问题
1. **CORS错误**：检查后端CORS配置，允许前端origin
2. **404错误**：确认API路径与前端请求路径一致
3. **类型错误**：检查返回数据格式是否与TypeScript定义匹配
4. **地图加载失败**：验证高德地图密钥是否正确配置
5. **数据不一致**：检查数据库与实体类映射关系

### 前端调试工具
1. **浏览器开发者工具**：查看网络请求和响应
2. **Vue DevTools**：检查组件状态和Props
3. **Pinia DevTools**：查看状态管理变化
4. **控制台日志**：前端API调用详细日志

## 后续优化建议

### 性能优化
1. **地图标记聚类**：院校数量多时使用MarkerCluster提升性能
2. **虚拟滚动**：详情和对比列表大数据量时使用虚拟滚动
3. **图片懒加载**：院校logo等图片资源懒加载
4. **API缓存**：频繁请求的数据添加本地缓存

### 功能增强
1. **用户收藏**：允许用户收藏院校和专业
2. **个性化推荐**：基于用户行为推荐院校
3. **数据导出**：支持对比数据导出为Excel/PDF
4. **移动端PWA**：支持离线访问和安装到桌面

## 联系方式

- **前端负责人**：frontend Agent (T003)
- **后端负责人**：backend Agent (T002)
- **集成协调**：integration Agent
- **问题反馈**：通过项目状态文档更新问题进展

---

*文档版本：1.0*
*最后更新：2026-04-18*
*更新者：frontend Agent (T003)*