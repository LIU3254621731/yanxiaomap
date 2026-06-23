<template>
  <!-- 这个组件不渲染任何DOM，只负责管理地图标记 -->
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import type { SchoolMarker as SchoolMarkerType } from '@/types/school'

// Props
interface Props {
  // 地图实例（必须）
  map: any
  // 院校数据
  school: SchoolMarkerType
  // 是否显示
  visible?: boolean
  // 是否可点击
  clickable?: boolean
  // 点击回调
  onClick?: (school: SchoolMarkerType) => void
  // 悬浮回调
  onHover?: (school: SchoolMarkerType) => void
}

const props = withDefaults(defineProps<Props>(), {
  visible: true,
  clickable: true
})

// 标记实例
const marker = ref<any>(null)
// 信息窗口实例
const infoWindow = ref<any>(null)
// 是否悬停
const isHovering = ref(false)

// 根据院校层次获取标记颜色
const getMarkerColor = (level: string) => {
  switch (level) {
    case '985':
      return '#ff4d4f' // 红色
    case '211':
      return '#1890ff' // 蓝色
    case 'double_first_class':
      return '#52c41a' // 绿色
    default:
      return '#faad14' // 黄色
  }
}

// 根据院校层次获取标记大小
const getMarkerSize = (level: string) => {
  switch (level) {
    case '985':
      return [30, 30]
    case '211':
      return [25, 25]
    case 'double_first_class':
      return [20, 20]
    default:
      return [15, 15]
  }
}

// 创建标记
const createMarker = () => {
  if (!props.map || !props.school.longitude || !props.school.latitude) {
    console.error('无法创建标记：缺少地图实例或坐标数据')
    return
  }

  try {
    // 创建标记内容（自定义HTML）
    const content = document.createElement('div')
    content.className = 'school-marker'
    content.style.cssText = `
      width: ${getMarkerSize(props.school.level)[0]}px;
      height: ${getMarkerSize(props.school.level)[1]}px;
      background-color: ${getMarkerColor(props.school.level)};
      border: 2px solid white;
      border-radius: 50%;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 12px;
      font-weight: bold;
    `
    
    // 如果是985/211院校，显示数字（简化显示）
    if (props.school.level === '985' || props.school.level === '211') {
      const levelText = document.createElement('span')
      levelText.textContent = props.school.level
      levelText.style.cssText = `
        font-size: 10px;
        color: white;
        text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
      `
      content.appendChild(levelText)
    }

    // 创建高德地图标记
    marker.value = new AMap.Marker({
      position: [props.school.longitude, props.school.latitude],
      content: content,
      offset: new AMap.Pixel(
        -getMarkerSize(props.school.level)[0] / 2,
        -getMarkerSize(props.school.level)[1] / 2
      ),
      zIndex: props.school.level === '985' ? 100 : 
              props.school.level === '211' ? 90 : 
              props.school.level === 'double_first_class' ? 80 : 70,
      extData: props.school // 将院校数据存储在标记中
    })

    // 添加到地图
    props.map.add(marker.value)

    // 添加事件监听
    if (props.clickable) {
      marker.value.on('click', handleMarkerClick)
    }
    
    // 添加鼠标事件
    marker.value.on('mouseover', handleMarkerMouseOver)
    marker.value.on('mouseout', handleMarkerMouseOut)

    // 设置可见性
    marker.value.setVisible(props.visible)

    console.log(`创建标记: ${props.school.name}`, marker.value)
  } catch (error) {
    console.error('创建标记失败:', error)
  }
}

// 创建信息窗口
const createInfoWindow = () => {
  if (!props.map) {return}

  // 信息窗口内容
  const content = `
    <div class="school-info-window">
      <div class="school-header">
        <h3>${props.school.name}</h3>
        <span class="school-level ${props.school.level}">${props.school.level}</span>
      </div>
      <div class="school-location">
        <span>📍 ${props.school.province}${props.school.city}</span>
      </div>
      ${props.school.majors && props.school.majors.length > 0 ? `
      <div class="school-majors">
        <h4>热门专业:</h4>
        <ul>
          ${props.school.majors.slice(0, 3).map(major => `
            <li>${major.name} (${major.year}年: ${major.score}分)</li>
          `).join('')}
        </ul>
      </div>
      ` : ''}
      <div class="school-action">
        <button class="detail-btn">查看详情</button>
      </div>
    </div>
  `

  infoWindow.value = new AMap.InfoWindow({
    content: content,
    offset: new AMap.Pixel(0, -30),
    closeWhenClickMap: true
  })
}

// 显示信息窗口
const showInfoWindow = () => {
  if (!marker.value || !infoWindow.value || !props.map) {return}
  
  infoWindow.value.open(props.map, marker.value.getPosition())
}

// 隐藏信息窗口
const hideInfoWindow = () => {
  if (infoWindow.value) {
    infoWindow.value.close()
  }
}

// 处理标记点击
const handleMarkerClick = (event: any) => {
  console.log('标记点击:', props.school)
  
  // 显示信息窗口
  showInfoWindow()
  
  // 触发点击回调
  if (props.onClick) {
    props.onClick(props.school)
  }
}

// 处理标记鼠标悬停
const handleMarkerMouseOver = (event: any) => {
  isHovering.value = true
  
  // 放大标记效果
  if (marker.value && marker.value.getContent()) {
    const content = marker.value.getContent() as HTMLElement
    content.style.transform = 'scale(1.2)'
    content.style.zIndex = '1000'
  }
  
  // 触发悬浮回调
  if (props.onHover) {
    props.onHover(props.school)
  }
}

// 处理标记鼠标离开
const handleMarkerMouseOut = (event: any) => {
  isHovering.value = false
  
  // 恢复标记大小
  if (marker.value && marker.value.getContent()) {
    const content = marker.value.getContent() as HTMLElement
    content.style.transform = 'scale(1)'
    content.style.zIndex = ''
  }
  
  // 隐藏信息窗口（如果悬停时显示了）
  hideInfoWindow()
}

// 销毁标记
const destroyMarker = () => {
  if (marker.value && props.map) {
    // 移除事件监听
    marker.value.off('click', handleMarkerClick)
    marker.value.off('mouseover', handleMarkerMouseOver)
    marker.value.off('mouseout', handleMarkerMouseOut)
    
    // 从地图移除
    props.map.remove(marker.value)
    marker.value = null
  }
  
  // 销毁信息窗口
  if (infoWindow.value) {
    infoWindow.value.destroy()
    infoWindow.value = null
  }
}

// 更新标记位置
const updateMarkerPosition = () => {
  if (marker.value && props.school.longitude && props.school.latitude) {
    marker.value.setPosition([props.school.longitude, props.school.latitude])
  }
}

// 更新标记可见性
const updateMarkerVisibility = () => {
  if (marker.value) {
    marker.value.setVisible(props.visible)
  }
}

// 监听props变化
watch(() => props.school, (newSchool, oldSchool) => {
  if (newSchool.longitude !== oldSchool.longitude || 
      newSchool.latitude !== oldSchool.latitude) {
    updateMarkerPosition()
  }
  
  // 如果院校数据变化大，重新创建标记
  if (newSchool.level !== oldSchool.level || 
      newSchool.name !== oldSchool.name) {
    destroyMarker()
    createMarker()
    createInfoWindow()
  }
}, { deep: true })

watch(() => props.visible, (newVisible) => {
  updateMarkerVisibility()
})

watch(() => props.map, (newMap, oldMap) => {
  if (newMap !== oldMap) {
    destroyMarker()
    if (newMap) {
      createMarker()
      createInfoWindow()
    }
  }
})

// 生命周期
onMounted(() => {
  if (props.map && props.school.longitude && props.school.latitude) {
    createMarker()
    createInfoWindow()
  }
})

onUnmounted(() => {
  destroyMarker()
})

// 暴露方法给父组件
defineExpose({
  getMarker: () => marker.value,
  getInfoWindow: () => infoWindow.value,
  showInfoWindow,
  hideInfoWindow,
  destroyMarker
})
</script>

<style scoped>
/* 组件的样式为空，因为标记样式通过JavaScript动态设置 */
</style>

<style>
/* 全局信息窗口样式 */
.school-info-window {
  padding: 12px;
  min-width: 200px;
  max-width: 300px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.school-info-window .school-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.school-info-window .school-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.school-info-window .school-header .school-level {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: bold;
  color: white;
}

.school-info-window .school-header .school-level.985 {
  background-color: #ff4d4f;
}

.school-info-window .school-header .school-level.211 {
  background-color: #1890ff;
}

.school-info-window .school-header .school-level.double_first_class {
  background-color: #52c41a;
}

.school-info-window .school-header .school-level.regular {
  background-color: #faad14;
}

.school-info-window .school-location {
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.school-info-window .school-majors {
  margin-bottom: 12px;
}

.school-info-window .school-majors h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #333;
}

.school-info-window .school-majors ul {
  margin: 0;
  padding-left: 16px;
}

.school-info-window .school-majors li {
  font-size: 13px;
  color: #666;
  margin-bottom: 2px;
}

.school-info-window .school-action {
  text-align: center;
}

.school-info-window .school-action .detail-btn {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.school-info-window .school-action .detail-btn:hover {
  background-color: #40a9ff;
}
</style>