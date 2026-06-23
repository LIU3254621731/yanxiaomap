# 前端API测试示例与指南

## 概述
本文档为frontend Agent提供具体的前端API测试示例、代码片段和调试技巧，帮助快速进行前后端接口联调。

## 测试环境配置

### 1. 确保使用真实API模式
在开始测试前，确保前端配置为使用真实API而不是模拟数据：

```typescript
// frontend/src/api/request.ts
// 确保useMock为false
const useMock = false;

// 或者通过环境变量控制
const useMock = import.meta.env.VITE_USE_MOCK_API === 'true';
```

### 2. 验证API基础URL
```typescript
// frontend/src/api/request.ts
// 确保baseURL正确指向后端服务
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

// 开发环境下，确保代理配置正确
// frontend/vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

## API测试示例代码

### 1. 地图配置接口测试
```vue
<template>
  <div>
    <button @click="testMapConfig">测试地图配置接口</button>
    <div v-if="configResult">
      <h3>配置信息:</h3>
      <pre>{{ configResult }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mapApi } from '@/api/mapApi'

const configResult = ref<any>(null)

async function testMapConfig() {
  try {
    console.log('开始测试地图配置接口...')
    const result = await mapApi.getAMapConfig()
    configResult.value = result
    
    if (result.success) {
      console.log('✅ 地图配置接口测试成功:', result.data)
      // 验证必要字段
      if (result.data.apiKey) {
        console.log('✅ API密钥获取成功')
      }
    } else {
      console.error('❌ 接口返回失败:', result.message)
    }
  } catch (error) {
    console.error('❌ 接口调用异常:', error)
  }
}
</script>
```

### 2. 院校地图数据接口测试
```vue
<template>
  <div>
    <div>
      <input v-model="province" placeholder="省份（如：北京市）" />
      <input v-model="level" placeholder="院校层次（如：985）" />
      <button @click="testSchoolsData">测试院校数据接口</button>
    </div>
    
    <div v-if="schoolsResult">
      <h3>查询结果:</h3>
      <p>院校数量: {{ schoolsResult.data?.length || 0 }}</p>
      <div v-for="school in schoolsResult.data?.slice(0, 5)" :key="school.id">
        {{ school.name }} ({{ school.level }}) - {{ school.province }}
      </div>
      <div v-if="schoolsResult.data && schoolsResult.data.length > 5">
        ... 还有 {{ schoolsResult.data.length - 5 }} 条数据
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mapApi } from '@/api/mapApi'
import type { SchoolMarker } from '@/api/mapApi'

const province = ref('')
const level = ref('')
const schoolsResult = ref<{ success: boolean; message: string; data?: SchoolMarker[] }>()

async function testSchoolsData() {
  try {
    const params: any = {}
    if (province.value) params.province = province.value
    if (level.value) params.level = level.value
    
    console.log('开始测试院校数据接口，参数:', params)
    const result = await mapApi.getMapSchools(params)
    schoolsResult.value = result
    
    if (result.success) {
      console.log(`✅ 院校数据接口测试成功，返回 ${result.data?.length || 0} 条数据`)
      
      // 验证数据格式
      if (result.data && result.data.length > 0) {
        const firstSchool = result.data[0]
        console.log('✅ 数据格式验证通过，样例数据:', {
          id: firstSchool.id,
          name: firstSchool.name,
          longitude: firstSchool.longitude,
          latitude: firstSchool.latitude,
          level: firstSchool.level
        })
      }
    } else {
      console.error('❌ 接口返回失败:', result.message)
    }
  } catch (error) {
    console.error('❌ 接口调用异常:', error)
  }
}
</script>
```

### 3. 院校详情接口测试
```vue
<template>
  <div>
    <input v-model.number="schoolId" type="number" placeholder="院校ID" />
    <button @click="testSchoolDetail">测试院校详情接口</button>
    
    <div v-if="detailResult">
      <h3>院校详情:</h3>
      <div v-if="detailResult.success">
        <p><strong>名称:</strong> {{ detailResult.data?.school?.name }}</p>
        <p><strong>层次:</strong> {{ detailResult.data?.school?.level }}</p>
        <p><strong>类型:</strong> {{ detailResult.data?.school?.type }}</p>
        <p><strong>地址:</strong> {{ detailResult.data?.school?.address }}</p>
        <p><strong>简介:</strong> {{ detailResult.data?.school?.introduction?.substring(0, 100) }}...</p>
      </div>
      <div v-else>
        <p style="color: red">{{ detailResult.message }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mapApi } from '@/api/mapApi'

const schoolId = ref(1)
const detailResult = ref<any>(null)

async function testSchoolDetail() {
  if (!schoolId.value) {
    alert('请输入院校ID')
    return
  }
  
  try {
    console.log(`开始测试院校详情接口，ID: ${schoolId.value}`)
    const result = await mapApi.getSchoolDetail(schoolId.value)
    detailResult.value = result
    
    if (result.success) {
      console.log('✅ 院校详情接口测试成功:', result.data?.school?.name)
    } else {
      console.error('❌ 接口返回失败:', result.message)
    }
  } catch (error) {
    console.error('❌ 接口调用异常:', error)
  }
}
</script>
```

## 完整测试页面示例

创建一个专门的测试页面，用于批量测试所有API：

```vue
<!-- frontend/src/views/ApiTestView.vue -->
<template>
  <div class="api-test-container">
    <h1>API接口测试页面</h1>
    
    <div class="test-section">
      <h2>1. 地图配置接口</h2>
      <button @click="testMapConfig">测试</button>
      <div v-if="testResults.mapConfig">
        <p :class="testResults.mapConfig.success ? 'success' : 'error'">
          {{ testResults.mapConfig.message }}
        </p>
        <pre v-if="testResults.mapConfig.success">{{ testResults.mapConfig.data }}</pre>
      </div>
    </div>
    
    <div class="test-section">
      <h2>2. 院校地图数据接口</h2>
      <div class="filter-controls">
        <input v-model="testParams.province" placeholder="省份" />
        <input v-model="testParams.level" placeholder="层次" />
        <button @click="testSchoolsData">测试</button>
      </div>
      <div v-if="testResults.schoolsData">
        <p :class="testResults.schoolsData.success ? 'success' : 'error'">
          {{ testResults.schoolsData.message }}
        </p>
        <p v-if="testResults.schoolsData.success">
          返回 {{ testResults.schoolsData.data?.length || 0 }} 条数据
        </p>
      </div>
    </div>
    
    <div class="test-section">
      <h2>3. 院校详情接口</h2>
      <input v-model.number="testParams.schoolId" type="number" placeholder="院校ID" />
      <button @click="testSchoolDetail">测试</button>
      <div v-if="testResults.schoolDetail">
        <p :class="testResults.schoolDetail.success ? 'success' : 'error'">
          {{ testResults.schoolDetail.message }}
        </p>
      </div>
    </div>
    
    <div class="test-section">
      <h2>批量测试</h2>
      <button @click="runAllTests">运行所有测试</button>
      <div v-if="allTestsResult">
        <h3>批量测试结果:</h3>
        <ul>
          <li v-for="(result, name) in allTestsResult" :key="name">
            {{ name }}: <span :class="result.success ? 'success' : 'error'">{{ result.success ? '✅' : '❌' }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { mapApi } from '@/api/mapApi'

interface TestResult {
  success: boolean
  message: string
  data?: any
}

const testParams = reactive({
  province: '北京市',
  level: '985',
  schoolId: 1
})

const testResults = reactive({
  mapConfig: null as TestResult | null,
  schoolsData: null as TestResult | null,
  schoolDetail: null as TestResult | null
})

const allTestsResult = ref<Record<string, boolean> | null>(null)

async function testMapConfig() {
  try {
    const result = await mapApi.getAMapConfig()
    testResults.mapConfig = result
  } catch (error) {
    testResults.mapConfig = {
      success: false,
      message: `调用异常: ${error}`
    }
  }
}

async function testSchoolsData() {
  try {
    const params: any = {}
    if (testParams.province) params.province = testParams.province
    if (testParams.level) params.level = testParams.level
    
    const result = await mapApi.getMapSchools(params)
    testResults.schoolsData = result
  } catch (error) {
    testResults.schoolsData = {
      success: false,
      message: `调用异常: ${error}`
    }
  }
}

async function testSchoolDetail() {
  try {
    const result = await mapApi.getSchoolDetail(testParams.schoolId)
    testResults.schoolDetail = result
  } catch (error) {
    testResults.schoolDetail = {
      success: false,
      message: `调用异常: ${error}`
    }
  }
}

async function runAllTests() {
  console.log('开始批量测试所有API...')
  
  const results: Record<string, boolean> = {}
  
  // 测试地图配置
  try {
    const configResult = await mapApi.getAMapConfig()
    results.mapConfig = configResult.success
    console.log('地图配置测试:', configResult.success ? '✅' : '❌')
  } catch (error) {
    results.mapConfig = false
    console.error('地图配置测试失败:', error)
  }
  
  // 测试院校数据
  try {
    const schoolsResult = await mapApi.getMapSchools({ province: '北京市' })
    results.schoolsData = schoolsResult.success
    console.log('院校数据测试:', schoolsResult.success ? '✅' : '❌')
  } catch (error) {
    results.schoolsData = false
    console.error('院校数据测试失败:', error)
  }
  
  // 测试院校详情
  try {
    const detailResult = await mapApi.getSchoolDetail(1)
    results.schoolDetail = detailResult.success
    console.log('院校详情测试:', detailResult.success ? '✅' : '❌')
  } catch (error) {
    results.schoolDetail = false
    console.error('院校详情测试失败:', error)
  }
  
  allTestsResult.value = results
  console.log('批量测试完成:', results)
}
</script>

<style scoped>
.api-test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin: 30px 0;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.filter-controls {
  display: flex;
  gap: 10px;
  margin: 10px 0;
}

.filter-controls input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

button {
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

.success {
  color: green;
}

.error {
  color: red;
}

pre {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow: auto;
}
</style>
```

## 调试技巧与工具

### 1. 浏览器开发者工具使用
1. **Network标签页监控**：
   - 查看所有API请求和响应
   - 检查请求头、请求体、响应状态码
   - 过滤XHR/Fetch请求

2. **Console标签页调试**：
   - 添加console.log输出调试信息
   - 捕获和查看错误堆栈

3. **Vue Devtools**：
   - 安装Vue Devtools浏览器扩展
   - 查看组件状态、props、事件

### 2. Vue响应式调试
```typescript
// 使用watch监控数据变化
import { watch } from 'vue'

watch(
  () => schoolsResult.value,
  (newValue) => {
    console.log('schoolsResult变化:', newValue)
  },
  { deep: true }
)
```

### 3. API请求拦截器调试
```typescript
// 在request.ts中添加调试信息
instance.interceptors.request.use(
  (config) => {
    console.log('请求拦截器:', {
      url: config.url,
      method: config.method,
      params: config.params,
      data: config.data
    })
    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    console.log('响应拦截器:', {
      status: response.status,
      data: response.data,
      url: response.config.url
    })
    return response
  },
  (error) => {
    console.error('响应拦截器错误:', error)
    return Promise.reject(error)
  }
)
```

## 常见问题解决方案

### 问题: API返回502错误
**解决方案**:
1. 检查后端服务是否启动
2. 验证代理配置
3. 检查网络连接

### 问题: TypeScript类型错误
**解决方案**:
1. 确保TypeScript类型定义与后端实体匹配
2. 更新类型定义文件
3. 使用类型断言临时解决

### 问题: 数据格式不一致
**解决方案**:
1. 对比前端TypeScript接口和后端Java实体
2. 检查字段名映射（驼峰vs下划线）
3. 验证枚举值定义

## 测试报告模板

测试完成后，可以在`project_status.md`中报告结果：

```markdown
### [时间] | frontend Agent
**类型**: API接口测试报告
**涉及任务**: T003前端开发
**状态**: 测试完成

**测试结果**:
- ✅ 地图配置接口: 成功
- ✅ 院校地图数据接口: 成功 (返回XX条数据)
- ✅ 院校详情接口: 成功
- ✅ 多校对比接口: 成功
- ❌ 专业详情接口: 失败 (原因: ...)

**发现问题**:
1. 字段名不一致: 后端返回`created_at`，前端期望`createdAt`
2. 枚举值不匹配: 后端返回`985`，前端类型为`'985' | '211' | ...`

**建议修复**:
1. 更新后端响应字段格式
2. 协调枚举值定义
3. 更新API文档

**下一步**: 等待后端修复后重新测试
```

## 联系方式

- **前端问题**: frontend Agent
- **后端问题**: backend Agent  
- **协调问题**: integration Agent
- **数据问题**: database Agent

---

*最后更新: 2026-04-18*
*文档版本: v1.0*