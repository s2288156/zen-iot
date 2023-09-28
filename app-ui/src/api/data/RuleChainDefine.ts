  export class EdgeDefine {
    private id: string;
    private sourceId: string;
    private sourcePort: string;
    private targetId: string;
    private targetPort: string;

    public static newFromCell(cell: any) {
      let edgeDefine = new EdgeDefine();
      edgeDefine.id = cell.id;
      edgeDefine.sourceId = cell.source.cell;
      edgeDefine.sourcePort = cell.source.port;
      edgeDefine.targetId = cell.target.cell;
      edgeDefine.targetPort = cell.target.port;
      return edgeDefine;
    }
  }

  export class NodeDefine {
    private id: string;
    private positionInfo: object;
    private nodeName: string;
    private shape: string;
    private nodeType: string;
    private configData: object;

    public static newFromCell(cell: any) {
      let nodeDefine = new NodeDefine();
      nodeDefine.id = cell.id;
      nodeDefine.positionInfo = cell.position;
      nodeDefine.nodeName = cell.attrs.text.text;
      nodeDefine.shape = cell.shape;
      nodeDefine.nodeType = cell.data.nodeType;
      nodeDefine.configData = cell.data;
      return nodeDefine;
    }
  }
