import { Cell, Graph } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';
import { NodeData } from '@/views/platform/rulechain/commons';

export interface IotGraph {
  getGraph(): Graph;

  getDnd(): Dnd;

  getCells(): Cell[];

  startDrag(node: NodeData, event: MouseEvent): void;
}
