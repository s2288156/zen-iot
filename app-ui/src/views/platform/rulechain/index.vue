<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Graph } from '@antv/x6';
let graph;
const data = {
  // 节点
  nodes: [
    {
      id: 'node1', // String，可选，节点的唯一标识
      x: 40, // Number，必选，节点位置的 x 值
      y: 40, // Number，必选，节点位置的 y 值
      width: 80, // Number，可选，节点大小的 width 值
      height: 40, // Number，可选，节点大小的 height 值
      label: 'hello' // String，节点标签
    },
    {
      id: 'node2', // String，节点的唯一标识
      x: 160, // Number，必选，节点位置的 x 值
      y: 180, // Number，必选，节点位置的 y 值
      width: 80, // Number，可选，节点大小的 width 值
      height: 40, // Number，可选，节点大小的 height 值
      label: 'world', // String，节点标签
      ports: {
        groups: {
          group1: {
            attrs: {
              circle: {
                r: 3,
                stroke: '#31d0c6',
                fill: '#fff'
              }
            }
          }
        },
        items: [
          { id: 'port1', group: 'group1' },
          { id: 'port2', group: 'group1' },
          { id: 'port3', group: 'group1' }
        ]
      }
    }
  ],
  // 边
  edges: [
    {
      source: 'node1', // String，必须，起始节点 id
      target: 'node2' // String，必须，目标节点 id
    }
  ]
};
onMounted(() => {
  graph = new Graph({
    container: document.getElementById('container'),
    width: 800,
    height: 600,
    background: {
      color: '#fffbe6'
    },
    grid: {
      type: 'dot',
      size: 20,
      visible: true,
      args: {
        color: 'red',
        thickness: 2
      }
    },
    panning: true
  });
  graph.fromJSON(data);
});
const zoomVal = ref();
const translateY = ref();
const zoom = () => {
  graph.zoom(zoomVal.value);
};
const translate = () => {
  graph.translate(80, translateY.value);
};
</script>

<template>
  <div id="container"></div>
  <div>
    <div>
      <el-input v-model="zoomVal"></el-input>
      <el-button @click="zoom">zoom</el-button>
    </div>
    <div>
      <el-input v-model="translateY"></el-input>
      <el-button @click="translate">translate</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss"></style>
