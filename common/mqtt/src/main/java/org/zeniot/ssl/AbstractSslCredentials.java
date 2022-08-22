package org.zeniot.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * @author Wu.Chunyang
 */
public abstract class AbstractSslCredentials implements SslCredentials{

    private char[] keyPasswordArray;

    private KeyStore keyStore;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    private X509Certificate[] chain;

    private X509Certificate[] trusts;

    protected abstract boolean canUse();

    protected abstract KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray);

    protected abstract void updateKeyAlias(String keyAlias);

    @Override
    public void init(boolean trustsOnly) throws IOException, GeneralSecurityException {

    }

    @Override
    public KeyStore getKeyStore() {
        return this.keyStore;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        return this.chain;
    }

    @Override
    public X509Certificate[] getTrustedCertificates() {
        return this.trusts;
    }

    @Override
    public TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        return null;
    }

    @Override
    public KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        return null;
    }
}
