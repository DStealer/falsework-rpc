package com.falsework.core.client;

import com.falsework.core.common.Builder;
import com.falsework.core.grpc.CompositeResolverFactoryManager;
import com.falsework.core.grpc.LoadBalancerProviderManager;
import com.falsework.core.grpc.SharedExecutorManager;
import com.google.common.base.Preconditions;
import io.grpc.ClientInterceptor;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.netty.InternalNettyChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 通道builder
 */
public class ChannelManagerBuilder implements Builder<ChannelManager> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManagerBuilder.class);
    private String name;
    private Executor executor;
    private NameResolver.Factory factory;
    private List<ClientInterceptor> interceptorList = new LinkedList<>();
    private Set<ChannelLifecycleListener> listeners = new LinkedHashSet<>();
    private int messageSizeByte;
    private int metadataSizeByte;

    private ChannelManagerBuilder() {
    }

    public static ChannelManagerBuilder newBuilder() {
        return new ChannelManagerBuilder();
    }

    /**
     * 服务名称
     *
     * @param name
     */

    public ChannelManagerBuilder name(String name) {
        Preconditions.checkNotNull(name, "name");
        this.name = name;
        return this;
    }

    /**
     * 自定义处理线程池
     *
     * @param executor
     * @return
     */

    public ChannelManagerBuilder executor(Executor executor) {
        Preconditions.checkNotNull(executor, "executor invalid");
        this.executor = executor;
        return this;
    }

    /**
     * 自定义名称工厂
     *
     * @param factory
     * @return
     */
    public ChannelManagerBuilder nameFactory(NameResolver.Factory factory) {
        Preconditions.checkNotNull(factory, "factory invalid");
        this.factory = factory;
        return this;
    }

    /**
     * 添加客户端拦截器
     *
     * @param interceptor
     * @return
     */

    public ChannelManagerBuilder intercept(ClientInterceptor interceptor) {
        Preconditions.checkNotNull(interceptor, "interceptor invalid");
        this.interceptorList.add(interceptor);
        return this;
    }

    /**
     * 添加监听器
     *
     * @param listener
     * @return
     */

    public ChannelManagerBuilder listener(ChannelLifecycleListener listener) {
        Preconditions.checkNotNull(listener, "listener invalid");
        this.listeners.add(listener);
        return this;
    }

    /**
     * 请求消息大小
     *
     * @param messageSizeByte
     * @param metadataSizeByte
     * @return
     */

    public ChannelManagerBuilder maxInMessageSize(int messageSizeByte, int metadataSizeByte) {
        Preconditions.checkArgument(messageSizeByte < 1024 * 1024, "messageSizeByte too small");
        Preconditions.checkArgument(metadataSizeByte < 1024 * 1024, "metadataSizeByte too small");
        this.messageSizeByte = messageSizeByte;
        this.metadataSizeByte = metadataSizeByte;
        return this;
    }


    @Override
    public ChannelManager build() {
        NettyChannelBuilder builder = NettyChannelBuilder.forTarget(this.name);
        if (this.executor != null) {
            builder.executor(this.executor);
        } else {
            builder.executor(SharedExecutorManager.getShared());
        }
        if (this.factory != null) {
            builder.nameResolverFactory(factory);
        } else {
            builder.nameResolverFactory(CompositeResolverFactoryManager.getFactory());
        }
        for (ClientInterceptor interceptor : this.interceptorList) {
            builder.intercept(interceptor);
        }
        if (this.messageSizeByte > 0 && this.metadataSizeByte > 0) {
            builder.maxInboundMessageSize(this.messageSizeByte);
            builder.maxInboundMetadataSize(this.metadataSizeByte);
        }
        //内部配置
        builder.usePlaintext();

        builder.loadBalancerFactory(LoadBalancerProviderManager.get());

        InternalNettyChannelBuilder.setStatsRecordStartedRpcs(builder, false);

        ManagedChannel channel = builder.build();

        ChannelManager manager = new ChannelManager(this.name, channel);
        for (ChannelLifecycleListener listener : this.listeners) {
            manager.addLifecycleListener(listener);
        }
        return manager;
    }
}
