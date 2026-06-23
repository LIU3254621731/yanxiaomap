// 路由配置
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由定义
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '首页 - 全国考研院校地图' }
  },
  {
    path: '/detail/:schoolId/:majorId?',
    name: 'Detail',
    component: () => import('../views/DetailView.vue'),
    meta: { title: '院校专业详情' },
    props: true
  },
  {
    path: '/compare',
    name: 'Compare',
    component: () => import('../views/CompareView.vue'),
    meta: { title: '多校对比' }
  },
  {
    path: '/compliance',
    name: 'Compliance',
    component: () => import('../views/ComplianceView.vue'),
    meta: { title: '合规声明' }
  },
  {
    path: '/api-test',
    name: 'ApiTest',
    component: () => import('../views/ApiTestView.vue'),
    meta: { title: 'API测试' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFoundView.vue'),
    meta: { title: '页面未找到' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title as string
  }
  next()
})

export default router