// 对比相关API
import { http } from './request'
import type { CompareParams } from '@/types/api'
import type { SchoolMajorDetailResponse } from '@/types/school'

export const compareApi = {
  // 获取对比数据
  async getCompareData(params: CompareParams): Promise<any[]> {
    try {
      return await http.post('/compare/schools', params)
    } catch (error) {
      console.error('获取对比数据失败:', error)
      return []
    }
  },

  // 批量获取院校专业详情用于对比
  async getBatchSchoolMajorDetails(requests: Array<{ schoolId: number; majorId: number }>): Promise<SchoolMajorDetailResponse[]> {
    try {
      return await http.post('/compare/school-major', requests)
    } catch (error) {
      console.error('批量获取详情失败:', error)
      return []
    }
  },

  // 获取对比分析报告
  async getCompareReport(items: Array<{ schoolId: number; majorId: number }>, year?: number): Promise<{
    summary: {
      bestScore: { schoolId: number; score: number }
      bestRatio: { schoolId: number; ratio: string }
      bestEnroll: { schoolId: number; enroll: number }
    }
    details: Array<{
      dimension: string
      values: Array<{ schoolId: number; value: any; rank: number }>
    }>
  }> {
    try {
      return await http.post('/compare/report', { items, year })
    } catch (error) {
      console.error('获取对比报告失败:', error)
      return {
        summary: {
          bestScore: { schoolId: 0, score: 0 },
          bestRatio: { schoolId: 0, ratio: '0:0' },
          bestEnroll: { schoolId: 0, enroll: 0 }
        },
        details: []
      }
    }
  },

  // 导出对比结果为Excel
  async exportToExcel(items: Array<{ schoolId: number; majorId: number }>, year?: number): Promise<Blob> {
    try {
      const response = await http.post(
        '/compare/export',
        { items, year },
        { responseType: 'blob' }
      )
      return response
    } catch (error) {
      console.error('导出对比结果失败:', error)
      throw error
    }
  },

  // 保存对比方案
  async saveCompareScheme(name: string, items: Array<{ schoolId: number; majorId: number }>, userId?: string): Promise<{
    id: number
    name: string
    createdAt: string
  }> {
    try {
      return await http.post('/compare/schemes', { name, items, userId })
    } catch (error) {
      console.error('保存对比方案失败:', error)
      throw error
    }
  },

  // 获取用户保存的对比方案
  async getCompareSchemes(userId?: string): Promise<Array<{
    id: number
    name: string
    items: Array<{ schoolId: number; majorId: number }>
    createdAt: string
  }>> {
    try {
      const params = userId ? { userId } : undefined
      return await http.get('/compare/schemes', { params })
    } catch (error) {
      console.error('获取对比方案失败:', error)
      return []
    }
  },

  // 删除对比方案
  async deleteCompareScheme(schemeId: number): Promise<boolean> {
    try {
      await http.delete(`/compare/schemes/${schemeId}`)
      return true
    } catch (error) {
      console.error('删除对比方案失败:', error)
      return false
    }
  },

  // 获取对比历史
  async getCompareHistory(userId?: string, limit: number = 10): Promise<Array<{
    id: number
    items: Array<{ schoolId: number; majorId: number }>
    comparedAt: string
  }>> {
    try {
      const params = { userId, limit }
      return await http.get('/compare/history', { params })
    } catch (error) {
      console.error('获取对比历史失败:', error)
      return []
    }
  },

  // 清空对比历史
  async clearCompareHistory(userId?: string): Promise<boolean> {
    try {
      const params = userId ? { userId } : undefined
      await http.delete('/compare/history', { params })
      return true
    } catch (error) {
      console.error('清空对比历史失败:', error)
      return false
    }
  }
}

export default compareApi