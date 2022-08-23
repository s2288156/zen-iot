package org.zeniot.mqtt.ssl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * @author Wu.Chunyang
 */
@Data
@Slf4j
public class SslCredentialsConfig {

    private boolean enabled = true;
    private SslCredentials credentials;
    private final String name;
    private final boolean trustOnly;
    private MqttSslCredentialsProperties mqttSslCredentialsProperties;

    public SslCredentialsConfig(String name, boolean trustOnly, MqttSslCredentialsProperties mqttSslCredentialsProperties) {
        this.name = name;
        this.trustOnly = trustOnly;
        this.mqttSslCredentialsProperties = mqttSslCredentialsProperties;
    }

    @PostConstruct
    public void init() {
        if (this.enabled) {
            if (mqttSslCredentialsProperties.getType() == SslCredentialsType.PEM
                    && mqttSslCredentialsProperties.getPem().canUse()) {
                this.credentials = mqttSslCredentialsProperties.getPem();
            } else {
                throw new RuntimeException(name + ": Invalid SSL credentials configuration.");
            }
            try {
                this.credentials.init(trustOnly);
            } catch (Exception e) {
                throw new RuntimeException(name + ": Failed to init SSL credentials configuration.", e);
            }
        } else {
            log.info("{}: Skipping initialization of disabled SSL credentials.", name);
        }
        log.info("inited mqtt ssl");
    }
}
