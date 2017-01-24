
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package com.github.marcnuth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class PortUnificationServerHandler extends ByteToMessageDecoder {

    private final SslContext sslCtx;
    private final boolean detectSsl;
    private final boolean detectGzip;

    public PortUnificationServerHandler(SslContext sslCtx) {
        this(sslCtx, true, true);
    }

    private PortUnificationServerHandler(SslContext sslCtx, boolean detectSsl, boolean detectGzip) {
        this.sslCtx = sslCtx;
        this.detectSsl = detectSsl;
        this.detectGzip = detectGzip;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Will use the first five bytes to detect a protocol.
        if (in.readableBytes() < 5) {
            return;
        }

        if (isSsl(in)) {
            Logger.getRootLogger().info("enter ssl");
            enableSsl(ctx);
        }
        else {

            final int magic1 = in.getUnsignedByte(in.readerIndex());
            final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);

            Logger.getRootLogger().info("enter non ssl" + magic1 + '_' + magic2);
            if (isGzip(magic1, magic2)) {
                Logger.getRootLogger().info("go to gzip");
                enableGzip(ctx);
            }
            else if (isHttp(magic1, magic2)) {
                Logger.getRootLogger().info("go to http");
                switchToHttp(ctx);
            }
            else if (isFactorial(magic1)) {
                Logger.getRootLogger().info("go to factorial");
                switchToFactorial(ctx);
            }
            else {
                Logger.getRootLogger().info("unknown!");
                switchToTcp(ctx);
                // Unknown protocol; discard everything and close the connection.
                //in.clear();
                //ctx.close();
            }
        }
    }

    private boolean isSsl(ByteBuf buf) {
        if (detectSsl) {
            return SslHandler.isEncrypted(buf);
        }
        return false;
    }

    private boolean isGzip(int magic1, int magic2) {
        if (detectGzip) {
            return magic1 == 31 && magic2 == 139;
        }
        return false;
    }

    private static boolean isHttp(int magic1, int magic2) {
        return
                magic1 == 'G' && magic2 == 'E' || // GET
                        magic1 == 'P' && magic2 == 'O' || // POST
                        magic1 == 'P' && magic2 == 'U' || // PUT
                        magic1 == 'H' && magic2 == 'E' || // HEAD
                        magic1 == 'O' && magic2 == 'P' || // OPTIONS
                        magic1 == 'P' && magic2 == 'A' || // PATCH
                        magic1 == 'D' && magic2 == 'E' || // DELETE
                        magic1 == 'T' && magic2 == 'R' || // TRACE
                        magic1 == 'C' && magic2 == 'O';   // CONNECT
    }

    private static boolean isFactorial(int magic1) {
        return magic1 == 'F';
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        try {
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

            p.addLast("ssl", new SslHandler(sslEngine));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //p.addLast("ssl", sslCtx.newHandler(ctx.alloc()));
        p.addLast("unificationA", new PortUnificationServerHandler(sslCtx, false, detectGzip));
        p.remove(this);
    }

    private void enableGzip(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        p.addLast("unificationB", new PortUnificationServerHandler(sslCtx, detectSsl, false));
        p.remove(this);
    }

    private void switchToHttp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("deflater", new HttpContentCompressor());
        p.addLast("handler", new HttpSnoopServerHandler());
        p.remove(this);
    }

    private void switchToFactorial(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("decoder", new BigIntegerDecoder());
        p.addLast("encoder", new NumberEncoder());
        p.addLast("handler", new FactorialServerHandler());
        p.remove(this);
    }

    private void switchToTcp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(StringDecoder.class.getName(), new StringDecoder(CharsetUtil.UTF_8));
        p.addLast("handler", new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                ctx.writeAndFlush(Unpooled.copiedBuffer("Ack", CharsetUtil.UTF_8));
            }
        });
        //p.addLast(new EchoServerHandler());
        p.remove(this);

    }

}
