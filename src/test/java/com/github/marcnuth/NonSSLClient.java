package com.github.marcnuth;

import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NonSSLClient extends ClientForTest {
    public NonSSLClient(String ip, int port, String prefix) {
        super(ip, port, prefix);
    }

    @Override
    public void run() {

        Logger.getRootLogger().info("start to run non ssl client...");

        Socket socket = null;
        try {
            Logger.getRootLogger().info("Ready connecting to server...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(_serverIp, _serverPort), 1000);
            Logger.getRootLogger().info(String.format("Succeed in connecting to server, %s:%s", _serverIp, _serverPort));

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