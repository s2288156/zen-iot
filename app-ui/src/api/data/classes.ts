export class EdgeDefine {

  public id:string;
  public sourceId:string;
  public sourcePort:string;
  public targetId:string;
  public targetPort:string;

  constructor() {
  }

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
