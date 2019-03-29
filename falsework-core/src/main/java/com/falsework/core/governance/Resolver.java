package com.falsework.core.governance;

import io.grpc.NameResolver;

public class Resolver {
    private final NameResolver.Helper helper;
    private final NameResolver.Listener listener;

    public Resolver(NameResolver.Helper helper, NameResolver.Listener listener) {
        this.helper = helper;
        this.listener = listener;
    }

    public NameResolver.Helper getHelper() {
        return helper;
    }

    public NameResolver.Listener getListener() {
        return listener;
    }
}
