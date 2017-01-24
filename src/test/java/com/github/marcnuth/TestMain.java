package com.github.marcnuth;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.junit.Test;
import java.util.concurrent.*;


/**
 * Created by marc on 17/1/23.
 */
public class TestMain {

    private final static String BIND_HOST = "127.0.0.1";
    private final static int BIND_PORT = 9999;

    private final static String SERVER_RESPONSE_PREFIX = "ACK:";
    private volatile boolean _existException = false;

    @Test
    public void testMain() throws Exception {
        BasicConfigurator.configure();

        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        executor.execute(() -> {
            try {
                new TCPPortUnificationServer()
                        .withHost(BIND_HOST)
                        .withPort(BIND_PORT)
                        .withSSLHandler(SSLHandlerBuilder.build())
                        .withMessageHandler(MessageHandlerBuilder.buildAckHandler(SERVER_RESPONSE_PREFIX))
                        .start();
            }
            catch (Exception e) {
                _existException = true;
                Logger.getRootLogger().error("Failed to start tcp port unification server", e);
            }
        });

        Thread.sleep(5 * 1000);

        executor.execute(new SSLClient(BIND_HOST, BIND_PORT, SERVER_RESPONSE_PREFIX));

        executor.awaitTermination(24, TimeUnit.DAYS);
        executor.shutdownNow();
    }

}

