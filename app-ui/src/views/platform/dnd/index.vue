<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Graph } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';

const dndRef = ref()
const contentRef = ref()
let graph;
let dnd;

onMounted(() => {
  graph = new Graph({
    container: contentRef.value,
    grid: true,
    background: {
      color: '#F2F7FA'
    }
  });
  graph.centerContent()
  dnd = new Dnd({
    target: graph,
    dndContainer: dndRef.value
  });
});
</script>

<template>
  <div class="flow-container">
    <div class="flow-dnd" ref="dndRef">
      <div class='dnd-rect' node-type='rect'>Rect</div>
      <div class='dnd-circle' node-type='circle'>Circle</div>
    </div>
    <div class="flow-content" ref="contentRef"></div>
  </div>
</template>

<style scoped lang="scss">
.flow-container {
  display: flex;
  padding: 0;
  height: 100%;
}
.flow-dnd {
  position: relative;
  width: 200px;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-content: flex-start;
}
.flow-content {
  flex: 1;
  margin-right: 8px;
  margin-left: 8px;
  box-shadow: 0 0 10px 1px #e9e9e9;
}
.dnd-rect {
  width: 100px;
  height: 40px;
  margin: 16px;
  line-height: 40px;
  text-align: center;
  border: 1px solid #8f8f8f;
  border-radius: 6px;
  cursor: move;
}

.dnd-circle {
  width: 60px;
  height: 60px;
  margin: 16px;
  line-height: 60px;
  text-align: center;
  border: 1px solid #8f8f8f;
  border-radius: 100%;
  cursor: move;
}
</style>
