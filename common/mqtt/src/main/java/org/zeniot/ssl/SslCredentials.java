package org.zeniot.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * @author Wu.Chunyang
 */
public interface SslCredentials {
    void init(boolean trustsOnly) throws IOException, GeneralSecurityException;

    KeyStore getKeyStore();

    String getKeyPassword();

    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    X509Certificate[] getCertificateChain();

    X509Certificate[] getTrustedCertificates();

    TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException;

    KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException;
}
