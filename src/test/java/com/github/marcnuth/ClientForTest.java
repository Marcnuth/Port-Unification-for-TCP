package com.github.marcnuth;


import com.google.common.io.BaseEncoding;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Random;

public abstract class ClientForTest implements Runnable {

    protected String _serverIp;
    protected int _serverPort;
    private String _serverResponsePrefix;

    public ClientForTest(String ip, int port, String prefix) {
        this._serverIp = ip;
        this._serverPort = port;
        this._serverResponsePrefix = prefix;
    }

    protected void testCommunication(InputStream inputStream, OutputStream outputStream) throws IOException {
        for (int i = 0; i < 10; ++i) {
            String toSend = _randomString();
            outputStream.write(toSend.getBytes());

            byte[] msg = new byte[1024];
            inputStream.read(msg);

            // notation: the netty transfer the data in stream, which make the received data maybe only part of the sent
            Logger.getRootLogger().info(String.format("%s<-->%s", toSend, new String(msg)));
        }
    }

    private final Random random = new Random();
    private String _randomString() {
        final byte[] buffer = new byte[20];
        random.nextBytes(buffer);
        return BaseEncoding.base64Url().omitPadding().encode(buffer);
    }

}