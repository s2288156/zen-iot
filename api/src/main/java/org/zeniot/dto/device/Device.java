package org.zeniot.dto.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.zeniot.data.base.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotNull(message = "Transport Type不能为空")
    private DeviceTransportTypeEnum transportType;

    private JsonNode transportConfig;

    @Setter
    private DeviceStatusEnum status;

}
