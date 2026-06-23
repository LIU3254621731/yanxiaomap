<template>
  <div class="home-view">
    <!-- 顶部筛选栏 -->
    <header class="header">
      <div class="container">
        <div class="logo">
          考研院校地图
        </div>
        <div
          v-loading="loading"
          class="filters"
        >
          <el-input
            v-model="searchKeyword"
            placeholder="搜索专业名称或别名"
            clearable
            class="search-input"
            :disabled="loading"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="selectedLevel"
            placeholder="院校层次"
            clearable
            :disabled="loading"
            @change="handleFilterChange"
          >
            <el-option
              label="985"
              value="985"
            />
            <el-option
              label="211"
              value="211"
            />
            <el-option
              label="双一流"
              value="double_first_class"
            />
            <el-option
              label="普通本科"
              value="regular"
            />
          </el-select>
          <el-select
            v-model="selectedProvince"
            placeholder="省份"
            clearable
            :disabled="loading"
            @change="handleFilterChange"
          >
            <el-option
              v-for="province in provinces"
              :key="province"
              :label="province"
              :value="province"
            />
          </el-select>
          <el-select
            v-model="selectedSort"
            placeholder="排序方式"
            :disabled="loading"
            @change="handleSortChange"
          >
            <el-option
              label="按复试分数线（从高到低）"
              value="score_desc"
            />
            <el-option
              label="按复试分数线（从低到高）"
              value="score_asc"
            />
            <el-option
              label="按招生人数（从多到少）"
              value="enroll_desc"
            />
            <el-option
              label="按报录比（从低到高）"
              value="ratio_asc"
            />
          </el-select>
          <el-button
            type="primary"
            @click="resetFilters"
          >
            重置
          </el-button>
        </div>
      </div>
    </header>

    <!-- 地图容器 -->
    <main class="main">
      <div class="map-container-wrapper">
        <MapContainer
          ref="mapContainerRef"
          :center="mapCenter"
          :zoom="mapZoom"
          :show-zoom-control="true"
          :show-location-control="true"
          :show-layer-control="true"
          @loaded="handleMapLoaded"
          @center-changed="handleCenterChanged"
          @zoom-changed="handleZoomChanged"
          @click="handleMapClick"
          @error="handleMapError"
        />
        <div class="map-legend">
          <div class="legend-item">
            <span class="dot dot-985" /> 985院校
          </div>
          <div class="legend-item">
            <span class="dot dot-211" /> 211院校
          </div>
          <div class="legend-item">
            <span class="dot dot-regular" /> 普通本科
          </div>
        </div>
      </div>
    </main>

    <!-- 底部合规声明入口 -->
    <footer class="footer">
      <div class="container">
        <el-link
          type="info"
          href="/compliance"
          :underline="false"
        >
          合规声明 | 数据来源 | 备案信息 | 版权信息
        </el-link>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MapContainer from '@/components/map/MapContainer.vue'
import { useMapStore } from '@/stores'
import { mapApi } from '@/api'
import type { School } from '@/types/school'

// 组件引用
const mapContainerRef = ref<InstanceType<typeof MapContainer>>()

// 响应式数据
const searchKeyword = ref('')
const selectedLevel = ref('')
const selectedProvince = ref('')
const selectedSort = ref('score_desc')
const loading = ref(false)
const provinces = ref(['北京', '上海', '广东', '江苏', '浙江', '山东', '湖北', '四川', '陕西', '辽宁'])
const mapCenter = ref<[number, number]>([116.397428, 39.90923]) // 北京
const mapZoom = ref(5)

// Store
const mapStore = useMapStore()

// Mock院校数据（模拟后端API返回）
const mockSchools: School[] = [
  {
    id: 1,
    name: '北京大学',
    code: '10001',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 116.310316,
    latitude: 39.992806,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 2,
    name: '清华大学',
    code: '10003',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '理工类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 116.326443,
    latitude: 39.999733,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 3,
    name: '复旦大学',
    code: '10246',
    level: '985',
    province: '上海',
    city: '上海市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 121.503411,
    latitude: 31.298974,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 4,
    name: '上海交通大学',
    code: '10248',
    level: '985',
    province: '上海',
    city: '上海市',
    type: '理工类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 121.433181,
    latitude: 31.199822,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 5,
    name: '南京大学',
    code: '10284',
    level: '985',
    province: '江苏',
    city: '南京市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 118.7778,
    latitude: 32.0572,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 6,
    name: '浙江大学',
    code: '10335',
    level: '985',
    province: '浙江',
    city: '杭州市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 120.081861,
    latitude: 30.262674,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 7,
    name: '武汉大学',
    code: '10486',
    level: '985',
    province: '湖北',
    city: '武汉市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 114.358861,
    latitude: 30.537842,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 8,
    name: '华中科技大学',
    code: '10487',
    level: '985',
    province: '湖北',
    city: '武汉市',
    type: '理工类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 114.408664,
    latitude: 30.518087,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 9,
    name: '中山大学',
    code: '10558',
    level: '985',
    province: '广东',
    city: '广州市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 113.297383,
    latitude: 23.09697,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 10,
    name: '四川大学',
    code: '10610',
    level: '985',
    province: '四川',
    city: '成都市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 104.075809,
    latitude: 30.633648,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 11,
    name: '西安交通大学',
    code: '10698',
    level: '985',
    province: '陕西',
    city: '西安市',
    type: '理工类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 108.996939,
    latitude: 34.24668,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 12,
    name: '北京航空航天大学',
    code: '10006',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '理工类',
    belong: '工信部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 116.347656,
    latitude: 39.980147,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 13,
    name: '北京理工大学',
    code: '10007',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '理工类',
    belong: '工信部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 116.312614,
    latitude: 39.961612,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 14,
    name: '北京师范大学',
    code: '10027',
    level: '985',
    province: '北京',
    city: '北京市',
    type: '师范类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 116.366794,
    latitude: 39.961345,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  },
  {
    id: 15,
    name: '南开大学',
    code: '10055',
    level: '985',
    province: '天津',
    city: '天津市',
    type: '综合类',
    belong: '教育部',
    status: 1,
    enrollmentUnit: '研究生院',
    longitude: 117.174229,
    latitude: 39.109563,
    createdAt: '2023-01-01',
    updatedAt: '2023-01-01'
  }
]

// 方法
// 防抖搜索
let searchTimer: ReturnType<typeof setTimeout> | null = null
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  searchTimer = setTimeout(() => {
    console.log('搜索关键词:', searchKeyword.value)
    applyFilters()
  }, 300)
}

// 筛选条件变化
const handleFilterChange = () => {
  console.log('筛选条件变化:', {
    level: selectedLevel.value,
    province: selectedProvince.value
  })
  applyFilters()
}

// 排序方式变化
const handleSortChange = () => {
  console.log('排序方式:', selectedSort.value)
  // 排序逻辑待实现
}

// 重置筛选
const resetFilters = () => {
  searchKeyword.value = ''
  selectedLevel.value = ''
  selectedProvince.value = ''
  selectedSort.value = 'score_desc'
  mapStore.resetFilters()
}

// 应用筛选条件（调用API）
const applyFilters = async () => {
  loading.value = true
  try {
    const params = {
      province: selectedProvince.value || undefined,
      level: selectedLevel.value || undefined,
      type: undefined, // 暂时不支持类型筛选
      belong: undefined // 暂时不支持隶属单位筛选
    }
    const schools = await mapApi.getMapSchools(params)
    mapStore.setSchools(schools)
    // 进行本地关键词筛选
    mapStore.filterSchools({
      keyword: searchKeyword.value,
      level: '', // 已在API中筛选，不再重复筛选
      province: '' // 已在API中筛选，不再重复筛选
    })
    ElMessage.success(`已筛选到${schools.length}所院校`)
  } catch (error) {
    console.error('筛选院校数据失败:', error)
    ElMessage.warning('使用模拟数据进行筛选')
    // 使用mock数据进行本地筛选
    let filtered = [...mockSchools]
    if (selectedProvince.value) {
      filtered = filtered.filter(school => school.province === selectedProvince.value)
    }
    if (selectedLevel.value) {
      filtered = filtered.filter(school => school.level === selectedLevel.value)
    }
    mapStore.setSchools(filtered)
    mapStore.filterSchools({
      keyword: searchKeyword.value,
      level: '',
      province: ''
    })
    ElMessage.success(`已筛选到${filtered.length}所院校（模拟数据）`)
  } finally {
    loading.value = false
  }
}

// 地图相关方法
const handleMapLoaded = (map: any) => {
  console.log('地图加载完成:', map)
  // 地图加载完成后，可以执行一些初始化操作
}

const handleCenterChanged = (center: [number, number]) => {
  mapCenter.value = center
  console.log('地图中心点变化:', center)
}

const handleZoomChanged = (zoom: number) => {
  mapZoom.value = zoom
  console.log('地图缩放级别变化:', zoom)
}

const handleMapClick = (event: any) => {
  console.log('地图点击事件:', event)
}

const handleMapError = (error: string) => {
  ElMessage.error(error)
}

// 初始化数据
const initData = async () => {
  try {
    // 调用API获取院校数据
    const schools = await mapApi.getMapSchools()
    // 设置数据到store
    mapStore.setSchools(schools)
    console.log('初始化院校数据:', schools.length, '条')
  } catch (error) {
    console.error('获取院校数据失败:', error)
    ElMessage.error('院校数据加载失败，请稍后重试')
    // 使用mock数据作为后备
    mapStore.setSchools(mockSchools)
  }
}

// 监听筛选结果变化
watch(() => mapStore.filteredSchools, (newSchools) => {
  console.log('筛选结果变化，显示院校数量:', newSchools.length)
  // 这里可以触发地图标记更新
}, { deep: true })

onMounted(() => {
  initData()
  // 初始筛选
  nextTick(() => {
    applyFilters()
  })
})
</script>

<style scoped lang="scss">
.home-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px 0;
  z-index: 1000;

  .container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }

  .logo {
    font-size: 24px;
    font-weight: bold;
    color: #1890ff;
  }

  .filters {
    display: flex;
    gap: 12px;
    align-items: center;

    .search-input {
      width: 280px;
    }

    .el-select {
      width: 140px;
    }
  }
}

.main {
  flex: 1;
  background: #f5f5f5;

  .map-container-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
  }

  .map-legend {
    position: absolute;
    bottom: 20px;
    right: 20px;
    background: white;
    padding: 12px 16px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    font-size: 12px;

    .legend-item {
      display: flex;
      align-items: center;
      margin-bottom: 6px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .dot {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      margin-right: 8px;

      &-985 {
        background: #ff4d4f;
      }

      &-211 {
        background: #1890ff;
      }

      &-regular {
        background: #52c41a;
      }
    }
  }
}

.footer {
  background: #fafafa;
  padding: 12px 0;
  text-align: center;
  border-top: 1px solid #e8e8e8;

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }
}

// 响应式设计
@media (max-width: 992px) {
  .header {
    padding: 12px 0;

    .container {
      flex-direction: column;
      gap: 12px;
      padding: 0 16px;
    }

    .logo {
      font-size: 20px;
      text-align: center;
      width: 100%;
    }

    .filters {
      flex-wrap: wrap;
      justify-content: center;
      width: 100%;

      .search-input {
        width: 100%;
        max-width: 400px;
      }

      .el-select {
        width: calc(50% - 6px);
        min-width: 120px;
      }

      .el-button {
        width: 100%;
        max-width: 200px;
      }
    }
  }

  .main {
    .map-legend {
      bottom: 10px;
      right: 10px;
      padding: 8px 12px;
      font-size: 10px;
    }
  }

  .footer {
    .container {
      padding: 0 16px;
    }
  }
}

@media (max-width: 576px) {
  .header {
    .filters {
      .el-select {
        width: 100%;
      }
    }
  }
}
</style>