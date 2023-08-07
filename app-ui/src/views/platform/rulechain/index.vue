<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Graph } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';
import { Stencil } from '@antv/x6-plugin-stencil';
import { Rect } from '@antv/x6/lib/shape';

let graph;
let stencil;
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
  let container = document.getElementById('container');
  graph = new Graph({
    container: container,
    width: 1200,
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
  stencil = new Stencil({
    target: graph,
    groups: [{ name: 'group1' }],

  });
  container.appendChild(stencil.container);
  let rect = new Rect({
    label: 'a',
    width: 80,
    height: 40
  });
  stencil.load([rect], 'group1');
});
</script>

<template>
  <div id="container"></div>
</template>

<style scoped lang="scss"></style>
