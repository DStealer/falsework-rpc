package com.falsework.gateway.resolver;

import com.falsework.core.generated.governance.InstanceInfo;
import com.falsework.gateway.discovery.GateWayDiscoveryClient;
import com.falsework.gateway.module.ServiceInfo;

import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RandomServiceResolver implements ServiceResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomServiceResolver.class);
    private final GateWayDiscoveryClient gateWayDiscoveryClient;
    private final PathResolver pathResolver;

    public RandomServiceResolver(GateWayDiscoveryClient gateWayDiscoveryClient,
                                 PathResolver pathResolver) {
        this.gateWayDiscoveryClient = gateWayDiscoveryClient;
        this.pathResolver = pathResolver;
    }

    @Override
    public Optional<InstanceInfo> resolve(Http2Headers headers, Channel channel) {
        Optional<ServiceInfo> gs = this.pathResolver.resolve(headers.path().toString());
        if (!gs.isPresent()) {
            LOGGER.warn("can't find service for:{} from :{}", headers, channel);
            return Optional.empty();
        }
        List<InstanceInfo> infos = this.gateWayDiscoveryClient.getServiceInstance(gs.get().getGroup(),
                gs.get().getService());
        if (infos == null || infos.size() == 0) {
            LOGGER.warn("can't fetch service info for:{}", gs.get());
            return Optional.empty();
        }
        return Optional.of(infos.get(ThreadLocalRandom.current().nextInt(infos.size())));
    }
}
