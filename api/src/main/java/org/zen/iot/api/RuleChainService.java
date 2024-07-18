package org.zen.iot.api;

import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
public interface RuleChainService {
    RuleChain createOrUpdateRuleChain(RuleChain ruleChain);

    PageResponse<RuleChain> findRuleChains(PageQuery pageQuery);

    RuleChain getRuleChain(Long id);

    void deleteRuleChain(Long id);
}
