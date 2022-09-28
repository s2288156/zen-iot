import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// import ElementPlus from 'element-plus'
// import 'element-plus/dist/index.css'
// import '@/style/index.scss'
import App from './App.vue'
import router from './router'

import '@/style/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// app.use(ElementPlus, { size: 'default' })
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
