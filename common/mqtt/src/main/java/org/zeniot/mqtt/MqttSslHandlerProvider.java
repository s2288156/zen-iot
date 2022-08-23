package org.zeniot.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.zeniot.mqtt.ssl.MqttSslCredentialsProperties;
import org.zeniot.mqtt.ssl.SslCredentialsConfig;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport.mqtt.ssl", value = "enabled", havingValue = "true")
public class MqttSslHandlerProvider {

    @Autowired
    private MqttSslCredentialsProperties mqttSslCredentialsProperties;

    @Bean
    public SslCredentialsConfig mqttSslCredentials() {
        log.info("SslCredentialsConfig loaded!");
        return new SslCredentialsConfig("MQTT SSL credentials", false, mqttSslCredentialsProperties);
    }
}
