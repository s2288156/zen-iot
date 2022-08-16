export type AccountId = {
  id: number
}

export type AccountData = {
  accountId?: AccountId,
  username: string,
  createTime?: string,
  updateTime?: string
}
