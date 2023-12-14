package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.dao.model.NodeEntity;
import org.zeniot.dao.model.NodeRelationEntity;
import org.zeniot.dao.model.RuleChainEntity;
import org.zeniot.data.enums.NodeTypeEnum;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/db_zen_iot_1",
})
class RuleChainRepositoryTest {
    @Autowired
    private RuleChainRepository ruleChainRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private NodeRelationRepository nodeRelationRepository;

    private Long ruleChainId;

    @BeforeEach
    void setUp() {
        RuleChainEntity ruleChainEntity = new RuleChainEntity();
        ruleChainEntity.setName("junit test");
        ruleChainRepository.save(ruleChainEntity);
        ruleChainId = ruleChainEntity.getId();

        NodeEntity node1 = new NodeEntity();
        node1.setId("1");
        node1.setNodeName("n1");
        node1.setRuleChainId(ruleChainEntity.getId());
        node1.setNodeType(NodeTypeEnum.SAVE_ATTRIBUTES);
        node1.setMetadata(JacksonUtil.newObjectNode());
        NodeEntity node2 = new NodeEntity();
        node2.setId("2");
        node2.setNodeName("n2");
        node2.setRuleChainId(ruleChainEntity.getId());
        node2.setNodeType(NodeTypeEnum.SAVE_ATTRIBUTES);
        node2.setMetadata(JacksonUtil.newObjectNode());
        nodeRepository.save(node1);
        nodeRepository.save(node2);

        NodeRelationEntity relation1 = new NodeRelationEntity();
        relation1.setId("12");
        relation1.setSourceId("1");
        relation1.setTargetId("2");
        relation1.setRuleChainId(ruleChainEntity.getId());
        nodeRelationRepository.save(relation1);
    }

    @AfterEach
    void tearDown() {
        ruleChainRepository.deleteAll();
        nodeRepository.deleteAll();
        nodeRelationRepository.deleteAll();
    }

    @Test
    void test_query_rule_chains() {
        RuleChainEntity ruleChainEntity = ruleChainRepository.findById(ruleChainId
        ).orElseThrow();
        assertNotNull(ruleChainEntity);
    }
}
