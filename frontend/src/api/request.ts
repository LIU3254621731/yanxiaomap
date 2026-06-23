// Axios请求封装
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { BaseResponse, ErrorResponse } from '@/types/api'

// 创建axios实例
const createAxiosInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json'
    }
  })

  // 请求拦截器
  instance.interceptors.request.use(
    (config) => {
      // 添加token等认证信息
      const token = localStorage.getItem('access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }

      // 添加请求时间戳
      config.headers['X-Request-Timestamp'] = Date.now()

      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse<BaseResponse>) => {
      const { data } = response

      // 如果是 Blob 或 ArrayBuffer 类型响应（文件下载），直接返回完整响应
      if (data instanceof Blob || data instanceof ArrayBuffer) {
        return response
      }

      // 如果响应格式不符合BaseResponse，直接返回
      if (!data || typeof data !== 'object') {
        return response
      }

      // 处理成功响应
      if (data.success) {
        return data.data
      }

      // 处理业务错误
      handleBusinessError(data as ErrorResponse)
      return Promise.reject(data)
    },
    (error) => {
      // 处理网络错误
      handleNetworkError(error)
      return Promise.reject(error)
    }
  )

  return instance
}

// 处理业务错误
const handleBusinessError = (error: ErrorResponse) => {
  const { code, message } = error

  // 根据错误码进行不同处理
  switch (code) {
    case 401:
      // 未授权，跳转到登录页
      ElMessageBox.alert('登录已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        callback: () => {
          localStorage.removeItem('access_token')
          window.location.href = '/login'
        }
      })
      break
    case 403:
      ElMessage.error('没有权限执行此操作')
      break
    case 404:
      ElMessage.error('请求的资源不存在')
      break
    case 500:
      ElMessage.error('服务器内部错误，请稍后再试')
      break
    default:
      ElMessage.error(message || '请求失败，请稍后再试')
  }
}

// 处理网络错误
const handleNetworkError = (error: any) => {
  if (error.response) {
    // 服务器响应了错误状态码
    const { status, data } = error.response

    switch (status) {
      case 400:
        ElMessage.error(data?.message || '请求参数错误')
        break
      case 401:
        ElMessage.error('登录已过期，请重新登录')
        break
      case 403:
        ElMessage.error('没有访问权限')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 500:
        ElMessage.error('服务器内部错误')
        break
      case 502:
        ElMessage.error('网关错误')
        break
      case 503:
        ElMessage.error('服务不可用')
        break
      case 504:
        ElMessage.error('网关超时')
        break
      default:
        ElMessage.error(`请求失败: ${status}`)
    }
  } else if (error.request) {
    // 请求已发出但没有收到响应
    ElMessage.error('网络连接异常，请检查网络设置')
  } else {
    // 请求配置出错
    ElMessage.error('请求配置错误')
  }

  console.error('请求错误:', error)
}

// 创建请求实例
const request = createAxiosInstance()

// 封装常用的请求方法
export const http = {
  // GET请求
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return request.get(url, config)
  },

  // POST请求
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.post(url, data, config)
  },

  // PUT请求
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.put(url, data, config)
  },

  // DELETE请求
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return request.delete(url, config)
  },

  // PATCH请求
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.patch(url, data, config)
  },

  // 上传文件
  upload: <T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> => {
    return request.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export default request