package org.zeniot.mqtt.ssl;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Wu.Chunyang
 */
@Data
public class PemSslCredentials extends AbstractSslCredentials {
    private static final String DEFAULT_KEY_ALIAS = "server";
    private String certFile;
    private String keyFile;
    private String keyPassword;

    @Override
    protected boolean canUse() {
        try {
            return ResourceUtils.getFile(this.certFile).exists();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("pem file not exists!");
        }
    }

    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        List<X509Certificate> certificates = new ArrayList<>();
        PrivateKey privateKey = null;
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        try (InputStream inStream = new FileInputStream(ResourceUtils.getFile(this.certFile))) {
            try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                Object object;
                while ((object = pemParser.readObject()) != null) {
                    if (object instanceof X509CertificateHolder) {
                        X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                        certificates.add(x509Cert);
                    } else if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    } else if (object instanceof PEMKeyPair) {
                        privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                    } else if (object instanceof PrivateKeyInfo) {
                        privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            }
        }
        if (privateKey == null && StringUtils.isNotBlank(this.keyFile)) {
            if (ResourceUtils.getFile(this.keyFile).exists()) {
                try (InputStream inStream = new FileInputStream(ResourceUtils.getFile(this.keyFile))) {
                    try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                        Object object;
                        while ((object = pemParser.readObject()) != null) {
                            if (object instanceof PEMEncryptedKeyPair) {
                                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                                privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                                break;
                            } else if (object instanceof PEMKeyPair) {
                                privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                                break;
                            } else if (object instanceof PrivateKeyInfo) {
                                privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                            }
                        }
                    }
                }
            }
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in certFile: " + this.certFile);
        }
        if (privateKey == null && !trustsOnly) {
            throw new IllegalArgumentException("Unable to load private key neither from certFile: " + this.certFile + " nor from keyFile: " + this.keyFile);
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        if (trustsOnly) {
            List<Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
            for (int i = 0; i < unique.size(); i++) {
                keyStore.setCertificateEntry("root-" + i, unique.get(i));
            }
        }
        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(DEFAULT_KEY_ALIAS, privateKey, keyPasswordArray, x509Certificates);
        }
        return keyStore;
    }

    @Override
    protected void updateKeyAlias(String keyAlias) {

    }

    @Override
    public String getKeyAlias() {
        return DEFAULT_KEY_ALIAS;
    }
}
