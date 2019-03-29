package com.falsework.core.grpc;

import com.falsework.core.common.Holder;
import io.grpc.ClientInterceptor;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ChannelConfigurer {
    private final CompositeResolverFactory factory = new CompositeResolverFactory();
    private Holder<String> loadBalancerPolicy = new Holder<>();
    private Holder<Executor> defaultChannelExecutor = new Holder<>();

    private List<ClientInterceptor> defaultClientInterceptors = new ArrayList<>();

    ChannelConfigurer() {

    }

    public String getLoadBalancerPolicy() {
        return this.loadBalancerPolicy.isSet() ? this.loadBalancerPolicy.get() : "round_robin";
    }

    public void setLoadBalancerPolicy(String policy) {
        this.loadBalancerPolicy.setNonNull(policy);
    }

    public void addResolverFactory(NameResolver.Factory factory) {
        this.factory.addFactory(factory);
    }

    public CompositeResolverFactory getResolverFactory() {
        return factory;
    }

    public Executor getDefaultChannelExecutor() {
        if (this.defaultChannelExecutor.isSet()) {
            return this.defaultChannelExecutor.get();
        } else {
            return SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
        }
    }

    public void setDefaultChannelExecutor(Executor executor) {
        this.defaultChannelExecutor.setNonNull(executor);
    }

    public void addClientInterceptor(ClientInterceptor interceptor) {
        this.defaultClientInterceptors.add(interceptor);
    }

    public List<ClientInterceptor> getDefaultClientInterceptors() {
        return defaultClientInterceptors;
    }
}
