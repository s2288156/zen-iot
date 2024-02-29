package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeniot.AbstractBootTest;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class RuleChainServiceImplTest extends AbstractBootTest {

    private Long id;

    @BeforeEach
    void setUp() {
        RuleChain createdRuleChain = ruleChainService.createOrUpdateRuleChain(ruleChain1);
        id = createdRuleChain.getId();
    }

    @AfterEach
    void tearDown() {
        ruleChainService.deleteRuleChain(id);
    }

    @Test
    void createOrUpdateRuleChain() {
    }

    @Test
    void findRuleChains() {
    }

    @Test
    void getRuleChain() {
    }

    @Test
    void deleteRuleChain() {
    }
}
