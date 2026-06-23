<template>
  <div class="not-found-view">
    <div class="container">
      <div class="content">
        <div class="error-code">
          404
        </div>
        <div class="error-message">
          页面未找到
        </div>
        <p class="error-description">
          抱歉，您访问的页面不存在或已被移除。
        </p>
        <div class="actions">
          <el-button
            type="primary"
            @click="goHome"
          >
            返回首页
          </el-button>
          <el-button @click="goBack">
            返回上一页
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped lang="scss">
.not-found-view {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.container {
  text-align: center;
}

.content {
  background: white;
  padding: 60px 40px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  max-width: 500px;
}

.error-code {
  font-size: 96px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 16px;
  line-height: 1;
}

.error-message {
  font-size: 24px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
}

.error-description {
  color: #666;
  margin-bottom: 32px;
  font-size: 16px;
  line-height: 1.5;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>