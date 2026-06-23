<template>
  <div class="detail-view">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <div class="container">
        <el-button
          type="text"
          icon="arrow-left"
          @click="goBack"
        >
          返回地图
        </el-button>
        <el-button
          type="primary"
          :disabled="compareDisabled"
          @click="addToCompare"
        >
          加入对比
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <el-skeleton
        :rows="5"
        animated
      />
    </div>

    <!-- 错误状态 -->
    <div
      v-else-if="error"
      class="error-container"
    >
      <el-alert
        :title="error"
        type="error"
        show-icon
      />
      <div class="error-actions">
        <el-button
          type="primary"
          @click="loadSchoolData"
        >
          重试
        </el-button>
        <el-button @click="goBack">
          返回地图
        </el-button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div
      v-else-if="school"
      class="content"
    >
      <div class="container">
        <!-- 院校信息 -->
        <el-card class="school-card">
          <template #header>
            <div class="card-header">
              <h2>{{ school.name }}</h2>
              <el-tag :type="getLevelTagType(school.level)">
                {{ getLevelLabel(school.level) }}
              </el-tag>
            </div>
          </template>
          <div class="school-info">
            <div class="info-item">
              <span class="label">院校代码：</span>
              <span class="value">{{ school.code }}</span>
            </div>
            <div class="info-item">
              <span class="label">所在地：</span>
              <span class="value">{{ school.province }} {{ school.city }}</span>
            </div>
            <div class="info-item">
              <span class="label">院校类型：</span>
              <span class="value">{{ school.type }}</span>
            </div>
            <div class="info-item">
              <span class="label">隶属单位：</span>
              <span class="value">{{ school.belong }}</span>
            </div>
            <div
              v-if="school.website"
              class="info-item"
            >
              <span class="label">官方网站：</span>
              <span class="value">
                <a
                  :href="school.website"
                  target="_blank"
                  class="website-link"
                >{{ school.website }}</a>
              </span>
            </div>
          </div>
        </el-card>

        <!-- 专业信息 -->
        <el-card
          v-if="major"
          class="major-card"
        >
          <template #header>
            <div class="card-header">
              <h3>{{ major.name }}</h3>
              <div class="header-actions">
                <el-select
                  v-model="selectedYear"
                  placeholder="选择年份"
                  @change="handleYearChange"
                >
                  <el-option
                    v-for="year in years"
                    :key="year"
                    :label="`${year}年`"
                    :value="year"
                  />
                </el-select>
                <el-button
                  type="text"
                  @click="refreshData"
                >
                  刷新数据
                </el-button>
              </div>
            </div>
          </template>
          <div class="major-info">
            <div class="info-item">
              <span class="label">专业代码：</span>
              <span class="value">{{ major.code }}</span>
            </div>
            <div class="info-item">
              <span class="label">专业名称：</span>
              <span class="value">{{ major.name }}</span>
            </div>
            <div class="info-item">
              <span class="label">培养类型：</span>
              <span class="value">{{ major.cultivationType }}</span>
            </div>
            <div class="info-item">
              <span class="label">学位类型：</span>
              <span class="value">{{ major.degreeType }}</span>
            </div>
          </div>
        </el-card>

        <!-- 录取数据表格 -->
        <el-card
          v-if="major"
          class="data-card"
        >
          <template #header>
            <h3>历年录取数据（{{ selectedYear }}年）</h3>
          </template>
          <el-table
            :data="filteredAdmissionData"
            border
            style="width: 100%"
          >
            <el-table-column
              prop="year"
              label="年份"
              width="100"
            />
            <el-table-column
              prop="planEnroll"
              label="计划招生人数"
              width="120"
            />
            <el-table-column
              prop="actualEnroll"
              label="实际招生人数"
              width="120"
            />
            <el-table-column
              prop="score"
              label="复试分数线"
              width="120"
            />
            <el-table-column
              prop="ratio"
              label="报录比"
              width="120"
            />
            <el-table-column
              prop="notes"
              label="备注"
            />
          </el-table>
          <div
            v-if="!filteredAdmissionData.length"
            class="empty-data"
          >
            暂无{{ selectedYear }}年录取数据
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import { schoolApi } from '@/api'
import type { School, Major, AdmissionData } from '@/types/school'

// 路由
const route = useRoute()
const router = useRouter()

// 响应式数据
const selectedYear = ref(2024)
const years = ref([2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017])
const loading = ref(false)
const error = ref('')

// 真实数据
const school = ref<School | null>(null)
const major = ref<Major | null>(null)
const admissionData = ref<AdmissionData[]>([])

// 数据获取函数
const loadSchoolData = async () => {
  const schoolId = Number(route.params.schoolId)
  const majorId = route.params.majorId ? Number(route.params.majorId) : undefined
  
  if (!schoolId) {
    error.value = '无效的院校ID'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 调用API获取院校专业详情
    const params = { schoolId, majorId }
    const data = await schoolApi.getSchoolMajorDetail(params)
    
    if (data) {
      school.value = data.school
      major.value = data.major
      admissionData.value = data.admissionHistory
      
      // 如果有录取数据，设置默认年份
      if (data.admissionHistory.length > 0) {
        const yearsSet = new Set(data.admissionHistory.map(item => item.year))
        years.value = Array.from(yearsSet).sort((a, b) => b - a) // 降序排列
        selectedYear.value = years.value[0] // 默认最新年份
      }
    } else {
      error.value = '未找到院校数据'
      // 使用模拟数据作为后备
      useFallbackData(schoolId, majorId)
    }
  } catch (err) {
    console.error('加载院校数据失败:', err)
    error.value = '数据加载失败，请稍后重试'
    // 使用模拟数据作为后备
    useFallbackData(schoolId, majorId)
  } finally {
    loading.value = false
  }
}

// 使用模拟数据作为后备
const useFallbackData = (schoolId: number, majorId?: number) => {
  // 模拟院校数据
  school.value = {
    id: schoolId,
    name: '清华大学',
    code: '10001',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '综合类',
    belong: '教育部',
    longitude: 116.326388,
    latitude: 39.999722,
    status: 1,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01'
  }
  
  // 模拟专业数据
  major.value = {
    id: majorId || 1,
    name: '计算机科学与技术',
    code: '081200',
    cultivationType: 'academic',
    degreeType: '工学硕士',
    schoolId: schoolId,
    duration: 3,
    status: 1,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01'
  }
  
  // 模拟录取数据
  admissionData.value = [
    { id: 1, schoolId: schoolId, majorId: majorId || 1, year: 2024, planEnroll: 50, actualEnroll: 52, score: 350, ratio: '8:1', notes: '复试比例1:1.2', status: 1, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
    { id: 2, schoolId: schoolId, majorId: majorId || 1, year: 2023, planEnroll: 48, actualEnroll: 48, score: 345, ratio: '7.5:1', notes: '无', status: 1, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
    { id: 3, schoolId: schoolId, majorId: majorId || 1, year: 2022, planEnroll: 45, actualEnroll: 46, score: 340, ratio: '7:1', notes: '新增人工智能方向', status: 1, createdAt: '2024-01-01', updatedAt: '2024-01-01' }
  ]
  
  // 设置年份
  years.value = [2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017]
  selectedYear.value = 2024
  
  ElMessage.warning('使用模拟数据进行展示')
}

// 组件挂载时加载数据
onMounted(() => {
  loadSchoolData()
})

// 计算属性
const compareDisabled = computed(() => {
  // 检查是否已在对比列表中
  return false
})

// 当前年份的录取数据
const filteredAdmissionData = computed(() => {
  if (!admissionData.value.length) {return []}
  return admissionData.value.filter(item => item.year === selectedYear.value)
})

// 方法
const goBack = () => {
  router.push('/')
}

const addToCompare = () => {
  ElMessage.success('已添加到对比列表')
}

const handleYearChange = () => {
  // 年份切换，filteredAdmissionData会自动更新
  console.log('切换年份:', selectedYear.value)
}

const refreshData = () => {
  // 重新加载数据
  loadSchoolData()
  ElMessage.info('数据刷新中...')
}

const getLevelTagType = (level: string) => {
  switch (level) {
    case '985': return 'danger'
    case '211': return 'primary'
    case '双一流': return 'success'
    case '双非': return 'info'
    default: return 'info'
  }
}

const getLevelLabel = (level: string) => {
  switch (level) {
    case '985': return '985院校'
    case '211': return '211院校'
    case '双一流': return '双一流'
    case '双非': return '普通本科'
    default: return '普通本科'
  }
}
</script>

<style scoped lang="scss">
.loading-container,
.error-container {
  max-width: 1400px;
  margin: 40px auto;
  padding: 0 24px;
}

.error-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-actions {
  display: flex;
  gap: 12px;
}

.detail-view {
  min-height: 100vh;
  background: #f5f5f5;
}

.back-bar {
  background: white;
  padding: 16px 0;
  border-bottom: 1px solid #e8e8e8;

  .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }
}

.content {
  padding: 24px 0;

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.school-info,
.major-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;

  .info-item {
    .label {
      color: #666;
      margin-right: 8px;
    }

    .value {
      font-weight: 500;
    }
  }
}

.empty-data {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

.website-link {
  color: #1890ff;
  text-decoration: none;
  
  &:hover {
    text-decoration: underline;
  }
}

// 响应式设计
@media (max-width: 992px) {
  .loading-container,
  .error-container {
    padding: 0 16px;
    margin: 24px auto;
  }

  .back-bar {
    padding: 12px 0;

    .container {
      flex-direction: column;
      gap: 12px;
      padding: 0 16px;
      align-items: flex-start;
    }
  }

  .content {
    padding: 16px 0;

    .container {
      padding: 0 16px;
    }
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;

    .header-actions {
      width: 100%;
      justify-content: flex-start;
    }
  }

  .school-info,
  .major-info {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 12px;
  }
}

@media (max-width: 576px) {
  .school-info,
  .major-info {
    grid-template-columns: 1fr;
  }

  .error-actions {
    flex-direction: column;
  }
}
</style>