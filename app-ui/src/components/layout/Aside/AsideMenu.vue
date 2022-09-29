<template>
  <div class="logo">
    <el-image :src="ZenLogo" />
  </div>
  <el-menu
    :default-active="defaultActive"
    class="menu"
    router
    @open="handleOpen"
    @close="handleClose"
  >
    <menu-item :menu-list="menuList" />
  </el-menu>
</template>

<script lang="ts" setup>
import MenuItem from '@/components/layout/Aside/MenuItem.vue'
import ZenLogo from '@/assets/zen-logo.svg'
import { computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import TheLayout from '@/components/layout/TheLayout.vue'

const route = useRoute()

const defaultActive = computed(() => {
  const { path } = route
  return path
})

const menuList = reactive([
  {
    path: '/',
    name: 'Home',
    meta: {
      title: '首页',
      icon: 'HomeFilled',
      roles: [],
    },
  },
  {
    path: '/system',
    name: 'System',
    meta: {
      title: '系统管理',
      icon: 'Setting',
      roles: ['ROLE_ADMIN'],
    },
    children: [
      {
        path: '/system/user',
        name: 'User',
        meta: {
          title: '用户管理',
          icon: 'User',
          roles: ['ROLE_ADMIN'],
        },
      },
      {
        path: '/system/menu',
        name: 'Menu',
        meta: {
          title: '菜单管理',
          icon: 'Menu',
          roles: ['ROLE_ADMIN'],
        },
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
    },
    children: [
      {
        path: '/platform/device',
        name: 'User',
        component: () => import('@/views/platform/DeviceManager.vue'),
        meta: {
          title: '设备管理',
          icon: 'Cpu',
          roles: ['ROLE_ADMIN'],
        },
      },
      {
        path: '/platform/parent',
        name: 'parent',
        component: () => import('@/views/platform/TheParent.vue'),
        meta: {
          title: 'parent',
          icon: 'Cpu',
          roles: ['ROLE_ADMIN'],
        },
      },
      {
        path: '/platform/sub',
        name: 'sub',
        component: () => import('@/views/platform/TheSub.vue'),
        meta: {
          title: 'sub',
          icon: 'Cpu',
          roles: ['ROLE_ADMIN'],
        },
      },
    ],
  },
])

const handleOpen = (key: string, keyPath: string[]) => {
  console.log('open', key, keyPath)
}

const handleClose = (key: string, keyPath: string[]) => {
  console.log('close', key, keyPath)
}
</script>

<style scoped lang="scss">
.logo {
  display: flex;
  width: 100%;
  height: 45px;

  .el-image {
    width: 90px;
    height: 45px;
    margin-left: 10px;
  }
}

.menu {
  background-color: #ced6e0;
}
</style>
