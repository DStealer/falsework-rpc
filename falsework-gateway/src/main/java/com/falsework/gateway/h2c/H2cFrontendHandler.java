package com.falsework.gateway.h2c;

import com.falsework.gateway.composite.ChannelUtil;

import io.grpc.netty.shaded.io.netty.bootstrap.Bootstrap;
import io.grpc.netty.shaded.io.netty.channel.*;
import io.grpc.netty.shaded.io.netty.channel.socket.SocketChannel;
import io.grpc.netty.shaded.io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2cFrontendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(H2cFrontendHandler.class);

    private Channel outboundChannel;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtil.close(this.outboundChannel);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        this.outboundChannel.writeAndFlush(msg)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ctx.channel().read();
                    } else {
                        LOGGER.warn("write data to:{} failed", this.outboundChannel.remoteAddress());
                        ChannelUtil.close(future.channel());
                    }
                });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionInfoEvent) {
            ConnectionInfoEvent event = (ConnectionInfoEvent) evt;
            Bootstrap b = new Bootstrap();
            b.group(ctx.pipeline().channel().eventLoop())
                    .channel(ctx.channel().getClass())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new H2cBackendHandler(ctx.pipeline().channel(), event.getBufData()));
                        }
                    })
                    .option(ChannelOption.AUTO_READ, false)
                    .option(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = b.connect(event.getRemoteHost(), event.getRemotePort());
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("connect backend {}:{} failed", event.getRemoteHost(), event.getRemotePort());
                    ChannelUtil.close(future.channel());
                }
            });
            this.outboundChannel = channelFuture.channel();
        }

        if (evt instanceof IdleStateEvent) {
            LOGGER.info("channel:{} idle timeout", ctx.channel());
            ChannelUtil.close(this.outboundChannel);
            ChannelUtil.close(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("channel:{} exception caught", ctx.channel(), cause);
        ChannelUtil.close(this.outboundChannel);
        ChannelUtil.close(ctx);
    }
}