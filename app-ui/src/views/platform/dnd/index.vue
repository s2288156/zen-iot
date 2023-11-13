<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { NodeData } from './commons';
import { GraphConfig } from '@/views/platform/dnd/GraphConfig';
import { saveRuleChain } from '@/api/rule-chain-apis';
import { RuleChain } from '@/api/data/types';
import { RuleChainDefine } from '@/api/data/RuleChainDefine';

const dndRef = ref();
const contentRef = ref();
const ruleChain = ref<RuleChain>({ name: '', edges: [], nodes: [] });
let graphConfig: GraphConfig;
const nodes = reactive<Array<NodeData>>([
  { className: 'node-rect', name: 'save timeseries', shape: 'rect_node', backgroundColor: '#4dd0e1', nodeType: 'SAVE_TIMESERIES' },
  { className: 'node-rect', name: 'save attributes', shape: 'rect_node', backgroundColor: '#e57373', nodeType: 'SAVE_ATTRIBUTES' }
]);
onMounted(() => {
  graphConfig = new GraphConfig(dndRef, contentRef);
  graphConfig.init();
});
// 拖动新增
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
            backgroundColor: '#EFF4FF'
          }
        }
      }
    ],
    data: {
      nodeType: node.nodeType
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
      position: { x: -590, y: -180 },
      size: { width: 160, height: 40 },
      attrs: { text: { text: 'save timeseries' }, body: { fill: '#4dd0e1' } },
      visible: true,
      shape: 'rect_node',
      ports: {
        groups: {
          top: { position: 'top', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          bottom: { position: 'bottom', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          left: { position: 'left', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          right: { position: 'right', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } }
        },
        items: [
          { id: 'port_1', group: 'top' },
          { id: 'port_2', group: 'bottom' },
          { id: 'port_3', group: 'left' },
          { id: 'port_4', group: 'right' }
        ]
      },
      id: '5d2ac1a3-644b-465c-9c82-288b954b74ae',
      data: { nodeType: 'SAVE_TIMESERIES' },
      tools: { items: [{ name: 'node-editor', args: { x: -630, y: 13, attrs: { backgroundColor: '#EFF4FF' } } }] },
      zIndex: 1
    },
    {
      position: { x: -230, y: -180 },
      size: { width: 160, height: 40 },
      attrs: { text: { text: 'save attributes' }, body: { fill: '#e57373' } },
      visible: true,
      shape: 'rect_node',
      ports: {
        groups: {
          top: { position: 'top', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          bottom: { position: 'bottom', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          left: { position: 'left', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } },
          right: { position: 'right', attrs: { circle: { magnet: true, stroke: '#8f8f8f', r: 3, style: { visibility: 'hidden' } } } }
        },
        items: [
          { id: 'port_1', group: 'top' },
          { id: 'port_2', group: 'bottom' },
          { id: 'port_3', group: 'left' },
          { id: 'port_4', group: 'right' }
        ]
      },
      id: 'a7ee81ee-a122-44f2-a5f7-755564918263',
      data: { nodeType: 'SAVE_ATTRIBUTES' },
      tools: { items: [{ name: 'node-editor', args: { x: -630, y: 13, attrs: { backgroundColor: '#EFF4FF' } } }] },
      zIndex: 2
    },
    {
      shape: 'edge',
      id: '4aa54ec9-826e-4756-a524-f88cee6db6f0',
      source: { cell: '5d2ac1a3-644b-465c-9c82-288b954b74ae', port: 'port_4' },
      target: { cell: 'a7ee81ee-a122-44f2-a5f7-755564918263', port: 'port_3' },
      zIndex: 3
    }
  ]);
  graphConfig
    .getGraph()
    .toJSON()
    .cells.forEach(cell => {
      if (cell.shape === 'edge') {
        let edge = RuleChainDefine.newEdgeFromCell(cell);
        ruleChain.value.edges.push(edge);
      } else if (cell.shape == 'rect_node') {
        let node = RuleChainDefine.newNodeFromCell(cell);
        ruleChain.value.nodes.push(node);
      }
    });
  saveRuleChain(ruleChain.value).then(resp => {
    if (resp.status === 200) {
      console.log('@@@@@@@@@@@@@@@@@@@@@@@@');
    }
  });
};
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
          <div :class="node.className" :style="{ backgroundColor: node.backgroundColor }" @mousedown="startDrag(node, $event)">{{ node.name }}</div>
        </template>
      </div>
      <div class="flow-content" ref="contentRef"></div>
      <div class="flow-info">
        <el-row class="flow-info-row">
          <span>规则链名称: </span>
          <el-input placeholder="规则链名称" v-model="ruleChain.name" />
        </el-row>
      </div>
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
.flow-info {
  width: 200px;
  background-color: #f1f3f4;
  border: 1px solid #dadce0;
  .el-row {
    margin: 5px;
  }
  span {
    margin: 1px;
  }
  .el-input {
    margin-top: 5px;
  }
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
