package org.zeniot.data.domain.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.zeniot.data.enums.TransportTypeEnum;

import java.io.Serializable;

/**
 * @author Wu.Chunyang
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MqttTransportConfig.class, name = "MQTT")
})
public abstract class TransportConfig implements Serializable {
    private static final long serialVersionUID = 5258055912570433780L;

    @JsonIgnore
    abstract TransportTypeEnum getType();

}
