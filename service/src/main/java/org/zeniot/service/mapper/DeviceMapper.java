package org.zeniot.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.*;
import org.zeniot.common.util.JacksonUtil;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.data.domain.device.Device;
import org.zeniot.data.domain.device.transport.DeviceTransportConfig;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeviceMapper {

    @Mapping(target = "transportConfig", ignore = true)
    Device entityToDevice(DeviceEntity entity);

    @AfterMapping
    default void afterToEntity(@MappingTarget DeviceEntity entity, Device device) {
        entity.setTransportConfig(JacksonUtil.convertValue(device.getTransportConfig(), JsonNode.class));
    }

    @AfterMapping
    default void afterToDto(@MappingTarget Device device, DeviceEntity entity) {
        device.setTransportConfig(JacksonUtil.convertValue(entity.getTransportConfig(), DeviceTransportConfig.class));
    }

    @Mapping(target = "transportConfig", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    DeviceEntity deviceToEntity(Device device);
}
