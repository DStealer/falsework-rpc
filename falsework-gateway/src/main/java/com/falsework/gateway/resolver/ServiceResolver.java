package com.falsework.gateway.resolver;

import com.falsework.core.generated.governance.InstanceInfo;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;


import java.util.Optional;

public interface ServiceResolver {
    Optional<InstanceInfo> resolve(Http2Headers headers, Channel channel);
}
