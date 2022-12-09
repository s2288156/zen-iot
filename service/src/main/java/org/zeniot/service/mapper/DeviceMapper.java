package org.zeniot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.data.domain.device.Device;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeviceMapper {

    Device entityToDevice(DeviceEntity entity);

    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    DeviceEntity deviceToEntity(Device device);
}
