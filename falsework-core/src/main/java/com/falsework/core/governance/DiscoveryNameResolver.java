package com.falsework.core.governance;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class DiscoveryNameResolver extends NameResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryNameResolver.class);
    private final ResolverGovernor resolverGovernor;
    private final URI uri;
    private final Attributes params;

    public DiscoveryNameResolver(ResolverGovernor resolverGovernor, URI uri, Attributes params) {
        this.resolverGovernor = resolverGovernor;
        this.uri = uri;
        this.params = params;
    }

    @Override
    public String getServiceAuthority() {
        return uri.getAuthority();
    }

    @Override
    public synchronized void start(Listener listener) {
        LOGGER.info("start name resolver for:{}", this.uri.toString());
        this.resolverGovernor.register(uri.getAuthority(), listener);
        this.resolverGovernor.refresh(uri.getAuthority());
    }

    @Override
    public synchronized void shutdown() {
        LOGGER.info("shutdown name resolver for:{}", this.uri.toString());
        this.resolverGovernor.deregister(uri.getAuthority());
    }

    @Override
    public synchronized void refresh() {
        this.resolverGovernor.refresh(uri.getAuthority());
    }
}
