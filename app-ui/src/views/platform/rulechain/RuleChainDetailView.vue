<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NodeData } from './commons';
import { RuleChainGraph } from '@/views/platform/rulechain/RuleChainGraph';
import { queryRuleChain, saveRuleChain } from '@/api/rule-chain-apis';
import { RuleChain } from '@/api/data/types';
import { RuleChainDefine } from '@/api/data/RuleChainDefine';
import { ElMessage } from 'element-plus';
import router from '@/router';
import {IotGraph} from "@/components/graph/IotGraph";
import {Cell, Edge, Node} from "@antv/x6";

const dndRef = ref();
const contentRef = ref();
const ruleChain = ref<RuleChain>({ name: '', edges: [], nodes: [] });
let graphConfig: IotGraph;
const nodeTemplates = ref<Array<NodeData>>([
  {
    className: 'node-rect',
    name: 'save timeseries',
    shape: 'rect_node',
    backgroundColor: '#4dd0e1',
    nodeType: 'SAVE_TIMESERIES'
  },
  {
    className: 'node-rect',
    name: 'save attributes',
    shape: 'rect_node',
    backgroundColor: '#e57373',
    nodeType: 'SAVE_ATTRIBUTES'
  }
]);

onMounted(() => {
  graphConfig = new RuleChainGraph(dndRef, contentRef);
});

const printNodes = () => {
  console.log(JSON.stringify(graphConfig.getGraph().toJSON()));
};

const clickSaveRuleChain = () => {
  graphConfig
    .getCells().forEach(cell => {
      if (cell.shape === 'edge') {
        const edge = RuleChainDefine.newEdgeFromCell(cell);
        ruleChain.value.edges.push(edge);
      } else if (cell.shape == 'rect_node') {
        const node = RuleChainDefine.newNodeFromCell(cell);
        ruleChain.value.nodes.push(node);
      }
    });
  saveRuleChain(ruleChain.value).then(resp => {
    if (resp.success) {
      ElMessage({
        message: 'Saved success.',
        type: 'success'
      });
    }
  });
};
// 加载rule chain数据
const params = new URLSearchParams(window.location.search);
if (params && params.get('id')) {
  queryRuleChain(params.get('id')).then(resp => {
    console.log(resp.data);
    ruleChain.value.name = resp.data.name;
    resp.data.nodes.forEach(n => {
      const node = new Node({
        position: n.metadata.position,
        size: {
          width: 160,
          height: 40
        },
        attrs: n.metadata.attrs,
        visible: true,
        shape: n.shape,
        ports: {
          groups: {
            "top": {
              "position": "top",
              "attrs": {
                "circle": {
                  "magnet": true,
                  "stroke": "#8f8f8f",
                  "r": 3,
                  "style": {
                    "visibility": "hidden"
                  }
                }
              }
            },
            "bottom": {
              "position": "bottom",
              "attrs": {
                "circle": {
                  "magnet": true,
                  "stroke": "#8f8f8f",
                  "r": 3,
                  "style": {
                    "visibility": "hidden"
                  }
                }
              }
            },
            "left": {
              "position": "left",
              "attrs": {
                "circle": {
                  "magnet": true,
                  "stroke": "#8f8f8f",
                  "r": 3,
                  "style": {
                    "visibility": "hidden"
                  }
                }
              }
            },
            "right": {
              "position": "right",
              "attrs": {
                "circle": {
                  "magnet": true,
                  "stroke": "#8f8f8f",
                  "r": 3,
                  "style": {
                    "visibility": "hidden"
                  }
                }
              }
            }
          },
          items: [
            {id: "port_1", group: "top"},
            {id: "port_2", group: "bottom"},
            {id: "port_3", group: "left"},
            {id: "port_4", group: "right"},
          ]
        },
        id: n.id,
        data: n.metadata.data,
        tools: {
          items: [
            {
              name: "node-editor",
              args: {
                "x": -630,
                "y": 13,
                "attrs": {
                  "backgroundColor": "#EFF4FF"
                }
              }
            }
          ]
        },
        zIndex: 1
      });
      graphConfig.getGraph().addNode(node)
    })
    resp.data.edges.forEach(e => {
      const edge = new Edge({
        shape: "edge",
        source: {
          cell: e.sourceId,
          port: e.sourcePort
        },
        target: {
          cell: e.targetId,
          port: e.targetPort
        }
      });
      graphConfig.getGraph().addEdge(edge)
    })
    console.log(graphConfig.getGraph().toJSON())
  });
}

const back = () => {
  router.back();
};
</script>

<template>
  <div class="flow-container">
    <div class="flow-header">
      <el-button @click="printNodes">Print</el-button>
      <el-button @click="clickSaveRuleChain">Save</el-button>
      <el-button @click="back">Back</el-button>
    </div>
    <div class="flow-main">
      <div ref="dndRef" class="flow-dnd">
        <template v-for="node in nodeTemplates" :key="node.id">
          <div :class="node.className" :data-type="node.nodeType" :style="{ backgroundColor: node.backgroundColor }" @mousedown="graphConfig.startDrag(node, $event)">
            {{ node.name }}
          </div>
        </template>
      </div>
      <div ref="contentRef" class="flow-content"></div>
      <div class="flow-info">
        <el-row class="flow-info-row">
          <span>规则链名称: </span>
          <el-input v-model="ruleChain.name" placeholder="规则链名称" />
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
