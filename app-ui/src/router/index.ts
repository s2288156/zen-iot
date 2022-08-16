import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import TheLayout from '@/components/layout/TheLayout.vue'
import TheHome from '@/views/home/TheHome.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: TheLayout,
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: TheHome,
        meta: {
          title: '首页',
          icon: 'HomeFilled',
          roles: []
        }
      }
    ]
  },
  {
    path: '/system',
    name: 'System',
    component: TheLayout,
    meta: {
      title: '系统管理',
      icon: 'Setting',
      roles: ['ROLE_ADMIN']
    },
    children: [
      {
        path: '/system/user',
        name: 'User',
        component: () => import('@/views/system/user/UserManager.vue'),
        meta: {
          title: '用户管理',
          icon: 'User',
          roles: ['ROLE_ADMIN']
        }
      },
      {
        path: '/system/menu',
        name: 'Menu',
        component: () => import('@/views/system/MenuManager.vue'),
        meta: {
          title: '菜单管理',
          icon: 'Menu',
          roles: ['ROLE_ADMIN']
        }
      }
    ]
  },
  {
    path: '/platform',
    name: 'Platform',
    component: TheLayout,
    meta: {
      title: '平台配置',
      icon: 'Platform',
      roles: ['ROLE_ADMIN']
    },
    children: [
      {
        path: '/platform/device',
        name: 'Device',
        component: () => import('@/views/platform/DeviceManager.vue'),
        meta: {
          title: '设备管理',
          icon: 'Cpu',
          roles: ['ROLE_ADMIN']
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
