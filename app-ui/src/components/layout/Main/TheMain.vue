<template>
  <router-view :key="key"/>
</template>

<script lang="ts" setup>

import {ref} from "vue"
import {useRoute, useRouter} from "vue-router"
import {useUserStore} from "@/stores/user";

const store = useUserStore();
const router = useRouter();

const key = ref(useRoute().path);

if (!store.getToken()) {
  router.push({path: '/login'})
}


</script>

<style scoped lang="scss"></style>
