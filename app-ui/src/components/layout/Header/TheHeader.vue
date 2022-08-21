<template>
  <el-page-header @updateContent="updateHeaderContent" :content="content" @back="goBack"/>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const content = ref('11')
function updateHeaderContent (data: string) {
  content.value = data
  console.log('kkkkkkkkkkk')
}
const goBack = () => {
  console.log('go back')
}
</script>

<style scoped lang="scss">
.el-page-header {
  margin-top: 15px;
}
</style>
