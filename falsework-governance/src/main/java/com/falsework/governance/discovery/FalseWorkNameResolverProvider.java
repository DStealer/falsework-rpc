package com.falsework.governance.discovery;

import com.falsework.governance.governor.ResolverGovernor;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

public class FalseWorkNameResolverProvider extends NameResolverProvider {
    private final ResolverGovernor resolverGovernor = new ResolverGovernor();

    @Override
    protected boolean isAvailable() {
        return false;
    }

    @Override
    protected int priority() {
        return 10;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI uri, Attributes attributes) {
        return null;
    }

    @Override
    public String getDefaultScheme() {
        return "dynamic";
    }
}
