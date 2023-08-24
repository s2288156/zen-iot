<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Graph } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';
import { NodeData } from './commons';
import { Keyboard } from '@antv/x6-plugin-keyboard';
import { Selection } from '@antv/x6-plugin-selection';
import { Clipboard } from '@antv/x6-plugin-clipboard';
import { Scroller } from '@antv/x6-plugin-scroller'

const dndRef = ref();
const contentRef = ref();
let graph: Graphd;
let dnd: Dnd;
const nodes = reactive<Array<NodeData>>([
  { className: 'dnd-rect', name: 'Rect', type: 'rect' },
  { className: 'dnd-circle', name: 'Circle', type: 'circle' }
]);
onMounted(() => {
  graph = new Graph({
    container: contentRef.value,
    grid: true,
    background: {
      color: '#F2F7FA'
    }
  });
  graph.use(
    new Keyboard({
      enabled: true,
      global: true
    })
  );
  graph.use(
    new Selection({
      enabled: true,
      showNodeSelectionBox: true,
      showEdgeSelectionBox: true
    })
  );
  graph.use(
    new Clipboard({
      enabled: true,
    })
  );
  graph.use(
    new Scroller({
      enabled: true,
      pannable: true
    }),
  )
  graph.centerContent();
  dnd = new Dnd({
    target: graph,
    dndContainer: dndRef.value
  });
  graph.bindKey('ctrl+c', () => {
    const cells = graph.getSelectedCells();
    if (cells.length) {
      graph.copy(cells);
    }
    return false;
  });
  graph.bindKey('ctrl+v', () => {
    if (!graph.isClipboardEmpty()) {
      const cells = graph.paste({ offset: 32 });
      graph.cleanSelection();
      graph.select(cells);
    }
    return false;
  });
  graph.bindKey('delete', () => {
    const cells = graph.getSelectedCells();
    if (cells.length) {
      graph.cut(cells);
      graph.cleanClipboard();
    }
    return false;
  });
});
const startDrag = (node: NodeData, event) => {
  const type = node.type;
  let graphNode: any;
  switch (type) {
    case 'rect':
      graphNode = graph.createNode({
        width: 100,
        height: 40,
        label: 'Rect',
        attrs: {
          body: {
            stroke: '#8f8f8f',
            strokeWidth: 1,
            fill: '#fff',
            rx: 6,
            ry: 6
          }
        }
      });
      break;
    case 'circle':
      graphNode = graph.createNode({
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
  dnd.start(graphNode, event);
};
</script>

<template>
  <div class="flow-container">
    <div class="flow-dnd" ref="dndRef">
      <template v-for="node in nodes">
        <div :class="node.className" @mousedown="startDrag(node, $event)">{{ node.name }}</div>
      </template>
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
