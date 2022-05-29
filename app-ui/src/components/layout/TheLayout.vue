<template>
  <el-container class="layout">
    <el-header>Header</el-header>
    <el-container>
      <el-aside width="200px">Aside</el-aside>
      <el-main>Main</el-main>
    </el-container>
  </el-container>
<!--  <el-container class="el-container">-->
<!--    <TheHeader/>-->
<!--    <el-container>-->
<!--      <TheAside/>-->
<!--      <TheMain/>-->
<!--    </el-container>-->
<!--  </el-container>-->
</template>

<script lang="ts" setup>
import TheHeader from '@/components/layout/Header/TheHeader.vue';
import TheAside from '@/components/layout/Aside/TheAside.vue';
import TheMain from '@/components/layout/Main/TheMain.vue';
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
  .el-header {
    background-color: #42b983;
  }
  .el-aside {
    background-color: azure;
  }
  .el-main {
    background-color: cornsilk;
  }
}
</style>
