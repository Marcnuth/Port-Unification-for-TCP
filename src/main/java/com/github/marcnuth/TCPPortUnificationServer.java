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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;

/**
 * Serves TCP and TCP on SSL using the same port
 */
public final class TCPPortUnificationServer {

    private String _host;
    private Integer _port;
    private SslHandler _sslHandler;
    private ChannelInboundHandlerAdapter _msgHandler;

    private int _acceptorThreadsNum = 1;
    private int _processorThreadsNum = Math.max(Runtime.getRuntime().availableProcessors() * 2, 2);

    public TCPPortUnificationServer withHost(String host) {
        this._host = host;
        return this;
    }

    public TCPPortUnificationServer withPort(int port) {
        this._port = port;
        return this;
    }

    public TCPPortUnificationServer withSSLHandler(SslHandler handler) {
        this._sslHandler = handler;
        return this;
    }

    public TCPPortUnificationServer withMessageHandler(ChannelInboundHandlerAdapter handler) {
        this._msgHandler = handler;
        return this;
    }

    public TCPPortUnificationServer withAcceptorsNum(int cnt) {
        this._acceptorThreadsNum = cnt;
        return this;
    }

    public TCPPortUnificationServer withProcessorsNum(int cnt) {
        this._processorThreadsNum = cnt;
        return this;
    }

    public void start() throws Exception {

        Preconditions.checkArgument(!Strings.isNullOrEmpty(_host), "Invalid host");
        Preconditions.checkArgument(_port != null, "Invalid port");
        Preconditions.checkArgument(_sslHandler != null, "SSL Handler must be provided!");
        Preconditions.checkArgument(_msgHandler != null, "Message Handler must be provided!");

        EventLoopGroup acceptors = new NioEventLoopGroup(_acceptorThreadsNum);
        EventLoopGroup processors = new NioEventLoopGroup(_processorThreadsNum);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(acceptors, processors)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new TCPPortUnificationServerHandler(_sslHandler, _msgHandler));
                        }
                    });

            bootstrap.bind(_host, _port).sync().channel().closeFuture().sync();

        } finally {
            acceptors.shutdownGracefully();
            processors.shutdownGracefully();
        }
    }
}