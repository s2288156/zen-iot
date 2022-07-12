<template>
  <div class="logo">
    <el-image :src="ZenLogo" />
  </div>
  <el-page-header content="detail" @back="goBack" />
</template>

<script lang="ts" setup>
import ZenLogo from '@/assets/zen-logo.png'

const goBack = () => {
  console.log('go back')
}
</script>

<style scoped lang="scss">

</style>
