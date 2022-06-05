import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import TheLayout from '@/components/layout/TheLayout.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'layout',
    component: TheLayout
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
