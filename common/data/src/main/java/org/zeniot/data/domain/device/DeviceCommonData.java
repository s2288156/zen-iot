package org.zeniot.data.domain.device;

import lombok.Data;
import org.zeniot.data.base.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Data
public class DeviceCommonData extends DTO {
    private List<DeviceTransportTypeEnum> transportTypes;
    private List<DeviceStatusEnum> statuses;

    private DeviceCommonData() {
    }

    public static DeviceCommonData create() {
        DeviceCommonData deviceCommonData = new DeviceCommonData();
        deviceCommonData.setTransportTypes(List.of(DeviceTransportTypeEnum.values()));
        deviceCommonData.setStatuses(List.of(DeviceStatusEnum.values()));
        return deviceCommonData;
    }
}
