<template>
  <div class="map-container">
    <div
      ref="mapContainer"
      class="map-content"
    />
    
    <!-- 地图控件容器 -->
    <div
      v-if="mapInstance"
      class="map-controls"
    >
      <!-- 缩放控件 -->
      <div class="control-group">
        <el-button
          type="primary"
          size="small"
          circle
          :disabled="currentZoom >= maxZoom"
          @click="zoomIn"
        >
          <el-icon><plus /></el-icon>
        </el-button>
        <el-button
          type="primary"
          size="small"
          circle
          :disabled="currentZoom <= minZoom"
          @click="zoomOut"
        >
          <el-icon><minus /></el-icon>
        </el-button>
      </div>
      
      <!-- 定位控件 -->
      <div class="control-group">
        <el-button
          type="primary"
          size="small"
          circle
          :loading="locating"
          @click="locateUser"
        >
          <el-icon><location /></el-icon>
        </el-button>
      </div>
      
      <!-- 图层切换 -->
      <div class="control-group">
        <el-button
          type="primary"
          size="small"
          circle
          @click="toggleSatellite"
        >
          <el-icon><picture /></el-icon>
        </el-button>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="map-loading"
    >
      <el-icon class="loading-icon">
        <loading />
      </el-icon>
      <span>地图加载中...</span>
    </div>
    
    <!-- 错误提示 -->
    <div
      v-if="error"
      class="map-error"
    >
      <el-alert
        :title="error"
        type="error"
        show-icon
        :closable="true"
        @close="clearError"
      />
    </div>
    
    <!-- 地图信息窗口 -->
    <MapInfoWindow
      v-if="showInfoWindow && hoveredSchool"
      :school="hoveredSchool"
      :position="infoWindowPosition"
      :visible="showInfoWindow"
      @close="showInfoWindow = false"
      @view-detail="handleViewDetail"
      @add-compare="handleAddCompare"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import AMapLoader from '@amap/amap-jsapi-loader'
import { useMapStore } from '@/stores'
import { mapApi } from '@/api'
import { Loading, Location, Plus, Minus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MapInfoWindow from './MapInfoWindow.vue'

// Props
interface Props {
  // 初始中心点坐标 [经度, 纬度]
  center?: [number, number]
  // 初始缩放级别
  zoom?: number
  // 是否显示缩放控件
  showZoomControl?: boolean
  // 是否显示定位控件
  showLocationControl?: boolean
  // 是否显示图层切换
  showLayerControl?: boolean
  // 地图样式：normal（标准）, satellite（卫星）
  mapStyle?: 'normal' | 'satellite'
}

const props = withDefaults(defineProps<Props>(), {
  center: () => [116.397428, 39.90923], // 北京
  zoom: 5,
  showZoomControl: true,
  showLocationControl: true,
  showLayerControl: true,
  mapStyle: 'normal'
})

// Emits
const emit = defineEmits<{
  // 地图加载完成
  loaded: [map: any]
  // 地图中心点变化
  'center-changed': [center: [number, number]]
  // 缩放级别变化
  'zoom-changed': [zoom: number]
  // 地图点击事件
  click: [event: any]
  // 错误事件
  error: [error: string]
}>()

// Refs
const mapContainer = ref<HTMLElement>()
const mapInstance = ref<any>(null)
const loading = ref(false)
const error = ref('')
const currentZoom = ref(props.zoom)
const currentCenter = ref<[number, number]>(props.center)
const isSatellite = ref(false)
const locating = ref(false)
const markers = ref<any[]>([]) // 地图标记数组
const markerCluster = ref<any>(null) // 标记聚合实例
const hoveredSchool = ref<any>(null) // 当前悬停的院校
const infoWindowPosition = ref({ x: 0, y: 0 }) // 信息窗口位置
const showInfoWindow = ref(false) // 是否显示信息窗口
const boundsTimer = ref<ReturnType<typeof setTimeout> | null>(null) // 边界筛选防抖定时器
const currentBounds = ref<any>(null) // 当前地图边界
const loadingBounds = ref(false) // 边界筛选加载状态

// Store
const mapStore = useMapStore()
const router = useRouter()

// 地图配置（将从后端动态获取）
const amapConfig = ref({
  key: '',
  version: '2.0',
  plugins: ['AMap.ToolBar', 'AMap.Scale', 'AMap.Geolocation', 'AMap.MarkerCluster', 'AMap.MapType']
})

// 地图限制
const minZoom = 3
const maxZoom = 18

// 计算属性
const isMapReady = computed(() => !!mapInstance.value && !loading.value)
const markerCount = computed(() => mapStore.filteredSchools.length)
const shouldUseSimpleMarkers = computed(() => markerCount.value > 50) // 超过50个标记时使用简单标记

// 方法
// 初始化地图
const initMap = async () => {
  if (!mapContainer.value) {return}
  
  try {
    loading.value = true
    error.value = ''
    
    // 获取高德地图配置（通过后端代理）
    const config = await mapApi.getAMapConfig()
    // 将后端返回的配置映射到amapConfig
    // 如果后端返回了version和plugin，使用后端配置；否则使用前端默认值
    const pluginArray = config.plugin ? config.plugin.split(',').map((p: string) => p.trim()) : amapConfig.value.plugins
    
    amapConfig.value = { 
      ...amapConfig.value,
      key: config.apiKey || amapConfig.value.key,
      version: config.version || amapConfig.value.version,
      plugins: pluginArray
    }
    
    // 加载高德地图（避免重复加载）
    // 检查全局AMap对象是否已存在，避免多个不一致的key错误
    if (!window.AMap) {
      await AMapLoader.load({
        key: amapConfig.value.key,
        version: amapConfig.value.version,
        plugins: amapConfig.value.plugins
      })
    } else {
      console.log('AMap已加载，跳过重复加载')
    }
    
    // 创建地图实例
    mapInstance.value = new AMap.Map(mapContainer.value, {
      zoom: currentZoom.value,
      center: currentCenter.value,
      viewMode: '2D',
      mapStyle: isSatellite.value ? 'amap://styles/satellite' : 'amap://styles/normal'
    })
    
    // 异步加载插件
    const pluginList: string[] = ['AMap.Geolocation']
    if (props.showZoomControl) {
      pluginList.push('AMap.ToolBar')
    }
    pluginList.push('AMap.Scale')
    
    AMap.plugin(pluginList, () => {
      // 添加缩放控件
      if (props.showZoomControl) {
        mapInstance.value!.addControl(new AMap.ToolBar({
          position: 'LT'
        }))
      }
      // 添加比例尺
      mapInstance.value!.addControl(new AMap.Scale({
        position: 'LB'
      }))
    })
    
    // 监听地图事件
    mapInstance.value.on('moveend', handleMapMove)
    mapInstance.value.on('zoomend', handleZoomChange)
    mapInstance.value.on('click', handleMapClick)
    
    // 更新store中的地图实例
    mapStore.setMapInstance(mapInstance.value)
    
    // 触发loaded事件
    emit('loaded', mapInstance.value)
    
    console.log('高德地图初始化成功')
    
    // 初始化标记
    createMarkers()
  } catch (err: any) {
    console.error('高德地图初始化失败:', err)
    error.value = `地图加载失败: ${err.message || '未知错误'}`
    emit('error', error.value)
  } finally {
    loading.value = false
  }
}

// 清除所有标记
const clearMarkers = () => {
  if (!mapInstance.value) {return}
  
  // 清除普通标记
  markers.value.forEach(marker => {
    marker.setMap(null)
  })
  markers.value = []
  
  // 清除标记聚合
  if (markerCluster.value) {
    markerCluster.value.clearMarkers()
    markerCluster.value = null
  }
}

// 创建标记内容（根据是否使用简单标记）
const createMarkerContent = (school: any) => {
  const getMarkerColor = (level: string) => {
    switch (level) {
      case '985': return '#ff4d4f'
      case '211': return '#1890ff'
      case '双一流': return '#52c41a'
      default: return '#faad14' // 默认处理'双非'和其他情况
    }
  }
  
  if (shouldUseSimpleMarkers.value) {
    // 简单标记：使用颜色圆点
    return `
      <div style="
        width: 16px;
        height: 16px;
        background: ${getMarkerColor(school.level)};
        border-radius: 50%;
        border: 2px solid white;
        box-shadow: 0 2px 4px rgba(0,0,0,0.3);
        cursor: pointer;
      "></div>
    `
  } else {
    // 详细标记：使用旋转方块显示层次信息
    return `
      <div style="
        width: 26px;
        height: 36px;
        background: ${getMarkerColor(school.level)};
        border-radius: 13px 13px 13px 0;
        transform: rotate(45deg);
        position: relative;
        cursor: pointer;
      ">
        <div style="
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%) rotate(-45deg);
          color: white;
          font-size: 12px;
          font-weight: bold;
        ">${school.level === '985' ? '985' : school.level === '211' ? '211' : school.level === '双一流' ? '双' : '普'}</div>
      </div>
    `
  }
}

// 创建标记
const createMarkers = () => {
  if (!mapInstance.value) {return}
  if (!mapStore.filteredSchools.length) {return}
  
  clearMarkers()
  
  mapStore.filteredSchools.forEach((school: any) => {
    if (!school.longitude || !school.latitude) {return}
    
    // 根据标记类型调整偏移量
    const offset = shouldUseSimpleMarkers.value 
      ? new AMap.Pixel(-8, -8) // 简单标记偏移
      : new AMap.Pixel(-13, -30) // 详细标记偏移
    
    // 创建标记
    const marker = new AMap.Marker({
      position: [school.longitude, school.latitude],
      title: school.name,
      offset: offset,
      content: createMarkerContent(school)
    })
    
    // 添加点击事件
    marker.on('click', () => {
      console.log('标记点击:', school.name)
      mapStore.selectSchool(school)
      // 跳转到详情页
      router.push({
        name: 'Detail',
        params: { schoolId: school.id }
      })
    })
    
    // 添加鼠标悬停事件
    marker.on('mouseover', () => {
      hoveredSchool.value = school
      // 计算标记在屏幕上的位置
      if (mapInstance.value) {
        const position = marker.getPosition()
        const pixelPosition = mapInstance.value.lngLatToContainer(position)
        infoWindowPosition.value = {
          x: pixelPosition.getX(),
          y: pixelPosition.getY()
        }
        showInfoWindow.value = true
      }
    })
    
    // 添加鼠标离开事件
    marker.on('mouseout', () => {
      showInfoWindow.value = false
      hoveredSchool.value = null
    })
    
    // 添加到地图
    marker.setMap(mapInstance.value)
    markers.value.push(marker)
  })
  
  console.log(`创建了 ${markers.value.length} 个标记`)
}

// 处理地图移动
const handleMapMove = () => {
  if (!mapInstance.value) {return}
  
  const center = mapInstance.value.getCenter()
  currentCenter.value = [center.getLng(), center.getLat()]
  emit('center-changed', currentCenter.value)
  
  // 获取地图边界并触发筛选（防抖处理）
  handleBoundsFilterDebounced()
}

// 处理缩放变化
const handleZoomChange = () => {
  if (!mapInstance.value) {return}
  
  currentZoom.value = mapInstance.value.getZoom()
  emit('zoom-changed', currentZoom.value)
  
  // 缩放变化也会改变可视区域，触发边界筛选
  handleBoundsFilterDebounced()
}

// 防抖处理边界筛选
const handleBoundsFilterDebounced = () => {
  if (boundsTimer.value) {
    clearTimeout(boundsTimer.value)
  }
  
  boundsTimer.value = setTimeout(() => {
    handleBoundsFilter()
  }, 500) // 500ms防抖延迟
}

// 处理边界筛选
const handleBoundsFilter = async () => {
  if (!mapInstance.value || loadingBounds.value) {return}
  
  try {
    loadingBounds.value = true
    
    // 获取地图边界
    const bounds = mapInstance.value.getBounds()
    if (!bounds) {
      loadingBounds.value = false
      return
    }
    
    // 保存当前边界
    currentBounds.value = bounds
    
    // 获取边界坐标
    const sw = bounds.getSouthWest() // 西南角
    const ne = bounds.getNorthEast() // 东北角
    
    const params = {
      minLng: sw.getLng(),
      minLat: sw.getLat(),
      maxLng: ne.getLng(),
      maxLat: ne.getLat()
    }
    
    console.log('地图边界筛选参数:', params)
    
    try {
      // 调用API获取边界内的院校数据
      const schools = await mapApi.getMapSchools(params)
      
      if (schools.length > 0) {
        // 更新store中的院校数据
        mapStore.setSchools(schools)
        console.log(`边界筛选获取到${schools.length}所院校`)
      } else {
        console.log('边界内未找到院校数据')
      }
    } catch (apiError) {
      console.error('边界筛选API调用失败:', apiError)
      // API调用失败，使用本地筛选作为后备
      filterSchoolsByBoundsLocal(params)
    }
  } catch (error) {
    console.error('边界筛选处理失败:', error)
  } finally {
    loadingBounds.value = false
  }
}

// 本地边界筛选（API不可用时的后备方案）
const filterSchoolsByBoundsLocal = (params: any) => {
  const { minLng, minLat, maxLng, maxLat } = params
  
  // 获取当前所有院校数据
  const allSchools = mapStore.schools
  if (allSchools.length === 0) {return}
  
  // 筛选在边界内的院校
  const filteredSchools = allSchools.filter((school: any) => {
    if (!school.longitude || !school.latitude) {return false}
    
    const lng = school.longitude
    const lat = school.latitude
    
    return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat
  })
  
  // 更新筛选后的院校数据
  mapStore.setSchools(filteredSchools)
  console.log(`本地边界筛选获取到${filteredSchools.length}所院校`)
}

// 处理地图点击
const handleMapClick = (event: any) => {
  emit('click', event)
}

// 放大
const zoomIn = () => {
  if (!mapInstance.value || currentZoom.value >= maxZoom) {return}
  mapInstance.value.zoomIn()
}

// 缩小
const zoomOut = () => {
  if (!mapInstance.value || currentZoom.value <= minZoom) {return}
  mapInstance.value.zoomOut()
}

// 定位到用户位置
const locateUser = async () => {
  if (!mapInstance.value) {return}
  
  try {
    locating.value = true
    
    // 使用高德地图的定位功能
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      buttonPosition: 'RB'
    })
    
    mapInstance.value.addControl(geolocation)
    
    geolocation.getCurrentPosition((status: string, result: any) => {
      if (status === 'complete') {
        const lnglat = [result.position.getLng(), result.position.getLat()]
        mapInstance.value.setCenter(lnglat)
        mapInstance.value.setZoom(15)
        console.log('定位成功:', lnglat)
      } else {
        error.value = '定位失败，请检查定位权限'
        console.error('定位失败:', result)
      }
      locating.value = false
    })
  } catch (err: any) {
    console.error('定位错误:', err)
    error.value = '定位失败'
    locating.value = false
  }
}

// 切换卫星图
const toggleSatellite = () => {
  if (!mapInstance.value) {return}
  
  isSatellite.value = !isSatellite.value
  const mapStyle = isSatellite.value ? 'amap://styles/satellite' : 'amap://styles/normal'
  mapInstance.value.setMapStyle(mapStyle)
}

// 查看院校详情
const handleViewDetail = (school: any) => {
  console.log('查看院校详情:', school.name)
  mapStore.selectSchool(school)
  // 跳转到详情页
  router.push({
    name: 'Detail',
    params: { schoolId: school.id }
  })
}

// 加入对比
const handleAddCompare = (school: any) => {
  console.log('加入对比:', school.name)
  // 这里可以调用对比store的addCompare方法
  ElMessage.success(`已添加 ${school.name} 到对比列表`)
}

// 清除错误
const clearError = () => {
  error.value = ''
}

// 重置地图到初始状态
const resetMap = () => {
  if (!mapInstance.value) {return}
  
  mapInstance.value.setCenter(props.center)
  mapInstance.value.setZoom(props.zoom)
}

// 销毁地图
const destroyMap = () => {
  // 清除所有标记
  clearMarkers()
  
  if (mapInstance.value) {
    // 移除所有事件监听器
    mapInstance.value.off('moveend', handleMapMove)
    mapInstance.value.off('zoomend', handleZoomChange)
    mapInstance.value.off('click', handleMapClick)
    
    // 销毁地图实例
    mapInstance.value.destroy()
    mapInstance.value = null
    
    // 清空store中的地图实例
    mapStore.setMapInstance(null)
  }
}

// 监听props变化
watch(() => props.center, (newCenter) => {
  if (mapInstance.value && newCenter) {
    mapInstance.value.setCenter(newCenter)
    currentCenter.value = newCenter
  }
})

watch(() => props.zoom, (newZoom) => {
  if (mapInstance.value && newZoom) {
    mapInstance.value.setZoom(newZoom)
    currentZoom.value = newZoom
  }
})

// 监听院校数据变化
watch(() => mapStore.filteredSchools, () => {
  if (mapInstance.value) {
    createMarkers()
  }
}, { deep: true })

// 生命周期
onMounted(() => {
  initMap()
})

onUnmounted(() => {
  destroyMap()
})

// 暴露方法给父组件
defineExpose({
  getMapInstance: () => mapInstance.value,
  zoomIn,
  zoomOut,
  locateUser,
  toggleSatellite,
  resetMap,
  destroyMap,
  isMapReady,
  currentCenter,
  currentZoom
})
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.map-content {
  width: 100%;
  height: 100%;
  background: $--color-gray-2;
}

.map-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 1000;
  
  .control-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: white;
    padding: 8px;
    border-radius: 4px;
    box-shadow: $--box-shadow-base;
    
    .el-button {
      width: 36px;
      height: 36px;
    }
  }
}

.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 1001;
  
  .loading-icon {
    font-size: 40px;
    color: $--color-primary;
    margin-bottom: 16px;
    animation: spin 1s linear infinite;
  }
  
  span {
    color: $--color-gray-7;
    font-size: 16px;
  }
}

.map-error {
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  z-index: 1002;
  
  :deep(.el-alert) {
    max-width: 400px;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 响应式调整
@media (max-width: $--screen-sm) {
  .map-controls {
    top: 10px;
    right: 10px;
    
    .control-group {
      padding: 4px;
      
      .el-button {
        width: 32px;
        height: 32px;
      }
    }
  }
}
</style>