
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import java.util.List;


/**
 * Manipulates the current pipeline dynamically to switch protocols
 */
public class TCPPortUnificationServerHandler extends ByteToMessageDecoder {

    private final SslHandler _sslHandler;
    private final ChannelInboundHandlerAdapter _msgHandler;

    public TCPPortUnificationServerHandler(SslHandler sslHandler, ChannelInboundHandlerAdapter msgHandler) {
        this._sslHandler = sslHandler;
        this._msgHandler = msgHandler;
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
            Logger.getRootLogger().info("unknown!");
            switchToTcp(ctx);
        }
    }

    private boolean isSsl(ByteBuf buf) {
        return SslHandler.isEncrypted(buf);
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();

        p.addLast("ssl", _sslHandler);
        p.addLast("unification", new TCPPortUnificationServerHandler(_sslHandler, _msgHandler));
        p.remove(this);
    }

    private void switchToTcp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(StringDecoder.class.getName(), new StringDecoder(CharsetUtil.UTF_8));
        p.addLast("handler", _msgHandler);
        p.remove(this);
    }
}
