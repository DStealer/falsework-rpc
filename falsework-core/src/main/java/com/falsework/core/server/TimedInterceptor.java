package com.falsework.core.server;

import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import io.grpc.*;
import io.grpc.netty.shaded.io.netty.channel.local.LocalAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("all")
public class TimedInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimedInterceptor.class);

    private TimedInterceptor() {
    }

    public static TimedInterceptor getInstance() {
        return new TimedInterceptor();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
            private final Stopwatch stopwatch = Stopwatch.createStarted();

            @Override
            public void onComplete() {
                delegate().onComplete();
                SocketAddress socketAddress;
                socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                String ip;
                if (socketAddress instanceof InetSocketAddress) {
                    ip = InetAddresses.toAddrString(((InetSocketAddress) socketAddress).getAddress());
                } else if (socketAddress instanceof LocalAddress) {
                    ip = ((LocalAddress) socketAddress).id();
                } else {
                    ip = String.valueOf(socketAddress);
                }
                LOGGER.info("Handle {} from {} use {} ms", call.getMethodDescriptor().getFullMethodName(), ip, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            }
        };
    }
}
