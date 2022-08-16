import request from '@/utils/request'
import { PageQuery } from '@/utils/datas'

export function getAccounts (data: PageQuery) {
  return request({
    url: '/api/accounts',
    method: 'get',
    params: data
  })
}
