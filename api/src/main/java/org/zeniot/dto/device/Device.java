package org.zeniot.dto.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.zeniot.data.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
@Builder
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
public class Device extends DTO {

    @Setter
    private Long id;

    private String name;

    private DeviceTransportTypeEnum transportType;

    private JsonNode transportConfig;

    @Setter
    private DeviceStatusEnum status;

}
