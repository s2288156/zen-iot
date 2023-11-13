package org.zeniot.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zeniot.api.RuleChainService;
import org.zeniot.data.base.SingleResponse;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@RequestMapping("/api")
@RestController
public class RuleChainController {

    @Autowired
    private RuleChainService ruleChainService;

    @PostMapping("/rule_chain")
    public SingleResponse<RuleChain> saveRuleChain(@RequestBody RuleChain ruleChain) {
        log.warn("{}", ruleChain);
        return SingleResponse.success(ruleChainService.saveOrUpdateRuleChain(ruleChain));
    }

}
