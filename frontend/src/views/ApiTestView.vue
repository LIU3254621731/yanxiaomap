<template>
  <div class="api-test-container">
    <h1>API接口测试页面</h1>
    <p class="description">
      此页面用于测试前后端API接口连通性，可在后端服务启动后进行完整联调测试。
    </p>
    
    <div class="test-controls">
      <el-button
        type="primary"
        :loading="runningAllTests"
        @click="runAllTests"
      >
        运行所有测试
      </el-button>
      <el-button @click="resetAllTests">
        重置测试
      </el-button>
      <el-button
        type="text"
        @click="toggleShowDetails"
      >
        {{ showDetails ? '隐藏详情' : '显示详情' }}
      </el-button>
    </div>
    
    <div
      v-if="testSummary"
      class="test-summary"
    >
      <h3>测试摘要</h3>
      <div class="summary-stats">
        <div class="stat-item">
          <span class="stat-label">总测试数:</span>
          <span class="stat-value">{{ testSummary.total }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">通过:</span>
          <span class="stat-value success">{{ testSummary.passed }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">失败:</span>
          <span class="stat-value error">{{ testSummary.failed }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">成功率:</span>
          <span class="stat-value">{{ testSummary.successRate }}%</span>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>1. 高德地图配置接口</h2>
      <div class="test-info">
        <p><strong>接口路径:</strong> GET /api/map/config</p>
        <p><strong>用途:</strong> 获取高德地图JS API的安全配置信息</p>
      </div>
      <div class="test-actions">
        <el-button
          :loading="loading.mapConfig"
          @click="testMapConfig"
        >
          单独测试
        </el-button>
      </div>
      <div
        v-if="testResults.mapConfig"
        class="test-result"
      >
        <div :class="['result-status', testResults.mapConfig.success ? 'success' : 'error']">
          {{ testResults.mapConfig.success ? '✅ 测试通过' : '❌ 测试失败' }}
        </div>
        <div class="result-message">
          {{ testResults.mapConfig.message }}
        </div>
        <div
          v-if="showDetails && testResults.mapConfig.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.mapConfig.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>2. 院校地图数据接口</h2>
      <div class="test-info">
        <p><strong>接口路径:</strong> GET /api/map/schools</p>
        <p><strong>用途:</strong> 获取地图上显示的院校列表，支持省份和院校层次筛选</p>
      </div>
      <div class="filter-controls">
        <el-input
          v-model="testParams.province"
          placeholder="省份（如：北京市）"
          style="width: 200px;"
        />
        <el-input
          v-model="testParams.level"
          placeholder="层次（985、211、双一流、双非）"
          style="width: 200px;"
        />
        <el-button
          :loading="loading.schoolsData"
          @click="testSchoolsData"
        >
          测试筛选
        </el-button>
      </div>
      <div
        v-if="testResults.schoolsData"
        class="test-result"
      >
        <div :class="['result-status', testResults.schoolsData.success ? 'success' : 'error']">
          {{ testResults.schoolsData.success ? '✅ 测试通过' : '❌ 测试失败' }}
        </div>
        <div class="result-message">
          {{ testResults.schoolsData.message }}
        </div>
        <div
          v-if="testResults.schoolsData.success"
          class="result-stats"
        >
          返回 {{ testResults.schoolsData.count || 0 }} 条数据
        </div>
        <div
          v-if="showDetails && testResults.schoolsData.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.schoolsData.data.slice(0, 3), null, 2) }}</pre>
          <p v-if="testResults.schoolsData.data.length > 3">
            ... 只显示前3条，共{{ testResults.schoolsData.data.length }}条
          </p>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>3. 院校详情接口</h2>
      <div class="test-info">
        <p><strong>接口路径:</strong> GET /api/schools/{schoolId}</p>
        <p><strong>用途:</strong> 获取单个院校的详细信息</p>
      </div>
      <div class="filter-controls">
        <el-input-number
          v-model="testParams.schoolId"
          :min="1"
          placeholder="院校ID"
          style="width: 200px;"
        />
        <el-button
          :loading="loading.schoolDetail"
          @click="testSchoolDetail"
        >
          测试详情
        </el-button>
      </div>
      <div
        v-if="testResults.schoolDetail"
        class="test-result"
      >
        <div :class="['result-status', testResults.schoolDetail.success ? 'success' : 'error']">
          {{ testResults.schoolDetail.success ? '✅ 测试通过' : '❌ 测试失败' }}
        </div>
        <div class="result-message">
          {{ testResults.schoolDetail.message }}
        </div>
        <div
          v-if="showDetails && testResults.schoolDetail.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.schoolDetail.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>4. 院校专业详情接口</h2>
      <div class="test-info">
        <p><strong>接口路径:</strong> GET /api/schools/{schoolId}/majors/{majorId}</p>
        <p><strong>用途:</strong> 获取院校和专业的详细信息，包括招生数据</p>
      </div>
      <div class="filter-controls">
        <el-input-number
          v-model="testParams.schoolId2"
          :min="1"
          placeholder="院校ID"
          style="width: 150px;"
        />
        <el-input-number
          v-model="testParams.majorId"
          :min="1"
          placeholder="专业ID"
          style="width: 150px;"
        />
        <el-button
          :loading="loading.schoolMajorDetail"
          @click="testSchoolMajorDetail"
        >
          测试专业详情
        </el-button>
      </div>
      <div
        v-if="testResults.schoolMajorDetail"
        class="test-result"
      >
        <div :class="['result-status', testResults.schoolMajorDetail.success ? 'success' : 'error']">
          {{ testResults.schoolMajorDetail.success ? '✅ 测试通过' : '❌ 测试失败' }}
        </div>
        <div class="result-message">
          {{ testResults.schoolMajorDetail.message }}
        </div>
        <div
          v-if="showDetails && testResults.schoolMajorDetail.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.schoolMajorDetail.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>5. 批量院校对比接口</h2>
      <div class="test-info">
        <p><strong>接口路径:</strong> POST /api/compare/batch</p>
        <p><strong>用途:</strong> 同时获取多个院校专业的详细信息，用于对比功能</p>
      </div>
      <div class="filter-controls">
        <el-button
          :loading="loading.batchCompare"
          @click="testBatchCompare"
        >
          测试批量对比
        </el-button>
      </div>
      <div
        v-if="testResults.batchCompare"
        class="test-result"
      >
        <div :class="['result-status', testResults.batchCompare.success ? 'success' : 'error']">
          {{ testResults.batchCompare.success ? '✅ 测试通过' : '❌ 测试失败' }}
        </div>
        <div class="result-message">
          {{ testResults.batchCompare.message }}
        </div>
        <div
          v-if="testResults.batchCompare.success"
          class="result-stats"
        >
          返回 {{ testResults.batchCompare.count || 0 }} 组对比数据
        </div>
        <div
          v-if="showDetails && testResults.batchCompare.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.batchCompare.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <div class="test-section">
      <h2>6. 连接状态测试</h2>
      <div class="test-info">
        <p><strong>后端服务URL:</strong> {{ baseURL }}</p>
        <p><strong>前端API基础URL:</strong> {{ apiBaseURL }}</p>
      </div>
      <div class="test-actions">
        <el-button
          :loading="loading.connection"
          @click="testConnection"
        >
          测试连接
        </el-button>
      </div>
      <div
        v-if="testResults.connection"
        class="test-result"
      >
        <div :class="['result-status', testResults.connection.success ? 'success' : 'error']">
          {{ testResults.connection.success ? '✅ 连接正常' : '❌ 连接失败' }}
        </div>
        <div class="result-message">
          {{ testResults.connection.message }}
        </div>
        <div
          v-if="showDetails && testResults.connection.data"
          class="result-details"
        >
          <pre>{{ JSON.stringify(testResults.connection.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <div
      v-if="failedTests.length > 0"
      class="test-section"
    >
      <h2>失败测试详情</h2>
      <div
        v-for="test in failedTests"
        :key="test.name"
        class="failed-test"
      >
        <h4>{{ test.name }}</h4>
        <p><strong>错误信息:</strong> {{ test.message }}</p>
        <p v-if="test.error">
          <strong>错误详情:</strong> {{ test.error }}
        </p>
      </div>
    </div>
    
    <div class="test-footer">
      <p><strong>测试说明:</strong></p>
      <ul>
        <li>所有测试使用真实API调用，需要后端服务正常运行（http://localhost:8080）</li>
        <li>如果后端服务未启动，测试将失败并回退到模拟数据模式</li>
        <li>测试结果会显示成功/失败状态、返回数据和错误信息</li>
        <li>可单独测试每个接口或使用"运行所有测试"批量测试</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { mapApi } from '@/api/mapApi'
import { schoolApi } from '@/api/schoolApi'
import { compareApi } from '@/api/compareApi'
import { http } from '@/api/request'

interface TestResult {
  success: boolean
  message: string
  data?: any
  count?: number
  error?: any
}

interface TestSummary {
  total: number
  passed: number
  failed: number
  successRate: number
}

// 测试参数
const testParams = reactive({
  province: '北京市',
  level: '985',
  schoolId: 1,
  schoolId2: 1,
  majorId: 1
})

// 测试结果
const testResults = reactive({
  mapConfig: null as TestResult | null,
  schoolsData: null as TestResult | null,
  schoolDetail: null as TestResult | null,
  schoolMajorDetail: null as TestResult | null,
  batchCompare: null as TestResult | null,
  connection: null as TestResult | null
})

// 加载状态
const loading = reactive({
  mapConfig: false,
  schoolsData: false,
  schoolDetail: false,
  schoolMajorDetail: false,
  batchCompare: false,
  connection: false
})

const runningAllTests = ref(false)
const showDetails = ref(false)

// 计算属性
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const apiBaseURL = baseURL

const failedTests = computed(() => {
  const failed: Array<{name: string, message: string, error?: any}> = []
  
  if (testResults.mapConfig && !testResults.mapConfig.success) {
    failed.push({
      name: '高德地图配置接口',
      message: testResults.mapConfig.message,
      error: testResults.mapConfig.error
    })
  }
  
  if (testResults.schoolsData && !testResults.schoolsData.success) {
    failed.push({
      name: '院校地图数据接口',
      message: testResults.schoolsData.message,
      error: testResults.schoolsData.error
    })
  }
  
  if (testResults.schoolDetail && !testResults.schoolDetail.success) {
    failed.push({
      name: '院校详情接口',
      message: testResults.schoolDetail.message,
      error: testResults.schoolDetail.error
    })
  }
  
  if (testResults.schoolMajorDetail && !testResults.schoolMajorDetail.success) {
    failed.push({
      name: '院校专业详情接口',
      message: testResults.schoolMajorDetail.message,
      error: testResults.schoolMajorDetail.error
    })
  }
  
  if (testResults.batchCompare && !testResults.batchCompare.success) {
    failed.push({
      name: '批量院校对比接口',
      message: testResults.batchCompare.message,
      error: testResults.batchCompare.error
    })
  }
  
  if (testResults.connection && !testResults.connection.success) {
    failed.push({
      name: '连接状态测试',
      message: testResults.connection.message,
      error: testResults.connection.error
    })
  }
  
  return failed
})

const testSummary = computed<TestSummary | null>(() => {
  const results = [
    testResults.mapConfig,
    testResults.schoolsData,
    testResults.schoolDetail,
    testResults.schoolMajorDetail,
    testResults.batchCompare,
    testResults.connection
  ].filter(r => r !== null) as TestResult[]
  
  if (results.length === 0) {return null}
  
  const total = results.length
  const passed = results.filter(r => r.success).length
  const failed = total - passed
  const successRate = Math.round((passed / total) * 100)
  
  return { total, passed, failed, successRate }
})

// 测试方法
async function testMapConfig() {
  loading.mapConfig = true
  try {
    const data = await mapApi.getAMapConfig()
    testResults.mapConfig = {
      success: true,
      message: '高德地图配置获取成功',
      data
    }
    console.log('地图配置测试成功:', data)
  } catch (error: any) {
    testResults.mapConfig = {
      success: false,
      message: `高德地图配置获取失败: ${error?.message || error}`,
      error
    }
    console.error('地图配置测试失败:', error)
  } finally {
    loading.mapConfig = false
  }
}

async function testSchoolsData() {
  loading.schoolsData = true
  try {
    const params: any = {}
    if (testParams.province) {params.province = testParams.province}
    if (testParams.level) {params.level = testParams.level}
    
    const data = await mapApi.getMapSchools(params)
    testResults.schoolsData = {
      success: true,
      message: `院校地图数据获取成功，筛选条件: ${testParams.province || '全部省份'} ${testParams.level || '全部层次'}`,
      data,
      count: data.length
    }
    console.log('院校数据测试成功:', data.length, '条数据')
  } catch (error: any) {
    testResults.schoolsData = {
      success: false,
      message: `院校地图数据获取失败: ${error?.message || error}`,
      error
    }
    console.error('院校数据测试失败:', error)
  } finally {
    loading.schoolsData = false
  }
}

async function testSchoolDetail() {
  loading.schoolDetail = true
  try {
    const data = await schoolApi.getSchoolDetail(testParams.schoolId)
    if (data) {
      testResults.schoolDetail = {
        success: true,
        message: `院校详情获取成功 (ID: ${testParams.schoolId})`,
        data
      }
      console.log('院校详情测试成功:', data)
    } else {
      testResults.schoolDetail = {
        success: false,
        message: `院校详情获取失败，返回数据为空 (ID: ${testParams.schoolId})`
      }
    }
  } catch (error: any) {
    testResults.schoolDetail = {
      success: false,
      message: `院校详情获取失败: ${error?.message || error}`,
      error
    }
    console.error('院校详情测试失败:', error)
  } finally {
    loading.schoolDetail = false
  }
}

async function testSchoolMajorDetail() {
  loading.schoolMajorDetail = true
  try {
    const data = await schoolApi.getSchoolMajorDetail({
      schoolId: testParams.schoolId2,
      majorId: testParams.majorId
    })
    if (data) {
      testResults.schoolMajorDetail = {
        success: true,
        message: `院校专业详情获取成功 (院校ID: ${testParams.schoolId2}, 专业ID: ${testParams.majorId})`,
        data
      }
      console.log('院校专业详情测试成功:', data)
    } else {
      testResults.schoolMajorDetail = {
        success: false,
        message: `院校专业详情获取失败，返回数据为空 (院校ID: ${testParams.schoolId2}, 专业ID: ${testParams.majorId})`
      }
    }
  } catch (error: any) {
    testResults.schoolMajorDetail = {
      success: false,
      message: `院校专业详情获取失败: ${error?.message || error}`,
      error
    }
    console.error('院校专业详情测试失败:', error)
  } finally {
    loading.schoolMajorDetail = false
  }
}

async function testBatchCompare() {
  loading.batchCompare = true
  try {
    const batchItems = [
      { schoolId: 1, majorId: 1 },
      { schoolId: 2, majorId: 2 }
    ]
    
    const data = await compareApi.getBatchSchoolMajorDetails(batchItems)
    if (data && data.length > 0) {
      testResults.batchCompare = {
        success: true,
        message: `批量院校对比获取成功，共 ${data.length} 组数据`,
        data,
        count: data.length
      }
      console.log('批量对比测试成功:', data.length, '组数据')
    } else {
      testResults.batchCompare = {
        success: false,
        message: '批量院校对比获取失败，返回数据为空或长度为零'
      }
    }
  } catch (error: any) {
    testResults.batchCompare = {
      success: false,
      message: `批量院校对比获取失败: ${error?.message || error}`,
      error
    }
    console.error('批量对比测试失败:', error)
  } finally {
    loading.batchCompare = false
  }
}

async function testConnection() {
  loading.connection = true
  try {
    // 尝试访问一个简单的健康检查接口或配置接口
    const response = await http.get('/map/config')
    testResults.connection = {
      success: true,
      message: '后端连接正常，服务可访问',
      data: response
    }
    console.log('连接测试成功:', response)
  } catch (error: any) {
    testResults.connection = {
      success: false,
      message: `后端连接失败: ${error?.message || error}`,
      error
    }
    console.error('连接测试失败:', error)
  } finally {
    loading.connection = false
  }
}

async function runAllTests() {
  runningAllTests.value = true
  console.log('开始批量测试所有API...')
  
  // 清空之前的结果
  resetAllTests()
  
  // 顺序执行所有测试
  await testMapConfig()
  await testSchoolsData()
  await testSchoolDetail()
  await testSchoolMajorDetail()
  await testBatchCompare()
  await testConnection()
  
  console.log('批量测试完成，摘要:', testSummary.value)
  runningAllTests.value = false
}

function resetAllTests() {
  Object.keys(testResults).forEach(key => {
    (testResults as any)[key] = null
  })
}

function toggleShowDetails() {
  showDetails.value = !showDetails.value
}
</script>

<style scoped>
.api-test-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 8px;
  color: #333;
}

.description {
  margin-bottom: 24px;
  color: #666;
  font-size: 14px;
}

.test-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: center;
}

.test-summary {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid #e9ecef;
}

.summary-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-value.success {
  color: #52c41a;
}

.stat-value.error {
  color: #f5222d;
}

.test-section {
  margin: 32px 0;
  padding: 24px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background-color: #fff;
}

.test-section h2 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #333;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 8px;
}

.test-info {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
}

.test-info p {
  margin: 4px 0;
  font-size: 13px;
}

.filter-controls {
  display: flex;
  gap: 12px;
  margin: 16px 0;
  align-items: center;
  flex-wrap: wrap;
}

.test-actions {
  margin: 16px 0;
}

.test-result {
  margin-top: 16px;
  padding: 16px;
  border-radius: 4px;
  background-color: #fafafa;
}

.result-status {
  font-weight: bold;
  margin-bottom: 8px;
}

.result-status.success {
  color: #52c41a;
}

.result-status.error {
  color: #f5222d;
}

.result-message {
  margin-bottom: 8px;
  font-size: 14px;
}

.result-stats {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
}

.result-details {
  margin-top: 12px;
}

.result-details pre {
  background-color: #2d2d2d;
  color: #f8f8f2;
  padding: 12px;
  border-radius: 4px;
  overflow: auto;
  font-size: 12px;
  max-height: 300px;
}

.failed-test {
  margin: 12px 0;
  padding: 12px;
  border-left: 4px solid #f5222d;
  background-color: #fff2f0;
}

.failed-test h4 {
  margin: 0 0 8px 0;
  color: #f5222d;
}

.failed-test p {
  margin: 4px 0;
  font-size: 13px;
}

.test-footer {
  margin-top: 32px;
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #91d5ff;
}

.test-footer ul {
  margin: 8px 0;
  padding-left: 20px;
}

.test-footer li {
  margin: 4px 0;
  font-size: 13px;
}
</style>