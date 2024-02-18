import request from '@/utils/request';
import { RuleChain } from '@/api/data/types';
import { PageQuery } from '@/api/global-types';

export function saveRuleChain(data: RuleChain) {
  return request.post({
    url: '/api/rule_chain',
    data: data
  });
}

export function queryRuleChains(data: PageQuery) {
  return request.get({
    url: '/api/rule_chains',
    params: data
  });
}

export function queryRuleChain(id: string) {
  return request.get({ url: '/api/rule_chain/' + id });
}

export function deleteRuleChain(id: string) {
  return request.delete({ url: '/api/rule_chain/' + id });

}
