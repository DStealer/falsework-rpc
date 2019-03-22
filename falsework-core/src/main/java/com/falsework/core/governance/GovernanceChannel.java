package com.falsework.core.governance;

import com.falsework.core.grpc.HttpResolverProvider;
import io.grpc.ManagedChannel;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

class GovernanceChannel {
    static final SharedResourceHolder.Resource<ManagedChannel> SHARED_HANDSHAKER_CHANNEL =
            new SharedResourceHolder.Resource<ManagedChannel>() {
                private EventLoopGroup eventGroup = null;

                @Override
                public ManagedChannel create() {
                    /* Use its own event loop thread pool to avoid blocking. */
                    if (eventGroup == null) {
                        eventGroup =
                                new NioEventLoopGroup(2, new DefaultThreadFactory("governance-pool", true));
                    }
                    return NettyChannelBuilder.forTarget("http://127.0.0.1:8080")
                            .nameResolverFactory(HttpResolverProvider.asFactory())
                            .directExecutor()
                            .eventLoopGroup(eventGroup)
                            .usePlaintext()
                            .build();
                }

                @Override
                @SuppressWarnings("FutureReturnValueIgnored") // netty ChannelFuture
                public void close(ManagedChannel instance) {
                    instance.shutdownNow();
                    if (eventGroup != null) {
                        eventGroup.shutdownGracefully();
                    }
                }

                @Override
                public String toString() {
                    return "falsework-governance-channel";
                }
            };
}