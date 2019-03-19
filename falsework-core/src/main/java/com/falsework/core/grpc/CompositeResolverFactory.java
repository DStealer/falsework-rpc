package com.falsework.core.grpc;

import io.grpc.Attributes;
import io.grpc.NameResolver;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 联合命名解析工厂
 */
public class CompositeResolverFactory extends NameResolver.Factory {
    private final List<NameResolver.Factory> factories = new ArrayList<>();

    public CompositeResolverFactory addFactory(NameResolver.Factory factory) {
        this.factories.add(factory);
        return this;
    }

    public CompositeResolverFactory removeFactory(NameResolver.Factory factory) {
        this.factories.remove(factory);
        return this;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        return this.factories.stream()
                .filter(f -> Objects.equals(targetUri.getScheme(), f.getDefaultScheme()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No found scheme"))
                .newNameResolver(targetUri, params);
    }

    @Override
    public String getDefaultScheme() {
        return factories.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No found scheme"))
                .getDefaultScheme();
    }
}
