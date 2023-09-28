import request from '@/utils/request';
import { RuleChain } from '@/api/data/RuleChainDefine';

export function saveRuleChain(data: RuleChain) {
  console.log(data);
  return request({
    url: '/api/rule_chain',
    method: 'post',
    data: data
  });
}

