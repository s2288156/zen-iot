<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Graph } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';
import { NodeData } from './commons';
import { Keyboard } from '@antv/x6-plugin-keyboard';
import { Selection } from '@antv/x6-plugin-selection';
import { Clipboard } from '@antv/x6-plugin-clipboard';
import { Snapline } from '@antv/x6-plugin-snapline'

const dndRef = ref();
const contentRef = ref();
let graph: Graph;
let dnd: Dnd;
const nodes = reactive<Array<NodeData>>([
  { className: 'node-rect', name: 'Rect', type: 'rect' },
  { className: 'node-circle', name: 'Circle', type: 'circle' }
]);
onMounted(() => {
  Graph.registerNode('zen-rect', {
    inherit: 'rect',
    width: 100,
    height: 40,
    attrs: {
      body: {
        stroke: '#8f8f8f',
        strokeWidth: 1,
        fill: '#fff',
        rx: 6,
        ry: 6
      }
    },
    ports: {
      groups: {
        top: {
          position: 'top',
          attrs: {
            circle: {
              magnet: true,
              stroke: '#8f8f8f',
              r: 3,
              style: {
                visibility: 'hidden'
              }
            },
          },
        },
        bottom: {
          position: 'bottom',
          attrs: {
            circle: {
              magnet: true,
              stroke: '#8f8f8f',
              r: 3,
              style: {
                visibility: 'hidden'
              }
            },
          },
        },
        left: {
          position: 'left',
          attrs: {
            circle: {
              magnet: true,
              stroke: '#8f8f8f',
              r: 3,
              style: {
                visibility: 'hidden'
              }
            },
          },
        },
        right: {
          position: 'right',
          attrs: {
            circle: {
              magnet: true,
              stroke: '#8f8f8f',
              r: 3,
              style: {
                visibility: 'hidden'
              }
            },
          },
        },
      },
    }
  },true)

  graph = new Graph({
    container: contentRef.value,
    grid: true,
    panning: {
      enabled: true,
      eventTypes: ['rightMouseDown']
    },
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
      snap: true,
      allowBlank: true,
      allowLoop: true,
      allowNode: true,
      allowEdge: false,
      allowPort: true,
      allowMulti: true
    },
    background: {
      color: '#F2F7FA'
    }
  });
  graph.use(
    new Snapline({
      enabled: true,
      sharp: true,
    }),
  );
  graph.use(
    new Keyboard({
      enabled: true,
      global: true
    })
  );
  graph.use(
    new Selection({
      enabled: true,
      multiple: true,
      rubberband: true,
      movable: true,
      showNodeSelectionBox: true,
      showEdgeSelectionBox: true,
    })
  );
  graph.use(
    new Clipboard({
      enabled: true
    })
  );
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
  graph.on('node:mouseenter', () => {
    const ports = contentRef.value.querySelectorAll(
      '.x6-port-body',
    ) as NodeListOf<SVGElement>
    showPorts(ports, true)
  })
  graph.on('node:mouseleave', () => {
    const ports = contentRef.value.querySelectorAll(
      '.x6-port-body',
    ) as NodeListOf<SVGElement>
    showPorts(ports, false)
  })
});
const startDrag = (node: NodeData, event) => {
  const type = node.type;
  let graphNode: any;
  switch (type) {
    case 'rect':
      graphNode = graph.createNode({
        shape: 'zen-rect',
        label: 'Rect',
        ports: {
          items: [
            {id: 'port_1', group: 'top'},
            {id: 'port_2', group: 'bottom'},
            {id: 'port_3', group: 'left'},
            {id: 'port_4', group: 'right'},
          ],
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
const printNodes = () => {
  console.log(JSON.stringify(graph.toJSON()))
}
// 控制连接桩显示/隐藏
const showPorts = (ports: NodeListOf<SVGElement>, show: boolean) => {
  for (let i = 0, len = ports.length; i < len; i += 1) {
    ports[i].style.visibility = show ? 'visible' : 'hidden'
  }
}
// #endregion
</script>

<template>
  <div class="flow-container">
    <div class="flow-header">
      <el-button @click='printNodes'>Test</el-button>
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
  width: 100px;
  height: 40px;
  margin: 16px;
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
