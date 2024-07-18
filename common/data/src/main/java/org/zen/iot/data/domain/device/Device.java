package org.zen.iot.data.domain.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.zen.iot.data.base.DTO;
import org.zen.iot.data.domain.device.transport.DeviceTransportConfig;
import org.zen.iot.data.enums.DeviceStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;


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
    private TransportTypeEnum transportType;

    @Setter
    private DeviceTransportConfig transportConfig;

    @Setter
    private DeviceStatusEnum status;

}
