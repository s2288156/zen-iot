import request from '@/utils/request';
import type { Account } from '@/api/data/types';
import type { PageQuery } from '@/api/global-types';

export function getAccounts(data: PageQuery) {
  return request.get({
    url: '/api/accounts',
    params: data
  });
}

export function registerAccount(data: Account) {
  return request.post({
    url: '/api/account/register',
    data: data
  });
}

export function deleteAccount(id: number) {
  return request.delete({
    url: '/api/account/' + id
  });
}

export function login(data: Account) {
  return request.post({
    url: '/api/login',
    data: data
  });
}
