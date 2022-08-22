package org.zeniot.ssl;

import lombok.Data;

import java.security.KeyStore;

/**
 * @author Wu.Chunyang
 */
@Data
public class PemSslCredentials extends AbstractSslCredentials{

    private String certFile;
    private String keyFile;
    private String keyPassword;

    @Override
    protected boolean canUse() {
        return false;
    }

    @Override
    protected KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray) {
        return null;
    }

    @Override
    protected void updateKeyAlias(String keyAlias) {

    }
}
