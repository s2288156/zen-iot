package org.zen.iot.data.domain.device.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.zen.iot.data.enums.TransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes(
        @JsonSubTypes.Type(value = DeviceMqttTransportConfig.class, name = "MQTT")
)
public abstract class DeviceTransportConfig {

    @JsonIgnore
    abstract TransportTypeEnum getType();
}
