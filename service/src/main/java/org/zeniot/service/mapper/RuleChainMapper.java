package org.zeniot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;
import org.zeniot.dao.model.NodeEntity;
import org.zeniot.dao.model.RuleChainEntity;
import org.zeniot.data.domain.rulechain.Node;
import org.zeniot.data.domain.rulechain.RuleChain;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RuleChainMapper {

    RuleChainMapper INSTANCE = Mappers.getMapper(RuleChainMapper.class);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    RuleChainEntity toEntity(RuleChain ruleChain);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    NodeEntity toEntity(Node node, Long ruleChainId);
}
