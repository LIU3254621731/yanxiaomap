// 地图状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { School, SchoolMarker } from '@/types/school'

export const useMapStore = defineStore('map', () => {
  // 状态
  const mapInstance = ref<any>(null) // 高德地图实例
  const schools = ref<School[]>([]) // 所有院校数据
  const filteredSchools = ref<School[]>([]) // 筛选后的院校数据
  const currentCenter = ref<[number, number]>([116.397428, 39.90923]) // 当前地图中心点（北京）
  const currentZoom = ref(5) // 当前缩放级别
  const selectedSchool = ref<School | null>(null) // 当前选中的院校
  const isLoading = ref(false) // 加载状态
  const error = ref<string | null>(null) // 错误信息

  // 计算属性
  const totalSchools = computed(() => schools.value.length)
  const visibleSchools = computed(() => filteredSchools.value.length)
  const hasSchools = computed(() => schools.value.length > 0)

  // 获取地图实例
  const getMapInstance = () => mapInstance.value

  // 设置地图实例
  const setMapInstance = (instance: any) => {
    mapInstance.value = instance
  }

  // 设置院校数据
  const setSchools = (data: School[]) => {
    schools.value = data
    filteredSchools.value = data // 初始时显示所有院校
  }

  // 筛选院校
  const filterSchools = (filters: {
    keyword?: string
    level?: string
    province?: string
  }) => {
    const { keyword = '', level = '', province = '' } = filters

    filteredSchools.value = schools.value.filter(school => {
      // 关键词筛选（匹配院校名称或专业名称）
      const keywordMatch = !keyword || 
        school.name.toLowerCase().includes(keyword.toLowerCase()) ||
        school.majors?.some(major => 
          major.name.toLowerCase().includes(keyword.toLowerCase())
        )

      // 层次筛选
      const levelMatch = !level || school.level === level

      // 省份筛选
      const provinceMatch = !province || school.province === province

      return keywordMatch && levelMatch && provinceMatch
    })

    // 如果地图实例存在，更新地图显示
    if (mapInstance.value) {
      updateMapMarkers()
    }
  }

  // 更新地图标记
  const updateMapMarkers = () => {
    // 这里实现地图标记更新逻辑
    console.log('更新地图标记，显示院校数量:', filteredSchools.value.length)
  }

  // 选择院校
  const selectSchool = (school: School) => {
    selectedSchool.value = school
    // 如果地图实例存在，将地图中心移动到该院校
    if (mapInstance.value && school.longitude && school.latitude) {
      mapInstance.value.setCenter([school.longitude, school.latitude])
      mapInstance.value.setZoom(15)
    }
  }

  // 清除选择
  const clearSelection = () => {
    selectedSchool.value = null
  }

  // 重置筛选
  const resetFilters = () => {
    filteredSchools.value = schools.value
    if (mapInstance.value) {
      updateMapMarkers()
    }
  }

  // 设置加载状态
  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  // 设置错误信息
  const setError = (err: string | null) => {
    error.value = err
  }

  // 清除错误
  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    mapInstance,
    schools,
    filteredSchools,
    currentCenter,
    currentZoom,
    selectedSchool,
    isLoading,
    error,

    // 计算属性
    totalSchools,
    visibleSchools,
    hasSchools,

    // 方法
    getMapInstance,
    setMapInstance,
    setSchools,
    filterSchools,
    updateMapMarkers,
    selectSchool,
    clearSelection,
    resetFilters,
    setLoading,
    setError,
    clearError
  }
})