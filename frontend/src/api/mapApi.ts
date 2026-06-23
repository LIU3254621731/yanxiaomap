// 地图相关API
import { http } from './request'
import type { MapSchoolsParams, AMapConfig } from '@/types/api'
import type { SchoolMarker, School } from '@/types/school'

// 获取地图院校数据
export const mapApi = {
  // 获取地图院校点位数据
  async getMapSchools(params?: MapSchoolsParams): Promise<School[]> {
    try {
      return await http.get<School[]>('/map/schools', { params })
    } catch (error) {
      console.error('获取地图院校数据失败:', error)
      return []
    }
  },

  // 获取高德地图配置（通过后端代理获取密钥，避免前端暴露）
  async getAMapConfig(): Promise<AMapConfig> {
    try {
      return await http.get<AMapConfig>('/map/config')
    } catch (error) {
      console.error('获取高德地图配置失败:', error)
      return {
        apiKey: 'f2a14d5a5748760eea937b4a756d6e81',
        securityKey: '986e6e43d1e301521ba536d63153d51a',
        version: '2.0',
        plugin: 'AMap.Geocoder,AMap.AutoComplete,AMap.PlaceSearch,AMap.ToolBar,AMap.Scale,AMap.Geolocation,AMap.MarkerCluster,AMap.MapType',
        appName: '研校地图-demo'
      }
    }
  },

  // 获取省份列表
  async getProvinces(): Promise<string[]> {
    try {
      return await http.get('/map/provinces')
    } catch (error) {
      console.error('获取省份列表失败:', error)
      return []
    }
  },

  // 获取院校层次列表
  async getLevels(): Promise<Array<{ label: string; value: string }>> {
    try {
      return await http.get('/map/levels')
    } catch (error) {
      console.error('获取院校层次列表失败:', error)
      return [
        { label: '985', value: '985' },
        { label: '211', value: '211' },
        { label: '双一流', value: 'double_first_class' },
        { label: '普通本科', value: 'regular' }
      ]
    }
  },

  // 根据坐标获取地址信息（逆地理编码）
  async getAddressByCoordinates(lng: number, lat: number): Promise<string> {
    try {
      const result = await http.get<{ address: string }>('/map/reverse-geocode', {
        params: { lng, lat }
      })
      return result.address || '未知地址'
    } catch (error) {
      console.error('获取地址信息失败:', error)
      return '未知地址'
    }
  },

  // 搜索附近院校
  async searchNearbySchools(lng: number, lat: number, radius: number = 5000): Promise<SchoolMarker[]> {
    try {
      const result = await http.get<{ schools: SchoolMarker[] }>('/map/nearby', {
        params: { lng, lat, radius }
      })
      return result.schools || []
    } catch (error) {
      console.error('搜索附近院校失败:', error)
      return []
    }
  }
}

export default mapApi