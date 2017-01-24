package com.github.marcnuth;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.*;

/**
 * Created by marc on 17/1/23.
 */
public class TestNetty2 {
    @Test
    public void testMain() throws Exception {
        BasicConfigurator.configure();


        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        executor.execute(() -> {
            try {
                new TCPPortUnificationServer()
                        .withHost("127.0.0.1")
                        .withPort(8080)
                        .withSSLHandler(_prepareSSLHandler())
                        .withMessageHandler(_prepareMsgHandler())
                        .start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });

        executor.execute(new SSLClient());

        executor.awaitTermination(24, TimeUnit.DAYS);
        executor.shutdownNow();
    }

    private SimpleChannelInboundHandler<String> _prepareMsgHandler() {
        return new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                ctx.writeAndFlush(Unpooled.copiedBuffer("Ack", CharsetUtil.UTF_8));
            }
        };
    }

    private SslHandler _prepareSSLHandler() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        SSLContext localSSL = new SSLContextBuilder()
                .withProtocol(Configs.SSL_PROTOCOL)
                .withKeystoreType(Configs.KEYSTORE_TYPE)
                .withTrustManagerAlgorithm(Configs.TRUST_MANAGER_ALGORITHM)
                .withCert(Configs.FILE_SERVER_JKS, Configs.STOREPASS_SERVER_CERT, Configs.KEYPASS_SERVER_CERT)
                .needTrusts(true)
                .withTrustCert(Configs.FILE_SERVER_TRUST_CERT, Configs.PWD_SERVER_TRUST_CERT)
                .build();

        SSLEngine sslEngine = localSSL.createSSLEngine();

        sslEngine.setUseClientMode(false); //服务器端模式
        sslEngine.setNeedClientAuth(false); //不需要验证客户端

        return new SslHandler(sslEngine);
    }

    public static class Client implements Runnable {
        @Override
        public void run() {

            Logger.getRootLogger().info("run client...");

            Socket socket = null;
            try {

                Thread.sleep(5 * 1000);

                String ip = "127.0.0.1";
                int port = 8080;

                Logger.getRootLogger().info("Ready connecting to server...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 1000);

                Logger.getRootLogger().info(String.format("Succeed in connecting to server"));

                socket.getOutputStream().write("are you server?".getBytes());
                Logger.getRootLogger().info("Client sent message from server...");

                //Thread.sleep(24 * 60 * 60 * 1000);

                byte[] msg = new byte[1024];
                socket.getInputStream().read(msg);
                Logger.getRootLogger().info("From server:" + new String(msg));


                socket.getOutputStream().write("i am client".getBytes());

                Logger.getRootLogger().info(String.format("Finish connecting to %s:%s and writing data", ip, port));

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

    public static class SSLClient implements Runnable {
        @Override
        public void run() {

            Logger.getRootLogger().info("run client...");

            SSLSocket socket = null;
            try {

                Thread.sleep(5 * 1000);

                String ip = "127.0.0.1";
                int port = 8080;

                Logger.getRootLogger().info("Ready connecting to server...");
                SSLSocketFactory factory = new SSLContextBuilder()
                        .withProtocol(Configs.SSL_PROTOCOL)
                        .withKeystoreType(Configs.KEYSTORE_TYPE)
                        .withTrustManagerAlgorithm(Configs.TRUST_MANAGER_ALGORITHM)
                        .withCert(Configs.FILE_CLIENT_JKS, Configs.STOREPASS_CLIENT_CERT, Configs.KEYPASS_CLIENT_CERT)
                        .needTrusts(true)
                        .withTrustCert(Configs.FILE_CLIENT_TRUST_CERT, Configs.PWD_CLIENT_TRUST_CERT)
                        .build().getSocketFactory();

                socket = (SSLSocket) factory.createSocket();

                //.getServerSocketFactory()
                //.createServerSocket(0);
                socket.connect(new InetSocketAddress(ip, port), 1000);

                Logger.getRootLogger().info(String.format("Succeed in connecting to server"));

                socket.getOutputStream().write("are you server?".getBytes());
                Logger.getRootLogger().info("Client sent message from server...");

                //Thread.sleep(24 * 60 * 60 * 1000);

                byte[] msg = new byte[1024];
                socket.getInputStream().read(msg);
                Logger.getRootLogger().info("From server:" + new String(msg));


                socket.getOutputStream().write("i am client".getBytes());

                Logger.getRootLogger().info(String.format("Finish connecting to %s:%s and writing data", ip, port));

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
}

