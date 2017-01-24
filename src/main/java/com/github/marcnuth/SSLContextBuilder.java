package com.github.marcnuth;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by marc on 17/1/23.
 */
public class SSLContextBuilder {

    private String _protocol;
    private String _keystoreType;
    private String _trustManagerAlgorithm;

    private String _fileTrustCert;
    private String _pwdTrustCert;

    private String _fileCert;
    private String _storePassCert;
    private String _keyPassCert;

    private boolean _needTrustedManager = false;

    public SSLContextBuilder() {}
    public SSLContext build() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_protocol), "Invalid protocol");

        KeyManager[] kms = _genKeyManagers();
        TrustManager[] tms = _genTrustManagersIfNeed();

        SSLContext sslContext = SSLContext.getInstance(_protocol);
        sslContext.init(kms, tms, null);

        return sslContext;
    }

    private KeyManager[] _genKeyManagers() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        Preconditions.checkArgument(!Strings.isNullOrEmpty(_keystoreType), "Invalid keystore type");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_trustManagerAlgorithm), "Invalid trust manager algorithm");

        Preconditions.checkArgument(!Strings.isNullOrEmpty(_fileCert), "Invalid server cert");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_storePassCert), "Invalid server cert storepass");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_keyPassCert), "Invalid server cert keypass");

        KeyStore keyStore = KeyStore.getInstance(_keystoreType);
        keyStore.load(new FileInputStream(_fileCert), _storePassCert.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(_trustManagerAlgorithm);
        keyManagerFactory.init(keyStore, _keyPassCert.toCharArray());

        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] _genTrustManagersIfNeed() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        if (!_needTrustedManager) {
            return null;
        }

        Preconditions.checkArgument(!Strings.isNullOrEmpty(_keystoreType), "Invalid keystore type");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_trustManagerAlgorithm), "Invalid trust manager algorithm");

        Preconditions.checkArgument(!Strings.isNullOrEmpty(_fileTrustCert), "Invalid trusted cert");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(_pwdTrustCert), "Invalid trusted cert password");

        KeyStore keyStore = KeyStore.getInstance(_keystoreType);
        keyStore.load(new FileInputStream(_fileTrustCert), _pwdTrustCert.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(_trustManagerAlgorithm);
        trustManagerFactory.init(keyStore);

        return trustManagerFactory.getTrustManagers();
    }

    public SSLContextBuilder withProtocol(String str) {
        _protocol = str;
        return this;
    }

    public SSLContextBuilder withKeystoreType(String type) {
        _keystoreType = type;
        return this;
    }

    public SSLContextBuilder withTrustManagerAlgorithm(String algo) {
        _trustManagerAlgorithm = algo;
        return this;
    }

    public SSLContextBuilder withTrustCert(String file, String pwd) {
        _fileTrustCert = file;
        _pwdTrustCert = pwd;
        return this;
    }

    public SSLContextBuilder withCert(String file, String storepass, String keypass) {
        _fileCert = file;
        _storePassCert = storepass;
        _keyPassCert = keypass;
        return this;
    }

    public SSLContextBuilder needTrusts(boolean flag) {
        _needTrustedManager = flag;
        return this;
    }


}
