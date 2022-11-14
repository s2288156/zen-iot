package org.zeniot.dto.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.data.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
public class Device extends DTO {

    private String name;

    private DeviceTransportTypeEnum transportType;

    private JsonNode transportConfig;

    private DeviceStatusEnum status;

    public static Device fromEntity(DeviceEntity entity) {
        Device device = new Device();
        device.setName(entity.getName());
        device.setStatus(entity.getStatus());
        device.setTransportType(entity.getTransportType());
        return device;
    }
}
