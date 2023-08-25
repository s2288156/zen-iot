<template>
  <el-container class="layout">
    <el-aside>
      <AsideMenu />
    </el-aside>
    <el-container>
      <el-header>
        <TheHeader />
      </el-header>
      <el-main>
        <TheMain/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script lang="ts" setup>
import TheHeader from '@/components/layout/Header/TheHeader.vue'
import AsideMenu from '@/components/layout/Aside/AsideMenu.vue'
import TheMain from "@/components/layout/Main/TheMain.vue"</script>

<style scoped lang="scss">
.layout {
  height: 100%;

  .el-header {
    height: 45px;
    background-color: #ffffff;
    border-bottom-style: solid;
    border-bottom-width: 0;
    border-bottom-color: #dfe4ea;
  }

  .el-aside {
    width: 200px;
    background-color: #f1f2f6;
    border-right-style: solid;
    border-right-width: 0;
    border-right-color: #dfe4ea;
  }

  .el-main {
    padding: 0;
    background-color: #ffffff;
  }
}
</style>
