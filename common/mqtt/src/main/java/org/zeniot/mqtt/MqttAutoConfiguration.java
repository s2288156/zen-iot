package org.zeniot.mqtt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.zeniot.mqtt.ssl.MqttSslCredentialsProperties;

/**
 * @author Wu.Chunyang
 */
@EnableConfigurationProperties({MqttSslCredentialsProperties.class})
public class MqttAutoConfiguration {
}
