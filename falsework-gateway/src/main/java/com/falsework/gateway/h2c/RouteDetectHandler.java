package com.falsework.gateway.h2c;

import com.falsework.core.common.Holder;
import com.falsework.gateway.composite.ChannelUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RouteDetectHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDetectHandler.class);
    private static final ByteBuf HTTP_1_X_BUF = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{'H', 'T', 'T', 'P', '/', '1', '.'})).asReadOnly();
    private final ByteBuf prefaceBuf = Http2CodecUtil.connectionPrefaceBuf();
    private final Http2FrameReader frameReader = new DefaultHttp2FrameReader();
    private final Holder<Http2Headers> http2HeadersHolder = new Holder<>();
    private final Holder<Http2Settings> http2SettingsHolder = new Holder<>();
    private final ByteBuf bufData = Unpooled.buffer();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().config().isAutoRead()) {
            ctx.read();
        } else {
            LOGGER.error("channel must not be auto read");
            ChannelUtil.close(ctx);
        }
        this.bufData.writeBytes(this.prefaceBuf.duplicate());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        if (prefaceBuf.isReadable()) {
            if (byteBuf.readableBytes() < prefaceBuf.readableBytes()) {
                ctx.read();
                return;
            }
            if (ByteBufUtil.equals(byteBuf, byteBuf.readerIndex(), prefaceBuf, prefaceBuf.readerIndex(), 24)) {
                prefaceBuf.skipBytes(24);
                byteBuf.skipBytes(24);
                ChannelUtil.writeSettings(ctx, Http2Settings.defaultSettings(), ctx.newPromise());
            } else {
                int http1Index = ByteBufUtil.indexOf(HTTP_1_X_BUF, byteBuf.slice(byteBuf.readerIndex(), Math.min(byteBuf.readableBytes(), 1024)));
                if (http1Index != -1) {
                    String chunk = byteBuf.toString(byteBuf.readerIndex(), http1Index - byteBuf.readerIndex(), CharsetUtil.US_ASCII);
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Unexpected HTTP/1.x request: %s", chunk);
                }
                throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP/2 client preface string missing or corrupt.");
            }
        }

        this.bufData.writeBytes(byteBuf.duplicate());

        this.frameReader.readFrame(ctx, byteBuf, new Http2FrameAdapter() {
            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) throws Http2Exception {
                http2HeadersHolder.setNonNull(headers);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
                http2HeadersHolder.setNonNull(headers);
            }

            @Override
            public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
                http2SettingsHolder.setNonNull(settings);
                ChannelUtil.writeSettingsAck(ctx, ctx.newPromise()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });

        if (http2SettingsHolder.isSet() && http2HeadersHolder.isSet()) {
            LOGGER.info("head:{}-channel:{}", http2HeadersHolder.get(), ctx.channel().remoteAddress());
            this.frameReader.close();
            ctx.pipeline().remove(this);
            ctx.fireUserEventTriggered(new ConnectionInfoEvent("127.0.0.1", 9002, this.bufData));
        } else {
            ctx.read();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("exception caught", cause);
        ChannelUtil.close(ctx);
    }
}
