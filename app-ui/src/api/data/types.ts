export type Account = {
  id?: any;
  username: string;
  password?: string;
  createTime?: string;
  updateTime?: string;
};

export type Device = {
  id?: any;
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
  id?: number;
  nodes: NodeDefine[];
  edges: EdgeDefine[];
};

export type NodeDefine = {
  id: string;
  nodeName: string;
  nodeType: string;
  metadata: object;
};

export type EdgeDefine = {
   id: string;
   sourceId: string;
   sourcePort: string;
   targetId: string;
   targetPort: string;
}
