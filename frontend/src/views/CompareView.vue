<template>
  <div class="compare-view">
    <!-- 头部 -->
    <header class="header">
      <div class="container">
        <h1>多院校对比</h1>
        <div class="actions">
          <el-button
            type="primary"
            :disabled="compareItems.length >= 4"
            @click="addMoreSchools"
          >
            添加院校
          </el-button>
          <el-button @click="clearAll">
            清空对比
          </el-button>
          <el-button
            type="text"
            icon="arrow-left"
            @click="goHome"
          >
            返回地图
          </el-button>
        </div>
      </div>
    </header>

    <!-- 对比管理 -->
    <div class="compare-manager">
      <div class="container">
        <div class="compare-cards">
          <div
            v-for="(item, index) in compareItems"
            :key="item.id"
            class="compare-card"
            :class="{ 'highlight-differences': highlightDifferences }"
          >
            <div class="card-header">
              <h3>{{ item.school.name }}</h3>
              <el-button
                type="text"
                icon="close"
                @click="removeSchool(index)"
              />
            </div>
            <div class="card-content">
              <div class="info-row">
                <span class="label">院校层次：</span>
                <span class="value">{{ item.school.level }}</span>
              </div>
              <div class="info-row">
                <span class="label">所在地：</span>
                <span class="value">{{ item.school.province }}{{ item.school.city }}</span>
              </div>
              <div class="info-row">
                <span class="label">专业：</span>
                <span class="value">{{ item.major.name }}</span>
              </div>
              <div class="info-row">
                <span class="label">复试线：</span>
                <span class="value">{{ item.admission.score }}分</span>
              </div>
              <div class="info-row">
                <span class="label">招生人数：</span>
                <span class="value">{{ item.admission.enroll }}人</span>
              </div>
              <div class="info-row">
                <span class="label">报录比：</span>
                <span class="value">{{ item.admission.ratio }}</span>
              </div>
            </div>
          </div>

          <!-- 空位提示 -->
          <div
            v-for="i in (4 - compareItems.length)"
            :key="`empty-${i}`"
            class="compare-card empty-card"
            @click="addMoreSchools"
          >
            <div class="empty-content">
              <el-icon size="40">
                <plus />
              </el-icon>
              <p>点击添加对比院校</p>
              <small>最多可对比4所院校</small>
            </div>
          </div>
        </div>

        <div class="compare-controls">
          <el-checkbox v-model="highlightDifferences">
            高亮差异数据
          </el-checkbox>
          <el-select
            v-model="selectedYear"
            placeholder="选择年份"
            style="width: 120px"
          >
            <el-option
              v-for="year in years"
              :key="year"
              :label="`${year}年`"
              :value="year"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 对比表格 -->
    <div class="compare-table-section">
      <div class="container">
        <el-card>
          <template #header>
            <h3>详细对比数据（{{ selectedYear }}年）</h3>
          </template>
          <el-table
            :data="tableData"
            border
            style="width: 100%"
          >
            <el-table-column
              prop="dimension"
              label="对比维度"
              width="150"
              fixed
            />
            <el-table-column
              v-for="(item, index) in compareItems"
              :key="item.id"
              :label="item.school.name"
              :prop="`school${index}`"
              width="200"
            >
              <template #default="{ row }">
                <span :class="{ 'highlight': highlightDifferences && row.isDifferent }">
                  {{ row[`school${index}`] }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div
            v-if="!compareItems.length"
            class="empty-table"
          >
            <el-empty description="暂无对比数据" />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useCompareStore } from '@/stores'
import { compareApi } from '@/api'
import type { School, Major, AdmissionData } from '@/types/school'

// 路由
const router = useRouter()

// Store
const compareStore = useCompareStore()

// 响应式数据
const compareItems = ref<any[]>([]) // 对比的院校数据列表
const highlightDifferences = ref(compareStore.highlightDifferences) // 是否高亮差异
const selectedYear = ref(compareStore.selectedYear) // 选中的年份
const years = ref([2024, 2023, 2022, 2021, 2020]) // 可选年份列表
const loading = ref(false) // 加载状态
const error = ref('') // 错误信息

// 详细对比数据
const detailedItems = ref<Array<{
  school: School
  major: Major
  admission: AdmissionData | null
}>>([])

// 加载对比数据
const loadCompareData = async () => {
  const storeItems = compareStore.compareItems
  
  if (compareItems.value.length === 0) {
    detailedItems.value = []
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 准备批量获取参数
    const batchItems = compareItems.value.map(item => ({
      schoolId: item.schoolId,
      majorId: item.majorId
    }))
    
    // 批量获取院校专业详情
    const details = await compareApi.getBatchSchoolMajorDetails(batchItems)
    
    if (details && details.length > 0) {
      // 处理获取到的详细数据
      detailedItems.value = details.map((detail, index) => {
        // 查找当前年份的录取数据
        const admissionData = detail.admissionHistory?.find(
          (ad: AdmissionData) => ad.year === selectedYear.value
        ) || null
        
        return {
          school: detail.school,
          major: detail.major,
          admission: admissionData
        }
      })
    } else {
      // API调用失败，使用模拟数据
      useFallbackData()
    }
  } catch (err) {
    console.error('加载对比数据失败:', err)
    error.value = '对比数据加载失败，请稍后重试'
    // 使用模拟数据
    useFallbackData()
  } finally {
    loading.value = false
  }
}

// 使用模拟数据作为后备
const useFallbackData = () => {
  const compareItems = compareStore.compareItems
  
  if (compareItems.length === 0) {
    detailedItems.value = []
    return
  }
  
  // 生成模拟详细数据
  detailedItems.value = compareItems.map((item, index) => {
    // 模拟院校数据
    const school: School = {
      id: item.schoolId,
      name: item.schoolName,
      code: `1000${index + 1}`,
      level: item.schoolLevel as '985' | '211' | '双一流' | '双非',
      province: item.province,
      city: item.city,
      type: '综合类',
      belong: '教育部',
      status: 1,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    }
    
    // 模拟专业数据
    const major: Major = {
      id: item.majorId,
      name: item.majorName,
      code: '081200',
      cultivationType: 'academic',
      degreeType: '工学硕士',
      schoolId: item.schoolId,
      duration: 3,
      status: 1,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    }
    
    // 模拟录取数据
    const scores = [350, 348, 345, 342]
    const enrolls = [52, 48, 60, 45]
    const ratios = ['8:1', '7.5:1', '6.5:1', '5:1']
    
    const admission: AdmissionData = {
      id: index + 1,
      schoolId: item.schoolId,
      majorId: item.majorId,
      year: selectedYear.value,
      planEnroll: enrolls[index] || 50,
      actualEnroll: enrolls[index] || 50,
      score: scores[index] || 340,
      ratio: ratios[index] || '6:1',
      notes: '模拟数据',
      status: 1,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01'
    }
    
    return { school, major, admission }
  })
  
  ElMessage.warning('使用模拟对比数据进行展示')
}

// 计算属性
const tableData = computed(() => {
  if (!detailedItems.value.length) {return []}

  return [
    {
      dimension: '院校名称',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.school.name
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.school.name)).size > 1
    },
    {
      dimension: '院校层次',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = getLevelLabel(item.school.level)
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.school.level)).size > 1
    },
    {
      dimension: '所在地域',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = `${item.school.province}${item.school.city}`
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => `${item.school.province}${item.school.city}`)).size > 1
    },
    {
      dimension: '专业名称',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.major?.name || '未知'
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.major?.name)).size > 1
    },
    {
      dimension: '隶属单位',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.school.belong || '未知'
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.school.belong)).size > 1
    },
    {
      dimension: '复试分数线',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.admission?.score ? `${item.admission.score}分` : '暂无数据'
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.admission?.score || 0)).size > 1
    },
    {
      dimension: '招生人数',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.admission?.actualEnroll ? `${item.admission.actualEnroll}人` : '暂无数据'
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.admission?.actualEnroll || 0)).size > 1
    },
    {
      dimension: '报录比',
      ...detailedItems.value.reduce((acc, item, index) => {
        acc[`school${index}`] = item.admission?.ratio || '暂无数据'
        return acc
      }, {} as Record<string, any>),
      isDifferent: new Set(detailedItems.value.map(item => item.admission?.ratio || '')).size > 1
    }
  ]
})

// 辅助函数：获取院校层次标签
const getLevelLabel = (level: string) => {
  switch (level) {
    case '985': return '985院校'
    case '211': return '211院校'
    case '双一流': return '双一流'
    case '双非': return '普通本科'
    default: return '普通本科'
  }
}

// 方法
const addMoreSchools = () => {
  // 跳转到地图页面进行选择
  router.push('/')
  ElMessage.info('请在地图页面选择院校进行对比')
}

const removeSchool = (index: number) => {
  compareStore.removeItem(index)
  loadCompareData()
  ElMessage.success('已移除对比项')
}

const clearAll = () => {
  compareStore.clearAll()
  detailedItems.value = []
  ElMessage.success('已清空对比列表')
}

const goHome = () => {
  router.push('/')
}

// 年份变化处理
const handleYearChange = () => {
  compareStore.setSelectedYear(selectedYear.value)
  loadCompareData()
}

// 高亮差异变化处理
const handleHighlightChange = () => {
  compareStore.setHighlightDifferences(highlightDifferences.value)
}

// 组件生命周期
onMounted(() => {
  loadCompareData()
})

// 监听对比列表变化
watch(() => compareStore.compareItems, () => {
  loadCompareData()
}, { deep: true })

// 监听年份变化
watch(selectedYear, () => {
  handleYearChange()
})

// 监听高亮差异变化
watch(highlightDifferences, () => {
  handleHighlightChange()
})
</script>

<style scoped lang="scss">
.compare-view {
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: white;
  padding: 24px 0;
  border-bottom: 1px solid #e8e8e8;

  .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;

    h1 {
      margin: 0;
      font-size: 28px;
      color: #1890ff;
    }

    .actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }
}

.compare-manager {
  padding: 32px 0;

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }

  .compare-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
    margin-bottom: 24px;

    .compare-card {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border: 1px solid #e8e8e8;
      transition: all 0.3s;

      &.highlight-differences {
        border-color: #1890ff;
      }

      &.empty-card {
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        border: 2px dashed #d9d9d9;
        background: #fafafa;

        &:hover {
          border-color: #1890ff;
          background: #e6f7ff;
        }

        .empty-content {
          text-align: center;
          color: #999;

          .el-icon {
            color: #bfbfbf;
            margin-bottom: 12px;
          }

          p {
            margin: 8px 0 4px;
            font-size: 16px;
          }

          small {
            font-size: 12px;
          }
        }
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #f0f0f0;

        h3 {
          margin: 0;
          font-size: 18px;
          color: #333;
        }
      }

      .card-content {
        .info-row {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          font-size: 14px;

          &:last-child {
            margin-bottom: 0;
          }

          .label {
            color: #666;
          }

          .value {
            font-weight: 500;
            color: #333;
          }
        }
      }
    }
  }

  .compare-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-top: 1px solid #e8e8e8;
  }
}

.compare-table-section {
  padding: 0 0 32px;

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }

  .el-card {
    border-radius: 8px;
    overflow: hidden;

    :deep(.el-card__header) {
      background: #fafafa;
      border-bottom: 1px solid #e8e8e8;
      padding: 16px 24px;

      h3 {
        margin: 0;
        font-size: 18px;
        color: #333;
      }
    }
  }

  .highlight {
    background: #fff2e8;
    font-weight: bold;
    padding: 2px 6px;
    border-radius: 2px;
  }

  .empty-table {
    padding: 60px 0;
  }
}

// 响应式设计
@media (max-width: 992px) {
  .header {
    padding: 16px 0;

    .container {
      flex-direction: column;
      gap: 16px;
      padding: 0 16px;
      align-items: flex-start;

      h1 {
        font-size: 24px;
      }

      .actions {
        width: 100%;
        justify-content: flex-start;
        flex-wrap: wrap;
      }
    }
  }

  .compare-manager {
    padding: 24px 0;

    .container {
      padding: 0 16px;
    }

    .compare-cards {
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
      margin-bottom: 16px;
    }

    .compare-controls {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start;
    }
  }

  .compare-table-section {
    .container {
      padding: 0 16px;
    }
  }
}

@media (max-width: 576px) {
  .compare-cards {
    grid-template-columns: 1fr;
  }

  .compare-card {
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>