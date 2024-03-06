import { EdgeDefine, NodeDefine } from '@/api/data/types';

export class RuleChainDefine {
  public static newEdgeFromCell(cell: any): EdgeDefine {
    return {
      id: cell.id,
      sourceId: cell.source.cell,
      sourcePort: cell.source.port,
      targetId: cell.target.cell,
      targetPort: cell.target.port
    };
  }

  public static newNodeFromCell(cell: any): NodeDefine {
    return {
      id: cell.id,
      nodeName: cell.attrs.text.text,
      nodeType: cell.data.nodeType,
      metadata: {
        position: cell.position,
        shape: cell.shape,
        data: cell.data,
        attrs: cell.attrs
      }
    };
  }
}
