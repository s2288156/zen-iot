package org.zeniot.ssl;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public class SslCredentialsConfig {

    private boolean enable;
    private SslCredentialsType type;
    private PemSslCredentials pem;

    private SslCredentials credentials;

    private final String name;
    private final boolean trustOnly;

    public SslCredentialsConfig(String name, boolean trustOnly) {
        this.name = name;
        this.trustOnly = trustOnly;
    }

    @PostConstruct
    public void init() {
        if (this.enable) {
            if (type == SslCredentialsType.PEM && pem.canUse()) {
                this.credentials = pem;
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
    }
}
