package com.falsework.governance.discovery;

import com.falsework.governance.governor.ResolverGovernor;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class FalseWorkNameResolver extends NameResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(FalseWorkNameResolver.class);
    private final ResolverGovernor resolverGovernor;
    private final URI uri;
    private final Attributes params;

    public FalseWorkNameResolver(ResolverGovernor resolverGovernor, URI uri, Attributes params) {
        this.resolverGovernor = resolverGovernor;
        this.uri = uri;
        this.params = params;
    }

    @Override
    public String getServiceAuthority() {
        return uri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        LOGGER.info("start name resolver for:{}", this.uri.toString());
        this.resolverGovernor.register(uri, listener);
    }

    @Override
    public void shutdown() {
        LOGGER.info("shutdown name resolver for:{}", this.uri.toString());
        this.resolverGovernor.deregister(uri);
    }

    @Override
    public void refresh() {

    }
}
