<template>
  <router-view :key="key"/>
</template>

<script lang="ts" setup>

import {ref} from "vue";
import {useRoute} from "vue-router";

const key = ref(useRoute().path)

</script>

<style scoped lang="scss"></style>
