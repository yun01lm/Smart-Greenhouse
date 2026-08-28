import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'dayjs/locale/zh-cn'
// Element Plus 深色主题变量（配合 <html class="dark">）
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './assets/main.css'

// 全站统一深色主题（方案丙：深色玻璃拟态）
document.documentElement.classList.add('dark')

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)

// Element Plus 按需引入（由 unplugin-vue-components 自动处理）
// 无需手动 import 组件，在 .vue 文件中直接使用即可

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Component:', vm)
  console.error('Info:', info)
}

app.mount('#app')
