<template>
  <el-page-header @back="goBack">
    <template #content>
      <span>{{ title }}</span>
    </template>
  </el-page-header>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const title = ref(useRoute().meta.title)

const goBack = () => {
  console.log('go back')
}
</script>

<style scoped lang="scss">
.el-page-header {
  margin-top: 15px;
}
</style>
