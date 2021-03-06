package com.falsework.core.client;

import io.grpc.CallOptions;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class ChannelManager implements ChannelLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManager.class);
    private final String name;
    private final ManagedChannel channel;
    private final Set<ChannelListener> listeners;
    private boolean running;

    ChannelManager(String name, ManagedChannel channel) {
        this.name = name;
        this.channel = channel;
        this.running = false;
        this.listeners = new LinkedHashSet<>();
    }

    @Override
    public void addListener(ChannelListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public Set<ChannelListener> findListeners() {
        return Collections.unmodifiableSet(this.listeners);
    }

    @Override
    public void removeListener(ChannelListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Channel:{} start", this.name);
        if (this.running) {
            throw new IllegalStateException("channel:" + this.name + " is running");
        } else if (this.channel.isShutdown() || this.channel.isTerminated()) {
            throw new IllegalStateException("channel:" + this.name + " shutdown or terminated");
        } else {
            this.running = true;
        }
        for (ChannelListener listener : this.listeners) {
            listener.beforeStart();
        }
        //do nothing
        for (ChannelListener listener : this.listeners) {
            listener.afterStart();
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Channel:{} close", this.name);
        if (!this.running) {
            throw new IllegalStateException("channel:" + this.name + " not running");
        } else {
            this.running = false;
        }
        for (ChannelListener listener : this.listeners) {
            listener.beforeStop();
        }
        this.channel.shutdown();

        for (ChannelListener listener : this.listeners) {
            listener.afterStop();
        }
    }

    /**
     * 新建存根
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier) {
        return supplier.apply(this.channel);
    }

    /**
     * 新建存根
     *
     * @param supplier
     * @param <T>
     * @return
     */
    public <T extends AbstractStub<T>> T newStub(Function<ManagedChannel, T> supplier, CallOptions callOptions, ClientInterceptor... interceptors) {
        T apply = supplier.apply(this.channel).withDeadline(callOptions.getDeadline())
                .withExecutor(callOptions.getExecutor())
                .withCompression(callOptions.getCompressor())
                .withCallCredentials(callOptions.getCredentials());
        if (callOptions.getMaxInboundMessageSize() != null) {
            apply = apply.withMaxInboundMessageSize(callOptions.getMaxInboundMessageSize());
        }
        if (callOptions.getMaxOutboundMessageSize() != null) {
            apply = apply.withMaxOutboundMessageSize(callOptions.getMaxOutboundMessageSize());
        }
        if (callOptions.isWaitForReady()) {
            apply = apply.withWaitForReady();
        }
        if (callOptions.isWaitForReady()) {
            apply = apply.withWaitForReady();
        }
        if (interceptors != null) {
            apply = apply.withInterceptors(interceptors);
        }
        return apply;
    }

    public String getName() {
        return name;
    }

    public ManagedChannel getChannel() {
        return channel;
    }
}
