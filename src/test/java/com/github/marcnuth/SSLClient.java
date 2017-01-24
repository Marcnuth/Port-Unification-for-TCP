package com.github.marcnuth;


import org.apache.log4j.Logger;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;

public class SSLClient extends ClientForTest {
    public SSLClient(String ip, int port, String prefix) {
        super(ip, port, prefix);
    }

    @Override
    public void run() {

        Logger.getRootLogger().info("Start to run ssl client...");
        SSLSocket socket = null;
        try {

            Logger.getRootLogger().info("Ready connecting to server...");
            socket = (SSLSocket) new SSLContextBuilder()
                    .withProtocol(ConfigsForTest.SSL_PROTOCOL)
                    .withKeystoreType(ConfigsForTest.KEYSTORE_TYPE)
                    .withTrustManagerAlgorithm(ConfigsForTest.TRUST_MANAGER_ALGORITHM)
                    .withCert(ConfigsForTest.FILE_CLIENT_JKS, ConfigsForTest.STOREPASS_CLIENT_CERT, ConfigsForTest.KEYPASS_CLIENT_CERT)
                    .needTrusts(true)
                    .withTrustCert(ConfigsForTest.FILE_CLIENT_TRUST_CERT, ConfigsForTest.PWD_CLIENT_TRUST_CERT)
                    .build()
                    .getSocketFactory()
                    .createSocket();

            socket.connect(new InetSocketAddress(_serverIp, _serverPort), 1000);
            Logger.getRootLogger().info(String.format("Succeed in connecting to server: %s:%s.", _serverIp, _serverPort));

            testCommunication(socket.getInputStream(), socket.getOutputStream());

            Logger.getRootLogger().info(String.format("Finish connecting to %s:%s and writing data", _serverIp, _serverPort));

        }
        catch (Throwable t) {
            Logger.getRootLogger().error("Failed to run client", t);
        }
        finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                }
                catch (Throwable ignore) {
                }
            }
        }
    }



}