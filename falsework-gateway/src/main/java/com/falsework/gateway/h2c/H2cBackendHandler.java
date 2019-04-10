package com.falsework.gateway.h2c;

import com.falsework.gateway.composite.ChannelUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2cBackendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(H2cBackendHandler.class);
    private final Channel inboundChannel;
    private final ByteBuf bufData;

    H2cBackendHandler(Channel inboundChannel, ByteBuf bufData) {
        this.inboundChannel = inboundChannel;
        this.bufData = bufData;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().config().isAutoRead()) {
            ctx.read();
        } else {
            LOGGER.error("channel must not be auto read");
            ChannelUtil.close(ctx);
        }
        ctx.writeAndFlush(this.bufData).addListener(
                (ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        this.inboundChannel.read();
                        ctx.read();
                    } else {
                        LOGGER.warn("write buf data to:{} failed", ctx.channel().remoteAddress());
                        ChannelUtil.close(future.channel());
                    }
                }
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtil.close(this.inboundChannel);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        this.inboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.read();
            } else {
                LOGGER.warn("write data to:{} failed", this.inboundChannel.remoteAddress());
                ChannelUtil.close(future.channel());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("channel:{} exception caught", ctx.channel(), cause);
        ChannelUtil.close(this.inboundChannel);
        ChannelUtil.close(ctx);
    }
}