package com.github.marcnuth;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import javax.net.ssl.SSLServerSocket;

@Ignore
public class TestSSLTCPConnection {

    private final static int _THREADS_NUM = 2;
    private final static int _TIMEOUT_IN_MS = 3 * 1000;
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

        //Logger.getRootLogger().info();

        ExecutorService executor = new ThreadPoolExecutor(_THREADS_NUM, _THREADS_NUM, 0 , TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        executor.execute(new Server());
        executor.execute(new Client());

        executor.awaitTermination(_TIMEOUT_IN_MS, TimeUnit.DAYS);
        //executor.shutdownNow();
    }



    public static class Server implements Runnable {
        @Override
        public void run() {
            SSLServerSocket serverSocket = null;
            try {

                serverSocket = (SSLServerSocket) new SSLContextBuilder()
                        .withProtocol(Configs.SSL_PROTOCOL)
                        .withKeystoreType(Configs.KEYSTORE_TYPE)
                        .withTrustManagerAlgorithm(Configs.TRUST_MANAGER_ALGORITHM)
                        .withCert(Configs.FILE_SERVER_JKS, Configs.STOREPASS_SERVER_CERT, Configs.KEYPASS_SERVER_CERT)
                        .needTrusts(true)
                        .withTrustCert(Configs.FILE_SERVER_TRUST_CERT, Configs.PWD_SERVER_TRUST_CERT)
                        .build()
                        .getServerSocketFactory()
                        .createServerSocket(0);

                serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
                serverSocket.setUseClientMode(true);

                serverSocket.setNeedClientAuth(false);
                //serverSocket.setWantClientAuth(true);

                registry.put(RegisterKey.SERVER_IP, serverSocket.getLocalSocketAddress());
                registry.put(RegisterKey.SERVER_PORT, serverSocket.getLocalPort());
                Logger.getRootLogger().info(String.format("Server is start, port=%s", serverSocket.getLocalPort()));

                Socket client = serverSocket.accept();
                Logger.getRootLogger().info(String.format("Client is accepted %s:%s", client.getInetAddress().getHostAddress(), client.getLocalPort()));


                byte[] resp = new byte[_MAX_READ_BYTES];
                client.getInputStream().read(resp);
                Logger.getRootLogger().info("From client: " + new String(resp));

                client.getOutputStream().write(_DATA_TO_CLIENT.getBytes());
                Logger.getRootLogger().info("Server has sent message to client...");

                resp = new byte[_MAX_READ_BYTES];
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
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip.getHostName(), port.intValue()), 1000);

                Logger.getRootLogger().info(String.format("Succeed in connecting to %s:%s", ip.getHostString(), port.intValue()));

                socket.getOutputStream().write(_DATA_TO_SERVER.getBytes());
                Logger.getRootLogger().info("Client sent message from server...");

                Thread.sleep(24 * 60 * 60 * 1000);

                byte[] msg = new byte[_MAX_READ_BYTES];
                socket.getInputStream().read(msg);
                Logger.getRootLogger().info(new String(msg));
                Assert.assertTrue(_DATA_TO_CLIENT.equals(new String(msg).replace("\0", "")));
                Logger.getRootLogger().info("Client received message from server...");

                socket.getOutputStream().write(_DATA_TO_SERVER.getBytes());

                Logger.getRootLogger().info(String.format("Finish connecting to %s:%s and writing data", ip.getHostString(), port.intValue()));

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
