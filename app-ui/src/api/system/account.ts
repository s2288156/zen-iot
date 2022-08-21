import request from '@/utils/request'
import { PageQuery } from '@/utils/datas'
import { Account } from '@/api/system/types'

export function getAccounts (data: PageQuery) {
  return request({
    url: '/api/accounts',
    method: 'get',
    params: data
  })
}

export function registerAccount (data: Account) {
  return request({
    url: '/api/account/register',
    method: 'post',
    data: data
  })
}
