package com.github.marcnuth;

import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Created by marc on 17/1/24.
 */
public class SSLHandlerBuilder {

    public static SslHandler build() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        SSLContext localSSL = new SSLContextBuilder()
                .withProtocol(ConfigsForTest.SSL_PROTOCOL)
                .withKeystoreType(ConfigsForTest.KEYSTORE_TYPE)
                .withTrustManagerAlgorithm(ConfigsForTest.TRUST_MANAGER_ALGORITHM)
                .withCert(ConfigsForTest.FILE_SERVER_JKS, ConfigsForTest.STOREPASS_SERVER_CERT, ConfigsForTest.KEYPASS_SERVER_CERT)
                .needTrusts(true)
                .withTrustCert(ConfigsForTest.FILE_SERVER_TRUST_CERT, ConfigsForTest.PWD_SERVER_TRUST_CERT)
                .build();

        SSLEngine sslEngine = localSSL.createSSLEngine();

        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);

        return new SslHandler(sslEngine);
    }
}
