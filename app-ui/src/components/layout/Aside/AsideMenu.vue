<template>
  <el-menu
    default-active="2"
    class="el-menu-vertical-demo"
    @open="handleOpen"
    @close="handleClose"
  >
    <MenuItem/>
  </el-menu>
</template>

<script lang="ts" setup>
import MenuItem from '@/components/layout/Aside/MenuItem.vue'
import { reactive } from 'vue'

const menuList = reactive([
  {
    path: '/system',
    name: 'System',
    meta: {
      title: '系统管理',
      icon: '',
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

</style>
