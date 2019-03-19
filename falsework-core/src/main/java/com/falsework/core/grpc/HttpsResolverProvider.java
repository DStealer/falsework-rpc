package com.falsework.core.grpc;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

public class HttpsResolverProvider extends NameResolverProvider {


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
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        return new DirectUriResolver(targetUri, params);
    }

    @Override
    public String getDefaultScheme() {
        return "https";
    }
}
