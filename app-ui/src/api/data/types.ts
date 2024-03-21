export type Account = {
  id?: any;
  username: string;
  password?: string;
  createTime?: string;
  updateTime?: string;
};

export type Device = {
  id?: string;
  name: string;
  transportType: string;
  transportConfig?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
};

export type DeviceCommon = {
  transportTypes?: string[];
  statuses?: string[];
  timeUnit?: string[];
  fieldTypes?: string[];
};

export type Simulator = {
  id?: any;
  name?: string;
  transportType?: string;
  transportConfig?: TransportConfig;
  status?: string;
  createTime?: string;
  updateTime?: string;
};

export type TransportConfig = {
  type?: string;
  saveTimeseriesTopic?: string;
  period?: number;
  timeUnit?: string;
  timeseriesFields: SimulatorJsonFieldDefine[];
};

export type SimulatorJsonFieldDefine = {
  name: string;
  fieldType: string;
  valueOrigin: string;
  valueBound: string;
};

export type RuleChain = {
  id?: string;
  name?: string;
  nodes?: NodeDefine[];
  edges?: EdgeDefine[];
  createTime?: string;
  updateTime?: string;
  graphJson: object
};

export type NodeDefine = {
  id: string;
  nodeName: string;
  nodeType: string;
  metadata: CellMetadata;
  createTime?: string;
  updateTime?: string;
};

export type EdgeDefine = {
  id: string;
  sourceId: string;
  sourcePort: string;
  targetId: string;
  targetPort: string;
  createTime?: string;
  updateTime?: string;
};

export type CellMetadata = {
  position?: Position
  shape?: string
  data?: CommonData
  attrs?: object
}

export type Position = {
  x: number
  y: number
}

export type CommonData = {
  nodeType?: string
}
