package org.zeniot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.zeniot.dao.model.SimulatorEntity;
import org.zeniot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SimulatorMapper {

    @Mapping(target = "transportConfig", ignore = true)
    Simulator entityToSimulator(SimulatorEntity entity);

    @Mapping(target = "transportConfig", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SimulatorEntity toEntity(Simulator simulator);

}
