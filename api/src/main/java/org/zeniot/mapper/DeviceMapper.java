package org.zeniot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.dto.device.Device;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = SPRING)
public interface DeviceMapper {

    Device entityToDevice(DeviceEntity entity);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    DeviceEntity deviceToEntity(Device device);
}
