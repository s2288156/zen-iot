import { RectNode, RectNodeModel } from '@logicflow/core'

class RedNodeModel extends RectNodeModel {
  initNodeData(data: any) {
    super.initNodeData(data);
  }
}

class RedNode extends RectNode {

}


export { RedNodeModel, RedNode };
