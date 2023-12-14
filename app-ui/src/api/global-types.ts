export type PageResponse<T> = {
  success?: boolean
  msg?: string
  data: T[]
  size: number
  totalPages: number
}

export type PageQuery = {
  page: number
  size: number
}

export type RestResponse<T> = {
  success: boolean
  msg: string
  code: number
  data: T
}
