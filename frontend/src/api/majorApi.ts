// 专业相关API
import { http } from './request'

export const majorApi = {
  // 搜索专业（支持别名）
  async searchMajors(keyword: string, page?: number, size?: number): Promise<{
    total: number
    majors: Array<{
      id: number
      name: string
      code: string
      alias?: string[]
      schoolCount: number
      averageScore: number
    }>
  }> {
    try {
      return await http.get('/search/majors', {
        params: { keyword, page, size }
      })
    } catch (error) {
      console.error('搜索专业失败:', error)
      return {
        total: 0,
        majors: []
      }
    }
  },

  // 获取专业详情
  async getMajorDetail(majorId: number): Promise<{
    id: number
    name: string
    code: string
    alias: string[]
    description: string
    cultivationType: string
    degreeType: string
    studyMode: string[]
    duration: number
    employmentRate?: number
    averageSalary?: number
  } | null> {
    try {
      return await http.get(`/details/majors/${majorId}`)
    } catch (error) {
      console.error('获取专业详情失败:', error)
      return null
    }
  },

  // 获取开设该专业的院校列表
  async getMajorSchools(majorId: number, params?: {
    province?: string
    level?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
    page?: number
    size?: number
  }): Promise<{
    total: number
    schools: Array<{
      id: number
      name: string
      level: string
      province: string
      city: string
      score: number
      enroll: number
      ratio: number
      year: number
    }>
  }> {
    try {
      return await http.get(`/majors/${majorId}/schools`, { params })
    } catch (error) {
      console.error('获取专业院校列表失败:', error)
      return {
        total: 0,
        schools: []
      }
    }
  },

  // 获取专业历年分数线趋势
  async getScoreTrend(majorId: number, years: number[] = [2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024]): Promise<Array<{
    year: number
    minScore: number
    maxScore: number
    avgScore: number
    schoolCount: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/score-trend`, {
        params: { years: years.join(',') }
      })
    } catch (error) {
      console.error('获取专业分数线趋势失败:', error)
      return []
    }
  },

  // 获取专业招生人数趋势
  async getEnrollTrend(majorId: number, years: number[] = [2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024]): Promise<Array<{
    year: number
    totalEnroll: number
    schoolCount: number
    avgEnroll: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/enroll-trend`, {
        params: { years: years.join(',') }
      })
    } catch (error) {
      console.error('获取专业招生趋势失败:', error)
      return []
    }
  },

  // 获取专业报录比趋势
  async getRatioTrend(majorId: number, years: number[] = [2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024]): Promise<Array<{
    year: number
    minRatio: number
    maxRatio: number
    avgRatio: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/ratio-trend`, {
        params: { years: years.join(',') }
      })
    } catch (error) {
      console.error('获取专业报录比趋势失败:', error)
      return []
    }
  },

  // 获取专业热门院校排名
  async getPopularSchools(majorId: number, limit: number = 10): Promise<Array<{
    rank: number
    schoolId: number
    schoolName: string
    level: string
    province: string
    score: number
    enroll: number
    ratio: number
    year: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/popular-schools`, {
        params: { limit }
      })
    } catch (error) {
      console.error('获取专业热门院校失败:', error)
      return []
    }
  },

  // 获取专业相关专业（相似专业）
  async getRelatedMajors(majorId: number, limit: number = 5): Promise<Array<{
    id: number
    name: string
    code: string
    similarity: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/related`, {
        params: { limit }
      })
    } catch (error) {
      console.error('获取相关专业失败:', error)
      return []
    }
  },

  // 获取专业就业情况
  async getEmploymentInfo(majorId: number): Promise<{
    employmentRate: number
    averageSalary: number
    popularIndustries: Array<{
      industry: string
      percentage: number
    }>
    popularPositions: Array<{
      position: string
      percentage: number
    }>
  } | null> {
    try {
      return await http.get(`/majors/${majorId}/employment`)
    } catch (error) {
      console.error('获取专业就业情况失败:', error)
      return null
    }
  },

  // 获取专业考试科目
  async getExamSubjects(majorId: number): Promise<Array<{
    subject: string
    isRequired: boolean
    score: number
  }>> {
    try {
      return await http.get(`/majors/${majorId}/exam-subjects`)
    } catch (error) {
      console.error('获取专业考试科目失败:', error)
      return []
    }
  }
}

export default majorApi