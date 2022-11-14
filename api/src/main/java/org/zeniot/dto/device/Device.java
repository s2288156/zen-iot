package org.zeniot.dto.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
}
