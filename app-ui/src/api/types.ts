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
}
