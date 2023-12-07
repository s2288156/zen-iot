import request from '@/utils/request';
import {RuleChain} from '@/api/data/types';
import {BaseDataPage, PageQuery} from "@/api/global-types";

export function saveRuleChain(data: RuleChain) {
  console.log(data);
  return request({
    url: '/api/rule_chain',
    method: 'post',
    data: data
  });
}

export function queryRuleChains(data: PageQuery){
  return request({
    url: '/api/rule_chains',
    method: 'get',
    params: data,
  })
}

export function queryRuleChain(id: string) {
  return request({
    url: '/api/device/' + id,
    method: 'get'
  })
}
