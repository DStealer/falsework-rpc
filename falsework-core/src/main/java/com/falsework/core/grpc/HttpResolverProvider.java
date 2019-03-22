package com.falsework.core.grpc;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;

@SuppressWarnings("all")
public class HttpResolverProvider extends NameResolverProvider {
    public static final HttpResolverProvider SINGLTON = new HttpResolverProvider();
    private static final String SCHEMA = "http";

    private HttpResolverProvider() {
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
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (SCHEMA.equals(targetUri.getScheme())) {
            return new NameResolver() {
                @Override
                public String getServiceAuthority() {
                    return targetUri.getAuthority();
                }

                @Override
                public void start(Listener listener) {
                    listener.onAddresses(Collections.singletonList(
                            new EquivalentAddressGroup(Collections.singletonList(new InetSocketAddress(targetUri.getHost(), targetUri.getPort())))),
                            params);
                }

                @Override
                public void shutdown() {

                }
            };
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }
}
