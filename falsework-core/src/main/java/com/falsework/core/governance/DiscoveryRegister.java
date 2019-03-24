package com.falsework.core.governance;

import com.falsework.core.server.ServerRegister;

public class DiscoveryRegister implements ServerRegister {
    private final DiscoveryClient client;

    public DiscoveryRegister(DiscoveryClient client) {
        this.client = client;
    }

    @Override
    public void register() throws Exception {
        this.client.start();
    }

    @Override
    public void unregister() throws Exception {
        this.client.stop();
    }
}
