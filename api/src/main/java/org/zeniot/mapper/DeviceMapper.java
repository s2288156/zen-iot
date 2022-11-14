package org.zeniot.mapper;

import org.mapstruct.Mapper;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.dto.device.Device;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = SPRING)
public interface DeviceMapper {

    Device entityToDevice(DeviceEntity entity);

    DeviceEntity deviceToEntity(Device device);
}
