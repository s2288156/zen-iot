<template>
  <router-view :key="key"/>
</template>

<script lang="ts" setup>

import {ref} from "vue"
import {useRoute} from "vue-router"
import router from "@/router";
import {useUserStore} from "@/stores/user";

const store = useUserStore();

const key = ref(useRoute().path);

if (!store.getToken()) {
  router.push({path: '/login'})
}


</script>

<style scoped lang="scss"></style>
