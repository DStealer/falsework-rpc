package com.falsework.core.governance;

import com.falsework.governance.generated.DiscoveryServiceGrpc;
import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

public class FalseWorkNameResolverProvider extends NameResolverProvider {
    private static final String SCHEMA = "dynamic";
    private final ResolverGovernor resolverGovernor;

    public FalseWorkNameResolverProvider(DiscoveryServiceGrpc.DiscoveryServiceStub stub) {
        Preconditions.checkNotNull(stub);
        this.resolverGovernor = new ResolverGovernor(stub);
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI uri, Attributes attributes) {
        if (SCHEMA.equals(uri.getScheme())) {
            return new FalseWorkNameResolver(resolverGovernor, uri, attributes);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }
}
