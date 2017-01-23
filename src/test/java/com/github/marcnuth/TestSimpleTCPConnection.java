package com.github.marcnuth;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

public class TestSimpleTCPConnection {

    private final static int _THREADS_NUM = 2;
    private final static long _TIMEOUT_IN_MS = 3 * 1000;
    private final static int _MAX_READ_BYTES = 1024;
    private final static String _DATA_TO_CLIENT = "DATA_TO_CLIENT";
    private final static String _DATA_TO_SERVER = "DATA TO SERVER";

    private final static ConcurrentHashMap<RegisterKey, Object> registry = new ConcurrentHashMap<>();
    private enum RegisterKey {
        SERVER_PORT, SERVER_IP
    }

    @Test
    public void testNormalCase() throws Exception {

        BasicConfigurator.configure();

        ExecutorService executor = new ThreadPoolExecutor(_THREADS_NUM, _THREADS_NUM, 0 , TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        executor.execute(new Server());
        executor.execute(new Client());

        executor.awaitTermination(_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
        executor.shutdownNow();
    }

    public static class Server implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(0);
                registry.put(RegisterKey.SERVER_IP, serverSocket.getLocalSocketAddress());
                registry.put(RegisterKey.SERVER_PORT, serverSocket.getLocalPort());
                Logger.getRootLogger().info(String.format("Server is start, port=%s", serverSocket.getLocalPort()));

                Socket client = serverSocket.accept();
                client.getOutputStream().write(_DATA_TO_CLIENT.getBytes());

                byte[] resp = new byte[_MAX_READ_BYTES];
                client.getInputStream().read(resp);
                Assert.assertEquals(_DATA_TO_SERVER, new String(resp).replace("\0", ""));

                Logger.getRootLogger().info("Finish serving");

            }
            catch (Throwable t) {
                Logger.getRootLogger().error("Failed to run server", t);
            }
            finally {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    }
                    catch (Throwable ignore) {}
                }
            }
        }
    }

    public static class Client implements Runnable {
        @Override
        public void run() {
            Socket socket = null;
            try {
                InetSocketAddress ip = (InetSocketAddress) registry.get(RegisterKey.SERVER_IP);
                Integer port = (Integer) registry.get(RegisterKey.SERVER_PORT);

                while (ip == null || port == null) {
                    ip = (InetSocketAddress) registry.get(RegisterKey.SERVER_IP);
                    port = (Integer) registry.get(RegisterKey.SERVER_PORT);
                }

                Logger.getRootLogger().info(String.format("Ready connecting to %s:%s", ip.getHostString(), port.intValue()));
                socket = new Socket(ip.getHostName(), port.intValue());

                byte[] msg = new byte[_MAX_READ_BYTES];
                socket.getInputStream().read(msg);
                Assert.assertTrue(_DATA_TO_CLIENT.equals(new String(msg).replace("\0", "")));

                socket.getOutputStream().write(_DATA_TO_SERVER.getBytes());

            }
            catch (Throwable t) {
                Logger.getRootLogger().error("Failed to run client", t);
            }
            finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    }
                    catch (Throwable ignore) {}
                }
            }
        }
    }
}
