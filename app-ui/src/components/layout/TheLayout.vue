<template>
  <el-container class="layout">
    <el-aside width="200px">
      <AsideMenu />
    </el-aside>
    <el-container>
      <el-header>
        <TheHeader :title="headerContent" />
      </el-header>
      <el-main>
        <router-view @header-title="callBackTitle" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script lang="ts" setup>
import TheHeader from '@/components/layout/Header/TheHeader.vue'
import AsideMenu from '@/components/layout/Aside/AsideMenu.vue'
import { ref } from 'vue'

const headerContent = ref('')

const callBackTitle = (title: string) => {
  console.log('##################' + title)
  headerContent.value = title
}
</script>

<style scoped lang="scss">
.layout {
  height: 100%;

  .el-header {
    height: 45px;
    background-color: #ffffff;
    border-bottom-style: solid;
    border-bottom-width: 1px;
    border-bottom-color: #dfe4ea;
  }

  .el-aside {
    background-color: #f1f2f6;
    border-right-style: solid;
    border-right-width: 1px;
    border-right-color: #dfe4ea;
  }

  .el-main {
    background-color: #ffffff;
  }
}
</style>
