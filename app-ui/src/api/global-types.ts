export type PageQuery = {
  page: number
  size: number
}

export type RestResponse<T> = {
  success?: boolean
  msg?: string
  code?: number
  data: T | T[]
  size: number
  totalPages: number
}
