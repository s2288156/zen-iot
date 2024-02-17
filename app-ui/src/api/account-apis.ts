import request from '@/utils/request';
import type { Account } from '@/api/data/types';
import type { PageQuery, PageResponse } from '@/api/global-types';

export function getAccounts(data: PageQuery) {
  return request<PageResponse<Account>>({
    url: '/api/accounts',
    method: 'get',
    params: data
  });
}

export function registerAccount(data: Account) {
  return request({
    url: '/api/account/register',
    method: 'post',
    data: data,
  })
}

export function deleteAccount(id: number) {
  return request({
    url: '/api/account/' + id,
    method: 'delete'
  })
}

export function login(data: Account) {
  return request({
    url: '/api/login',
    method: 'post',
    data: data
  });
}
