package org.zen.iot.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.*;
import org.zen.iot.common.util.JacksonUtil;
import org.zen.iot.dao.model.SimulatorEntity;
import org.zen.iot.data.domain.simulator.Simulator;
import org.zen.iot.data.domain.simulator.transport.SimulatorTransportConfig;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SimulatorMapper {

    @Mapping(target = "transportConfig", ignore = true)
    Simulator entityToSimulator(SimulatorEntity entity);

    @AfterMapping
    default void afterToEntity(@MappingTarget SimulatorEntity entity, Simulator simulator) {
        entity.setTransportConfig(JacksonUtil.convertValue(simulator.getTransportConfig(), JsonNode.class));
    }

    @AfterMapping
    default void afterToDto(@MappingTarget Simulator simulator, SimulatorEntity entity) {
        simulator.setTransportConfig(JacksonUtil.convertValue(entity.getTransportConfig(), SimulatorTransportConfig.class));
    }

    @Mapping(target = "transportConfig", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SimulatorEntity toEntity(Simulator simulator);

}
