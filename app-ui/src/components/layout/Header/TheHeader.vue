<template>
  <el-page-header
    :content="title"
    @updateContent="updateHeaderContent"
    @back="goBack"
  />
</template>

<script lang="ts" setup>
import { ref } from 'vue'
defineProps({
  title: String,
})
const content = ref('11')

function updateHeaderContent(data: string) {
  content.value = data
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
