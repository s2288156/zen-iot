<template>
  <h1>{{ props.msg || 'Default msg' }}</h1>
</template>

<script lang="ts" setup>
const props = defineProps({
  msg: Number
})
</script>

<style scoped lang="scss">

</style>
