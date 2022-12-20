import type {TransportType, TimeUnit} from "@/api/data/enums";

export type Account = {
  id?: any
  username: string
  password?: string
  createTime?: string
  updateTime?: string
}

export type Device = {
  id?: any
  name: string
  transportType: string
  transportConfig?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export type DeviceCommon = {
  transportTypes: string[]
  statuses: string[]
  timeUnit: string[]
}

export type Simulator = {
  id?: any
  name: string
  transportType: string
  transportConfig: TransportConfig
  status?: string
}

export type TransportConfig = {
  type: string
  saveTimeseriesTopic?: string
  period?: number
  timeUnit?: string
}
