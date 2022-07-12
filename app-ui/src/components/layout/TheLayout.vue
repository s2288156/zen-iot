<template>
  <el-container class="layout">
    <el-header>
      <TheHeader/>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <AsideMenu/>
      </el-aside>
      <el-main>
        <TheMain/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script lang="ts" setup>
import TheHeader from '@/components/layout/Header/TheHeader.vue'
import AsideMenu from '@/components/layout/Aside/AsideMenu.vue'
import TheMain from '@/components/layout/Main/TheMain.vue'
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
  .el-header {
    background-color: #ffffff;
  }
  .el-aside {
    background-color: #f1f2f6;
  }
  .el-main {
    background-color: #ffffff;
  }
}
</style>
