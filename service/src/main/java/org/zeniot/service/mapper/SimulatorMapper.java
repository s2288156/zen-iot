package org.zeniot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.zeniot.dao.model.SimulatorEntity;
import org.zeniot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SimulatorMapper {

    Simulator entityToSimulator(SimulatorEntity entity);

    SimulatorEntity toEntity(Simulator simulator);

}
