package com.falsework.gateway.composite;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameTypes;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.internal.ObjectUtil;

public class ChannelUtil {
    public static ChannelFuture writeSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise) {
        try {
            ObjectUtil.checkNotNull(settings, "settings");
            int payloadLength = Http2CodecUtil.SETTING_ENTRY_LENGTH * settings.size();
            ByteBuf buf = ctx.alloc().buffer(Http2CodecUtil.FRAME_HEADER_LENGTH + payloadLength);
            writeFrameHeader(buf, payloadLength, Http2FrameTypes.SETTINGS, new Http2Flags(), 0);
            for (Http2Settings.PrimitiveEntry<Long> entry : settings.entries()) {
                buf.writeChar(entry.key());
                buf.writeInt(entry.value().intValue());
            }
            return ctx.writeAndFlush(buf, promise);
        } catch (Throwable throwable) {
            return promise.setFailure(throwable);
        }
    }

    private static void writeFrameHeader(ByteBuf out, int payloadLength, byte type, Http2Flags flags, int streamId) {
        out.writeMedium(payloadLength);
        out.writeByte(type);
        out.writeByte(flags.value());
        out.writeInt(streamId);
    }

    public static ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise) {
        try {
            ByteBuf buf = ctx.alloc().buffer(Http2CodecUtil.FRAME_HEADER_LENGTH);
            writeFrameHeader(buf, 0, Http2FrameTypes.SETTINGS, new Http2Flags().ack(true), 0);
            return ctx.writeAndFlush(buf, promise);
        } catch (Throwable t) {
            return promise.setFailure(t);
        }
    }

    public static void close(ChannelHandlerContext ctx) {
        if (ctx != null && ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static void close(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static ChannelFuture sendPrefaceAndSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise) {
        try {
            ObjectUtil.checkNotNull(settings, "settings");
            int payloadLength = Http2CodecUtil.SETTING_ENTRY_LENGTH * settings.size();
            ByteBuf buf = ctx.alloc().buffer(Http2CodecUtil.FRAME_HEADER_LENGTH + payloadLength + 24);
            buf.writeBytes(Http2CodecUtil.connectionPrefaceBuf());
            writeFrameHeader(buf, payloadLength, Http2FrameTypes.SETTINGS, new Http2Flags(), 0);
            for (Http2Settings.PrimitiveEntry<Long> entry : settings.entries()) {
                buf.writeChar(entry.key());
                buf.writeInt(entry.value().intValue());
            }
            return ctx.writeAndFlush(buf, promise);
        } catch (Throwable throwable) {
            return promise.setFailure(throwable);
        }
    }
}
