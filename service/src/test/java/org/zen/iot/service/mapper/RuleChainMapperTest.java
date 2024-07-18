package org.zen.iot.service.mapper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.zen.iot.dao.model.NodeEntity;
import org.zen.iot.data.domain.rulechain.Node;
import org.zen.iot.data.enums.NodeTypeEnum;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class RuleChainMapperTest {

    RuleChainMapper ruleChainMapper = RuleChainMapper.INSTANCE;

    @Test
    void test_node_toEntity() {
        Long ruleChainId = 1L;
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Node node = new Node();
            node.setId("n_" + i);
            node.setNodeName("n_" + i + "_name");
            node.setNodeType(i == 1 ? NodeTypeEnum.SAVE_ATTRIBUTES : NodeTypeEnum.SAVE_TIMESERIES);
            nodes.add(node);
        }
        List<NodeEntity> entities = nodes.stream().map(node -> ruleChainMapper.toEntity(node, ruleChainId))
                .toList();
        assertEquals(nodes.size(), entities.size());
        entities.forEach(nodeEntity -> assertAll(
                () -> assertEquals(ruleChainId, nodeEntity.getRuleChainId()),
                () -> assertNotNull(nodeEntity.getNodeType()),
                () -> assertNotNull(nodeEntity.getNodeName()),
                () -> assertNotNull(nodeEntity.getId())
        ));
    }

    @Test
    void test_ruleChain_toEntity() {
    }
}
