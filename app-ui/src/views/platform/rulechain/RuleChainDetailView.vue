<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NodeData } from './commons';
import { RuleChainGraph } from '@/views/platform/rulechain/RuleChainGraph';
import { queryRuleChain, saveRuleChain } from '@/api/rule-chain-apis';
import { RuleChain } from '@/api/data/types';
import { RuleChainDefine } from '@/api/data/RuleChainDefine';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { IotGraph } from '@/components/graph/IotGraph';

const dndRef = ref();
const contentRef = ref();
const ruleChain = ref<RuleChain>({ name: '', edges: [], nodes: [], graphJson: {} });
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
  graphConfig.getCells().forEach(cell => {
    if (cell.shape === 'edge') {
      const edge = RuleChainDefine.newEdgeFromCell(cell);
      ruleChain.value.edges.push(edge);
    } else if (cell.shape == 'rect_node') {
      const node = RuleChainDefine.newNodeFromCell(cell);
      ruleChain.value.nodes.push(node);
    }
  });
  ruleChain.value.graphJson = graphConfig.getGraph().toJSON();
  saveRuleChain(ruleChain.value).then(resp => {
    if (resp.success) {
      ruleChain.value.id = resp.data.id;
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
    if (resp.data.id) {
      ruleChain.value.id = resp.data.id;
    }
    ruleChain.value.name = resp.data.name;
    ruleChain.value.createTime = resp.data.createTime;
    graphConfig.getGraph().fromJSON(resp.data.graphJson);
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
