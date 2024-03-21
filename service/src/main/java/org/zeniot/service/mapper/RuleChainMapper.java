package org.zeniot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.zeniot.dao.model.NodeEntity;
import org.zeniot.dao.model.NodeRelationEntity;
import org.zeniot.dao.model.RuleChainEntity;
import org.zeniot.data.domain.rulechain.Edge;
import org.zeniot.data.domain.rulechain.Node;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@Mapper()
public interface RuleChainMapper {

    RuleChainMapper INSTANCE = Mappers.getMapper(RuleChainMapper.class);

    @Mapping(target = "nodeRelationEntities", ignore = true)
    @Mapping(target = "nodeEntities", ignore = true)
    RuleChainEntity toEntity(RuleChain ruleChain);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "ruleChainEntity", ignore = true)
    @Mapping(source = "ruleChainId", target = "ruleChainId")
    NodeEntity toEntity(Node node, Long ruleChainId);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "ruleChainEntity", ignore = true)
    @Mapping(source = "ruleChainId", target = "ruleChainId")
    NodeRelationEntity toEntity(Edge edge, Long ruleChainId);

    @Mapping(target = "nodes", source = "nodeEntities")
    @Mapping(target = "edges", source = "nodeRelationEntities")
    RuleChain entityToRuleChain(RuleChainEntity entity);

    default Node entityToNode(NodeEntity entity) {
        return new Node(entity.getId(), entity.getNodeName(), entity.getNodeType(), entity.getMetadata(), entity.getRuleChainId());
    }

    default Edge entityToEdge(NodeRelationEntity entity) {
        return new Edge(entity.getId(), entity.getSourceId(), "sourcePort", entity.getTargetId(), "targetPort", entity.getRuleChainId());
    }
}
