package com.falsework.core.governance;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
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
        return this.client.isFetchRegistry();
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI uri, Attributes attributes) {
        if (SCHEMA.equals(uri.getScheme())) {
            return new DiscoveryNameResolver(this.client.getGovernor(), uri, attributes);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }
}
