package com.falsework.core.grpc;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DirectUriResolver extends NameResolver {
    private URI targetUri;
    private Attributes params;
    private ConcurrentMap<URI, List<SocketAddress>> cache = new ConcurrentHashMap<>();

    public DirectUriResolver(URI targetUri, Attributes params) {
        this.targetUri = targetUri;
        this.params = params;
    }

    @Override
    public String getServiceAuthority() {
        return targetUri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        listener.onAddresses(Collections.singletonList(
                new EquivalentAddressGroup(Collections.singletonList(new InetSocketAddress(targetUri.getHost(), targetUri.getPort())))),
                Attributes.EMPTY);
    }

    @Override
    public void shutdown() {
        this.cache.clear();
    }
}
