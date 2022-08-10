import { BasePage } from '@/api/global-types'

export type AccountId = {
  id: number
}

export type AccountData = {
  accountId: AccountId,
  username: string,
  createTime: string,
  updateTime: string
}

export type AccountPage = BasePage & {
  data: AccountData[]
}
