package com.falsework.core.etcd;

import com.coreos.jetcd.Client;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Objects;

public class EtcdNameResolverProvider extends NameResolverProvider {


    private final Client client;

    public EtcdNameResolverProvider(Client client) {
        this.client = client;
    }

    @Override
    protected boolean isAvailable() {
        return Objects.nonNull(this.client);
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        return new EtcdNameResolver(client, targetUri, params);
    }

    @Override
    public String getDefaultScheme() {
        return "grpc";
    }
}
