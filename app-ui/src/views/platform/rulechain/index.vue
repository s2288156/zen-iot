<script setup lang="ts">
import LogicFlow from '@logicflow/core'
import '@logicflow/core/dist/style/index.css'
import { onMounted, ref } from 'vue'

const container = ref(null)
let lf

onMounted(() => {
  lf = new LogicFlow({
    container: container.value,
    grid: true,
    keyboard: {
      enabled: true,
      shortcuts: [
        {
          keys: ['backspace', 'delete'],
          callback: () => {
            const elements = lf.getSelectElements(true)
            lf.clearSelectElements()
            elements.edges.forEach(edge => lf.deleteEdge(edge.id))
            elements.nodes.forEach(node => lf.deleteNode(node.id))
          }
        }
      ]
    }
  })
  lf.render()
})

const init = () => {
  lf.render({
    nodes: [
      {
        id: 'node_id_1',
        type: 'rect',
        x: 100,
        y: 100,
        text: { x: 100, y: 100, value: '节点1' },
        properties: {}
      },
      {
        id: 'node_id_2',
        type: 'circle',
        x: 200,
        y: 300,
        text: { x: 300, y: 300, value: '节点2' },
        properties: {}
      },
      {
        id: 'node_id_3',
        type: 'ellipse',
        x: 300,
        y: 100,
        text: { x: 300, y: 100, value: 'ellipse' },
        properties: {}
      },
      {
        id: 'node_id_4',
        type: 'polygon',
        x: 400,
        y: 100,
        text: { x: 400, y: 100, value: 'polygon' },
        properties: {}
      },
      {
        id: 'node_id_5',
        type: 'diamond',
        x: 500,
        y: 100,
        text: { x: 500, y: 100, value: 'diamond' },
        properties: {}
      },
      {
        id: 'node_id_6',
        type: 'text',
        x: 600,
        y: 100,
        text: { x: 600, y: 100, value: 'text' },
        properties: {}
      },
      {
        id: 'node_id_7',
        type: 'html',
        x: 700,
        y: 100,
        text: { x: 700, y: 100, value: 'html' },
        properties: {}
      }
    ],
    edges: [
      {
        id: 'edge_id',
        type: 'polyline',
        sourceNodeId: 'node_id_1',
        targetNodeId: 'node_id_2',
        text: { x: 139, y: 200, value: '连线' },
        startPoint: { x: 100, y: 140 },
        endPoint: { x: 200, y: 250 },
        pointsList: [
          { x: 100, y: 140 },
          { x: 100, y: 200 },
          { x: 200, y: 200 },
          { x: 200, y: 250 }
        ],
        properties: {}
      }
    ]
  })
}
</script>

<template>
  <el-row class="container">
    <el-col class="container-item" :span="20"></el-col>
    <el-col class="container-item" :span="4">
      <el-button @click="init" type="primary">init</el-button>
    </el-col>
  </el-row>
</template>

<style scoped lang="scss">
.container {
  display: flex;
  width: 1300px;
  height: 800px;
}
.container-item {
  display: flex;
}
</style>
