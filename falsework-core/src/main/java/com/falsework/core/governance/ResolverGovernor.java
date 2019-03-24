package com.falsework.core.governance;

import com.falsework.governance.generated.InstanceInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务监听处理
 */
public class ResolverGovernor implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverGovernor.class);
    private final ConcurrentHashMap<String, NameResolver.Listener> listeners = new ConcurrentHashMap<>();
    private final DiscoveryClient client;

    ResolverGovernor(DiscoveryClient client) {
        Preconditions.checkNotNull(client);
        this.client = client;
    }

    /**
     * 注册
     *
     * @param serviceName
     * @param listener
     */
    public void register(String serviceName, NameResolver.Listener listener) {
        LOGGER.info("register dependency:{}", serviceName);
        if (this.listeners.containsKey(serviceName)) {
            throw new UnsupportedOperationException("duplicates key...");
        }
        this.listeners.putIfAbsent(serviceName, listener);
        this.client.registerDependency(serviceName);
    }

    /**
     * 注销
     *
     * @param serviceName
     */
    public void deregister(String serviceName) {
        LOGGER.info("deregister dependency:{}", serviceName);
        this.listeners.remove(serviceName);
        this.client.deregisterDependency(serviceName);
    }

    /**
     * 有实例变更
     *
     * @param serviceName
     * @param instanceInfos
     */
    public void onChange(String serviceName, List<InstanceInfo> instanceInfos) {
        LOGGER.info("service instances change:{}", serviceName);
        NameResolver.Listener listener = this.listeners.get(serviceName);
        if (listener != null) {
            List<EquivalentAddressGroup> groups = instanceInfos
                    .stream()
                    .map(e -> {
                        Attributes.Builder builder = Attributes.newBuilder();
                        for (Map.Entry<String, String> me : e.getAttributesMap().entrySet()) {
                            builder.set(Attributes.Key.create(me.getKey()), me.getValue());
                        }
                        return new EquivalentAddressGroup(Lists.newArrayList(new InetSocketAddress(e.getHostname(), e.getPort())),
                                builder.build());
                    }).collect(Collectors.toList());
            listener.onAddresses(groups, Attributes.EMPTY);
        }
    }

    /**
     * 更新在此注册 listener
     *
     * @param serviceName
     */
    public void refresh(String serviceName) {
        LOGGER.debug("refresh for:{}", serviceName);
        List<InstanceInfo> instanceInfos = this.client.getServiceInstance(serviceName);
        if (instanceInfos.size() > 0) {
            List<EquivalentAddressGroup> groups = instanceInfos
                    .stream()
                    .map(e -> {
                        Attributes.Builder builder = Attributes.newBuilder();
                        for (Map.Entry<String, String> me : e.getAttributesMap().entrySet()) {
                            builder.set(Attributes.Key.create(me.getKey()), me.getValue());
                        }
                        return new EquivalentAddressGroup(Lists.newArrayList(new InetSocketAddress(e.getHostname(), e.getPort())),
                                builder.build());
                    }).collect(Collectors.toList());
            NameResolver.Listener listener = this.listeners.get(serviceName);
            listener.onAddresses(groups, Attributes.EMPTY);
        } else {
            LOGGER.warn("refresh service:{} with 0 instance", serviceName);
        }
    }
}
