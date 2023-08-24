import {createRouter, createWebHistory} from 'vue-router'
import TheLayout from '@/components/layout/TheLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/TheLogin.vue'),
      meta: {
        title: '登录'
      },
    },
    {
      path: '/',
      component: TheLayout,
      redirect: '/home',
      children: [
        {
          path: '/home',
          name: 'Home',
          component: () => import('@/views/home/TheHome.vue'),
          meta: {
            title: '首页',
            icon: 'HomeFilled',
            roles: [],
            isMenuRoot: true
          },
        },
      ],
    },
    {
      path: '/system',
      name: 'System',
      component: TheLayout,
      meta: {
        title: '系统管理',
        icon: 'Setting',
        roles: ['ROLE_ADMIN'],
        isMenuRoot: true
      },
      children: [
        {
          path: '/system/user',
          name: 'User',
          component: () => import('@/views/system/user/index.vue'),
          meta: {
            title: '用户管理',
            icon: 'User',
            roles: ['ROLE_ADMIN'],
          },
        },
        {
          path: '/system/menu',
          name: 'Menu',
          component: () => import('@/views/system/MenuManager.vue'),
          meta: {
            title: '菜单管理',
            icon: 'Menu',
            roles: ['ROLE_ADMIN'],
          }
        },
        {
          path: '/system/pinia',
          name: 'Pinia',
          component: () => import('@/views/system/demo/PiniaDemo.vue'),
          meta: {
            title: 'Pinia',
            icon: 'Menu',
            roles: ['ROLE_ADMIN'],
          }
        },
      ],
    },
    {
      path: '/platform',
      name: 'Platform',
      component: TheLayout,
      meta: {
        title: '平台配置',
        icon: 'Platform',
        roles: ['ROLE_ADMIN'],
        isMenuRoot: true
      },
      children: [
        {
          path: '/platform/device',
          name: 'Device',
          component: () => import('@/views/platform/device/index.vue'),
          meta: {
            title: '设备管理',
            icon: 'Cpu',
            roles: ['ROLE_ADMIN'],
          },
        },
        {
          path: '/platform/rulechain',
          name: 'RuleChain',
          component: () => import('@/views/platform/rulechain/index.vue'),
          meta: {
            title: '规则链',
            icon: 'Cpu',
            roles: ['ROLE_ADMIN'],
          },
        },
        {
          path: '/platform/dnd',
          name: 'Dnd',
          component: () => import('@/views/platform/dnd/index.vue'),
          meta: {
            title: '规则链Dnd',
            icon: 'Cpu',
            roles: ['ROLE_ADMIN'],
          },
        }
      ],
    },
    {
      path: '/simulator',
      name: 'Simulator',
      component: TheLayout,
      meta: {
        title: '仿真平台',
        icon: 'Box',
        roles: ['ROLE_ADMIN'],
        isMenuRoot: true
      },
      children: [
        {
          path: '/simulator/device',
          name: 'Simulator Device',
          component: () => import('@/views/simulator/device/index.vue'),
          meta: {
            title: '仿真设备管理',
            icon: 'Cpu',
            roles: ['ROLE_ADMIN'],
          },
        }
      ],
    },
  ],
})

export default router
