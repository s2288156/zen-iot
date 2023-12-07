package org.zeniot.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.api.RuleChainService;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
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
        return SingleResponse.success(ruleChainService.createOrUpdateRuleChain(ruleChain));
    }

    @GetMapping("/rule_chains")
    public PageResponse<RuleChain> listRuleChains(@Validated PageQuery pageQuery) {
        return ruleChainService.findRuleChains(pageQuery);
    }

    @GetMapping("/rule_chain/{id}")
    public SingleResponse<RuleChain> getRuleChain(@PathVariable Long id) {
        return SingleResponse.success(ruleChainService.getRuleChain(id));
    }

}
