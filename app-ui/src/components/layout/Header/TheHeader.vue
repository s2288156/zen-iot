<template>
  <el-page-header @back="goBack">
    <template #content>
      <span>{{ title }}</span>
    </template>
    <template #extra>
      <el-dropdown>
      <span class="el-dropdown-link">
        <el-icon class="el-icon--right">
          <Setting/>
        </el-icon>
        Setting
      </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>Action 1</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">Logout</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </el-page-header>


</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {useUserStore} from "@/stores/user";
import router from "@/router";

const userStore = useUserStore();

const title = computed(() => {
  return useRoute().meta.title
})

const goBack = () => {
  console.log('go back')
}

const handleLogout = () => {
  userStore.logout().then(() => {
    router.push({path: '/login'})
  })
}
</script>

<style scoped lang="scss">
.el-page-header {
  margin-top: 15px;
}
</style>
