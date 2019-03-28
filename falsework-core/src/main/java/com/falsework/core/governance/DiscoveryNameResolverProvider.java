package com.falsework.core.governance;

import com.google.common.base.Preconditions;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

public class DiscoveryNameResolverProvider extends NameResolverProvider {
    private static final String SCHEMA = "dynamic";
    private final DiscoveryClient client;

    public DiscoveryNameResolverProvider(DiscoveryClient client) {
        Preconditions.checkNotNull(client);
        this.client = client;
    }

    @Override
    protected boolean isAvailable() {
        return this.client.isFetchRegistryEnable();
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Helper helper) {
        if (SCHEMA.equals(targetUri.getScheme())) {
            return new DiscoveryNameResolver(this.client.getGovernor(), targetUri, helper);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }
}
