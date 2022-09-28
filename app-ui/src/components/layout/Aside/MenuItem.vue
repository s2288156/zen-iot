<template>
  <template v-for="menu in menuList" :key="menu.path">
    <el-sub-menu
      v-if="menu.children && menu.children.length > 0"
      :index="menu.path"
    >
      <template #title>
        <el-icon>
          <component :is="menu.meta.icon"></component>
        </el-icon>
        <span>{{ menu.meta.title }}</span>
      </template>
      <menu-item :menu-list="menu.children"></menu-item>
    </el-sub-menu>
    <el-menu-item v-else :index="menu.path">
      <el-icon>
        <component :is="menu.meta.icon"></component>
      </el-icon>
      <span>{{ menu.meta.title }}</span>
    </el-menu-item>
  </template>
</template>

<script lang="ts" setup>
defineProps(["menuList"]);
</script>

<style scoped lang="scss"></style>
