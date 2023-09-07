<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { NodeData } from './commons';
import { GraphConfig } from '@/views/platform/dnd/GraphConfig';

const dndRef = ref();
const contentRef = ref();
let graphConfig: GraphConfig;
const nodes = reactive<Array<NodeData>>([
  { className: 'node-rect', name: 'save timeseries', shape: 'RECT_NODE', backgroundColor: '#4dd0e1' },
  { className: 'node-rect', name: 'save attributes', shape: 'RECT_NODE', backgroundColor: '#e57373' }
]);
onMounted(() => {
  graphConfig = new GraphConfig(dndRef, contentRef);
  graphConfig.init();
});
const startDrag = (node: NodeData, event: MouseEvent) => {
  let graphNode = graphConfig.getGraph().createNode({
    shape: node.shape,
    label: node.name,
    attrs: {
      body: {
        stroke: '#8f8f8f',
        strokeWidth: 1,
        fill: node.backgroundColor
      }
    },
    ports: {
      items: [
        { id: 'port_1', group: 'top' },
        { id: 'port_2', group: 'bottom' },
        { id: 'port_3', group: 'left' },
        { id: 'port_4', group: 'right' }
      ]
    },
    tools: [
      {
        name: 'node-editor',
        args: {
          x: -630,
          y: 13,
          attrs: {
            backgroundColor: '#EFF4FF',
          },
        },
      },
    ],
    data: {

    }
  });
  graphConfig.getDnd().start(graphNode, event);
};
const printNodes = () => {
  console.log(JSON.stringify(graphConfig.getGraph().toJSON()));
};

const testCreateNodes = () => {
  graphConfig.getGraph().fromJSON([
    {
      "position": {
        "x": -520,
        "y": -250
      },
      "size": {
        "width": 160,
        "height": 40
      },
      "attrs": {
        "text": {
          "text": "save timeseries"
        },
        "body": {
          "fill": "#4dd0e1"
        }
      },
      "visible": true,
      "shape": "zen-rect",
      "id": "d298a15a-d7b6-4ccf-9ae9-f82d2c977119",
      "data": {},
      "zIndex": 1
    },
    {
      "position": {
        "x": -82,
        "y": -250
      },
      "size": {
        "width": 160,
        "height": 40
      },
      "attrs": {
        "text": {
          "text": "save attributes"
        },
        "body": {
          "fill": "#e57373"
        }
      },
      "visible": true,
      "shape": "zen-rect",
      "id": "125a84b5-df49-45a8-b70b-da407a8b3587",
      "data": {},
      "zIndex": 2
    },
    {
      "shape": "edge",
      "id": "fdcd3e3d-821a-425b-a01a-95fd77ea351e",
      "source": {
        "cell": "d298a15a-d7b6-4ccf-9ae9-f82d2c977119",
        "port": "port_4"
      },
      "target": {
        "cell": "125a84b5-df49-45a8-b70b-da407a8b3587",
        "port": "port_3"
      },
      "zIndex": 3
    }
  ])
}
</script>

<template>
  <div class="flow-container">
    <div class="flow-header">
      <el-button @click="printNodes">Print</el-button>
      <el-button @click="testCreateNodes">Create</el-button>
    </div>
    <div class="flow-main">
      <div class="flow-dnd" ref="dndRef">
        <template v-for="node in nodes">
          <div :class="node.className" :style="{backgroundColor: node.backgroundColor}" @mousedown="startDrag(node, $event)">{{ node.name }}</div>
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
