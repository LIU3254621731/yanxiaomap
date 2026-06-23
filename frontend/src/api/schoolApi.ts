// 院校相关API
import { http } from './request'
import type { SearchSchoolsParams, SchoolMajorParams } from '@/types/api'
import type { School, Major, AdmissionData, FilterResult, SchoolMajorDetailResponse } from '@/types/school'

export const schoolApi = {
  // 搜索院校
  async searchSchools(params?: SearchSchoolsParams): Promise<FilterResult> {
    try {
      return await http.get('/search/schools', { params })
    } catch (error) {
      console.error('搜索院校失败:', error)
      return {
        total: 0,
        schools: []
      }
    }
  },

  // 获取院校详情
  async getSchoolDetail(schoolId: number): Promise<School | null> {
    try {
      return await http.get(`/details/schools/${schoolId}`)
    } catch (error) {
      console.error('获取院校详情失败:', error)
      return null
    }
  },

  // 获取院校下的专业列表
  async getSchoolMajors(schoolId: number): Promise<Major[]> {
    try {
      return await http.get(`/schools/${schoolId}/majors`)
    } catch (error) {
      console.error('获取院校专业列表失败:', error)
      return []
    }
  },

  // 获取院校专业详情（包含录取数据）
  async getSchoolMajorDetail(params: SchoolMajorParams): Promise<SchoolMajorDetailResponse | null> {
    const { schoolId, majorId } = params
    try {
      if (majorId) {
        return await http.get<SchoolMajorDetailResponse>(`/details/schools/${schoolId}/majors/${majorId}`)
      } else {
        // 如果没有指定majorId，获取该院校第一个专业
        const majors = await this.getSchoolMajors(schoolId)
        if (majors.length > 0) {
          return await http.get<SchoolMajorDetailResponse>(`/details/schools/${schoolId}/majors/${majors[0].id}`)
        }
        return null
      }
    } catch (error) {
      console.error('获取院校专业详情失败:', error)
      return null
    }
  },

  // 获取院校历年录取数据
  async getAdmissionData(schoolId: number, majorId: number, year?: number): Promise<AdmissionData[]> {
    try {
      const params = year ? { year } : undefined
      return await http.get(`/details/schools/${schoolId}/admission/history`, { params })
    } catch (error) {
      console.error('获取录取数据失败:', error)
      return []
    }
  },

  // 获取院校对比数据
  async getCompareData(schoolIds: number[], majorIds: number[], year?: number): Promise<any[]> {
    try {
      return await http.post('/compare/schools', {
        schoolIds,
        majorIds,
        year
      })
    } catch (error) {
      console.error('获取对比数据失败:', error)
      return []
    }
  },

  // 获取院校层次统计
  async getLevelStatistics(): Promise<Record<string, number>> {
    try {
      return await http.get('/schools/statistics/level')
    } catch (error) {
      console.error('获取层次统计失败:', error)
      return {}
    }
  },

  // 获取省份统计
  async getProvinceStatistics(): Promise<Record<string, number>> {
    try {
      return await http.get('/schools/statistics/province')
    } catch (error) {
      console.error('获取省份统计失败:', error)
      return {}
    }
  },

  // 获取热门专业
  async getPopularMajors(limit: number = 10): Promise<Array<{
    name: string
    count: number
    averageScore: number
  }>> {
    try {
      return await http.get('/schools/popular-majors', { params: { limit } })
    } catch (error) {
      console.error('获取热门专业失败:', error)
      return []
    }
  },

  // 获取院校排名变化
  async getRankingTrend(schoolId: number, years: number[] = [2020, 2021, 2022, 2023, 2024]): Promise<Array<{
    year: number
    rank: number
  }>> {
    try {
      return await http.get(`/schools/${schoolId}/ranking-trend`, {
        params: { years: years.join(',') }
      })
    } catch (error) {
      console.error('获取排名趋势失败:', error)
      return []
    }
  }
}

export default schoolApi