package org.zeniot.ssl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Slf4j
public abstract class AbstractSslCredentials implements SslCredentials {

    private char[] keyPasswordArray;

    private KeyStore keyStore;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    private X509Certificate[] chain;

    private X509Certificate[] trusts;

    protected abstract boolean canUse();

    protected abstract KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray) throws IOException, GeneralSecurityException;

    protected abstract void updateKeyAlias(String keyAlias);

    @Override
    public void init(boolean trustsOnly) throws IOException, GeneralSecurityException {
        String keyPassword = getKeyPassword();
        if (StringUtils.isBlank(keyPassword)) {
            this.keyPasswordArray = new char[0];
        } else {
            this.keyPasswordArray = keyPassword.toCharArray();
        }
        this.keyStore = this.loadKeyStore(trustsOnly, this.keyPasswordArray);
        Set<X509Certificate> trustedCerts = getTrustedCerts();
        this.trusts = trustedCerts.toArray(new X509Certificate[0]);
        if (!trustsOnly) {
            PrivateKeyEntry privateKeyEntry = null;
            if (StringUtils.isNotBlank(this.getKeyAlias())) {
                privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, this.getKeyAlias(), this.keyPasswordArray);
            } else {
                for (Enumeration<String> e = this.keyStore.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, alias, this.keyPasswordArray);
                    if (privateKeyEntry != null) {
                        this.updateKeyAlias(alias);
                        break;
                    }
                }
            }
            if (privateKeyEntry == null) {
                throw new IllegalArgumentException("Failed to get private key from the keystore or pem files. " +
                        "Please check if the private key exists in the keystore or pem files and if the provided private key password is valid.");
            }
            this.chain = asX509Certificates(privateKeyEntry.getCertificateChain());
            this.privateKey = privateKeyEntry.getPrivateKey();
            if (this.chain.length > 0) {
                this.publicKey = this.chain[0].getPublicKey();
            }
        }
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
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(this.keyStore);
        return trustManagerFactory;
    }

    @Override
    public KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(this.keyStore, this.keyPasswordArray);
        return keyManagerFactory;
    }

    private Set<X509Certificate> getTrustedCerts() {
        Set<X509Certificate> set = new HashSet<>();
        try {
            for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                if (keyStore.isCertificateEntry(alias)) {
                    Certificate certificate = keyStore.getCertificate(alias);
                    if (certificate instanceof X509Certificate) {
                        set.add((X509Certificate) certificate);
                    }
                } else if (keyStore.isKeyEntry(alias)) {
                    Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                    if ((certificateChain != null) && (certificateChain.length > 0) && (certificateChain[0] instanceof X509Certificate)) {
                        set.add((X509Certificate) certificateChain[0]);
                    }
                }
            }
        } catch (KeyStoreException e) {
            log.warn("", e);
        }
        return set;
    }

    private static PrivateKeyEntry tryGetPrivateKeyEntry(KeyStore keyStore, String alias, char[] pwd) {
        PrivateKeyEntry privateKeyEntry = null;
        try {
            if (keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
                try {
                    privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(pwd));
                } catch (UnsupportedOperationException e) {
                    log.warn("", e);
                    PrivateKey key = (PrivateKey) keyStore.getKey(alias, pwd);
                    Certificate[] certs = keyStore.getCertificateChain(alias);
                    privateKeyEntry = new PrivateKeyEntry(key, certs);
                }
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException ignored) {
        }
        return privateKeyEntry;
    }

    private static X509Certificate[] asX509Certificates(Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            throw new IllegalArgumentException("certificates missing!");
        }
        X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            if (certificates[i] == null) {
                throw new IllegalArgumentException("[" + i + "] is null!");
            }
            x509Certificates[i] = (X509Certificate) certificates[i];
        }
        return x509Certificates;
    }
}
