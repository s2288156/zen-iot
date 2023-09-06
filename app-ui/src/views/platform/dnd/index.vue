<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { NodeData } from './commons';
import { GraphConfig } from '@/views/platform/dnd/GraphConfig';

const dndRef = ref();
const contentRef = ref();
let graphConfig: GraphConfig;
const nodes = reactive<Array<NodeData>>([
  { className: 'node-rect', name: 'save timeseries', type: 'rect' },
  { className: 'node-rect', name: 'save attributes', type: 'rect' },
  // { className: 'node-circle', name: 'Circle', type: 'circle' }
]);
onMounted(() => {
  graphConfig = new GraphConfig(dndRef, contentRef);
  graphConfig.init();
});
const startDrag = (node: NodeData, event: MouseEvent) => {
  const type = node.type;
  let graphNode: any;
  switch (type) {
    case 'rect':
      graphNode = graphConfig.getGraph().createNode({
        shape: 'zen-rect',
        label: 'Rect',
        ports: {
          items: [
            { id: 'port_1', group: 'top' },
            { id: 'port_2', group: 'bottom' },
            { id: 'port_3', group: 'left' },
            { id: 'port_4', group: 'right' }
          ]
        }
      });
      break;
    case 'circle':
      graphNode = graphConfig.getGraph().createNode({
        width: 60,
        height: 60,
        shape: 'circle',
        label: 'Circle',
        attrs: {
          body: {
            stroke: '#8f8f8f',
            strokeWidth: 1,
            fill: '#fff'
          }
        }
      });
      break;
  }
  graphConfig.getDnd().start(graphNode, event);
};
const printNodes = () => {
  console.log(JSON.stringify(graphConfig.getGraph().toJSON()));
};
</script>

<template>
  <div class="flow-container">
    <div class="flow-header">
      <el-button @click="printNodes">Test</el-button>
    </div>
    <div class="flow-main">
      <div class="flow-dnd" ref="dndRef">
        <template v-for="node in nodes">
          <div :class="node.className" @mousedown="startDrag(node, $event)">{{ node.name }}</div>
        </template>
      </div>
      <div class="flow-content" ref="contentRef"></div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.flow-container {
  display: flex;
  padding: 0;
  height: calc(100vh - 45px);
  flex-wrap: wrap;
}
.flow-header {
  padding: 0;
  height: 50px;
  width: 100%;
  background-color: #f1f3f4;
  border: 1px solid #dadce0;
  box-sizing: border-box;
}
.flow-main {
  display: flex;
  padding: 0;
  height: calc(100vh - 95px);
  width: 100%;
}
.flow-dnd {
  width: 200px;
  background-color: #f1f3f4;
  border: 1px solid #dadce0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-content: flex-start;
}
.flow-content {
  flex: 1;
  margin-right: 3px;
  margin-left: 3px;
  box-shadow: 0 0 10px 1px #dadce0;
}

// cell styles
.node-rect {
  font-size: 16px;
  width: 160px;
  height: 40px;
  margin: 10px;
  line-height: 40px;
  text-align: center;
  border: 1px solid #8f8f8f;
  border-radius: 6px;
  cursor: move;
}
.node-circle {
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
