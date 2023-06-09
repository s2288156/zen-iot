<template>
  <h1>Home</h1>
  <div id="myChart" style="height: 300px;"></div>
</template>

<script lang="ts" setup>
import * as echarts from 'echarts'
import { onMounted } from 'vue'

onMounted(() => {
  let myChart = echarts.init(document.getElementById('myChart'))
  myChart.setOption({
    title: {
      text: 'ECharts 入门示例'
    },
    tooltip: {},
    xAxis: {
      data: ['衬衫', '羊毛衫', '雪纺衫', '裤子', '高跟鞋', '袜子']
    },
    yAxis: {},
    series: [
      {
        name: '销量',
        type: 'bar',
        data: [5, 20, 36, 10, 10, 20]
      }
    ]
  })
  window.onresize = function () {
    myChart.resize()
  }
})
</script>

<style scoped lang="scss"></style>
