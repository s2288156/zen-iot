package org.zeniot.mqtt.ssl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Wu.Chunyang
 */
@Data
@ConfigurationProperties(prefix = "transport.mqtt.ssl.credentials")
public class MqttSslCredentialsProperties {
    private SslCredentialsType type;
    private PemSslCredentials pem;
}
