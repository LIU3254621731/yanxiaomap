// 对比功能状态管理
import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { CompareItem } from '@/types/school'

export const useCompareStore = defineStore('compare', () => {
  // 对比项列表
  const compareItems = ref<CompareItem[]>([])
  // 是否高亮差异
  const highlightDifferences = ref(true)
  // 选中年份
  const selectedYear = ref(2024)

  // 存储键名
  const STORAGE_KEY = 'school_compare_items'
  const MAX_ITEMS = 4

  // 初始化时从localStorage加载
  const loadFromStorage = () => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const items = JSON.parse(stored)
        if (Array.isArray(items) && items.length > 0) {
          compareItems.value = items.slice(0, MAX_ITEMS)
        }
      }
    } catch (error) {
      console.error('加载对比数据失败:', error)
      clearStorage()
    }
  }

  // 保存到localStorage
  const saveToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(compareItems.value))
    } catch (error) {
      console.error('保存对比数据失败:', error)
    }
  }

  // 清除localStorage
  const clearStorage = () => {
    localStorage.removeItem(STORAGE_KEY)
  }

  // 计算属性
  const itemCount = computed(() => compareItems.value.length)
  const isFull = computed(() => compareItems.value.length >= MAX_ITEMS)
  const isEmpty = computed(() => compareItems.value.length === 0)

  // 院校ID列表
  const schoolIds = computed(() => 
    compareItems.value.map(item => item.schoolId)
  )

  // 专业ID列表
  const majorIds = computed(() => 
    compareItems.value.map(item => item.majorId)
  )

  // 检查是否已存在
  const exists = (schoolId: number, majorId: number) => {
    return compareItems.value.some(
      item => item.schoolId === schoolId && item.majorId === majorId
    )
  }

  // 添加对比项
  const addItem = (item: CompareItem) => {
    // 检查是否已满
    if (isFull.value) {
      throw new Error(`最多只能对比${MAX_ITEMS}所院校`)
    }

    // 检查是否已存在
    if (exists(item.schoolId, item.majorId)) {
      throw new Error('该院校专业已在对比列表中')
    }

    compareItems.value.push(item)
    saveToStorage()
  }

  // 移除对比项
  const removeItem = (index: number) => {
    if (index >= 0 && index < compareItems.value.length) {
      compareItems.value.splice(index, 1)
      saveToStorage()
    }
  }

  // 清空对比列表
  const clearAll = () => {
    compareItems.value = []
    clearStorage()
  }

  // 移动项目位置
  const moveItem = (fromIndex: number, toIndex: number) => {
    if (
      fromIndex >= 0 && fromIndex < compareItems.value.length &&
      toIndex >= 0 && toIndex < compareItems.value.length &&
      fromIndex !== toIndex
    ) {
      const item = compareItems.value[fromIndex]
      compareItems.value.splice(fromIndex, 1)
      compareItems.value.splice(toIndex, 0, item)
      saveToStorage()
    }
  }

  // 更新对比项数据
  const updateItem = (index: number, updates: Partial<CompareItem>) => {
    if (index >= 0 && index < compareItems.value.length) {
      compareItems.value[index] = {
        ...compareItems.value[index],
        ...updates
      }
      saveToStorage()
    }
  }

  // 设置高亮差异
  const setHighlightDifferences = (value: boolean) => {
    highlightDifferences.value = value
  }

  // 设置选中年份
  const setSelectedYear = (year: number) => {
    selectedYear.value = year
  }

  // 获取对比表格数据
  const getTableData = () => {
    if (isEmpty.value) {return []}

    const dimensions = [
      { key: 'schoolName', label: '院校名称' },
      { key: 'level', label: '院校层次' },
      { key: 'location', label: '所在地' },
      { key: 'majorName', label: '专业名称' },
      { key: 'score', label: '复试分数线' },
      { key: 'enroll', label: '招生人数' },
      { key: 'ratio', label: '报录比' }
    ]

    return dimensions.map(dimension => {
      const row: any = { dimension: dimension.label }
      const values = compareItems.value.map(item => {
        switch (dimension.key) {
          case 'schoolName':
            return item.schoolName
          case 'level':
            return item.schoolLevel
          case 'location':
            return `${item.province}${item.city}`
          case 'majorName':
            return item.majorName
          case 'score':
            return `${item.score}分`
          case 'enroll':
            return `${item.enroll}人`
          case 'ratio':
            return item.ratio
          default:
            return ''
        }
      })

      // 添加每列数据
      values.forEach((value, index) => {
        row[`school${index}`] = value
      })

      // 判断是否为差异数据
      row.isDifferent = highlightDifferences.value && 
        new Set(values).size > 1

      return row
    })
  }

  // 监听对比项变化，自动保存
  watch(compareItems, saveToStorage, { deep: true })

  // 初始化加载
  loadFromStorage()

  return {
    // 状态
    compareItems,
    highlightDifferences,
    selectedYear,

    // 计算属性
    itemCount,
    isFull,
    isEmpty,
    schoolIds,
    majorIds,

    // 方法
    exists,
    addItem,
    removeItem,
    clearAll,
    moveItem,
    updateItem,
    setHighlightDifferences,
    setSelectedYear,
    getTableData,
    loadFromStorage,
    saveToStorage,
    clearStorage
  }
})