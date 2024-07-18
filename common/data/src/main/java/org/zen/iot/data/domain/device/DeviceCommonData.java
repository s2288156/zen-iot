package org.zen.iot.data.domain.device;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zen.iot.data.base.DTO;
import org.zen.iot.data.enums.DeviceCredentialTypeEnum;
import org.zen.iot.data.enums.DeviceStatusEnum;
import org.zen.iot.data.enums.FieldTypeEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceCommonData extends DTO {
    private List<TransportTypeEnum> transportTypes;
    private List<DeviceStatusEnum> statuses;
    private List<TimeUnit> timeUnit;
    private List<DeviceCredentialTypeEnum> credentialTypes;
    private List<FieldTypeEnum> fieldTypes;

    private DeviceCommonData() {
    }

    public static DeviceCommonData create() {
        DeviceCommonData deviceCommonData = new DeviceCommonData();
        deviceCommonData.setTransportTypes(List.of(TransportTypeEnum.MQTT));
        deviceCommonData.setStatuses(List.of(DeviceStatusEnum.values()));
        deviceCommonData.setTimeUnit(List.of(TimeUnit.MILLISECONDS, TimeUnit.SECONDS));
        deviceCommonData.setCredentialTypes(List.of(DeviceCredentialTypeEnum.ACCESS_TOKEN, DeviceCredentialTypeEnum.MQTT_BASE));
        deviceCommonData.setFieldTypes(List.of(FieldTypeEnum.values()));
        return deviceCommonData;
    }
}
