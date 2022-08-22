package org.zeniot.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.zeniot.mqtt.ssl.SslCredentialsConfig;

/**
 * @author Wu.Chunyang
 */
@Component
@ConditionalOnProperty(prefix = "transport.mqtt.ssl", value = "enabled", havingValue = "true")
public class MqttSslHandlerProvider {

    @Bean
    @ConfigurationProperties(prefix = "transport.mqtt.ssl.credentials")
    public SslCredentialsConfig mqttSslCredentials() {
        return new SslCredentialsConfig("MQTT SSL credentials", false);
    }
}
