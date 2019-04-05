package com.falsework.gateway.mock.hex;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http2OrHttpHandler extends ChannelInitializer<SocketChannel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http2OrHttpHandler.class);


    private void configureHttp2(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new HexDumpProxyInitializer("127.0.0.1", 8002));
    }

    private void configureHttp1(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addLast(new HttpServerCodec(),
                new HttpObjectAggregator(10_0000),
                new FallbackRequestHandler());
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

    }
}
