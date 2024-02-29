package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeniot.AbstractBootTest;
import org.zeniot.data.domain.rulechain.RuleChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class RuleChainServiceImplTest extends AbstractBootTest {

    RuleChain createdRuleChain;

    @BeforeEach
    void setUp() {
        createdRuleChain = ruleChainService.createOrUpdateRuleChain(ruleChain1);
    }

    @AfterEach
    void tearDown() {
        ruleChainService.deleteRuleChain(createdRuleChain.getId());
    }

    @Test
    void createOrUpdateRuleChain() {
        assertEquals(ruleChain1.getName(), createdRuleChain.getName(), "created rule chain data exception.");
    }

    @Test
    void findRuleChains() {
    }

    @Test
    void getRuleChain() {
        RuleChain ruleChain = ruleChainService.getRuleChain(createdRuleChain.getId());
        assertEquals(2, ruleChain.getNodes().size());
        assertEquals(1, ruleChain.getEdges().size());
        assertNotNull(ruleChain.getEdges().getFirst().getId());
        assertNotNull(ruleChain.getNodes().getFirst().getId());
    }

}
