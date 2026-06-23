<template>
  <div
    class="map-info-window"
    :style="style"
  >
    <!-- 关闭按钮 -->
    <div
      class="close-btn"
      @click="onClose"
    >
      <el-icon><close /></el-icon>
    </div>
    
    <!-- 院校基本信息 -->
    <div class="school-info">
      <h3 class="school-name">
        {{ school.name }}
      </h3>
      <div class="school-meta">
        <el-tag
          :type="getLevelTagType(school.level)"
          size="small"
        >
          {{ getLevelLabel(school.level) }}
        </el-tag>
        <span class="school-location">{{ school.province }}{{ school.city ? ` · ${school.city}` : '' }}</span>
      </div>
      
      <!-- 院校类型和招生单位 -->
      <div class="school-details">
        <div class="detail-item">
          <el-icon><office-building /></el-icon>
          <span>{{ school.type || '综合类' }}</span>
        </div>
        <div class="detail-item">
          <el-icon><school-icon /></el-icon>
          <span>{{ school.belong || '教育部' }}</span>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button
          type="primary"
          size="small"
          @click="onViewDetail"
        >
          查看详情
        </el-button>
        <el-button
          type="default"
          size="small"
          @click="onAddCompare"
        >
          加入对比
        </el-button>
      </div>
    </div>
    
    <!-- 三角箭头 -->
    <div
      class="arrow"
      :style="arrowStyle"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Close, OfficeBuilding, School as SchoolIcon } from '@element-plus/icons-vue'
import type { School } from '@/types/school'

// Props
interface Props {
  school: School
  position: { x: number; y: number }
  visible: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  close: []
  'view-detail': [school: School]
  'add-compare': [school: School]
}>()

// 计算样式
const style = computed(() => ({
  left: `${props.position.x}px`,
  top: `${props.position.y}px`,
  display: props.visible ? 'block' : 'none'
}))

// 箭头样式（根据位置调整）
const arrowStyle = computed(() => ({
  left: '50%',
  marginLeft: '-8px'
}))

// 方法
const getLevelTagType = (level: string) => {
  switch (level) {
    case '985': return 'danger'
    case '211': return 'primary'
    case '双一流': return 'success'
    default: return 'info' // '双非'和其他情况
  }
}

const getLevelLabel = (level: string) => {
  switch (level) {
    case '985': return '985院校'
    case '211': return '211院校'
    case '双一流': return '双一流'
    case '双非': return '普通本科'
    default: return '普通本科'
  }
}

const onClose = () => {
  emit('close')
}

const onViewDetail = () => {
  emit('view-detail', props.school)
}

const onAddCompare = () => {
  emit('add-compare', props.school)
}
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.map-info-window {
  position: absolute;
  background: white;
  border-radius: 8px;
  box-shadow: $--box-shadow-dark;
  padding: 16px;
  min-width: 280px;
  max-width: 320px;
  z-index: 2000;
  transform: translate(-50%, -100%);
  margin-top: -20px;

  .close-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: $--color-gray-3;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background 0.3s;

    &:hover {
      background: $--color-gray-4;
    }

    .el-icon {
      font-size: 12px;
      color: $--color-gray-7;
    }
  }

  .school-info {
    .school-name {
      font-size: 16px;
      font-weight: bold;
      color: $--color-gray-8;
      margin: 0 0 8px 0;
      line-height: 1.4;
    }

    .school-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;

      .school-location {
        font-size: 12px;
        color: $--color-gray-6;
      }
    }

    .school-details {
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-bottom: 16px;

      .detail-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: $--color-gray-7;

        .el-icon {
          font-size: 14px;
          color: $--color-gray-5;
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 8px;

      .el-button {
        flex: 1;
      }
    }
  }

  .arrow {
    position: absolute;
    bottom: -8px;
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-top: 8px solid white;
    filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.1));
  }
}

// 响应式调整
@media (max-width: $--screen-sm) {
  .map-info-window {
    min-width: 240px;
    max-width: 280px;
    padding: 12px;
  }
}
</style>