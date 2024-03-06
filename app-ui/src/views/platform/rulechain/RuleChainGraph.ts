import { Cell, Graph } from '@antv/x6';
import { Ref } from 'vue';
import { Snapline } from '@antv/x6-plugin-snapline';
import { Keyboard } from '@antv/x6-plugin-keyboard';
import { Selection } from '@antv/x6-plugin-selection';
import { Clipboard } from '@antv/x6-plugin-clipboard';
import { Dnd } from '@antv/x6-plugin-dnd';
import { NodeData } from '@/views/platform/rulechain/commons';
import { IotGraph } from '@/components/graph/IotGraph';

export class RuleChainGraph implements IotGraph{
  private dndRef: Ref;
  private contentRef: Ref;
  private graph: Graph;
  private dnd: Dnd;

  getCells(): Cell[] {
    return this.graph.getCells();
  }

  constructor(dndRef: Ref, contentRef: Ref) {
    this.dndRef = dndRef
    this.contentRef = contentRef
    this.init()
  }

  public startDrag(node: NodeData, event: MouseEvent) {
    const graphNode = this.getGraph().createNode({
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
          {id: 'port_1', group: 'top'},
          {id: 'port_2', group: 'bottom'},
          {id: 'port_3', group: 'left'},
          {id: 'port_4', group: 'right'}
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
    this.getDnd().start(graphNode, event);
  }

  private init() {
    this.registerNode();
    this.newGraph();
    this.loadPlugin();
    this.getGraph().centerContent();
    this.loadDnd();
    this.loadEvent();
  }

  public getGraph(): Graph {
    return this.graph;
  }

  public getDnd(): Dnd {
    return this.dnd;
  }

  private registerNode() {
    Graph.registerNode(
      'rect_node',
      {
        inherit: 'rect',
        width: 160,
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
                }
              }
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
                }
              }
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
                }
              }
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
                }
              }
            }
          }
        }
      },
      true
    );
  }

  private newGraph(): Graph {
    this.graph =  new Graph({
      container: this.contentRef.value,
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
    return this.graph;
  }

  private loadPlugin(): void {
    this.graph.use(
      new Snapline({
        enabled: true,
        sharp: true
      })
    );
    this.graph.use(
      new Keyboard({
        enabled: true,
        global: true
      })
    );
    this.graph.use(
      new Selection({
        enabled: true,
        multiple: true,
        rubberband: true,
        movable: true,
        showNodeSelectionBox: true,
        showEdgeSelectionBox: true
      })
    );
    this.graph.use(
      new Clipboard({
        enabled: true
      })
    );
  }

  private loadDnd(): void {
    this.dnd = new Dnd({
      target: this.graph,
      dndContainer: this.dndRef.value
    });
  }

  private loadEvent(): void {
    this.graph.bindKey('ctrl+c', () => {
      const cells = this.graph.getSelectedCells();
      if (cells.length) {
        this.graph.copy(cells);
      }
      return false;
    });
    this.graph.bindKey('ctrl+v', () => {
      if (!this.graph.isClipboardEmpty()) {
        const cells = this.graph.paste({ offset: 32 });
        this.graph.cleanSelection();
        this.graph.select(cells);
      }
      return false;
    });
    this.graph.bindKey('delete', () => {
      const cells = this.graph.getSelectedCells();
      if (cells.length) {
        this.graph.cut(cells);
        this.graph.cleanClipboard();
      }
      return false;
    });
    this.graph.on('node:mouseenter', () => {
      const ports = this.contentRef.value.querySelectorAll('.x6-port-body') as NodeListOf<SVGElement>;
      const cells = this.contentRef.value.querySelectorAll('.x6-node') as NodeListOf<SVGElement>;
      this.showPorts(ports, true);
      this.changeCursor(cells, 'pointer')
    });
    this.graph.on('node:mouseleave', () => {
      const ports = this.contentRef.value.querySelectorAll('.x6-port-body') as NodeListOf<SVGElement>;
      const cells = this.contentRef.value.querySelectorAll('.x6-node') as NodeListOf<SVGElement>;
      this.showPorts(ports, false);
      this.changeCursor(cells, 'default')
    });
    this.graph.on('cell:mousedown', () => {
      const cells = this.contentRef.value.querySelectorAll('.x6-cell') as NodeListOf<SVGElement>;
      this.changeCursor(cells, 'move')
    })
    this.graph.on('graph:mouseenter', () => {
      this.contentRef.value.style.cursor = 'default'
    })
  }

  // 控制连接桩显示/隐藏
  private showPorts(ports: NodeListOf<SVGElement>, show: boolean) {
    for (let i = 0, len = ports.length; i < len; i += 1) {
      ports[i].style.visibility = show ? 'visible' : 'hidden';
    }
  }
  private changeCursor(cells: NodeListOf<SVGElement>, cursorType: string) {
    for (let i = 0, len = cells.length; i < len; i += 1) {
      cells[i].style.cursor = cursorType;
    }
  }
  // #endregion
}
