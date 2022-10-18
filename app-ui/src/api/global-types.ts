export type BaseDataPage<T> = {
  data: T[]
  size: number
  totalPages: number
}

export type PageQuery = {
  page: number
  size: number
}

export type RestResponse = {
  success: boolean
  msg: string
}
