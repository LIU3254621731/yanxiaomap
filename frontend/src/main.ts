import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// 引入Element Plus样式
import 'element-plus/dist/index.css'
// 引入全局样式
import './styles/index.scss'

const app = createApp(App)
const pinia = createPinia()

// 使用插件
app.use(pinia)
app.use(router)

app.mount('#app')
