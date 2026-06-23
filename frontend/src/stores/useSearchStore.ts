// 搜索筛选状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useSearchStore = defineStore('search', () => {
  // 筛选条件
  const keyword = ref('') // 搜索关键词
  const level = ref('') // 院校层次
  const province = ref('') // 省份
  const sortBy = ref('score_desc') // 排序字段
  const sortOrder = ref('desc') // 排序方向

  // 筛选选项
  const levelOptions = ref([
    { label: '985', value: '985' },
    { label: '211', value: '211' },
    { label: '双一流', value: 'double_first_class' },
    { label: '普通本科', value: 'regular' }
  ])

  const provinceOptions = ref([
    '北京', '上海', '天津', '重庆', '河北', '山西', '辽宁', '吉林', '黑龙江',
    '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北', '湖南',
    '广东', '海南', '四川', '贵州', '云南', '陕西', '甘肃', '青海', '台湾',
    '内蒙古', '广西', '西藏', '宁夏', '新疆', '香港', '澳门'
  ])

  const sortOptions = ref([
    { label: '按复试分数线（从高到低）', value: 'score_desc' },
    { label: '按复试分数线（从低到高）', value: 'score_asc' },
    { label: '按招生人数（从多到少）', value: 'enroll_desc' },
    { label: '按招生人数（从少到多）', value: 'enroll_asc' },
    { label: '按报录比（从低到高）', value: 'ratio_asc' },
    { label: '按报录比（从高到低）', value: 'ratio_desc' }
  ])

  // 计算属性
  const hasActiveFilters = computed(() => {
    return !!keyword.value || !!level.value || !!province.value
  })

  const filterParams = computed(() => ({
    keyword: keyword.value,
    level: level.value,
    province: province.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value
  }))

  // 方法
  const setKeyword = (value: string) => {
    keyword.value = value
  }

  const setLevel = (value: string) => {
    level.value = value
  }

  const setProvince = (value: string) => {
    province.value = value
  }

  const setSort = (value: string) => {
    sortBy.value = value
    // 解析排序字段和方向
    if (value.endsWith('_desc')) {
      sortOrder.value = 'desc'
    } else if (value.endsWith('_asc')) {
      sortOrder.value = 'asc'
    }
  }

  // 重置筛选条件
  const resetFilters = () => {
    keyword.value = ''
    level.value = ''
    province.value = ''
    sortBy.value = 'score_desc'
    sortOrder.value = 'desc'
  }

  // 获取排序参数
  const getSortParams = () => {
    const [field, order] = sortBy.value.split('_')
    return { field, order }
  }

  // 应用筛选（触发地图和列表更新）
  const applyFilters = () => {
    // 这里会触发地图store的filterSchools方法
    console.log('应用筛选条件:', filterParams.value)
    return filterParams.value
  }

  // 清除搜索关键词
  const clearKeyword = () => {
    keyword.value = ''
  }

  return {
    // 筛选条件
    keyword,
    level,
    province,
    sortBy,
    sortOrder,

    // 筛选选项
    levelOptions,
    provinceOptions,
    sortOptions,

    // 计算属性
    hasActiveFilters,
    filterParams,

    // 方法
    setKeyword,
    setLevel,
    setProvince,
    setSort,
    resetFilters,
    getSortParams,
    applyFilters,
    clearKeyword
  }
})