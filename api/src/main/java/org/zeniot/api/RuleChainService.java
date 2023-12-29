package org.zeniot.api;

import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
public interface RuleChainService {
    RuleChain createOrUpdateRuleChain(RuleChain ruleChain);

    PageResponse<RuleChain> findRuleChains(PageQuery pageQuery);

    RuleChain getRuleChain(Long id);

    void deleteRuleChain(Long id);
}
