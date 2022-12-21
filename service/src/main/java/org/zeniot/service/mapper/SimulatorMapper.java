package org.zeniot.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.*;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.dao.model.SimulatorEntity;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.domain.transport.TransportConfig;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SimulatorMapper {

    @Mapping(target = "transportConfig", ignore = true)
    Simulator entityToSimulator(SimulatorEntity entity);

    @AfterMapping
    default void finishSimulatorEntity(@MappingTarget SimulatorEntity entity, Simulator simulator) {
        entity.setTransportConfig(JacksonUtil.convertValue(simulator.getTransportConfig(), JsonNode.class));
    }

    @AfterMapping
    default void finishSimulator(@MappingTarget Simulator simulator, SimulatorEntity entity) {
        simulator.setTransportConfig(JacksonUtil.convertValue(entity.getTransportConfig(), TransportConfig.class));
    }

    @Mapping(target = "transportConfig", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SimulatorEntity toEntity(Simulator simulator);

}
