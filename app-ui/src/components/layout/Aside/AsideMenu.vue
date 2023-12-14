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
import {computed, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'

const defaultActive = computed(() => {
  const {path} = useRoute()
  return path
})

// const routerMenuList: any[] = []
const menuList = ref<any[]>([])

useRouter().getRoutes().forEach(route => {
  if (route.meta.isMenuRoot) {
    menuList.value.push(route)
  }
})


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
