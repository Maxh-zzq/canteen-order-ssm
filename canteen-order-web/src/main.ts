import { createApp, type Component } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import '@/styles/index.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'
import type { LoginResult } from './types'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as Component)
}

window.addEventListener('canteen:session-refreshed', (event) => {
  const detail = (event as CustomEvent<LoginResult>).detail
  if (detail) {
    useAuthStore(pinia).applySession(detail)
  }
})

app.mount('#app')
