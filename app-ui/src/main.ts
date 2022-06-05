import { createApp } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import ElementPlus from 'element-plus'

import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import store from './store'

const app = createApp(App)

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
app.use(store).use(router).use(ElementPlus, { size: 'default' }).mount('#app')
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
