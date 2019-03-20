package com.falsework.governance.governor;

import io.grpc.NameResolver;

import java.net.URI;
import java.util.EventListener;
import java.util.EventObject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务监听处理
 */
public class ResolverGovernor implements EventListener {
    private final ConcurrentHashMap<URI, NameResolver.Listener> listenerConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * 注册
     *
     * @param uri
     * @param listener
     */
    public void register(URI uri, NameResolver.Listener listener) {
        this.listenerConcurrentHashMap.putIfAbsent(uri, listener);
    }

    /**
     * 注销
     *
     * @param uri
     */
    public void deregister(URI uri) {
        this.listenerConcurrentHashMap.remove(uri);
    }

    /**
     * 事件发生
     *
     * @param object
     */
    public void onEvent(EventObject object) {

    }
}
