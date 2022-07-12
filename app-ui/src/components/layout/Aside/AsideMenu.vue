<template>
  <div class="logo">
    <el-image :src="ZenLogo"/>
  </div>
  <el-menu
    default-active="2"
    class="menu"
    @open="handleOpen"
    @close="handleClose"
  >
    <menu-item :menuList="menuList"/>
  </el-menu>
</template>

<script lang="ts" setup>
import MenuItem from '@/components/layout/Aside/MenuItem.vue'
import ZenLogo from '@/assets/zen-logo.svg'
import { reactive } from 'vue'

const menuList = reactive([
  {
    path: '/system',
    name: 'System',
    meta: {
      title: '系统管理',
      icon: 'Setting',
      roles: ['ROLE_ADMIN']
    },
    children: [
      {
        path: '/system/user',
        name: 'User',
        meta: {
          title: '用户管理',
          icon: 'User',
          roles: ['ROLE_ADMIN']
        }
      },
      {
        path: '/system/menu',
        name: 'Menu',
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
    meta: {
      title: '平台配置',
      icon: 'Platform',
      roles: ['ROLE_ADMIN']
    }
  }
])

const handleOpen = (key: string, keyPath: string[]) => {
  console.log(key, keyPath)
}

const handleClose = (key: string, keyPath: string[]) => {
  console.log(key, keyPath)
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
