package org.zen.iot.data.domain.simulator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zen.iot.data.base.DTO;
import org.zen.iot.data.enums.DeviceStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
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
