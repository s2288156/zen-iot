<template>
  <h1>{{ num }}</h1>
  <div>
    <el-button @click="increment">Counter</el-button>
  </div>
  <Dialog :msg="num"/>
</template>

<script lang="ts" setup>
import {useCounterStore} from "@/stores/counter";
import {ref} from "vue";
import Dialog from "@/views/system/demo/Dialog.vue";

const num = ref(0)
const counter = useCounterStore()

const increment = () => {
  counter.increment()
  num.value = counter.doubleCount
}

</script>

<style lang="scss" scoped>

</style>
