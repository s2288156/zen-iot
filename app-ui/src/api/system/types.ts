export type AccountId = {
  id: number
}

export type Account = {
  accountId?: AccountId
  username: string
  password?: string
  createTime?: string
  updateTime?: string
}
