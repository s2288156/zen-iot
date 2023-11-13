package org.zeniot.api;

import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
public interface RuleChainService {
    RuleChain createOrUpdateRuleChain(RuleChain ruleChain);
}
