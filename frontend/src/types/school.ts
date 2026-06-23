// 院校相关类型定义

// 院校基础信息（与后端实体匹配）
export interface School {
  id: number
  name: string
  code: string
  level: '985' | '211' | '双一流' | '双非'
  province: string
  city: string
  type: string
  belong: string // 隶属单位
  enrollmentUnit?: string // 招生单位
  longitude?: number
  latitude?: number
  website?: string
  logo?: string
  status: number // 状态：1=启用，0=禁用
  address?: string // 详细地址
  introduction?: string // 院校简介
  establishedYear?: number // 建校年份
  majors?: Major[] // 院校专业列表
  createdAt: string
  updatedAt: string
  deletedAt?: string
}

// 专业信息（与后端实体匹配）
export interface Major {
  id: number
  schoolId: number
  name: string
  code: string
  categoryId?: number // 学科门类ID
  disciplineId?: number // 一级学科ID
  cultivationType?: 'academic' | 'professional' // 学术型/专业型
  degreeType: string // 学位类型
  studyMode?: 'full_time' | 'part_time' // 全日制/非全日制
  duration: number // 学制（年）
  status?: number // 状态：1=启用，0=禁用
  description?: string // 专业描述
  trainingObjective?: string // 培养目标
  mainCourses?: string // 主要课程
  employmentDirection?: string // 就业方向
  createdAt: string
  updatedAt: string
}

// 录取数据（与后端实体匹配）
export interface AdmissionData {
  id: number
  schoolId: number
  majorId: number
  year: number
  planEnroll: number // 计划招生人数
  actualEnroll: number // 实际招生人数
  score: number // 复试分数线
  ratio: string // 报录比
  recommendedCount?: number // 推免人数
  reExamTotalScore?: number // 复试总分线
  singleSubjectScore?: number // 单科线
  averageAdmissionScore?: number // 录取平均分
  status?: number // 状态：1=启用，0=禁用
  notes?: string // 备注
  createdAt: string
  updatedAt: string
}

// 地图标记
export interface SchoolMarker {
  id: number
  name: string
  longitude: number
  latitude: number
  level: string
  province: string
  city: string
  majors: Array<{
    name: string
    year: number
    score: number
  }>
}

// 对比项
export interface CompareItem {
  id: number
  schoolId: number
  schoolName: string
  schoolLevel: string
  province: string
  city: string
  majorId: number
  majorName: string
  score: number
  enroll: number
  ratio: string
  addedAt: string
}

// 筛选参数
export interface FilterParams {
  keyword?: string
  level?: string
  province?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

// 筛选结果
export interface FilterResult {
  total: number
  schools: Array<{
    id: number
    name: string
    level: string
    province: string
    majorName: string
    year: number
    score: number
    enroll: number
    ratio: number
  }>
}

// 院校-专业详情响应（与后端接口规范匹配）
export interface SchoolMajorDetailResponse {
  school: School
  major: Major
  schoolMajor?: {
    id: number
    schoolId: number
    majorId: number
    department?: string // 所属院系
    researchDirection?: string // 研究方向
    status: number // 状态：1=启用，0=禁用
  }
  admissionHistory: AdmissionData[]
}

// 对比相关类型（与后端接口规范匹配）
export interface SchoolComparison {
  schools: School[]
  comparison: Record<string, string[]>
  highlights: string[]
}

export interface CombinationComparison {
  combinations: Array<{
    schoolId: number
    schoolName: string
    majorId: number
    majorName: string
    admissionData?: {
      year: number
      planEnroll: number
      actualEnroll: number
      admissionRatio: number
    }
  }>
  comparison: Record<string, Array<string | number>>
  highlights: string[]
}

export interface AdmissionComparison {
  data: AdmissionData[]
  statistics: {
    totalRecords: number
    schoolCount: number
    majorCount: number
    yearRange: string
    totalPlanEnroll: number
    averageAdmissionRatio: number
  }
  trends: Array<{
    year: number
    recordCount: number
    planEnroll: number
    averageAdmissionRatio: number
  }>
  highlights: string[]
}

// 分页响应类型
export interface PaginatedResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}