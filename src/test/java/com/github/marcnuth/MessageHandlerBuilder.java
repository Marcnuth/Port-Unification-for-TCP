package com.github.marcnuth;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Created by marc on 17/1/24.
 */
public class MessageHandlerBuilder {

    public static SimpleChannelInboundHandler<String> buildAckHandler(String ackPrefix) {
        return new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                ctx.writeAndFlush(Unpooled.copiedBuffer(ackPrefix + msg, CharsetUtil.UTF_8));
            }
        };
    }
}
