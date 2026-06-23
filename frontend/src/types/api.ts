// API相关类型定义

// 基础响应格式
export interface BaseResponse<T = any> {
  success: boolean
  code: number
  message: string
  data: T
  timestamp: string
}

// 分页参数
export interface PaginationParams {
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 分页数据
export interface PaginationData<T> {
  total: number
  pages: number
  current: number
  size: number
  records: T[]
}

// 错误响应
export interface ErrorResponse {
  success: false
  code: number
  message: string
  errors?: Record<string, string[]>
  timestamp: string
}

// 地图数据接口参数
export interface MapSchoolsParams {
  province?: string
  city?: string
  level?: string
  type?: string
  belong?: string
  minLng?: number
  maxLng?: number
  minLat?: number
  maxLat?: number
}

// 筛选查询接口参数
export interface SearchSchoolsParams extends PaginationParams {
  keyword?: string
  province?: string
  city?: string
  level?: string
  type?: string
  belong?: string
  year?: number
  minScore?: number
  maxScore?: number
}

// 院校专业详情接口参数
export interface SchoolMajorParams {
  schoolId: number
  majorId?: number
}

// 对比接口参数
export interface CompareParams {
  schoolIds: number[]
  majorIds: number[]
  year?: number
}

// 高德地图配置（后端接口返回格式）
export interface AMapConfig {
  apiKey: string
  securityKey: string
  version?: string
  plugin?: string
  appName?: string
}