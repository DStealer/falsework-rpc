package com.falsework.gateway.h2c;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channel:{} active", ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channel:{} inactive", ctx.channel());
        super.channelInactive(ctx);
    }
}
