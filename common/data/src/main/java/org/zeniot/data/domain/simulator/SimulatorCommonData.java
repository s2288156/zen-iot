package org.zeniot.data.domain.simulator;

import lombok.Data;
import org.zeniot.data.base.DTO;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Data
public class SimulatorCommonData extends DTO {
    private List<TransportTypeEnum> transportTypes;
    private List<DeviceStatusEnum> statuses;

    private SimulatorCommonData() {
    }

    public static SimulatorCommonData create() {
        SimulatorCommonData deviceCommonData = new SimulatorCommonData();
        deviceCommonData.setTransportTypes(List.of(TransportTypeEnum.values()));
        deviceCommonData.setStatuses(List.of(DeviceStatusEnum.values()));
        return deviceCommonData;
    }
}
