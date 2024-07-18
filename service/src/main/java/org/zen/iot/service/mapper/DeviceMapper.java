package org.zen.iot.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.*;
import org.zen.iot.common.util.JacksonUtil;
import org.zen.iot.dao.model.DeviceEntity;
import org.zen.iot.data.domain.device.Device;
import org.zen.iot.data.domain.device.transport.DeviceTransportConfig;

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

    @Mapping(target = "deviceCredentialEntity", ignore = true)
    @Mapping(target = "transportConfig", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    DeviceEntity deviceToEntity(Device device);
}
