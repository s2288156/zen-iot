package org.zeniot.data.domain.device;

import lombok.Data;
import org.zeniot.data.base.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Data
public class DeviceCommonData extends DTO {
    private List<TransportTypeEnum> transportTypes;
    private List<DeviceStatusEnum> statuses;

    private DeviceCommonData() {
    }

    public static DeviceCommonData create() {
        DeviceCommonData deviceCommonData = new DeviceCommonData();
        deviceCommonData.setTransportTypes(List.of(TransportTypeEnum.values()));
        deviceCommonData.setStatuses(List.of(DeviceStatusEnum.values()));
        return deviceCommonData;
    }
}
